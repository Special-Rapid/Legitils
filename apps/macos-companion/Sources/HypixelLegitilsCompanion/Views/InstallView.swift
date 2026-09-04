import SwiftUI

struct InstallView: View {
    @EnvironmentObject private var store: CompanionStore

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text("導入")
                .font(.largeTitle.weight(.bold))
            Text("CompanionがLoaderとMODをこのMacの安定したローカル領域へ配置し、Lunar Client のゲーム設定 → JVM引数 に使う引数を生成します。Companion は実行中のゲームへ注入しません。")
                .foregroundStyle(.secondary)
            GroupBox("JVM引数の形式") {
                HStack {
                    Text(store.installedRuntime?.jvmArgument ?? "準備中")
                        .font(.system(.body, design: .monospaced))
                        .textSelection(.enabled)
                    Spacer()
                    Button("コピー") {
                        guard let argument = store.installedRuntime?.jvmArgument else { return }
                        NSPasteboard.general.clearContents()
                        NSPasteboard.general.setString(argument, forType: .string)
                    }
                    .disabled(store.installedRuntime == nil)
                }
                .padding(.vertical, 4)
            }
            Text(store.runtimeInstallStatus)
                .foregroundStyle(.secondary)
            Text(store.bakeCacheInvalidationStatus)
                .foregroundStyle(.secondary)
            Divider()
            GroupBox("Lunar bake cache") {
                VStack(alignment: .leading, spacing: 8) {
                    Text("MODの版が変わった時だけ、Lunarのハッシュ名キャッシュ階層を自動走査します。LunarまたはMinecraftのprocessとゲームwindowがすべて終了していることを確認してから、見つかった bake.zip をゴミ箱へ移動します。")
                        .foregroundStyle(.secondary)
                    Text("Lunarまたはゲームの起動中は削除せず、Companionを開いたまま終了すれば自動で再確認します。")
                        .foregroundStyle(.secondary)
                }
                .padding(.vertical, 4)
            }
            Spacer()
        }
    }
}
