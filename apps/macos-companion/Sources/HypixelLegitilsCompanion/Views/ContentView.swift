import SwiftUI

struct ContentView: View {
    enum Section: String, CaseIterable, Identifiable {
        case overview = "概要"
        case install = "導入"
        case settings = "設定"
        case stats = "Stats"

        var id: String { rawValue }
        var symbol: String {
            switch self {
            case .overview: "checkmark.shield"
            case .install: "arrow.down.app"
            case .settings: "slider.horizontal.3"
            case .stats: "chart.bar"
            }
        }
    }

    @EnvironmentObject private var store: CompanionStore
    @State private var selection: Section? = .overview

    var body: some View {
        NavigationSplitView {
            List(Section.allCases, selection: $selection) { section in
                Label(section.rawValue, systemImage: section.symbol)
                    .tag(section)
            }
            .navigationTitle("Legitils")
        } detail: {
            Group {
                switch selection ?? .overview {
                case .overview: OverviewView()
                case .install: InstallView()
                case .settings: SettingsView()
                case .stats: StatsView()
                }
            }
            .padding(28)
        }
        .task { store.startAutomaticMaintenance() }
    }
}
