import Foundation

protocol StatsHTTPTransport {
    func load(_ request: URLRequest, completion: @escaping (Result<(Data, HTTPURLResponse), Error>) -> Void)
}

struct URLSessionStatsHTTPTransport: StatsHTTPTransport {
    func load(_ request: URLRequest, completion: @escaping (Result<(Data, HTTPURLResponse), Error>) -> Void) {
        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error {
                completion(.failure(error))
                return
            }
            guard let data, let response = response as? HTTPURLResponse else {
                completion(.failure(StatsProviderLookupError.invalidResponse))
                return
            }
            completion(.success((data, response)))
        }.resume()
    }
}

enum StatsProviderLookupError: Error {
    case invalidResponse
}

/// Companion-only provider client. It sends only real player UUIDs to fixed HTTPS endpoints,
/// caches one normalized result per match, and drops every raw response before crossing the bridge.
final class StatsProviderLookup {
    private static let responseTimeout: TimeInterval = 4
    private static let maximumCachedMatches = 4

    private let keychainStore: StatsProviderKeyReading
    private let transport: StatsHTTPTransport
    private let hypixelCache: HypixelStatsCache
    private let queue = DispatchQueue(label: "com.snkisk.hypixellegitils.stats-provider")
    private var matchCache: [String: StatsBridgeRosterResponse] = [:]
    private var cacheOrder: [String] = []

    init(
        keychainStore: StatsProviderKeyReading,
        transport: StatsHTTPTransport = URLSessionStatsHTTPTransport(),
        hypixelCache: HypixelStatsCache = HypixelStatsCache()
    ) {
        self.keychainStore = keychainStore
        self.transport = transport
        self.hypixelCache = hypixelCache
    }

    func lookup(_ roster: StatsBridgeRosterRequest, completion: @escaping (StatsBridgeRosterResponse) -> Void) {
        queue.async { [weak self] in
            guard let self else { return }
            if let cached = self.matchCache[roster.matchID] {
                completion(cached)
                return
            }
            self.fetch(roster, completion: completion)
        }
    }

    private func fetch(_ roster: StatsBridgeRosterRequest, completion: @escaping (StatsBridgeRosterResponse) -> Void) {
        let records = Dictionary(uniqueKeysWithValues: roster.players.map { player in
            (player.name.lowercased(), MutablePlayer(name: player.name, uuid: player.uuid))
        })
        var knownByName = Dictionary(uniqueKeysWithValues: roster.players.compactMap { player -> (String, StatsBridgeRosterMember)? in
            guard let uuid = player.uuid else { return nil }
            return (player.name.lowercased(), StatsBridgeRosterMember(name: player.name, uuid: uuid.lowercased()))
        })
        let resolveGroup = DispatchGroup()
        for player in roster.players where player.uuid == nil {
            resolveGroup.enter()
            transport.load(Self.mojangProfileRequest(name: player.name)) { result in
                self.queue.async {
                    defer { resolveGroup.leave() }
                    guard case let .success((data, response)) = result, response.statusCode == 200,
                          let uuid = Self.parseMojangProfileUUID(data) else { return }
                    let resolved = StatsBridgeRosterMember(name: player.name, uuid: uuid)
                    guard resolved.isValid else { return }
                    knownByName[player.name.lowercased()] = resolved
                    records[player.name.lowercased()]?.resolve(uuid: uuid)
                }
            }
        }
        resolveGroup.notify(queue: queue) {
            self.fetchProviderData(
                roster,
                records: records,
                known: Array(knownByName.values),
                completion: completion
            )
        }
    }

