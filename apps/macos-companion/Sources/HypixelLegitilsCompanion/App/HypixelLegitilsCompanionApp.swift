import SwiftUI

@main
struct HypixelLegitilsCompanionApp: App {
    @StateObject private var store = CompanionStore()

    var body: some Scene {
        WindowGroup("Hypixel Legitils") {
            ContentView()
                .environmentObject(store)
                .frame(minWidth: 820, minHeight: 560)
        }
        .defaultSize(width: 980, height: 680)
    }
}
