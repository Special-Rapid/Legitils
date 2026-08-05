import SwiftUI

struct SettingsView: View {
    @EnvironmentObject private var store: CompanionStore

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text("設定")
                .font(.largeTitle.weight(.bold))
            if let configuration = store.configuration, store.settingsDraft != nil {
                Form {
                    Section("Anti-cheat") {
                        ForEach(CompanionConfiguration.DetectorID.allCases) { detector in
                            Toggle(detector.displayName, isOn: store.detectorBinding(detector))
                        }
                        Picker("感度", selection: sensitivityBinding) {
                            ForEach(CompanionConfiguration.Sensitivity.allCases, id: \.self) { sensitivity in
                                Text(sensitivity.rawValue.capitalized).tag(sensitivity)
                            }
                        }
                    }
                    Section("通知") {
                        Toggle("Chat", isOn: notificationBinding(\.chat))
                        Toggle("Action Bar", isOn: notificationBinding(\.overlay))
                        Toggle("Sound", isOn: notificationBinding(\.sound))
                    }
                    Section("観測") {
                        Toggle("Nick Detect", isOn: nickDetectionBinding)
                        Toggle("Party Detect", isOn: partyDetectionBinding)
                    }
                    Section("自動ブラックリスト") {
                        Toggle("有効", isOn: markerEnabledBinding)
                        Stepper("必要な accepted flag: \(store.settingsDraft?.markers.threshold ?? 2)", value: markerThresholdBinding, in: 2...10)
                    }
                }
                .formStyle(.grouped)
                HStack {
                    Text("設定 revision \(configuration.revision)")
                        .foregroundStyle(.secondary)
                    Spacer()
                    Button("保存して即時反映") { store.saveSettings() }
                        .buttonStyle(.borderedProminent)
                }
                Text("保存後、Lunar起動中のMODは最大0.5秒で新しい設定を読み込みます。保存競合時は上書きしません。")
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

private extension SettingsView {
    var sensitivityBinding: Binding<CompanionConfiguration.Sensitivity> {
        Binding(
            get: { store.settingsDraft?.sensitivity ?? .balanced },
            set: { value in store.settingsDraft?.sensitivity = value }
        )
    }

    func notificationBinding(_ keyPath: WritableKeyPath<CompanionConfiguration.Notifications, Bool>) -> Binding<Bool> {
        Binding(
            get: { store.settingsDraft?.notifications[keyPath: keyPath] ?? false },
            set: { value in store.settingsDraft?.notifications[keyPath: keyPath] = value }
        )
    }

    var nickDetectionBinding: Binding<Bool> {
        Binding(
            get: { store.settingsDraft?.nickDetection.enabled ?? false },
            set: { value in store.settingsDraft?.nickDetection.enabled = value }
        )
    }

    var partyDetectionBinding: Binding<Bool> {
        Binding(
            get: { store.settingsDraft?.partyDetection.enabled ?? false },
            set: { value in store.settingsDraft?.partyDetection.enabled = value }
        )
    }

    var markerEnabledBinding: Binding<Bool> {
        Binding(
            get: { store.settingsDraft?.markers.enabled ?? false },
            set: { value in store.settingsDraft?.markers.enabled = value }
        )
    }

    var markerThresholdBinding: Binding<Int> {
        Binding(
            get: { store.settingsDraft?.markers.threshold ?? 2 },
            set: { value in store.settingsDraft?.markers.threshold = value }
        )
    }
}