    /// A visible pregame chatter can be queried only when their current chat name resolves to a real profile.
    /// A failed lookup is deliberately treated as unavailable, not as a recovered Nick identity.
    private func fetchProviderData(
        _ roster: StatsBridgeRosterRequest,
        records: [String: MutablePlayer],
        known: [StatsBridgeRosterMember],
        completion: @escaping (StatsBridgeRosterResponse) -> Void
    ) {
        guard !known.isEmpty else {
            finish(matchID: roster.matchID, records: records, completion: completion)
            return
        }

        let group = DispatchGroup()
        let deadline = DispatchTime.now() + Self.responseTimeout

        if let key = try? keychainStore.readSecret(account: StatsProvider.hypixel.keychainAccount), !key.isEmpty {
            for player in known {
                if let cached = hypixelCache.stats(for: player.uuid!) {
                    records[player.name.lowercased()]?.apply(cached)
                    continue
                }
                group.enter()
                transport.load(Self.hypixelRequest(uuid: player.uuid!, apiKey: key)) { result in
                    self.queue.async {
                        defer { group.leave() }
                        guard case let .success((data, response)) = result, response.statusCode == 200,
                              let stats = Self.parseHypixelStats(data, gameMode: roster.gameMode) else { return }
                        records[player.name.lowercased()]?.apply(stats)
                        self.hypixelCache.store(stats, for: player.uuid!)
                    }
                }
            }
        }

        if let key = try? keychainStore.readSecret(account: StatsProvider.urchin.keychainAccount), !key.isEmpty {
            group.enter()
            transport.load(Self.urchinRequest(uuids: known.compactMap(\.uuid), apiKey: key)) { result in
                self.queue.async {
                    defer { group.leave() }
                    guard case let .success((data, response)) = result, response.statusCode == 200 else { return }
                    for (uuid, labels) in Self.parseUrchinTags(data) {
                        guard let member = known.first(where: { $0.uuid?.caseInsensitiveCompare(uuid) == .orderedSame }) else { continue }
                        records[member.name.lowercased()]?.communityTags.append(contentsOf: labels.map {
                            StatsBridgeCommunityTag(source: StatsProvider.urchin.rawValue, label: $0)
                        })
                    }
                }
            }
        }

        if let key = try? keychainStore.readSecret(account: StatsProvider.seraph.keychainAccount), !key.isEmpty {
            for player in known {
                group.enter()
                transport.load(Self.seraphRequest(uuid: player.uuid!, apiKey: key)) { result in
                    self.queue.async {
                        defer { group.leave() }
                        guard case let .success((data, response)) = result, response.statusCode == 200 else { return }
                        records[player.name.lowercased()]?.communityTags.append(contentsOf: Self.parseSeraphTags(data).map {
                            StatsBridgeCommunityTag(source: StatsProvider.seraph.rawValue, label: $0)
                        })
                    }
                }
            }
        }

        DispatchQueue.global(qos: .utility).async {
            if group.wait(timeout: deadline) == .timedOut {
                // Timed-out work may still finish later; this match has already received its single response.
            }
            self.queue.async {
                self.finish(matchID: roster.matchID, records: records, completion: completion)
            }
        }
    }

    private func finish(
        matchID: String,
        records: [String: MutablePlayer],
        completion: @escaping (StatsBridgeRosterResponse) -> Void
    ) {
        let players = records.values
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
            .map { $0.result }
        let response = StatsBridgeRosterResponse(
            schemaVersion: StatsBridgeRosterRequest.schemaVersion,
            availability: .ready,
            players: players
        )
        matchCache[matchID] = response
        cacheOrder.removeAll { $0 == matchID }
        cacheOrder.append(matchID)
        while cacheOrder.count > Self.maximumCachedMatches {
            matchCache.removeValue(forKey: cacheOrder.removeFirst())
        }
        completion(response)
    }
}

extension StatsProviderLookup {
    struct HypixelStats: Codable, Equatable {
        let stars: Int?
        let finalKillDeathRatio: Double?
        let modeWinStreak: Int?
    }

    final class MutablePlayer {
        let name: String
        private var resolvedUUID: String?
        var stars: Int?
        var finalKillDeathRatio: Double?
        var modeWinStreak: Int?
        var communityTags: [StatsBridgeCommunityTag] = []

        init(name: String, uuid: String?) {
            self.name = name
            self.resolvedUUID = uuid
        }

        func resolve(uuid: String) {
            resolvedUUID = uuid
        }

        func apply(_ stats: HypixelStats) {
            stars = stats.stars
            finalKillDeathRatio = stats.finalKillDeathRatio
            modeWinStreak = stats.modeWinStreak
        }

        var result: StatsBridgePlayerResult {
            let distinctTags = Dictionary(grouping: communityTags, by: { "\($0.source)\u{0}\($0.label)" })
                .values.compactMap(\.first)
                .sorted {
                    $0.source == $1.source
                        ? $0.label.localizedCaseInsensitiveCompare($1.label) == .orderedAscending
                        : $0.source.localizedCaseInsensitiveCompare($1.source) == .orderedAscending
                }
                .prefix(8)
            return StatsBridgePlayerResult(
                name: name,
                // A failed name lookup is not proof of a Nick and never produces an identity mapping.
                nickStatus: resolvedUUID == nil ? .unavailable : .known,
                stars: stars,
                finalKillDeathRatio: finalKillDeathRatio,
                modeWinStreak: modeWinStreak,
                communityTags: Array(distinctTags)
            )
        }
    }

    static func hypixelRequest(uuid: String, apiKey: String) -> URLRequest {
        var components = URLComponents(string: "https://api.hypixel.net/v2/player")!
        components.queryItems = [URLQueryItem(name: "uuid", value: uuid)]
        var request = URLRequest(url: components.url!)
        request.timeoutInterval = responseTimeout
        request.setValue(apiKey, forHTTPHeaderField: "ApiKey")
        return request
    }

