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

    static var communityTagCacheURL: URL {
        applicationSupportDirectory.appendingPathComponent("community-tag-cache.json")
    }

    /// Contains only a bounded sequence and provider identifier, never an API key.
    static var providerKeyChangeEventsURL: URL {
        applicationSupportDirectory.appendingPathComponent("provider-key-change-events.json")
    }

    static var lunarBakeCacheFingerprintURL: URL {
        applicationSupportDirectory.appendingPathComponent("lunar-bake-cache-mod-fingerprint.txt")
    }

    /// Deliberately avoids the space in "Application Support": Lunar's JVM-argument field receives a plain argument string.
    static var loaderRuntimeDirectory: URL {
        let library = FileManager.default.urls(for: .libraryDirectory, in: .userDomainMask)[0]
        return library.appendingPathComponent(productDirectoryName, isDirectory: true)
            .appendingPathComponent("runtime", isDirectory: true)
    }

    static var lunarLauncherSettingsURL: URL {
        FileManager.default.homeDirectoryForCurrentUser
            .appendingPathComponent(".lunarclient/settings/launcher.json")
    }
}
