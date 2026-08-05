import SwiftUI

struct SettingsView: View {
    @EnvironmentObject private var store: CompanionStore

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text("設定")
                .font(.largeTitle.weight(.bold))
            if let configuration = store.configuration {
                GroupBox("現在のMOD設定") {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("有効な検知器: \(configuration.enabledDetectors.map(\.displayName).joined(separator: ", ").ifEmpty("なし"))")
                        Text("通知: Chat \(configuration.notifications.chat ? "ON" : "OFF") / Action Bar \(configuration.notifications.overlay ? "ON" : "OFF") / Sound \(configuration.notifications.sound ? "ON" : "OFF")")
                        Text("Nick Detect: \(configuration.nickDetection.enabled ? "ON" : "OFF")  /  Party Detect: \(configuration.partyDetection.enabled ? "ON" : "OFF")")
                        Text("設定 revision \(configuration.revision)")
                            .foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                Text("編集UIとMODの即時再読込は、同じschema v4を用いて次の実装単位で接続します。現在は意図しない設定上書きを避けるため読み取り専用です。")
                    .foregroundStyle(.secondary)
            } else {
                VStack(alignment: .leading, spacing: 8) {
                    Label("設定がありません", systemImage: "doc.questionmark")
                        .font(.title3.weight(.semibold))
                    Text("Lunar を一度起動して、Legitils の config.json を作成してください。")
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            Spacer()
        }
    }
}

private extension String {
    func ifEmpty(_ fallback: String) -> String { isEmpty ? fallback : self }
}
