import SwiftUI

struct StatsView: View {
    @EnvironmentObject private var store: CompanionStore

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text("Stats")
                .font(.largeTitle.weight(.bold))
            Text("各キーはこのMacのKeychainだけに保存します。MOD・config.json・Minecraftチャットには保存しません。")
                .foregroundStyle(.secondary)
            GroupBox("プロバイダー") {
                Grid(alignment: .leading, horizontalSpacing: 28, verticalSpacing: 12) {
                    GridRow { Text("Hypixel"); keyState(store.hasHypixelKey) }
                    GridRow { Text("Urchin"); keyState(store.hasUrchinKey) }
                    GridRow { Text("Seraph"); keyState(store.hasSeraphKey) }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            GroupBox("表示方針") {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Strong: Stars ≥ 100 かつ FKDR ≥ 5、または mode WS ≥ 10")
                    Text("Elite: Stars ≥ 100、FKDR ≥ 1、または mode WS ≥ 3")
                    Text("Strong と Elite は試合開始時のChatに上限なしで表示し、コミュニティタグはTabとChatに表示します。")
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            Text("Stats Bridge とキー入力UIは未接続です。この画面はKeychainの登録状態だけを読み取ります。")
                .foregroundStyle(.secondary)
            Spacer()
        }
    }

    @ViewBuilder
    private func keyState(_ present: Bool) -> some View {
        Label(present ? "登録済み" : "未登録", systemImage: present ? "checkmark.circle.fill" : "minus.circle")
            .foregroundStyle(present ? .green : .secondary)
    }
}
