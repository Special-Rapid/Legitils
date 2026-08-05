import Foundation

enum CompanionPaths {
    static let productDirectoryName = "HypixelLegitils"

    static var applicationSupportDirectory: URL {
        let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        return base.appendingPathComponent(productDirectoryName, isDirectory: true)
    }

    static var configurationURL: URL {
        applicationSupportDirectory.appendingPathComponent("config.json")
    }

    static var runtimeStatusURL: URL {
        applicationSupportDirectory.appendingPathComponent("runtime-status.json")
    }

    static var statsBridgeDescriptorURL: URL {
        applicationSupportDirectory.appendingPathComponent("stats-bridge.json")
    }

    static var hypixelStatsCacheURL: URL {
        applicationSupportDirectory.appendingPathComponent("hypixel-stats-cache.json")
    }
}
