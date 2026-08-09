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
        let manualLookup = roster.matchID.hasPrefix("manual_")
        let records = Dictionary(uniqueKeysWithValues: roster.players.map { player in
            (player.name.lowercased(), MutablePlayer(name: player.name, uuid: player.uuid))
        })
        var diagnostics: [StatsBridgeCommunityTag] = []
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
                          let uuid = Self.parseMojangProfileUUID(data) else {
                        if manualLookup { diagnostics.append(Self.diagnostic("Mojang", result)) }
                        return
                    }
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
                manualLookup: manualLookup,
                diagnostics: diagnostics,
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
        manualLookup: Bool,
        diagnostics initialDiagnostics: [StatsBridgeCommunityTag],
        completion: @escaping (StatsBridgeRosterResponse) -> Void
    ) {
        var diagnostics = initialDiagnostics
        guard !known.isEmpty else {
            finish(matchID: roster.matchID, records: records, diagnostics: diagnostics, completion: completion)
            return
        }

        let group = DispatchGroup()
        let deadline = DispatchTime.now() + Self.responseTimeout

        if let key = try? keychainStore.readSecret(account: StatsProvider.hypixel.keychainAccount), !key.isEmpty {
            for player in known {
                if !manualLookup, let cached = hypixelCache.stats(for: player.uuid!, gameMode: roster.gameMode) {
                    records[player.name.lowercased()]?.apply(cached)
                    continue
                }
                group.enter()
                transport.load(Self.hypixelRequest(uuid: player.uuid!, apiKey: key)) { result in
                    self.queue.async {
                        defer { group.leave() }
                        guard case let .success((data, response)) = result, response.statusCode == 200,
                              let stats = Self.parseHypixelStats(data, gameMode: roster.gameMode) else {
                            if manualLookup { diagnostics.append(Self.diagnostic("Hypixel", result)) }
                            return
                        }
                        records[player.name.lowercased()]?.apply(stats)
                        if manualLookup { diagnostics.append(Self.providerStatus("Hypixel")) }
                        self.hypixelCache.store(stats, for: player.uuid!, gameMode: roster.gameMode)
                    }
                }
            }
        } else if manualLookup {
            diagnostics.append(Self.diagnostic("Hypixel", nil))
        }

        if let key = try? keychainStore.readSecret(account: StatsProvider.urchin.keychainAccount), !key.isEmpty {
            group.enter()
            transport.load(Self.urchinRequest(uuids: known.compactMap(\.uuid), apiKey: key)) { result in
                self.queue.async {
                    defer { group.leave() }
                    guard case let .success((data, response)) = result, response.statusCode == 200 else {
                        if manualLookup { diagnostics.append(Self.diagnostic("Urchin", result)) }
                        return
                    }
                    let tagsByUUID = Self.parseUrchinTags(data)
                    for (uuid, labels) in tagsByUUID {
                        guard let member = known.first(where: { $0.uuid?.caseInsensitiveCompare(uuid) == .orderedSame }) else { continue }
                        records[member.name.lowercased()]?.communityTags.append(contentsOf: labels.map {
                            StatsBridgeCommunityTag(source: StatsProvider.urchin.rawValue, label: $0.label, tooltip: $0.tooltip)
                        })
                    }
                    if manualLookup, let player = known.first {
                        let labels = tagsByUUID[player.uuid!.lowercased()] ?? []
                        diagnostics.append(Self.providerStatus("Urchin", detail: labels.isEmpty ? "no active tags" : labels.map(\.label).joined(separator: ", ")))
                    }
                }
            }
        } else if manualLookup {
            diagnostics.append(Self.diagnostic("Urchin", nil))
        }

        for player in known {
            group.enter()
            transport.load(Self.seraphRequest(uuid: player.uuid!)) { result in
                self.queue.async {
                    defer { group.leave() }
                    guard case let .success((data, response)) = result, response.statusCode == 200 else {
                        if manualLookup { diagnostics.append(Self.diagnostic("Seraph", result)) }
                        return
                    }
                    let labels = Self.parseSeraphTags(data)
                    records[player.name.lowercased()]?.communityTags.append(contentsOf: labels.map {
                        StatsBridgeCommunityTag(source: StatsProvider.seraph.rawValue, label: $0.label, tooltip: $0.tooltip)
                    })
                    if manualLookup {
                        diagnostics.append(Self.providerStatus("Seraph", detail: labels.isEmpty ? "no active tags" : labels.map(\.label).joined(separator: ", ")))
                    }
                }
            }
        }

        DispatchQueue.global(qos: .utility).async {
            if group.wait(timeout: deadline) == .timedOut {
                // Timed-out work may still finish later; this match has already received its single response.
            }
            self.queue.async {
                self.finish(matchID: roster.matchID, records: records, diagnostics: diagnostics, completion: completion)
            }
        }
    }

    private func finish(
        matchID: String,
        records: [String: MutablePlayer],
        diagnostics: [StatsBridgeCommunityTag] = [],
        completion: @escaping (StatsBridgeRosterResponse) -> Void
    ) {
        if !diagnostics.isEmpty, records.count == 1, let record = records.values.first {
            record.communityTags.append(contentsOf: diagnostics)
        }
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

    struct ProviderTag: Equatable {
        let label: String
        let tooltip: String?
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
        request.setValue(apiKey, forHTTPHeaderField: "API-Key")
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

    static func seraphRequest(uuid: String) -> URLRequest {
        var request = URLRequest(url: URL(string: "https://developer-api.seraph.si/player/\(uuid)")!)
        request.timeoutInterval = responseTimeout
        return request
    }

    static func parseHypixelStats(_ data: Data, gameMode: StatsBridgeGameMode?) -> HypixelStats? {
        guard let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              root["success"] as? Bool == true,
              let player = root["player"] as? [String: Any] else { return nil }
        let achievements = player["achievements"] as? [String: Any]
        let bedwars = ((player["stats"] as? [String: Any])?["Bedwars"]) as? [String: Any]
        let stars = integer(achievements?["bedwars_level"])
        let finalKills = number(bedwars?["final_kills_bedwars"])
        let finalDeaths = number(bedwars?["final_deaths_bedwars"])
        let modeWinStreak = gameMode.flatMap { integer(bedwars?[$0.hypixelWinStreakKey]) }
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

    static func diagnostic(
        _ provider: String,
        _ result: Result<(Data, HTTPURLResponse), Error>?
    ) -> StatsBridgeCommunityTag {
        let detail: String
        if case let .success((_, response))? = result {
            switch response.statusCode {
            case 401, 403: detail = "authorization failed"
            case 404: detail = "profile not found"
            case 429: detail = "rate limited"
            default: detail = "unavailable"
            }
        } else if result == nil {
            detail = "API key unavailable"
        } else {
            detail = "unavailable"
        }
        return StatsBridgeCommunityTag(source: "diagnostic", label: "\(provider): \(detail)")
    }

    static func providerStatus(_ provider: String, detail: String = "OK") -> StatsBridgeCommunityTag {
        StatsBridgeCommunityTag(source: "provider", label: "\(provider): \(detail)")
    }

    static func parseUrchinTags(_ data: Data) -> [String: [ProviderTag]] {
        guard let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let players = root["players"] else { return [:] }

        let entries = playersToEntries(players)
        var tagsByUUID: [String: [ProviderTag]] = [:]
        for entry in entries {
            guard let normalized = normalizedUUID(entry.uuid) else { continue }
            let playerTags: [ProviderTag]
            if let tags = entry.payload as? [[String: Any]] {
                playerTags = collectUrchinLabels(tags)
            } else if let record = entry.payload as? [String: Any] {
                playerTags = playersValueToTags(record)
            } else {
                continue
            }
            if !playerTags.isEmpty {
                tagsByUUID[normalized] = playerTags
            }
        }
        return tagsByUUID
    }

    private static func playersToEntries(_ players: Any) -> [(uuid: String, payload: Any)] {
        if let map = players as? [String: Any] {
            return map.map { (uuid: $0.key, payload: $0.value) }
        }
        guard let list = players as? [Any] else { return [] }
        return list.compactMap { item in
            guard let record = item as? [String: Any] else { return nil }
            guard let uuid = (record["uuid"] as? String)
                ?? (record["id"] as? String)
                ?? (record["player_uuid"] as? String)
                ?? (record["playerId"] as? String) else { return nil }
            return (uuid: uuid, payload: record)
        }
    }

    private static func playersValueToTags(_ value: [String: Any]) -> [ProviderTag] {
        let tagValues = value["tags"] as? [[String: Any]]
            ?? (value["data"] as? [[String: Any]])
        if let tags = tagValues {
            return collectUrchinLabels(tags)
        }
        if let type = value["tag_type"] as? String {
            return collectUrchinLabels([value], fallback: ["tag_type": type])
        }
        if let tag = value["tag"] as? String {
            return collectUrchinLabels([value], fallback: ["tag": tag])
        }
        return []
    }

    private static func collectUrchinLabels(_ tags: [[String: Any]], fallback: [String: String]? = nil) -> [ProviderTag] {
        var result: [ProviderTag] = []
        if let fallback {
            result.append(contentsOf: collectUrchinLabels(fromValues: fallback))
        }
        result.append(contentsOf: collectUrchinLabels(fromValuesArray: tags))
        return distinctProviderTags(result)
    }

    private static func collectUrchinLabels(fromValuesArray tags: [[String: Any]]) -> [ProviderTag] {
        tags.compactMap { tag in
            let value = tag["tag_type"] as? String
                ?? tag["tag"] as? String
                ?? tag["type"] as? String
            guard let label = normalizeUrchinLabel(value) else { return nil }
            return ProviderTag(label: label, tooltip: sanitizedTooltip(from: tag))
        }
    }

    private static func collectUrchinLabels(fromValues values: [String: String]) -> [ProviderTag] {
        values.compactMap { _, value in
            normalizeUrchinLabel(value).map { ProviderTag(label: $0, tooltip: nil) }
        }
    }

    private static func normalizeUrchinLabel(_ value: String?) -> String? {
        canonicalTagLabel(value, source: .urchin)
    }

    private static func normalizedUUID(_ uuid: String) -> String? {
        let normalized = uuid.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        return normalized.isEmpty ? nil : normalized
    }

    /// Converts only known Seraph tag identifiers to display labels. Reasons, reporter attribution,
    /// timestamps, and every other raw provider field are deliberately discarded at this boundary.
    static func parseSeraphTags(_ data: Data) -> [ProviderTag] {
        guard let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return [] }
        let record = (root["player"] as? [String: Any])
            ?? (root["data"] as? [String: Any])
            ?? root
        var tags: [ProviderTag] = []

        if let blacklist = record["blacklist"] as? [String: Any] {
            tags.append(contentsOf: collectSeraphLabels(blacklist))
        }
        if let rawTags = record["tags"] as? [[String: Any]] {
            for tag in rawTags { tags.append(contentsOf: collectSeraphLabels(tag)) }
        }
        for identifier in ["annoylist", "bot"] {
            guard let tag = record[identifier] as? [String: Any], tag["tagged"] as? Bool == true else { continue }
            if let label = canonicalTagLabel(identifier, source: .seraph) {
                tags.append(ProviderTag(label: label, tooltip: sanitizedTooltip(from: tag)))
            }
        }
        return distinctProviderTags(tags)
    }

    private static func collectSeraphLabels(_ values: [String: Any]) -> [ProviderTag] {
        let identifiers = [
            values["report_type"] as? String,
            values["tag_type"] as? String,
            values["tag"] as? String,
            values["type"] as? String,
            values["tag_name"] as? String
        ]
        return identifiers.compactMap {
            canonicalTagLabel($0, source: .seraph).map { ProviderTag(label: $0, tooltip: sanitizedTooltip(from: values)) }
        }
    }

    private static func distinctProviderTags(_ tags: [ProviderTag]) -> [ProviderTag] {
        var byLabel: [String: ProviderTag] = [:]
        for tag in tags where byLabel[tag.label] == nil || byLabel[tag.label]?.tooltip == nil {
            byLabel[tag.label] = tag
        }
        return byLabel.values.sorted { $0.label.localizedCaseInsensitiveCompare($1.label) == .orderedAscending }
    }

    private static func sanitizedTooltip(from values: [String: Any]) -> String? {
        let raw = (values["tooltip"] as? String)
            ?? (values["reason"] as? String)
            ?? (values["description"] as? String)
            ?? (values["details"] as? String)
        guard let raw else { return nil }
        let permitted = raw.unicodeScalars.filter { scalar in
            scalar == "\n" || (scalar.value >= 0x20 && scalar.value != 0x00A7 && scalar.value != 0x007F)
        }
        let normalized = String(String.UnicodeScalarView(permitted))
            .replacingOccurrences(of: "\r\n", with: "\n")
            .replacingOccurrences(of: "\r", with: "\n")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalized.isEmpty else { return nil }
        return String(normalized.prefix(384))
    }

    /**
     * This is the sole provider-to-Mod tag vocabulary. Unknown values are not
     * bridge data: accepting one would risk passing reasons or new provider
     * metadata to the game client without an explicit product decision.
     */
    private static func canonicalTagLabel(_ value: String?, source: StatsProvider) -> String? {
        guard let value else { return nil }
        let identifier = value
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased()
            .replacingOccurrences(of: "-", with: "_")
            .replacingOccurrences(of: " ", with: "_")
        guard !identifier.isEmpty else { return nil }
        switch source {
        case .seraph:
            switch identifier {
            case "cheating_blatant", "blatant", "blatant_cheating": return "Blatant Cheating"
            case "cheating_closet", "closet", "closet_cheating": return "Closet Cheating"
            case "sniping", "sniper": return "Sniping"
            case "sniper_legit", "legit_sniper": return "Legit Sniper"
            case "sniping_potential", "potential_sniper": return "Potential Sniper"
            case "alt", "alt_account": return "Alt Account"
            case "bot": return "Bot"
            case "annoylist", "annoying": return "Annoying"
            case "caution": return "Caution"
            default: return nil
            }
        case .urchin:
            switch identifier {
            case "sniper": return "Sniper"
            case "possiblesniper", "possible_sniper": return "Possible Sniper"
            case "legitsniper", "legit_sniper": return "Legit Sniper"
            case "confirmedcheater", "confirmed_cheater": return "Confirmed Cheater"
            case "blatantcheater", "blatant_cheater": return "Blatant Cheater"
            case "closetcheater", "closet_cheater": return "Closet Cheater"
            case "caution": return "Caution"
            case "account", "alt", "alt_account": return "Account"
            default: return nil
            }
        default:
            return nil
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
