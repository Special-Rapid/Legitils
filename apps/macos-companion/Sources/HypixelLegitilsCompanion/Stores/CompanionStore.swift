import Foundation
import SwiftUI

@MainActor
final class CompanionStore: ObservableObject {
    private enum RuntimePreparationResult {
        case deferred
        case prepared(InstalledRuntime, LunarBakeCacheInvalidator.Outcome)
    }

    @Published private(set) var configuration: CompanionConfiguration?
    @Published var settingsDraft: CompanionConfiguration?
    @Published private(set) var runtimeStatus: RuntimeStatus?
    @Published private(set) var installedRuntime: InstalledRuntime?
    @Published private(set) var runtimeInstallStatus = "JVM runtime を準備していません。"
    @Published private(set) var bakeCacheInvalidationStatus = "MOD更新後にLunar bake cacheを自動確認します。"
    @Published private(set) var backgroundPreparerStatus = "バックグラウンド更新準備を確認していません。"
    @Published private(set) var statusMessage = "Legitils の状態を確認しています。"
    @Published private(set) var statsStatusMessage = "APIキーはこのMacのKeychainだけに保存します。"
    @Published private(set) var statsBridgeStatus = "Stats Bridge: 停止中"
    @Published private(set) var hasHypixelKey = false
    @Published private(set) var hasUrchinKey = false
    @Published private(set) var hasSeraphKey = false
    @Published private(set) var needsHypixelKeyReentry = false
    @Published private(set) var needsUrchinKeyReentry = false
    @Published private(set) var needsSeraphKeyReentry = false

    private let configurationStore: ConfigurationStore
    private let keychainStore: KeychainStore
    private let providerKeyChangeEventStore: ProviderKeyChangeEventStore
    private let statsProviderLookup: StatsProviderLookup
    private let statsBridgeServer: StatsBridgeServer
    private let runtimeInstaller: RuntimeInstaller
    private let lunarLauncherSettingsUpdater: LunarLauncherSettingsUpdater
    private let lunarBakeCacheInvalidator: LunarBakeCacheInvalidator
    private let backgroundPreparerLaunchAgent: BackgroundPreparerLaunchAgent
    private var bakeCacheRetryTimer: Timer?
    private var runtimeStatusRefreshTimer: Timer?
    private var statsBridgeHealthTimer: Timer?

    init() {
        let keychainStore = KeychainStore()
        self.configurationStore = ConfigurationStore()
        self.keychainStore = keychainStore
        self.providerKeyChangeEventStore = ProviderKeyChangeEventStore()
        let lookup = StatsProviderLookup(keychainStore: keychainStore)
        self.statsProviderLookup = lookup
        self.statsBridgeServer = StatsBridgeServer(
            lookup: lookup.lookup,
            hypixelKeyValidation: lookup.validateHypixelAPIKey
        )
        self.runtimeInstaller = RuntimeInstaller()
        self.lunarLauncherSettingsUpdater = LunarLauncherSettingsUpdater()
        self.lunarBakeCacheInvalidator = LunarBakeCacheInvalidator()
        self.backgroundPreparerLaunchAgent = BackgroundPreparerLaunchAgent()
        self.statsBridgeServer.observeAvailability { [weak self] available in
            Task { @MainActor in
                guard let self else { return }
                self.statsBridgeStatus = available
                    ? "Stats Bridge: 準備完了・MODリクエスト待機中"
                    : "Stats Bridge: 接続を再確立中"
            }
        }
    }

    deinit {
        bakeCacheRetryTimer?.invalidate()
        runtimeStatusRefreshTimer?.invalidate()
        statsBridgeHealthTimer?.invalidate()
    }

    /// Starts maintenance at launch. Subsequent retries happen only when a MOD update
    /// was safely deferred because a Minecraft game window still exists.
    func startAutomaticMaintenance() {
        installBackgroundPreparer()
        refresh()
        startRuntimeStatusRefresh()
        startStatsBridgeHealthRefresh()
    }

