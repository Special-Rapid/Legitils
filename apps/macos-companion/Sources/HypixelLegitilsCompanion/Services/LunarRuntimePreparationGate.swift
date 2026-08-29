import Darwin
import Foundation

/// Do not replace runtime artifacts or launcher configuration while any Lunar phase is active.
enum LunarRuntimePreparationGate {
    static func defaultLunarRuntimeIsActive() -> Bool {
        LunarLauncherSettingsUpdater.defaultLunarLauncherIsRunning()
            || LunarBakeCacheInvalidator.defaultMinecraftGameWindowExists()
            || LunarBakeCacheInvalidator.defaultMinecraftProcessExists()
    }

    static func withExclusivePreparationLock<T>(
        lockURL: URL = CompanionPaths.lunarLauncherSettingsURL.deletingLastPathComponent()
            .appendingPathComponent(".hypixel-legitils-runtime.lock"),
        _ body: () throws -> T
    ) throws -> T {
        let descriptor = open(lockURL.path, O_CREAT | O_RDWR, S_IRUSR | S_IWUSR)
        guard descriptor >= 0 else { throw PreparationError.lockUnavailable }
        defer { close(descriptor) }
        guard flock(descriptor, LOCK_EX) == 0 else { throw PreparationError.lockUnavailable }
        defer { flock(descriptor, LOCK_UN) }
        return try body()
    }

    enum PreparationError: LocalizedError {
        case lockUnavailable

        var errorDescription: String? {
            "Lunar runtime更新の排他ロックを取得できませんでした。"
        }
    }
}
