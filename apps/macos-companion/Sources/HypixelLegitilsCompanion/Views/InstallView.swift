import SwiftUI

struct InstallView: View {
    private let runtimeInstaller = RuntimeInstaller()
    private let bakeCache = LunarBakeCacheService()
    @State private var runtime: InstalledRuntime?
    @State private var runtimeStatus = "JVM引数を準備しています…"
    @State private var bakeArchives: [LunarBakeArchive] = []
    @State private var bakeStatus = "未走査"
    @State private var showsTrashConfirmation = false

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text("導入")
                .font(.largeTitle.weight(.bold))
            Text("CompanionがLoaderとMODをこのMacの安定したローカル領域へ配置し、Lunar Client のゲーム設定 → JVM引数 に使う引数を生成します。Companion は実行中のゲームへ注入しません。")
                .foregroundStyle(.secondary)
            GroupBox("JVM引数の形式") {
                HStack {
                    Text(runtime?.jvmArgument ?? "準備中")
                        .font(.system(.body, design: .monospaced))
                        .textSelection(.enabled)
                    Spacer()
                    Button("コピー") {
                        guard let argument = runtime?.jvmArgument else { return }
                        NSPasteboard.general.clearContents()
                        NSPasteboard.general.setString(argument, forType: .string)
                    }
                    .disabled(runtime == nil)
                }
                .padding(.vertical, 4)
            }
            Text(runtimeStatus)
                .foregroundStyle(.secondary)
            Divider()
            GroupBox("Lunar bake cache") {
                VStack(alignment: .leading, spacing: 10) {
                    Text("Lunar は更新ごとにハッシュ名のキャッシュ階層へ bake.zip を作るため、固定フォルダ名には依存せず全体を走査します。")
                        .foregroundStyle(.secondary)
                    HStack {
                        Text(bakeStatus)
                        Spacer()
                        Button("走査") { scanBakeArchives() }
                        Button("全てゴミ箱へ移動（\(bakeArchives.count)）") {
                            showsTrashConfirmation = true
                        }
                        .disabled(bakeArchives.isEmpty)
                    }
                }
                .padding(.vertical, 4)
            }
            Spacer()
        }
        .task {
            prepareRuntime()
            scanBakeArchives()
        }
        .alert("見つかった bake.zip をゴミ箱へ移動しますか？", isPresented: $showsTrashConfirmation) {
            Button("キャンセル", role: .cancel) { }
            Button("ゴミ箱へ移動", role: .destructive) { trashBakeArchives() }
        } message: {
            Text("検出した全ての bake.zip（\(bakeArchives.count) 件）をゴミ箱へ移動します。Finderのゴミ箱から復元できます。")
        }
    }

    private func scanBakeArchives() {
        do {
            bakeArchives = try bakeCache.scan()
            bakeStatus = bakeArchives.isEmpty ? "bake.zip は見つかりませんでした。" : "\(bakeArchives.count) 件の bake.zip が見つかりました。"
        } catch {
            bakeArchives = []
            bakeStatus = "走査できませんでした: \(error.localizedDescription)"
        }
    }

    private func prepareRuntime() {
        do {
            runtime = try runtimeInstaller.prepare()
            runtimeStatus = "準備完了。コピーした1行をLunarのJVM引数へ貼り付けて、Minecraft 1.8.9を起動してください。"
        } catch {
            runtime = nil
            runtimeStatus = "JVM引数を準備できませんでした: \(error.localizedDescription)"
        }
    }

    private func trashBakeArchives() {
        do {
            let removed = try bakeCache.moveToTrash(bakeArchives)
            bakeStatus = "\(removed) 件をゴミ箱へ移動しました。"
            scanBakeArchives()
        } catch {
            bakeStatus = "移動できませんでした: \(error.localizedDescription)"
        }
    }
}