    private func installBackgroundPreparer() {
        do {
            let plistURL = try backgroundPreparerLaunchAgent.install()
            backgroundPreparerStatus = "バックグラウンド更新準備を登録しました（\(plistURL.lastPathComponent)）。"
        } catch {
            backgroundPreparerStatus = "バックグラウンド更新準備を登録できませんでした: \(error.localizedDescription)"
        }
    }

    private func refresh() {
        prepareRuntime()
        refreshRuntimeStatus()
        refreshProviderKeyStates()
        syncStatsBridge()
        do {
            configuration = try configurationStore.load()
            settingsDraft = configuration
            statusMessage = runtimeStatus == nil
                ? "設定を読み込みました。Lunar 起動後は実行状態も表示されます。"
                : "MOD の設定と実行状態を読み込みました。"
        } catch {
            configuration = nil
            settingsDraft = nil
            statusMessage = error.localizedDescription
        }
    }

    /// The MOD atomically rewrites this status when it injects. Keep the Companion's
    /// displayed build identifier synchronized without requiring a manual refresh.
    private func startRuntimeStatusRefresh() {
        guard runtimeStatusRefreshTimer == nil else { return }
        let timer = Timer.scheduledTimer(withTimeInterval: 0.5, repeats: true) { [weak self] _ in
            Task { @MainActor in
                self?.refreshRuntimeStatus()
            }
        }
        RunLoop.main.add(timer, forMode: .common)
        runtimeStatusRefreshTimer = timer
    }

    /// A stale descriptor must not strand the MOD after the loopback listener is interrupted.
    private func startStatsBridgeHealthRefresh() {
        guard statsBridgeHealthTimer == nil else { return }
        let timer = Timer.scheduledTimer(withTimeInterval: 2, repeats: true) { [weak self] _ in
            Task { @MainActor in
                self?.ensureStatsBridge()
            }
        }
        RunLoop.main.add(timer, forMode: .common)
        statsBridgeHealthTimer = timer
    }

    private func refreshRuntimeStatus() {
        let updated = configurationStore.loadRuntimeStatus()
        guard updated != runtimeStatus else { return }
        runtimeStatus = updated
        if updated != nil {
            statusMessage = "MOD の設定と実行状態を読み込みました。"
        }
    }

    func prepareRuntime() {
        do {
            let result = try LunarRuntimePreparationGate.withExclusivePreparationLock { () throws -> RuntimePreparationResult in
                guard !LunarRuntimePreparationGate.defaultLunarRuntimeIsActive() else { return .deferred }
                switch try lunarLauncherSettingsUpdater.preflight(jvmArgument: RuntimeInstaller.runtimeJVMArgument) {
                case .updated, .unchanged: break
                case .noLegitilsAgent, .deferredWhileLunarLauncherRunning:
                    return .deferred
                }
                guard !LunarRuntimePreparationGate.defaultLunarRuntimeIsActive() else { return .deferred }
                let runtime = try runtimeInstaller.prepare()
                guard !LunarRuntimePreparationGate.defaultLunarRuntimeIsActive() else { return .deferred }
                switch try lunarLauncherSettingsUpdater.install(jvmArgument: runtime.jvmArgument) {
                case .updated, .unchanged: break
                case .noLegitilsAgent, .deferredWhileLunarLauncherRunning:
                    return .deferred
                }
                guard !LunarRuntimePreparationGate.defaultLunarRuntimeIsActive() else { return .deferred }
                return .prepared(runtime, try lunarBakeCacheInvalidator.invalidateIfNeeded(for: runtime.modFingerprint))
            }
            switch result {
            case .prepared(let runtime, let bakeOutcome):
                installedRuntime = runtime
                runtimeInstallStatus = "最新のLoaderとMODを配置し、LunarのJVM引数を更新しました。"
                updateBakeCacheInvalidationStatus(bakeOutcome)
            case .deferred:
                stopBakeCacheRetry()
                installedRuntime = nil
                runtimeInstallStatus = "Lunarが起動中のため、JVM引数の更新は終了後にバックグラウンドで再試行します。"
                bakeCacheInvalidationStatus = "bake.zip: Lunar終了後にruntime更新と合わせて確認します。"
            }
        } catch {
            stopBakeCacheRetry()
            installedRuntime = nil
            runtimeInstallStatus = "JVM runtime を準備できませんでした: \(error.localizedDescription)"
            bakeCacheInvalidationStatus = "bake.zip: 自動確認できませんでした: \(error.localizedDescription)"
        }
    }

