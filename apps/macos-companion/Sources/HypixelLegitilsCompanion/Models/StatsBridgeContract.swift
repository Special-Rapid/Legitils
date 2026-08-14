import Foundation

struct StatsBridgeRosterMember: Codable, Equatable {
    let name: String
    let uuid: String?

    var isValid: Bool {
        let validName = name.range(of: "^[A-Za-z0-9_]{1,16}$", options: .regularExpression) != nil
        guard validName else { return false }
        guard let uuid else { return true }
        return uuid.range(of: "^[0-9a-fA-F-]{32,36}$", options: .regularExpression) != nil
    }
}

/// The exact Bed Wars mode parsed from the visible client sidebar. These values deliberately
/// correspond to Hypixel's public Bed Wars win-streak fields, not to a server-side team roster.
enum StatsBridgeGameMode: String, Codable, Equatable {
    case solo = "eight_one"
    case doubles = "eight_two"
    case threes = "four_three"
    case fours = "four_four"
    case fourVFour = "two_four"

    var hypixelWinStreakKey: String {
        rawValue + "_winstreak"
    }
}

struct StatsBridgeRosterRequest: Codable, Equatable {
    static let schemaVersion = 2
    static let maximumMembers = 64

    let schemaVersion: Int
    let matchID: String
    /// Optional because current visible Bed Wars sidebars can omit mode. In that case the
    /// Companion returns other public stats but leaves the mode-specific win streak absent.
    let gameMode: StatsBridgeGameMode?
    let players: [StatsBridgeRosterMember]

    var isValid: Bool {
        schemaVersion == Self.schemaVersion
            && matchID.range(of: "^[A-Za-z0-9_-]{1,80}$", options: .regularExpression) != nil
            && !players.isEmpty
            && players.count <= Self.maximumMembers
            && players.allSatisfy(\.isValid)
            && Set(players.map { $0.name.lowercased() }).count == players.count
    }
}

enum StatsBridgeAvailability: String, Codable, Equatable {
    case ready
    case unavailable
}

struct StatsBridgeRosterResponse: Codable, Equatable {
    let schemaVersion: Int
    let availability: StatsBridgeAvailability
    let players: [StatsBridgePlayerResult]

    static func unavailable() -> StatsBridgeRosterResponse {
        StatsBridgeRosterResponse(
            schemaVersion: StatsBridgeRosterRequest.schemaVersion,
            availability: .unavailable,
            players: []
        )
    }
}

/// A fixed, capability-protected request that contains no player or key data.
struct HypixelAPIKeyValidationRequest: Codable, Equatable {
    static let schemaVersion = 1

    let schemaVersion: Int

    var isValid: Bool {
        schemaVersion == Self.schemaVersion
    }
}

/// The only Hypixel-key state exposed to the MOD. Raw responses and key metadata stay in the Companion.
enum HypixelAPIKeyValidationStatus: String, Codable, Equatable {
    case valid
    case invalid
    case unavailable
}

struct HypixelAPIKeyValidationResponse: Codable, Equatable {
    let schemaVersion: Int
    let status: HypixelAPIKeyValidationStatus
}

/// Deliberately normalized data returned to the MOD. Raw provider payloads and API keys never cross the bridge.
struct StatsBridgePlayerResult: Codable, Equatable {
    let name: String
    let nickStatus: NickStatus
    let stars: Int?
    let finalKillDeathRatio: Double?
    let modeWinStreak: Int?
    let communityTags: [StatsBridgeCommunityTag]
}

enum NickStatus: String, Codable, Equatable {
    case known
    case nicked
    case unavailable
}

struct StatsBridgeCommunityTag: Codable, Equatable {
    let source: String
    let label: String
    /// The sole provider-text exception: an optional, sanitised, length-bounded explanation for Chat hover.
    let tooltip: String?

    init(source: String, label: String, tooltip: String? = nil) {
        self.source = source
        self.label = label
        self.tooltip = tooltip
    }
}
