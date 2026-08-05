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

struct StatsBridgeRosterRequest: Codable, Equatable {
    static let schemaVersion = 1
    static let maximumMembers = 64

    let schemaVersion: Int
    let matchID: String
    let players: [StatsBridgeRosterMember]

    var isValid: Bool {
        schemaVersion == Self.schemaVersion
            && matchID.range(of: "^[A-Za-z0-9_-]{1,80}$", options: .regularExpression) != nil
            && !players.isEmpty
            && players.count <= Self.maximumMembers
            && players.allSatisfy(\.isValid)
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
}
