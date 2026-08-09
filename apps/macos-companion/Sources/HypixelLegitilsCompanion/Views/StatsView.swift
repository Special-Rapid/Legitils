import SwiftUI

struct StatsView: View {
    @EnvironmentObject private var store: CompanionStore
    @State private var draftKeys = Dictionary(uniqueKeysWithValues: StatsProvider.allCases.map { ($0, "") })

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text("Stats")
                .font(.largeTitle.weight(.bold))
            Text("Hypixel・UrchinのキーはこのMacのKeychainだけに保存します。Seraphは公開APIのためキー不要です。")
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
                    Toggle("試合開始後に自動 /who でStatsを更新", isOn: store.statsBinding(\.autoWho))
                    Toggle("Tabのチーム間を合計FKDR順に並べ替え", isOn: store.statsBinding(\.tabTeamSorting))
                    Toggle("Tabのチーム内をNick/FKDR順に並べ替え", isOn: store.statsBinding(\.tabPlayerSorting))
                    Button("表示設定を保存") { store.saveSettings() }
                    Text("Target Player: Stars ≥ 100、FKDR ≥ 1、または mode WS ≥ 3")
                    Text("Target Player は試合開始時のChatに上限なしで表示します。pregameで発言した実在プロフィールは、Target条件にかかわらずStatsをChat表示します。コミュニティタグは略号でChat・Tab・Nametagに表示し、Chatでは略号にhoverすると説明を確認できます。")
                    Text("Tabでは同じBed Warsチームを常にまとめて表示します。チーム間の並べ替えは任意で、既知FKDRの合計にNick 1人あたり5.0を加算します。チーム内も任意でNickを先頭、その後FKDR順です。同点・Stats不明はHypixel本来の順序を維持します。")
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

    @ViewBuilder
    private func providerKeyRow(_ provider: StatsProvider) -> some View {
        if provider.requiresAPIKey {
            keyProviderRow(provider)
        } else {
            publicProviderRow(provider)
        }
    }

    private func keyProviderRow(_ provider: StatsProvider) -> some View {
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

    private func publicProviderRow(_ provider: StatsProvider) -> some View {
        HStack(spacing: 10) {
            Text(provider.displayName)
                .frame(width: 70, alignment: .leading)
            Label("公開API・キー不要", systemImage: "globe")
                .foregroundStyle(.green)
            Spacer()
            if store.hasKey(for: provider) {
                Button("保存済みキーを削除", role: .destructive) {
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
