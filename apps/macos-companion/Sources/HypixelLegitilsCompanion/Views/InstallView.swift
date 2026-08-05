import SwiftUI

struct InstallView: View {
    private let argument = "-javaagent:/absolute/path/to/hypixel-legitils-loader.jar"
    private let bakeCache = LunarBakeCacheService()
    @State private var bakeArchives: [LunarBakeArchive] = []
    @State private var bakeStatus = "未走査"
    @State private var showsTrashConfirmation = false

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text("導入")
                .font(.largeTitle.weight(.bold))
            Text("Lunar Client のゲーム設定 → JVM引数 に loader JAR の絶対パスを指定します。Companion は Lunar やゲームプロセスへ注入しません。")
                .foregroundStyle(.secondary)
            GroupBox("JVM引数の形式") {
                HStack {
                    Text(argument)
                        .font(.system(.body, design: .monospaced))
                        .textSelection(.enabled)
                    Spacer()
                    Button("コピー") {
                        NSPasteboard.general.clearContents()
                        NSPasteboard.general.setString(argument, forType: .string)
                    }
                }
                .padding(.vertical, 4)
            }
            Text("実際の loader JAR を選ぶ導線と署名済み配布は、Companionの次段階で追加します。")
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
        .task { scanBakeArchives() }
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
