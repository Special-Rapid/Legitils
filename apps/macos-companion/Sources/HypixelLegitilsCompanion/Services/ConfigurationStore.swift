import Foundation
import Darwin

enum ConfigurationStoreError: LocalizedError, Equatable {
    case missing
    case unsupportedSchema(Int)
    case revisionConflict
    case lockFailure

    var errorDescription: String? {
        switch self {
        case .missing:
            "Legitils の設定ファイルはまだ作成されていません。Lunar を一度起動してください。"
        case .unsupportedSchema(let version):
            "未対応の設定形式です (schema \(version))。"
        case .revisionConflict:
            "設定が他の画面またはMOD内コマンドで更新されました。更新してからもう一度保存してください。"
        case .lockFailure:
            "設定ロックを取得できませんでした。もう一度試してください。"
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

    func replace(
        _ configuration: CompanionConfiguration,
        expectedRevision: Int64,
        to url: URL = CompanionPaths.configurationURL
    ) throws -> CompanionConfiguration {
        guard configuration.schemaVersion == CompanionConfiguration.schemaVersion else {
            throw ConfigurationStoreError.unsupportedSchema(configuration.schemaVersion)
        }
        try FileManager.default.createDirectory(at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
        return try withConfigurationLock(for: url) {
            let current = try load(at: url)
            guard current.revision == expectedRevision else { throw ConfigurationStoreError.revisionConflict }
            guard expectedRevision < Int64.max else { throw ConfigurationStoreError.revisionConflict }
            var replacement = configuration
            replacement.revision = expectedRevision + 1
            let data = try encoder.encode(replacement)
            let temporary = url.deletingLastPathComponent().appendingPathComponent("config.json.tmp-\(UUID().uuidString)")
            try data.write(to: temporary)
            _ = try FileManager.default.replaceItemAt(url, withItemAt: temporary, backupItemName: nil, options: [])
            return replacement
        }
    }

    private func withConfigurationLock<T>(for url: URL, body: () throws -> T) throws -> T {
        let normalizedPath = url.standardizedFileURL.path
        let lockURL = URL(fileURLWithPath: "/tmp").appendingPathComponent(
            "hypixellegitils-config-\(javaHash(normalizedPath)).lock"
        )
        let descriptor = Darwin.open(lockURL.path, O_CREAT | O_RDWR, S_IRUSR | S_IWUSR)
        guard descriptor >= 0 else { throw ConfigurationStoreError.lockFailure }
        var lock = flock()
        lock.l_type = Int16(F_WRLCK)
        lock.l_whence = Int16(SEEK_SET)
        lock.l_start = 0
        lock.l_len = 0
        guard Darwin.fcntl(descriptor, F_SETLKW, &lock) != -1 else {
            Darwin.close(descriptor)
            throw ConfigurationStoreError.lockFailure
        }
        defer {
            lock.l_type = Int16(F_UNLCK)
            _ = Darwin.fcntl(descriptor, F_SETLK, &lock)
            Darwin.close(descriptor)
        }
        return try body()
    }

    private func javaHash(_ value: String) -> String {
        var hash: Int32 = 0
        for unit in value.utf16 {
            hash = (hash &* 31) &+ Int32(unit)
        }
        return String(UInt32(bitPattern: hash), radix: 16)
    }
}
