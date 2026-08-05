import Foundation

struct CompanionConfiguration: Codable, Equatable {
    static let schemaVersion = 4

    var schemaVersion: Int
    var revision: Int64
    var enabledDetectors: [DetectorID]
    var sensitivity: Sensitivity
    var notifications: Notifications
    var cooldowns: Cooldowns
    var debug: Bool
    var markers: Markers
    var nickDetection: NickDetection
    var partyDetection: PartyDetection

    static let `default` = CompanionConfiguration(
        schemaVersion: schemaVersion,
        revision: 0,
        enabledDetectors: [],
        sensitivity: .balanced,
        notifications: Notifications(chat: true, overlay: false, sound: false),
        cooldowns: Cooldowns(normalMillis: 1_000, airStallMillis: 30_000),
        debug: false,
        markers: Markers(enabled: true, threshold: 2),
        nickDetection: NickDetection(enabled: true),
        partyDetection: PartyDetection(enabled: true)
    )

    enum DetectorID: String, Codable, CaseIterable, Identifiable {
        case autoBlock = "AUTO_BLOCK"
        case noSlow = "NO_SLOW"
        case killAura = "KILL_AURA"
        case legitScaffold = "LEGIT_SCAFFOLD"
        case bedNuke = "BED_NUKE"
        case combatDesync = "COMBAT_DESYNC"
        case airStall = "AIR_STALL"
        case noBreakDelay = "NO_BREAK_DELAY"

        var id: String { rawValue }

        var displayName: String {
            switch self {
            case .autoBlock: "AutoBlock"
            case .noSlow: "NoSlow"
            case .killAura: "KillAura"
            case .legitScaffold: "LegitScaffold"
            case .bedNuke: "BedNuke"
            case .combatDesync: "Blink"
            case .airStall: "Timer"
            case .noBreakDelay: "NoBreakDelay"
            }
        }
    }

    enum Sensitivity: String, Codable, CaseIterable {
        case conservative
        case balanced
        case sensitive
    }

    struct Notifications: Codable, Equatable {
        var chat: Bool
        var overlay: Bool
        var sound: Bool
    }

    struct Cooldowns: Codable, Equatable {
        var normalMillis: Int
        var airStallMillis: Int
    }

    struct Markers: Codable, Equatable {
        var enabled: Bool
        var threshold: Int
    }

    struct NickDetection: Codable, Equatable {
        var enabled: Bool
    }

    struct PartyDetection: Codable, Equatable {
        var enabled: Bool
    }
}
