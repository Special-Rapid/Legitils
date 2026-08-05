import Foundation

enum ConfigurationStoreError: LocalizedError {
    case missing
    case unsupportedSchema(Int)

    var errorDescription: String? {
        switch self {
        case .missing:
            "Legitils の設定ファイルはまだ作成されていません。Lunar を一度起動してください。"
        case .unsupportedSchema(let version):
            "未対応の設定形式です (schema \(version))。"
        }
    }
}

struct ConfigurationStore {
    private let encoder: JSONEncoder
    private let decoder: JSONDecoder

    init() {
        encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        decoder = JSONDecoder()
    }

    func load(at url: URL = CompanionPaths.configurationURL) throws -> CompanionConfiguration {
        guard FileManager.default.fileExists(atPath: url.path) else {
            throw ConfigurationStoreError.missing
        }
        let configuration = try decoder.decode(CompanionConfiguration.self, from: Data(contentsOf: url))
        guard configuration.schemaVersion == CompanionConfiguration.schemaVersion else {
            throw ConfigurationStoreError.unsupportedSchema(configuration.schemaVersion)
        }
        return configuration
    }

    func loadRuntimeStatus(at url: URL = CompanionPaths.runtimeStatusURL) -> RuntimeStatus? {
        guard let data = try? Data(contentsOf: url) else { return nil }
        return try? decoder.decode(RuntimeStatus.self, from: data)
    }

    func write(_ configuration: CompanionConfiguration, to url: URL = CompanionPaths.configurationURL) throws {
        guard configuration.schemaVersion == CompanionConfiguration.schemaVersion else {
            throw ConfigurationStoreError.unsupportedSchema(configuration.schemaVersion)
        }
        try FileManager.default.createDirectory(at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
        let data = try encoder.encode(configuration)
        let temporary = url.deletingLastPathComponent().appendingPathComponent("config.json.tmp-\(UUID().uuidString)")
        try data.write(to: temporary)
        if FileManager.default.fileExists(atPath: url.path) {
            _ = try FileManager.default.replaceItemAt(url, withItemAt: temporary, backupItemName: nil, options: [])
        } else {
            try FileManager.default.moveItem(at: temporary, to: url)
        }
    }
}
