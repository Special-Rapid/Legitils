import Foundation

struct LunarBakeArchive: Identifiable, Equatable {
    let url: URL

    var id: URL { url }
}

/// Locates Lunar's content-addressed bake caches without assuming their hash directory names.
final class LunarBakeCacheService {
    static let defaultCacheRoot = FileManager.default.homeDirectoryForCurrentUser
        .appendingPathComponent(".lunarclient/offline/multiver/cache", isDirectory: true)

    private let cacheRoot: URL
    private let fileManager: FileManager
    private let trash: (URL) throws -> Void

    init(
        cacheRoot: URL = LunarBakeCacheService.defaultCacheRoot,
        fileManager: FileManager = .default,
        trash: @escaping (URL) throws -> Void = LunarBakeCacheService.moveToTrash
    ) {
        self.cacheRoot = cacheRoot.standardizedFileURL
        self.fileManager = fileManager
        self.trash = trash
    }

    func scan() throws -> [LunarBakeArchive] {
        guard fileManager.fileExists(atPath: cacheRoot.path) else { return [] }
        let keys: Set<URLResourceKey> = [.isRegularFileKey]
        guard let enumerator = fileManager.enumerator(
            at: cacheRoot,
            includingPropertiesForKeys: Array(keys),
            options: [.skipsPackageDescendants]
        ) else { return [] }

        var archives: [LunarBakeArchive] = []
        for case let candidate as URL in enumerator {
            guard candidate.lastPathComponent == "bake.zip", isSafeBakeArchive(candidate),
                  let values = try? candidate.resourceValues(forKeys: keys),
                  values.isRegularFile == true else { continue }
            archives.append(LunarBakeArchive(url: candidate))
        }
        return archives.sorted { $0.url.path.localizedStandardCompare($1.url.path) == .orderedAscending }
    }

    @discardableResult
    func moveToTrash(_ archives: [LunarBakeArchive]) throws -> Int {
        var removed = 0
        for archive in archives {
            guard isSafeBakeArchive(archive.url), fileManager.fileExists(atPath: archive.url.path) else { continue }
            try trash(archive.url)
            removed += 1
        }
        return removed
    }

    private func isSafeBakeArchive(_ candidate: URL) -> Bool {
        let resolvedRoot = cacheRoot.resolvingSymlinksInPath().standardizedFileURL.path + "/"
        let resolvedCandidate = candidate.resolvingSymlinksInPath().standardizedFileURL
        return resolvedCandidate.lastPathComponent == "bake.zip" && resolvedCandidate.path.hasPrefix(resolvedRoot)
    }

    private static func moveToTrash(_ url: URL) throws {
        var resultingURL: NSURL?
        try FileManager.default.trashItem(at: url, resultingItemURL: &resultingURL)
    }
}
