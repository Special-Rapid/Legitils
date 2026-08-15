import Foundation

/// A bounded local cache of already-normalized Seraph and Urchin tags.
/// It never stores provider credentials or raw provider responses.
final class CommunityTagCache {
    static let lifetime: TimeInterval = 24 * 60 * 60
    // Version 1 persisted empty Seraph results produced before the public
    // Developer API's `payload` envelope was understood. Retain valid Urchin
    // entries but force Seraph to refresh after this repair.
    private static let schemaVersion = 2
    private static let legacySchemaVersion = 1
    private static let maximumEntries = 1_024

    private struct StoredCache: Codable {
        let schemaVersion: Int
        let entries: [String: Entry]
    }

    private struct Entry: Codable {
        let fetchedAtMillis: Int64
        let tags: [StatsProviderLookup.ProviderTag]
    }

    private let url: URL
    private let now: () -> Date
    private var entries: [String: Entry] = [:]

    init(url: URL = CompanionPaths.communityTagCacheURL, now: @escaping () -> Date = Date.init) {
        self.url = url
        self.now = now
        load()
    }

    /// `nil` means no usable cached response; an empty array is a cached no-tag response.
    func tags(for provider: StatsProvider, uuid: String) -> [StatsProviderLookup.ProviderTag]? {
        guard provider == .seraph || provider == .urchin else { return nil }
        let key = cacheKey(provider: provider, uuid: uuid)
        guard let entry = entries[key], !isExpired(entry) else {
            if entries.removeValue(forKey: key) != nil { persist() }
            return nil
        }
        return entry.tags
    }

    func store(_ tags: [StatsProviderLookup.ProviderTag], for provider: StatsProvider, uuid: String) {
        guard provider == .seraph || provider == .urchin,
              tags.allSatisfy({ validTag($0, provider: provider) }) else { return }
        entries[cacheKey(provider: provider, uuid: uuid)] = Entry(
            // Truncate rather than round: a rounded-up timestamp looks briefly future-dated and expires immediately.
            fetchedAtMillis: Int64(now().timeIntervalSince1970 * 1_000),
            tags: tags
        )
        prune()
        persist()
    }

    /// Removes only one authenticated provider's normalized entries after its key changes.
    /// Seraph remains keyless and is deliberately retained.
    func removeAll(for provider: StatsProvider) {
        guard provider == .urchin else { return }
        let prefix = provider.rawValue + ":"
        entries = entries.filter { !$0.key.hasPrefix(prefix) }
        persist()
    }

    private func load() {
        guard let data = try? Data(contentsOf: url),
              let stored = try? JSONDecoder().decode(StoredCache.self, from: data),
              stored.schemaVersion == Self.schemaVersion || stored.schemaVersion == Self.legacySchemaVersion else {
            return
        }
        entries = stored.entries.filter {
            guard let provider = provider(forCacheKey: $0.key) else { return false }
            guard validEntry($0.value, provider: provider), !isExpired($0.value) else { return false }
            return stored.schemaVersion == Self.schemaVersion || provider != .seraph
        }
        prune()
    }

    private func validEntry(_ entry: Entry, provider: StatsProvider) -> Bool {
        entry.fetchedAtMillis >= 0 && entry.tags.allSatisfy { validTag($0, provider: provider) }
    }

    private func validTag(_ tag: StatsProviderLookup.ProviderTag, provider: StatsProvider) -> Bool {
        guard tag.label.count <= 64,
              StatsProviderLookup.isCanonicalTagLabel(tag.label, source: provider) else { return false }
        guard let tooltip = tag.tooltip else { return true }
        return tooltip.count <= 384
            && tooltip.unicodeScalars.allSatisfy { $0 == "\n" || ($0.value >= 0x20 && $0.value != 0x00A7 && $0.value != 0x007F) }
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
            let temporary = directory.appendingPathComponent("community-tag-cache.tmp-\(UUID().uuidString)")
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

    private func cacheKey(provider: StatsProvider, uuid: String) -> String {
        provider.rawValue + ":" + normalizedUUID(uuid)
    }

    private func normalizedUUID(_ value: String) -> String {
        value.replacingOccurrences(of: "-", with: "").lowercased()
    }

    private func validCacheKey(_ value: String) -> Bool {
        provider(forCacheKey: value) != nil
    }

    private func provider(forCacheKey value: String) -> StatsProvider? {
        let components = value.split(separator: ":", maxSplits: 1, omittingEmptySubsequences: false)
        guard components.count == 2,
              let provider = StatsProvider(rawValue: String(components[0])),
              provider == .seraph || provider == .urchin,
              String(components[1]).range(of: "^[0-9a-f]{32}$", options: .regularExpression) != nil else {
            return nil
        }
        return provider
    }
}
