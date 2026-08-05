import Foundation
import SwiftUI

@MainActor
final class CompanionStore: ObservableObject {
    @Published private(set) var configuration: CompanionConfiguration?
    @Published var settingsDraft: CompanionConfiguration?
    @Published private(set) var runtimeStatus: RuntimeStatus?
    @Published private(set) var statusMessage = "Legitils の状態を確認しています。"
    @Published private(set) var statsStatusMessage = "APIキーはこのMacのKeychainだけに保存します。"
    @Published private(set) var statsBridgeStatus = "Stats Bridge: 停止中"
    @Published private(set) var hasHypixelKey = false
    @Published private(set) var hasUrchinKey = false
    @Published private(set) var hasSeraphKey = false

    private let configurationStore: ConfigurationStore
    private let keychainStore: KeychainStore
    private let statsProviderLookup: StatsProviderLookup
    private let statsBridgeServer: StatsBridgeServer

    init() {
        let keychainStore = KeychainStore()
        self.configurationStore = ConfigurationStore()
        self.keychainStore = keychainStore
        let lookup = StatsProviderLookup(keychainStore: keychainStore)
        self.statsProviderLookup = lookup
        self.statsBridgeServer = StatsBridgeServer(lookup: lookup.lookup)
    }

    func refresh() {
        runtimeStatus = configurationStore.loadRuntimeStatus()
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

    func saveProviderKey(_ rawKey: String, for provider: StatsProvider) {
        let key = rawKey.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !key.isEmpty else {
            statsStatusMessage = "\(provider.displayName) のAPIキーを入力してください。"
            return
        }
        do {
            try keychainStore.save(secret: key, account: provider.keychainAccount)
            refreshProviderKeyStates()
            syncStatsBridge()
            statsStatusMessage = "\(provider.displayName) のAPIキーをKeychainに保存しました。"
        } catch {
            statsStatusMessage = error.localizedDescription
        }
    }

    func removeProviderKey(for provider: StatsProvider) {
        do {
            try keychainStore.remove(account: provider.keychainAccount)
            refreshProviderKeyStates()
            syncStatsBridge()
            statsStatusMessage = "\(provider.displayName) のAPIキーをKeychainから削除しました。"
        } catch {
            statsStatusMessage = error.localizedDescription
        }
    }

    private func refreshProviderKeyStates() {
        hasHypixelKey = keychainStore.hasSecret(account: StatsProvider.hypixel.keychainAccount)
        hasUrchinKey = keychainStore.hasSecret(account: StatsProvider.urchin.keychainAccount)
        hasSeraphKey = keychainStore.hasSecret(account: StatsProvider.seraph.keychainAccount)
    }

    private func syncStatsBridge() {
        guard hasHypixelKey || hasUrchinKey || hasSeraphKey else {
            statsBridgeServer.stop()
            statsBridgeStatus = "Stats Bridge: 停止中（APIキー未登録）"
            return
        }
        statsBridgeStatus = "Stats Bridge: ローカル接続を準備中"
        statsBridgeServer.start { [weak self] result in
            DispatchQueue.main.async {
                switch result {
                case .success:
                    self?.statsBridgeStatus = "Stats Bridge: ローカル接続待機中"
                case .failure:
                    self?.statsBridgeStatus = "Stats Bridge: 利用できません"
                }
            }
        }
    }
}
