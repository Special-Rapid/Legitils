import Foundation

/// A bounded local cache of already-normalized Hypixel values. It deliberately never stores
/// API keys or raw provider responses, and is only consulted by the Companion process.
final class HypixelStatsCache {
    static let lifetime: TimeInterval = 24 * 60 * 60
    private static let schemaVersion = 1
    private static let maximumEntries = 512

    private struct StoredCache: Codable {
        let schemaVersion: Int
        let entries: [String: Entry]
    }

    private struct Entry: Codable {
        let fetchedAtMillis: Int64
        let stats: StatsProviderLookup.HypixelStats
    }

    private let url: URL
    private let now: () -> Date
    private var entries: [String: Entry] = [:]

    init(url: URL = CompanionPaths.hypixelStatsCacheURL, now: @escaping () -> Date = Date.init) {
        self.url = url
        self.now = now
        load()
    }

    func stats(for uuid: String) -> StatsProviderLookup.HypixelStats? {
        let key = normalizedUUID(uuid)
        guard let entry = entries[key], !isExpired(entry) else {
            if entries.removeValue(forKey: key) != nil { persist() }
            return nil
        }
        return entry.stats
    }

    func store(_ stats: StatsProviderLookup.HypixelStats, for uuid: String) {
        entries[normalizedUUID(uuid)] = Entry(
            fetchedAtMillis: Int64((now().timeIntervalSince1970 * 1_000).rounded()),
            stats: stats
        )
        prune()
        persist()
    }

    private func load() {
        guard let data = try? Data(contentsOf: url),
              let stored = try? JSONDecoder().decode(StoredCache.self, from: data),
              stored.schemaVersion == Self.schemaVersion else {
            return
        }
        entries = stored.entries.filter { normalizedUUID($0.key) == $0.key && !isExpired($0.value) }
        prune()
    }

    private func prune() {
        entries = entries.filter { !isExpired($0.value) }
        guard entries.count > Self.maximumEntries else { return }
        let oldest = entries.sorted { $0.value.fetchedAtMillis < $1.value.fetchedAtMillis }
        for entry in oldest.prefix(entries.count - Self.maximumEntries) {
            entries.removeValue(forKey: entry.key)
        }
    }

    private func isExpired(_ entry: Entry) -> Bool {
        let age = now().timeIntervalSince1970 - Double(entry.fetchedAtMillis) / 1_000
        return age < 0 || age > Self.lifetime
    }

    private func persist() {
        guard let data = try? JSONEncoder().encode(StoredCache(schemaVersion: Self.schemaVersion, entries: entries)) else { return }
        do {
            let directory = url.deletingLastPathComponent()
            try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
            let temporary = directory.appendingPathComponent("hypixel-stats-cache.tmp-\(UUID().uuidString)")
            try data.write(to: temporary, options: .atomic)
            if FileManager.default.fileExists(atPath: url.path) {
                _ = try FileManager.default.replaceItemAt(url, withItemAt: temporary, backupItemName: nil, options: [])
            } else {
                try FileManager.default.moveItem(at: temporary, to: url)
            }
        } catch {
            // Stats remain optional. A cache write failure must never prevent a bridge response.
        }
    }

    private func normalizedUUID(_ value: String) -> String {
        value.replacingOccurrences(of: "-", with: "").lowercased()
    }
}