    private func retryBakeCacheInvalidation() {
        guard let installedRuntime else {
            stopBakeCacheRetry()
            return
        }
        do {
            let outcome = try LunarRuntimePreparationGate.withExclusivePreparationLock {
                guard !LunarRuntimePreparationGate.defaultLunarRuntimeIsActive() else {
                    return LunarBakeCacheInvalidator.Outcome.deferredWhileMinecraftGameWindowExists
                }
                return try lunarBakeCacheInvalidator.invalidateIfNeeded(for: installedRuntime.modFingerprint)
            }
            updateBakeCacheInvalidationStatus(outcome)
        } catch {
            stopBakeCacheRetry()
            bakeCacheInvalidationStatus = "bake.zip: 自動確認できませんでした: \(error.localizedDescription)"
        }
    }

    private func updateBakeCacheInvalidationStatus(for modFingerprint: String) throws {
        updateBakeCacheInvalidationStatus(try lunarBakeCacheInvalidator.invalidateIfNeeded(for: modFingerprint))
    }

    private func updateBakeCacheInvalidationStatus(_ outcome: LunarBakeCacheInvalidator.Outcome) {
        switch outcome {
            case .unchanged:
                stopBakeCacheRetry()
                bakeCacheInvalidationStatus = "bake.zip: MOD更新なし（自動削除なし）"
            case .deferredWhileMinecraftGameWindowExists:
                scheduleBakeCacheRetry()
                bakeCacheInvalidationStatus = "bake.zip: Minecraftのwindowまたはprocessを検出。終了後に自動削除します。"
            case .movedToTrash(let count):
                stopBakeCacheRetry()
                bakeCacheInvalidationStatus = "bake.zip: MOD更新を検出し、\(count) 件をゴミ箱へ移動しました。"
        }
    }

    private func scheduleBakeCacheRetry() {
        guard bakeCacheRetryTimer == nil else { return }
        let timer = Timer.scheduledTimer(withTimeInterval: 5, repeats: true) { [weak self] _ in
            Task { @MainActor in
                self?.retryBakeCacheInvalidation()
            }
        }
        RunLoop.main.add(timer, forMode: .common)
        bakeCacheRetryTimer = timer
    }

    private func stopBakeCacheRetry() {
        bakeCacheRetryTimer?.invalidate()
        bakeCacheRetryTimer = nil
    }

    func detectorBinding(_ detector: CompanionConfiguration.DetectorID) -> Binding<Bool> {
        Binding(
            get: { self.settingsDraft?.enabledDetectors.contains(detector) ?? false },
            set: { enabled in
                guard var draft = self.settingsDraft else { return }
                if enabled {
                    if !draft.enabledDetectors.contains(detector) { draft.enabledDetectors.append(detector) }
                } else {
                    draft.enabledDetectors.removeAll { $0 == detector }
                }
                self.settingsDraft = draft
            }
        )
    }

    func statsBinding(_ keyPath: WritableKeyPath<CompanionConfiguration.Stats, Bool>) -> Binding<Bool> {
        Binding(
            get: { self.settingsDraft?.stats[keyPath: keyPath] ?? false },
            set: { value in
                guard var draft = self.settingsDraft else { return }
                draft.stats[keyPath: keyPath] = value
                self.settingsDraft = draft
            }
        )
    }

    func saveSettings() {
        guard let loaded = configuration, let draft = settingsDraft else { return }
        do {
            let saved = try configurationStore.replace(draft, expectedRevision: loaded.revision)
            configuration = saved
            settingsDraft = saved
            statusMessage = "設定 revision \(saved.revision) を保存しました。Lunar起動中なら最大0.5秒で反映されます。"
        } catch {
            statusMessage = error.localizedDescription
        }
    }

