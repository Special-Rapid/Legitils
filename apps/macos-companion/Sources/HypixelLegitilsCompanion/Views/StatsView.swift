import SwiftUI

struct StatsView: View {
    @EnvironmentObject private var store: CompanionStore
    @State private var draftKeys = Dictionary(uniqueKeysWithValues: StatsProvider.allCases.map { ($0, "") })

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text("Stats")
                .font(.largeTitle.weight(.bold))
            Text("各キーはこのMacのKeychainだけに保存します。MOD・config.json・Minecraftチャットには保存しません。")
                .foregroundStyle(.secondary)
            GroupBox("プロバイダー") {
                VStack(alignment: .leading, spacing: 14) {
                    ForEach(StatsProvider.allCases) { provider in
                        providerKeyRow(provider)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            GroupBox("表示方針") {
                VStack(alignment: .leading, spacing: 8) {
                    Toggle("Stats を有効化", isOn: store.statsBinding(\.enabled))
                    Toggle("Tab に表示", isOn: store.statsBinding(\.tab))
                    Toggle("Stars", isOn: store.statsBinding(\.stars))
                    Toggle("FKDR", isOn: store.statsBinding(\.fkdr))
                    Toggle("Win Streak", isOn: store.statsBinding(\.winStreak))
                    Toggle("Target Player とタグをchat表示", isOn: store.statsBinding(\.chat))
                    Button("表示設定を保存") { store.saveSettings() }
                    Text("Target Player: Stars ≥ 100、FKDR ≥ 1、または mode WS ≥ 3")
                    Text("Target Player は試合開始時のChatに上限なしで表示します。pregameで発言した実在プロフィールは、Target条件にかかわらずStatsをChat表示します。コミュニティタグはTabとChatに表示します。")
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            Text(store.statsStatusMessage)
                .foregroundStyle(.secondary)
            Text(store.statsBridgeStatus)
                .foregroundStyle(.secondary)
            Text("MOD・config.json・ログ・MinecraftチャットへAPIキーや取得元の生データは渡しません。Hypixelの正規化済みStatsだけを最大24時間ローカルに保持します。")
                .foregroundStyle(.secondary)
            Spacer()
        }
    }

    private func providerKeyRow(_ provider: StatsProvider) -> some View {
        HStack(spacing: 10) {
            Text(provider.displayName)
                .frame(width: 70, alignment: .leading)
            keyState(store.hasKey(for: provider))
                .frame(width: 74, alignment: .leading)
            SecureField("APIキー", text: Binding(
                get: { draftKeys[provider, default: ""] },
                set: { draftKeys[provider] = $0 }
            ))
            .textFieldStyle(.roundedBorder)
            Button("保存") {
                store.saveProviderKey(draftKeys[provider, default: ""], for: provider)
                draftKeys[provider] = ""
            }
            .disabled(draftKeys[provider, default: ""].trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            if store.hasKey(for: provider) {
                Button("削除", role: .destructive) {
                    store.removeProviderKey(for: provider)
                }
            }
        }
    }

    @ViewBuilder
    private func keyState(_ present: Bool) -> some View {
        Label(present ? "登録済み" : "未登録", systemImage: present ? "checkmark.circle.fill" : "minus.circle")
            .foregroundStyle(present ? .green : .secondary)
    }
}
