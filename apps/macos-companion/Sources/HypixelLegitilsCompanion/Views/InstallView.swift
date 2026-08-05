import SwiftUI

struct InstallView: View {
    private let argument = "-javaagent:/absolute/path/to/hypixel-legitils-loader.jar"

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
            Spacer()
        }
    }
}
