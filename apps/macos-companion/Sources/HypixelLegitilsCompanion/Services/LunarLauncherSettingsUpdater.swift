import Foundation

protocol LunarLauncherSettingsUpdating {
    func preflight(jvmArgument: String) throws -> LunarLauncherSettingsUpdater.Outcome
    func install(jvmArgument: String) throws -> LunarLauncherSettingsUpdater.Outcome
}

/// Updates only Legitils' Java agent entry in Lunar's existing launcher settings.
/// Other JVM arguments and all unrelated launcher fields remain untouched.
struct LunarLauncherSettingsUpdater: LunarLauncherSettingsUpdating {
    enum Outcome: Equatable {
        case unchanged
        case updated
        case noLegitilsAgent
        case deferredWhileLunarLauncherRunning
    }

    enum Error: LocalizedError, Equatable {
        case emptyJVMArgument
        case missingLauncherSettings
        case invalidLauncherSettings

        var errorDescription: String? {
            switch self {
            case .emptyJVMArgument:
                "Lunarへ設定するJVM引数が空です。"
            case .missingLauncherSettings:
                "Lunarのlauncher設定が見つかりません。Lunarを一度起動してから再試行してください。"
            case .invalidLauncherSettings:
                "Lunarのlauncher設定を安全に更新できませんでした。"
            }
        }
    }

    private let launcherSettingsURL: URL
    private let fileManager: FileManager
    private let lunarLauncherIsRunning: () -> Bool

    init(
        launcherSettingsURL: URL = CompanionPaths.lunarLauncherSettingsURL,
        fileManager: FileManager = .default,
        lunarLauncherIsRunning: @escaping () -> Bool = LunarLauncherSettingsUpdater.defaultLunarLauncherIsRunning
    ) {
        self.launcherSettingsURL = launcherSettingsURL
        self.fileManager = fileManager
        self.lunarLauncherIsRunning = lunarLauncherIsRunning
    }

    func install(jvmArgument: String) throws -> Outcome {
        guard Self.isValidLegitilsRuntimeArgument(jvmArgument) else { throw Error.invalidLauncherSettings }
        let outcome = try preflight(jvmArgument: jvmArgument)
        guard outcome == .updated else { return outcome }
        let data = try Data(contentsOf: launcherSettingsURL)
        guard var document = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              var settings = document["settings"] as? [String: Any] else { throw Error.invalidLauncherSettings }
        guard let currentArguments = settings["jvmArgs"] as? String,
              Self.isSafelyTokenizableJVMArguments(currentArguments),
              Self.containsLegitilsLoaderAgent(currentArguments) else { return .noLegitilsAgent }
        guard !lunarLauncherIsRunning() else { return .deferredWhileLunarLauncherRunning }
        settings["jvmArgs"] = Self.replacingLegitilsAgent(in: currentArguments, with: jvmArgument)
        document["settings"] = settings
        try JSONSerialization.data(withJSONObject: document, options: [.prettyPrinted, .sortedKeys]).write(to: launcherSettingsURL, options: .atomic)
        return .updated
    }

    func preflight(jvmArgument: String) throws -> Outcome {
        guard !jvmArgument.isEmpty else { throw Error.emptyJVMArgument }
        guard fileManager.fileExists(atPath: launcherSettingsURL.path) else {
            throw Error.missingLauncherSettings
        }
        let data = try Data(contentsOf: launcherSettingsURL)
        guard let document = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let settings = document["settings"] as? [String: Any] else {
            throw Error.invalidLauncherSettings
        }
        guard settings["jvmArgs"] == nil || settings["jvmArgs"] is String else {
            throw Error.invalidLauncherSettings
        }
        let currentArguments = settings["jvmArgs"] as? String ?? ""
        guard Self.isSafelyTokenizableJVMArguments(currentArguments) else {
            throw Error.invalidLauncherSettings
        }
        let updatedArguments = Self.replacingLegitilsAgent(in: currentArguments, with: jvmArgument)
        guard Self.containsLegitilsLoaderAgent(currentArguments) else { return .noLegitilsAgent }
        guard updatedArguments != currentArguments else { return .unchanged }
        // Lunar does not coordinate through the preparation lock, so never write while it is alive.
        guard !lunarLauncherIsRunning() else { return .deferredWhileLunarLauncherRunning }
        return .updated
    }

    static func defaultLunarLauncherIsRunning() -> Bool {
        let process = Process()
        let output = Pipe()
        process.executableURL = URL(fileURLWithPath: "/bin/ps")
        process.arguments = ["-ax", "-o", "command="]
        process.standardOutput = output
        process.standardError = FileHandle.nullDevice
        do {
            try process.run()
            let data = output.fileHandleForReading.readDataToEndOfFile()
            process.waitUntilExit()
            guard process.terminationStatus == 0,
                  let commands = String(data: data, encoding: .utf8) else { return true }
            return commands.lowercased().contains("lunar client.app/contents/macos/lunar client")
        } catch {
            return true
        }
    }

    static func replacingLegitilsAgent(in currentArguments: String, with jvmArgument: String) -> String {
        let tokens = currentArguments.split(whereSeparator: \.isWhitespace)
        guard tokens.contains(where: { isLegitilsLoaderArgument(String($0)) }) else {
            return currentArguments
        }
        return tokens.map { isLegitilsLoaderArgument(String($0)) ? Substring(jvmArgument) : $0 }.joined(separator: " ")
    }

    static func isLegitilsLoaderArgument(_ argument: String) -> Bool {
        let normalized = argument.lowercased()
        return normalized.hasPrefix("-javaagent:")
            && normalized.contains("hypixel-legitils-loader")
    }

    static func containsLegitilsLoaderAgent(_ arguments: String) -> Bool {
        arguments.split(whereSeparator: \.isWhitespace).contains {
            isLegitilsLoaderArgument(String($0))
        }
    }

    static func isValidLegitilsRuntimeArgument(_ argument: String) -> Bool {
        isLegitilsLoaderArgument(argument)
            && !argument.contains(where: { $0.isWhitespace || $0.isNewline || $0 == "\"" || $0 == "'" || $0 == "\\" })
            && argument.contains("=")
            && argument.contains("loader-config.json")
    }

    static func isSafelyTokenizableJVMArguments(_ arguments: String) -> Bool {
        guard !arguments.contains(where: { $0.isNewline || $0 == "\"" || $0 == "'" || $0 == "\\" }) else { return false }
        return arguments.split(whereSeparator: \.isWhitespace).allSatisfy {
            let token = String($0)
            return token != "-javaagent" && token != "-javaagent:"
        }
    }
}
