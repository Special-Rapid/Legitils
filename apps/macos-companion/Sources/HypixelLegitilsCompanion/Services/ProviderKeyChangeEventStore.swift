import Foundation

/// Writes a small, local-only notification for the running MOD after a Keychain save succeeds.
/// The persisted payload intentionally contains neither an API key nor any key-derived value.
struct ProviderKeyChangeEventStore {
    private static let schemaVersion = 1
    private static let maximumEvents = 16

    private let url: URL
    private let fileManager: FileManager

    init(url: URL = CompanionPaths.providerKeyChangeEventsURL, fileManager: FileManager = .default) {
        self.url = url
        self.fileManager = fileManager
    }

    func recordSavedKey(for provider: StatsProvider) throws {
        guard provider == .hypixel || provider == .urchin else { return }

        var document = loadDocument()
        let nextSequence = (document.events.map(\.sequence).max() ?? 0) + 1
        document.events.append(Event(sequence: nextSequence, provider: provider.rawValue))
        if document.events.count > Self.maximumEvents {
            document.events.removeFirst(document.events.count - Self.maximumEvents)
        }

        try fileManager.createDirectory(at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.sortedKeys]
        try encoder.encode(document).write(to: url, options: .atomic)
    }

    private func loadDocument() -> Document {
        guard
            let data = try? Data(contentsOf: url),
            let document = try? JSONDecoder().decode(Document.self, from: data),
            document.schemaVersion == Self.schemaVersion,
            document.events.count <= Self.maximumEvents,
            document.events.allSatisfy({ $0.sequence > 0 && ($0.provider == StatsProvider.hypixel.rawValue || $0.provider == StatsProvider.urchin.rawValue) }),
            zip(document.events, document.events.dropFirst()).allSatisfy({ $0.sequence < $1.sequence })
        else {
            return Document(schemaVersion: Self.schemaVersion, events: [])
        }
        return document
    }
}

private extension ProviderKeyChangeEventStore {
    struct Document: Codable, Equatable {
        let schemaVersion: Int
        var events: [Event]
    }

    struct Event: Codable, Equatable {
        let sequence: Int
        let provider: String
    }
}
