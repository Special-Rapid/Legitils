import CryptoKit
import Foundation

/// Installs the bundled Java artifacts outside the app bundle so a copied JVM argument remains valid after updates.
struct RuntimeInstaller {
    static let loaderFileName = "hypixel-legitils-loader.jar"
    static let modFileName = "hypixel-legitils-mod.jar"
    static let configurationFileName = "loader-config.json"

    private let bundledLoaderURL: URL?
    private let bundledModURL: URL?
    private let runtimeDirectory: URL
    private let fileManager: FileManager

    init(
        bundledLoaderURL: URL? = Bundle.main.url(
            forResource: "hypixel-legitils-loader-0.1.0-SNAPSHOT",
            withExtension: "jar",
            subdirectory: "LegitilsRuntime"
        ),
        bundledModURL: URL? = Bundle.main.url(
            forResource: "hypixel-legitils-0.1.0-SNAPSHOT",
            withExtension: "jar",
            subdirectory: "LegitilsRuntime"
        ),
        runtimeDirectory: URL = CompanionPaths.loaderRuntimeDirectory,
        fileManager: FileManager = .default
    ) {
        self.bundledLoaderURL = bundledLoaderURL
        self.bundledModURL = bundledModURL
        self.runtimeDirectory = runtimeDirectory
        self.fileManager = fileManager
    }

    func prepare() throws -> InstalledRuntime {
        guard let bundledLoaderURL, isRegularFile(bundledLoaderURL) else {
            throw RuntimeInstallerError.missingBundledLoader
        }
        guard let bundledModURL, isRegularFile(bundledModURL) else {
            throw RuntimeInstallerError.missingBundledMod
        }

        try fileManager.createDirectory(at: runtimeDirectory, withIntermediateDirectories: true)
        let loaderURL = runtimeDirectory.appendingPathComponent(Self.loaderFileName)
        let modURL = runtimeDirectory.appendingPathComponent(Self.modFileName)
        let configurationURL = runtimeDirectory.appendingPathComponent(Self.configurationFileName)
        let modFingerprint = try fingerprint(of: bundledModURL)

        try replace(loaderURL, with: bundledLoaderURL)
        try replace(modURL, with: bundledModURL)
        let configuration: [String: Any] = [
            "schemaVersion": 1,
            "modJar": modURL.path,
            "mixinConfig": "mixins.hypixellegitils.json",
            "injectedProperty": "hypixellegitils.agent.injected"
        ]
        let data = try JSONSerialization.data(withJSONObject: configuration, options: [.sortedKeys])
        try data.write(to: configurationURL, options: .atomic)
        return InstalledRuntime(loaderURL: loaderURL, modURL: modURL, configurationURL: configurationURL, modFingerprint: modFingerprint)
    }

    private func isRegularFile(_ url: URL) -> Bool {
        var isDirectory: ObjCBool = false
        return fileManager.fileExists(atPath: url.path, isDirectory: &isDirectory) && !isDirectory.boolValue
    }

    private func replace(_ destination: URL, with source: URL) throws {
        let temporary = runtimeDirectory.appendingPathComponent(".install-\(UUID().uuidString)")
        try fileManager.copyItem(at: source, to: temporary)
        if fileManager.fileExists(atPath: destination.path) {
            _ = try fileManager.replaceItemAt(destination, withItemAt: temporary, backupItemName: nil, options: [])
        } else {
            try fileManager.moveItem(at: temporary, to: destination)
        }
    }

    private func fingerprint(of file: URL) throws -> String {
        let digest = SHA256.hash(data: try Data(contentsOf: file))
        return digest.map { String(format: "%02x", $0) }.joined()
    }
}

struct InstalledRuntime: Equatable {
    let loaderURL: URL
    let modURL: URL
    let configurationURL: URL
    /// Content digest of the bundled MOD used to decide whether Lunar's bake cache is stale.
    let modFingerprint: String

    var jvmArgument: String {
        "-javaagent:\(loaderURL.path)=\(configurationURL.path)"
    }
}

enum RuntimeInstallerError: LocalizedError, Equatable {
    case missingBundledLoader
    case missingBundledMod

    var errorDescription: String? {
        switch self {
        case .missingBundledLoader:
            return "Bundled Loader JAR が見つかりません。Companionを再インストールしてください。"
        case .missingBundledMod:
            return "Bundled MOD JAR が見つかりません。Companionを再インストールしてください。"
        }
    }
}
