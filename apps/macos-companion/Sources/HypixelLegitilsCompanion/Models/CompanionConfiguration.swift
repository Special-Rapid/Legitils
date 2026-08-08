import Foundation

struct CompanionConfiguration: Codable, Equatable {
    static let schemaVersion = 6

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
    var stats: Stats

    private enum CodingKeys: String, CodingKey {
        case schemaVersion
        case revision
        case enabledDetectors
        case sensitivity
        case notifications
        case cooldowns
        case debug
        case markers
        case nickDetection
        case partyDetection
        case stats
    }

    init(
        schemaVersion: Int,
        revision: Int64,
        enabledDetectors: [DetectorID],
        sensitivity: Sensitivity,
        notifications: Notifications,
        cooldowns: Cooldowns,
        debug: Bool,
        markers: Markers,
        nickDetection: NickDetection,
        partyDetection: PartyDetection,
        stats: Stats
    ) {
        self.schemaVersion = schemaVersion
        self.revision = revision
        self.enabledDetectors = enabledDetectors
        self.sensitivity = sensitivity
        self.notifications = notifications
        self.cooldowns = cooldowns
        self.debug = debug
        self.markers = markers
        self.nickDetection = nickDetection
        self.partyDetection = partyDetection
        self.stats = stats
    }

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
        partyDetection: PartyDetection(enabled: true),
        stats: defaultStats
    )

    static let defaultStats = Stats(
        enabled: true,
        tab: true,
        stars: true,
        fkdr: true,
        winStreak: true,
        chat: true,
        nametag: false,
        nametagFkdrThreshold: 1
    )

    /// Reads schemas before the optional nametag setting without losing existing configuration.
    /// A later save persists the normalized current schema.
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let storedSchemaVersion = try container.decode(Int.self, forKey: .schemaVersion)
        guard storedSchemaVersion == 4 || storedSchemaVersion == 5 || storedSchemaVersion == Self.schemaVersion else {
            throw ConfigurationStoreError.unsupportedSchema(storedSchemaVersion)
        }

        schemaVersion = Self.schemaVersion
        revision = try container.decode(Int64.self, forKey: .revision)
        enabledDetectors = try container.decode([DetectorID].self, forKey: .enabledDetectors)
        sensitivity = try container.decode(Sensitivity.self, forKey: .sensitivity)
        notifications = try container.decode(Notifications.self, forKey: .notifications)
        cooldowns = try container.decode(Cooldowns.self, forKey: .cooldowns)
        debug = try container.decode(Bool.self, forKey: .debug)
        markers = try container.decode(Markers.self, forKey: .markers)
        nickDetection = try container.decode(NickDetection.self, forKey: .nickDetection)
        partyDetection = try container.decode(PartyDetection.self, forKey: .partyDetection)
        stats = try container.decodeIfPresent(Stats.self, forKey: .stats) ?? Self.defaultStats
    }

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

    struct Stats: Codable, Equatable {
        var enabled: Bool
        var tab: Bool
        var stars: Bool
        var fkdr: Bool
        var winStreak: Bool
        var chat: Bool
        var nametag: Bool
        var nametagFkdrThreshold: Double

        private enum CodingKeys: String, CodingKey {
            case enabled
            case tab
            case stars
            case fkdr
            case winStreak
            case chat
            case nametag
            case nametagFkdrThreshold
        }

        init(enabled: Bool, tab: Bool, stars: Bool, fkdr: Bool, winStreak: Bool, chat: Bool, nametag: Bool, nametagFkdrThreshold: Double) {
            self.enabled = enabled
            self.tab = tab
            self.stars = stars
            self.fkdr = fkdr
            self.winStreak = winStreak
            self.chat = chat
            self.nametag = nametag
            self.nametagFkdrThreshold = nametagFkdrThreshold
        }

        init(from decoder: Decoder) throws {
            let container = try decoder.container(keyedBy: CodingKeys.self)
            enabled = try container.decode(Bool.self, forKey: .enabled)
            tab = try container.decode(Bool.self, forKey: .tab)
            stars = try container.decode(Bool.self, forKey: .stars)
            fkdr = try container.decode(Bool.self, forKey: .fkdr)
            winStreak = try container.decode(Bool.self, forKey: .winStreak)
            chat = try container.decode(Bool.self, forKey: .chat)
            nametag = try container.decodeIfPresent(Bool.self, forKey: .nametag) ?? false
            nametagFkdrThreshold = try container.decodeIfPresent(Double.self, forKey: .nametagFkdrThreshold) ?? 1
        }
    }
}
