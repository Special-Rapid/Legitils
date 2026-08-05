import Foundation
import SwiftUI

@MainActor
final class CompanionStore: ObservableObject {
    @Published private(set) var configuration: CompanionConfiguration?
    @Published var settingsDraft: CompanionConfiguration?
    @Published private(set) var runtimeStatus: RuntimeStatus?
    @Published private(set) var statusMessage = "Legitils の状態を確認しています。"
    @Published private(set) var hasHypixelKey = false
    @Published private(set) var hasUrchinKey = false
    @Published private(set) var hasSeraphKey = false

    private let configurationStore = ConfigurationStore()
    private let keychainStore = KeychainStore()

    func refresh() {
        runtimeStatus = configurationStore.loadRuntimeStatus()
        hasHypixelKey = keychainStore.hasSecret(account: "hypixel-api-key")
        hasUrchinKey = keychainStore.hasSecret(account: "urchin-api-key")
        hasSeraphKey = keychainStore.hasSecret(account: "seraph-api-key")
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
}
