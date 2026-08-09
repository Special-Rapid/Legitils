import AppKit
import Foundation

/// Removes only stale Lunar bake archives after the bundled MOD changes.
final class LunarBakeCacheInvalidator {
    enum Outcome: Equatable {
        case unchanged
        case deferredWhileLunarRuns
        case movedToTrash(Int)
    }

    private let fingerprintURL: URL
    private let cache: LunarBakeCacheService
    private let fileManager: FileManager
    private let lunarIsRunning: () -> Bool

    init(
        fingerprintURL: URL = CompanionPaths.lunarBakeCacheFingerprintURL,
        cache: LunarBakeCacheService = LunarBakeCacheService(),
        fileManager: FileManager = .default,
        lunarIsRunning: @escaping () -> Bool = LunarBakeCacheInvalidator.defaultLunarIsRunning
    ) {
        self.fingerprintURL = fingerprintURL
        self.cache = cache
        self.fileManager = fileManager
        self.lunarIsRunning = lunarIsRunning
    }

    /// Records a fingerprint only after safe cleanup, including when no bake archive exists.
    func invalidateIfNeeded(for modFingerprint: String) throws -> Outcome {
        guard !modFingerprint.isEmpty else { throw InvalidatorError.emptyFingerprint }
        if completedFingerprint() == modFingerprint { return .unchanged }
        if lunarIsRunning() { return .deferredWhileLunarRuns }
        let moved = try cache.moveToTrash(cache.scan())
        try fileManager.createDirectory(at: fingerprintURL.deletingLastPathComponent(), withIntermediateDirectories: true)
        guard let data = modFingerprint.data(using: .utf8) else { throw InvalidatorError.emptyFingerprint }
        try data.write(to: fingerprintURL, options: .atomic)
        return .movedToTrash(moved)
    }

    private func completedFingerprint() -> String? {
        guard let data = try? Data(contentsOf: fingerprintURL),
              let value = String(data: data, encoding: .utf8)?.trimmingCharacters(in: .whitespacesAndNewlines),
              !value.isEmpty else { return nil }
        return value
    }

    private static func defaultLunarIsRunning() -> Bool {
        NSWorkspace.shared.runningApplications.contains { $0.bundleIdentifier == "com.moonsworth.client" }
    }

    enum InvalidatorError: LocalizedError, Equatable {
        case emptyFingerprint

        var errorDescription: String? {
            switch self {
            case .emptyFingerprint: "MOD fingerprint が空です。"
            }
        }
    }
}
