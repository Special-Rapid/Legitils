import AppKit
import Foundation

/// Removes only stale Lunar bake archives after the bundled MOD changes.
final class LunarBakeCacheInvalidator {
    enum Outcome: Equatable {
        case unchanged
        case deferredWhileMinecraftGameWindowExists
        case movedToTrash(Int)
    }

    private let fingerprintURL: URL
    private let cache: LunarBakeCacheService
    private let fileManager: FileManager
    private let minecraftGameWindowExists: () -> Bool

    init(
        fingerprintURL: URL = CompanionPaths.lunarBakeCacheFingerprintURL,
        cache: LunarBakeCacheService = LunarBakeCacheService(),
        fileManager: FileManager = .default,
        minecraftGameWindowExists: @escaping () -> Bool = LunarBakeCacheInvalidator.defaultMinecraftGameWindowExists
    ) {
        self.fingerprintURL = fingerprintURL
        self.cache = cache
        self.fileManager = fileManager
        self.minecraftGameWindowExists = minecraftGameWindowExists
    }

    /// Records a fingerprint only after safe cleanup, including when no bake archive exists.
    func invalidateIfNeeded(for modFingerprint: String) throws -> Outcome {
        guard !modFingerprint.isEmpty else { throw InvalidatorError.emptyFingerprint }
        if completedFingerprint() == modFingerprint { return .unchanged }
        if minecraftGameWindowExists() { return .deferredWhileMinecraftGameWindowExists }
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

    /// Checks for an actual Minecraft game window, not merely the Lunar launcher home window.
    static func defaultMinecraftGameWindowExists() -> Bool {
        guard let windows = CGWindowListCopyWindowInfo([.optionAll, .excludeDesktopElements], kCGNullWindowID) as? [[String: Any]] else {
            // Do not invalidate a cache if macOS cannot prove that no game window exists.
            return true
        }
        return windows.contains { window in
            guard (window[kCGWindowLayer as String] as? Int) == 0 else {
                return false
            }
            let owner = window[kCGWindowOwnerName as String] as? String ?? ""
            let title = window[kCGWindowName as String] as? String ?? ""
            return isMinecraftGameWindow(ownerName: owner, title: title)
        }
    }

    static func isMinecraftGameWindow(ownerName: String, title: String) -> Bool {
        let owner = ownerName.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let normalized = title.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard normalized != "home - lunar client" else { return false }
        // Lunar leaves auxiliary launcher windows with an empty title alive after
        // Minecraft exits. An owner-name match alone would block safe cache
        // maintenance forever, so Lunar requires a game-shaped window title.
        if owner.contains("lunar") {
            return normalized.contains("minecraft") || normalized.contains("lunar client 1.")
        }
        return normalized.contains("minecraft")
            || normalized.contains("badlion")
            || owner.contains("minecraft")
            || owner.contains("badlion")
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