    static func mojangProfileRequest(name: String) -> URLRequest {
        let encodedName = name.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? name
        var request = URLRequest(url: URL(string: "https://api.mojang.com/users/profiles/minecraft/\(encodedName)")!)
        request.timeoutInterval = responseTimeout
        return request
    }

    static func urchinRequest(uuids: [String], apiKey: String) -> URLRequest {
        var request = URLRequest(url: URL(string: "https://api.urchin.gg/v3/players")!)
        request.httpMethod = "POST"
        request.timeoutInterval = responseTimeout
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(apiKey, forHTTPHeaderField: "X-API-Key")
        request.httpBody = try? JSONSerialization.data(withJSONObject: ["uuids": uuids])
        return request
    }

    static func seraphRequest(uuid: String, apiKey: String) -> URLRequest {
        var request = URLRequest(url: URL(string: "https://api.seraph.si/\(uuid)/blacklist")!)
        request.timeoutInterval = responseTimeout
        request.setValue(apiKey, forHTTPHeaderField: "X-API-Key")
        return request
    }

    static func parseHypixelStats(_ data: Data, gameMode: StatsBridgeGameMode) -> HypixelStats? {
        guard let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              root["success"] as? Bool == true,
              let player = root["player"] as? [String: Any] else { return nil }
        let achievements = player["achievements"] as? [String: Any]
        let bedwars = ((player["stats"] as? [String: Any])?["Bedwars"]) as? [String: Any]
        let stars = integer(achievements?["bedwars_level"])
        let finalKills = number(bedwars?["final_kills_bedwars"])
        let finalDeaths = number(bedwars?["final_deaths_bedwars"])
        let modeWinStreak = integer(bedwars?[gameMode.hypixelWinStreakKey])
        let ratio: Double?
        if let finalKills, let finalDeaths {
            ratio = finalKills / max(finalDeaths, 1)
        } else {
            ratio = nil
        }
        return HypixelStats(stars: stars, finalKillDeathRatio: ratio, modeWinStreak: modeWinStreak)
    }

    static func parseMojangProfileUUID(_ data: Data) -> String? {
        guard let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let uuid = root["id"] as? String else { return nil }
        let identity = StatsBridgeRosterMember(name: "Player", uuid: uuid.lowercased())
        return identity.isValid ? uuid.lowercased() : nil
    }

    static func parseUrchinTags(_ data: Data) -> [String: [String]] {
        guard let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let players = root["players"] as? [String: Any] else { return [:] }
        return players.reduce(into: [:]) { result, entry in
            guard let tags = entry.value as? [[String: Any]] else { return }
            let labels = tags.compactMap { tag -> String? in
                guard let type = tag["tag_type"] as? String else { return nil }
                let normalized = type.trimmingCharacters(in: .whitespacesAndNewlines)
                return normalized.isEmpty ? nil : String(normalized.prefix(48))
            }
            if !labels.isEmpty { result[entry.key.lowercased()] = labels }
        }
    }

    /// Converts documented boolean/tag fields to compact advisory labels only. Reasons,
    /// reporter attribution, timestamps, and statistics deliberately remain inside the Companion.
    static func parseSeraphTags(_ data: Data) -> [String] {
        guard let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              root["success"] as? Bool == true,
              let payload = root["data"] as? [String: Any] else { return [] }
        var tags: [String] = []
        if tagged(payload["blacklist"]) { tags.append("blacklist") }
        if tagged(payload["safelist"]) { tags.append("safelist") }
        if tagged(payload["member"]) { tags.append("member") }
        if tagged(payload["annoylist"]) { tags.append("annoylist") }
        if tagged(payload["bot"]) { tags.append("bot") }
        if tagged(payload["name_change"]) { tags.append("name change") }
        if let custom = payload["customTag"] as? String {
            let normalized = custom
                .replacingOccurrences(of: "§", with: "")
                .trimmingCharacters(in: .whitespacesAndNewlines)
            if !normalized.isEmpty { tags.append(String(normalized.prefix(48))) }
        }
        return Array(NSOrderedSet(array: tags)) as? [String] ?? []
    }

    private static func tagged(_ value: Any?) -> Bool {
        (value as? [String: Any])?["tagged"] as? Bool == true
    }

    static func number(_ value: Any?) -> Double? {
        if let value = value as? Double { return value }
        if let value = value as? Int { return Double(value) }
        if let value = value as? NSNumber { return value.doubleValue }
        return nil
    }

    static func integer(_ value: Any?) -> Int? {
        guard let value = number(value), value.isFinite, value.rounded() == value,
              value >= Double(Int.min), value <= Double(Int.max) else { return nil }
        return Int(value)
    }
}
