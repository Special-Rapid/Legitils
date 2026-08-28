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
    }

    static let argument = "--prepare-runtime"

    private let runtimePreparer: any RuntimePreparing
    private let cacheInvalidator: any LunarBakeCacheInvalidating
    private let sleep: (TimeInterval) -> Void
    private let diagnostic: (String) -> Void
    private let retryInterval: TimeInterval

    init(
        runtimePreparer: any RuntimePreparing = RuntimeInstaller(),
        cacheInvalidator: any LunarBakeCacheInvalidating = LunarBakeCacheInvalidator(),
        retryInterval: TimeInterval = 5,
        sleep: @escaping (TimeInterval) -> Void = { Thread.sleep(forTimeInterval: $0) },
        diagnostic: @escaping (String) -> Void = { FileHandle.standardError.write(Data(($0 + "\n").utf8)) }
    ) {
        self.runtimePreparer = runtimePreparer
        self.cacheInvalidator = cacheInvalidator
        self.retryInterval = retryInterval
        self.sleep = sleep
        self.diagnostic = diagnostic
    }

    static func isRequested(arguments: [String]) -> Bool {
        arguments.contains(argument)
    }

    func prepareOnce() throws -> Result {
        let runtime = try runtimePreparer.prepare()
        return try invalidateCache(for: runtime)
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
            let runtime = try runtimePreparer.prepare()
            while true {
                switch try invalidateCache(for: runtime) {
                case .prepared:
                    return 0
                case .deferredWhileMinecraftGameWindowExists:
                    sleep(retryInterval)
                }
            }
        } catch {
            diagnostic("[HypixelLegitils Background Preparer] \(error.localizedDescription)")
            return 1
        }
    }
}
