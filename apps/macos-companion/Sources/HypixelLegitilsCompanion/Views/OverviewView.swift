import SwiftUI

struct OverviewView: View {
    @EnvironmentObject private var store: CompanionStore

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            Text("Hypixel Legitils")
                .font(.largeTitle.weight(.bold))
            Text("Lunar 1.8.9 用の観測・通知 Companion")
                .foregroundStyle(.secondary)

            GroupBox("接続状態") {
                VStack(alignment: .leading, spacing: 10) {
                    Label(store.statusMessage, systemImage: store.configuration == nil ? "exclamationmark.triangle" : "checkmark.circle.fill")
                        .foregroundStyle(store.configuration == nil ? .orange : .green)
                    if let runtime = store.runtimeStatus {
                        Text("MOD: \(runtime.modVersion)  /  設定 revision \(runtime.configRevision)")
                        if runtime.configUsedDefaults {
                            Text("MOD はデフォルト設定で起動しました。")
                                .foregroundStyle(.orange)
                        }
                    } else {
                        Text("runtime-status.json はまだありません。Lunar を起動すると表示されます。")
                            .foregroundStyle(.secondary)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }

            GroupBox("今回の安全境界") {
                VStack(alignment: .leading, spacing: 8) {
                    Text("• APIキーはこのアプリのKeychainにだけ保存します。")
                    Text("• MODには集計済みのStatsだけを返し、キーや生レスポンスは渡しません。")
                    Text("• Nickは表示名の状態だけを扱い、実名の復元・対応付け・保存はしません。")
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            Spacer()
        }
    }
}
