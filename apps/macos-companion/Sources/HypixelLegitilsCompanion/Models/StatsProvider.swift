import Foundation

enum StatsProvider: String, CaseIterable, Identifiable {
    case hypixel
    case urchin
    case seraph

    var id: String { rawValue }

    var displayName: String {
        rawValue.capitalized
    }

    var keychainAccount: String {
        "\(rawValue)-api-key"
    }

    var requiresAPIKey: Bool {
        self != .seraph
    }
}
