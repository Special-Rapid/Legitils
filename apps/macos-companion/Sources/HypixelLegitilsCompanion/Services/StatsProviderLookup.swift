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
    private let queue = DispatchQueue(label: "com.snkisk.hypixellegitils.stats-provider")
    private var matchCache: [String: StatsBridgeRosterResponse] = [:]
    private var cacheOrder: [String] = []

    init(keychainStore: StatsProviderKeyReading, transport: StatsHTTPTransport = URLSessionStatsHTTPTransport()) {
        self.keychainStore = keychainStore
        self.transport = transport
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
        let known = roster.players.compactMap { player -> StatsBridgeRosterMember? in
            guard let uuid = player.uuid else { return nil }
            return StatsBridgeRosterMember(name: player.name, uuid: uuid.lowercased())
        }
        guard !known.isEmpty else {
            finish(matchID: roster.matchID, records: records, completion: completion)
            return
        }

        let group = DispatchGroup()
        let deadline = DispatchTime.now() + Self.responseTimeout

        if let key = try? keychainStore.readSecret(account: StatsProvider.hypixel.keychainAccount), !key.isEmpty {
            for player in known {
                group.enter()
                transport.load(Self.hypixelRequest(uuid: player.uuid!, apiKey: key)) { result in
                    self.queue.async {
                        defer { group.leave() }
                        guard case let .success((data, response)) = result, response.statusCode == 200,
                              let stats = Self.parseHypixelStats(data) else { return }
                        records[player.name.lowercased()]?.stars = stats.stars
                        records[player.name.lowercased()]?.finalKillDeathRatio = stats.finalKillDeathRatio
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

        // Seraph's documented public Player endpoint takes UUIDs and does not require this
        // Companion's stored key. Its current documented response has no community-tag field,
        // so the response is deliberately not guessed or rendered as a tag.
        for player in known {
            group.enter()
            transport.load(Self.seraphRequest(uuid: player.uuid!)) { result in
                self.queue.async { group.leave() }
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
    struct HypixelStats {
        let stars: Int?
        let finalKillDeathRatio: Double?
    }

    final class MutablePlayer {
        let name: String
        let uuid: String?
        var stars: Int?
        var finalKillDeathRatio: Double?
        var modeWinStreak: Int?
        var communityTags: [StatsBridgeCommunityTag] = []

        init(name: String, uuid: String?) {
            self.name = name
            self.uuid = uuid
        }

        var result: StatsBridgePlayerResult {
            let distinctTags = Dictionary(grouping: communityTags, by: { "\($0.source)\u{0}\($0.label)" })
                .values.compactMap(\.first)
                .prefix(8)
            return StatsBridgePlayerResult(
                name: name,
                // Absence of a UUID alone is not proof of a Nick. The MOD will later pass
                // an explicit pregame Nick-status result through a separate, identity-safe flow.
                nickStatus: uuid == nil ? .unavailable : .known,
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

    static func urchinRequest(uuids: [String], apiKey: String) -> URLRequest {
        var request = URLRequest(url: URL(string: "https://api.urchin.gg/v3/players")!)
        request.httpMethod = "POST"
        request.timeoutInterval = responseTimeout
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(apiKey, forHTTPHeaderField: "X-API-Key")
        request.httpBody = try? JSONSerialization.data(withJSONObject: ["uuids": uuids])
        return request
    }

    static func seraphRequest(uuid: String) -> URLRequest {
        var request = URLRequest(url: URL(string: "https://stash.seraph.si/player/\(uuid)")!)
        request.timeoutInterval = responseTimeout
        return request
    }

    static func parseHypixelStats(_ data: Data) -> HypixelStats? {
        guard let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              root["success"] as? Bool == true,
              let player = root["player"] as? [String: Any] else { return nil }
        let achievements = player["achievements"] as? [String: Any]
        let bedwars = ((player["stats"] as? [String: Any])?["Bedwars"]) as? [String: Any]
        let stars = integer(achievements?["bedwars_level"])
        let finalKills = number(bedwars?["final_kills_bedwars"])
        let finalDeaths = number(bedwars?["final_deaths_bedwars"])
        let ratio: Double?
        if let finalKills, let finalDeaths {
            ratio = finalKills / max(finalDeaths, 1)
        } else {
            ratio = nil
        }
        return HypixelStats(stars: stars, finalKillDeathRatio: ratio)
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
