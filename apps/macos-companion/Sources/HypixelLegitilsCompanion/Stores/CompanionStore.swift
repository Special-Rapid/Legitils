import Foundation

@MainActor
final class CompanionStore: ObservableObject {
    @Published private(set) var configuration: CompanionConfiguration?
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
            statusMessage = runtimeStatus == nil
                ? "設定を読み込みました。Lunar 起動後は実行状態も表示されます。"
                : "MOD の設定と実行状態を読み込みました。"
        } catch {
            configuration = nil
            statusMessage = error.localizedDescription
        }
    }
}
