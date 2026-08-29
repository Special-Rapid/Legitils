import Foundation

protocol RuntimePreparing {
    func prepare() throws -> InstalledRuntime
}

extension RuntimeInstaller: RuntimePreparing {}

protocol LunarBakeCacheInvalidating {
    func invalidateIfNeeded(for modFingerprint: String) throws -> LunarBakeCacheInvalidator.Outcome
}

extension LunarBakeCacheInvalidator: LunarBakeCacheInvalidating {}

/// Headless maintenance invoked by launchd. It never creates a window or attaches to Minecraft.
struct BackgroundRuntimePreparer {
    enum Result: Equatable {
        case prepared(LunarBakeCacheInvalidator.Outcome)
        case deferredWhileMinecraftGameWindowExists
        case deferredWhileLunarLauncherRunning
        case deferredWhileLunarRuntimeIsActive
    }

    static let argument = "--prepare-runtime"

    private let runtimePreparer: any RuntimePreparing
    private let lunarRuntimeIsActive: () -> Bool
    private let launcherSettingsUpdater: any LunarLauncherSettingsUpdating
    private let cacheInvalidator: any LunarBakeCacheInvalidating
    private let sleep: (TimeInterval) -> Void
    private let diagnostic: (String) -> Void
    private let retryInterval: TimeInterval

    init(
        runtimePreparer: any RuntimePreparing = RuntimeInstaller(),
        lunarRuntimeIsActive: @escaping () -> Bool = LunarRuntimePreparationGate.defaultLunarRuntimeIsActive,
        launcherSettingsUpdater: any LunarLauncherSettingsUpdating = LunarLauncherSettingsUpdater(),
        cacheInvalidator: any LunarBakeCacheInvalidating = LunarBakeCacheInvalidator(),
        retryInterval: TimeInterval = 5,
        sleep: @escaping (TimeInterval) -> Void = { Thread.sleep(forTimeInterval: $0) },
        diagnostic: @escaping (String) -> Void = { FileHandle.standardError.write(Data(($0 + "\n").utf8)) }
    ) {
        self.runtimePreparer = runtimePreparer
        self.lunarRuntimeIsActive = lunarRuntimeIsActive
        self.launcherSettingsUpdater = launcherSettingsUpdater
        self.cacheInvalidator = cacheInvalidator
        self.retryInterval = retryInterval
        self.sleep = sleep
        self.diagnostic = diagnostic
    }

    static func isRequested(arguments: [String]) -> Bool {
        arguments.contains(argument)
    }

    func prepareOnce() throws -> Result {
        try prepareTransaction().result
    }

    private func prepareTransaction() throws -> (runtime: InstalledRuntime?, result: Result) {
        try LunarRuntimePreparationGate.withExclusivePreparationLock {
            guard !lunarRuntimeIsActive() else { return (nil, .deferredWhileLunarRuntimeIsActive) }
            switch try launcherSettingsUpdater.preflight(jvmArgument: RuntimeInstaller.runtimeJVMArgument) {
            case .updated, .unchanged: break
            case .deferredWhileLunarLauncherRunning:
                return (nil, .deferredWhileLunarLauncherRunning)
            case .noLegitilsAgent:
                return (nil, .deferredWhileLunarRuntimeIsActive)
            }
            guard !lunarRuntimeIsActive() else { return (nil, .deferredWhileLunarRuntimeIsActive) }
            let runtime = try runtimePreparer.prepare()
            guard !lunarRuntimeIsActive() else { return (nil, .deferredWhileLunarRuntimeIsActive) }
            switch try launcherSettingsUpdater.install(jvmArgument: runtime.jvmArgument) {
            case .updated, .unchanged: break
            case .deferredWhileLunarLauncherRunning:
                return (nil, .deferredWhileLunarLauncherRunning)
            case .noLegitilsAgent:
                return (nil, .deferredWhileLunarRuntimeIsActive)
            }
            guard !lunarRuntimeIsActive() else { return (nil, .deferredWhileLunarRuntimeIsActive) }
            return (runtime, try invalidateCache(for: runtime))
        }
    }

    private func invalidateCache(for runtime: InstalledRuntime) throws -> Result {
        let outcome = try cacheInvalidator.invalidateIfNeeded(for: runtime.modFingerprint)
        if outcome == .deferredWhileMinecraftGameWindowExists {
            return .deferredWhileMinecraftGameWindowExists
        }
        return .prepared(outcome)
    }

    /// Returns only after runtime preparation is complete. It remains alive solely while
    /// a real game window makes bake-cache maintenance unsafe.
    func run() -> Int32 {
        do {
            let transaction = try prepareTransaction()
            while true {
                switch transaction.result {
                case .prepared:
                    return 0
                case .deferredWhileLunarLauncherRunning, .deferredWhileLunarRuntimeIsActive:
                    diagnostic("[HypixelLegitils Background Preparer] Lunar is active; runtime update deferred.")
                    return 0
                case .deferredWhileMinecraftGameWindowExists:
                    break
                }
                guard let runtime = transaction.runtime else { return 0 }
                sleep(retryInterval)
                let result = try LunarRuntimePreparationGate.withExclusivePreparationLock { () throws -> Result in
                    guard !lunarRuntimeIsActive() else { return .deferredWhileLunarRuntimeIsActive }
                    return try invalidateCache(for: runtime)
                }
                switch result {
                case .prepared:
                    return 0
                case .deferredWhileMinecraftGameWindowExists:
                    continue
                case .deferredWhileLunarLauncherRunning:
                    diagnostic("[HypixelLegitils Background Preparer] Lunar launcher is active; JVM argument update deferred.")
                    return 0
                case .deferredWhileLunarRuntimeIsActive:
                    diagnostic("[HypixelLegitils Background Preparer] Lunar is active; runtime update deferred.")
                    return 0
                }
            }
        } catch {
            diagnostic("[HypixelLegitils Background Preparer] \(error.localizedDescription)")
            return 1
        }
    }
}