    func hasKey(for provider: StatsProvider) -> Bool {
        switch provider {
        case .hypixel: hasHypixelKey
        case .urchin: hasUrchinKey
        case .seraph: hasSeraphKey
        }
    }

    func needsKeyReentry(for provider: StatsProvider) -> Bool {
        switch provider {
        case .hypixel: needsHypixelKeyReentry
        case .urchin: needsUrchinKeyReentry
        case .seraph: needsSeraphKeyReentry
        }
    }

    func saveProviderKey(_ rawKey: String, for provider: StatsProvider) {
        let key = rawKey.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !key.isEmpty else {
            statsStatusMessage = "\(provider.displayName) のAPIキーを入力してください。"
            return
        }
        do {
            try keychainStore.save(secret: key, account: provider.keychainAccount)
            statsProviderLookup.invalidateCachedResults(for: provider)
            do {
                try providerKeyChangeEventStore.recordKeyChange(for: provider)
            } catch {
                refreshProviderKeyStates()
                syncStatsBridge()
                statsStatusMessage = "\(provider.displayName) のAPIキーはKeychainに保存しましたが、Minecraftへの変更通知を準備できませんでした。"
                return
            }
            refreshProviderKeyStates()
            syncStatsBridge()
            statsStatusMessage = "\(provider.displayName) のAPIキーをKeychainに保存しました。MinecraftのChatにも変更を通知します。"
        } catch {
            statsStatusMessage = error.localizedDescription
        }
    }

    func removeProviderKey(for provider: StatsProvider) {
        do {
            try keychainStore.remove(account: provider.keychainAccount)
            statsProviderLookup.invalidateCachedResults(for: provider)
            do {
                try providerKeyChangeEventStore.recordKeyChange(for: provider)
            } catch {
                refreshProviderKeyStates()
                syncStatsBridge()
                statsStatusMessage = "\(provider.displayName) のAPIキーはKeychainから削除しましたが、Minecraftへの変更通知を準備できませんでした。"
                return
            }
            refreshProviderKeyStates()
            syncStatsBridge()
            statsStatusMessage = "\(provider.displayName) のAPIキーをKeychainから削除しました。MinecraftのChatにも変更を通知します。"
        } catch {
            statsStatusMessage = error.localizedDescription
        }
    }

    private func refreshProviderKeyStates() {
        hasHypixelKey = keychainStore.hasSecret(account: StatsProvider.hypixel.keychainAccount)
        hasUrchinKey = keychainStore.hasSecret(account: StatsProvider.urchin.keychainAccount)
        hasSeraphKey = keychainStore.hasSecret(account: StatsProvider.seraph.keychainAccount)
        needsHypixelKeyReentry = KeychainStore.needsLegacyReentry(
            for: .hypixel,
            hasCurrent: hasHypixelKey,
            hasLegacy: keychainStore.hasLegacySecret(account: StatsProvider.hypixel.keychainAccount)
        )
        needsUrchinKeyReentry = KeychainStore.needsLegacyReentry(
            for: .urchin,
            hasCurrent: hasUrchinKey,
            hasLegacy: keychainStore.hasLegacySecret(account: StatsProvider.urchin.keychainAccount)
        )
        needsSeraphKeyReentry = KeychainStore.needsLegacyReentry(
            for: .seraph,
            hasCurrent: hasSeraphKey,
            hasLegacy: keychainStore.hasLegacySecret(account: StatsProvider.seraph.keychainAccount)
        )
    }

    private func syncStatsBridge() {
        statsBridgeStatus = "Stats Bridge: ローカル接続を準備中"
        ensureStatsBridge()
    }

    private func ensureStatsBridge() {
        statsBridgeServer.start { [weak self] result in
            DispatchQueue.main.async {
                switch result {
                case .success:
                    self?.statsBridgeStatus = "Stats Bridge: 準備完了・MODリクエスト待機中"
                case .failure:
                    self?.statsBridgeStatus = "Stats Bridge: 利用できません"
                }
            }
        }
    }
}
