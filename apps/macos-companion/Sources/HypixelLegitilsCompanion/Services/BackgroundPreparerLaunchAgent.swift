import Darwin
import Foundation

/// Registers the Companion's existing executable as a short-lived, per-user launchd job.
/// The job runs without a UI window and never needs elevated privileges.
final class BackgroundPreparerLaunchAgent {
    static let label = "com.snkisk.hypixellegitils.runtime-preparer"
    /// launchd WatchPaths is low-latency but not a delivery guarantee. This bounds a missed update.
    static let safetyRunInterval: Int = 60

    enum Error: LocalizedError, Equatable {
        case missingExecutable
        case invalidBundlePath
        case launchctlFailed(Int32)

        var errorDescription: String? {
            switch self {
            case .missingExecutable:
                "Companion の実行ファイルが見つかりません。Companionを再インストールしてください。"
            case .invalidBundlePath:
                "Companion bundle の場所を確認できません。Companionを再インストールしてください。"
            case .launchctlFailed:
                "バックグラウンド更新準備を登録できませんでした。"
            }
        }
    }

    private let executableURL: URL
    private let bundleURL: URL
    private let launchAgentsDirectory: URL
    private let userID: uid_t
    private let fileManager: FileManager
    private let launchctl: ([String]) throws -> Void

    init(
        executableURL: URL? = Bundle.main.executableURL,
        bundleURL: URL = Bundle.main.bundleURL,
        launchAgentsDirectory: URL = FileManager.default.homeDirectoryForCurrentUser
            .appendingPathComponent("Library/LaunchAgents", isDirectory: true),
        userID: uid_t = getuid(),
        fileManager: FileManager = .default,
        launchctl: @escaping ([String]) throws -> Void = BackgroundPreparerLaunchAgent.runLaunchctl
    ) {
        self.executableURL = executableURL ?? URL(fileURLWithPath: "")
        self.bundleURL = bundleURL
        self.launchAgentsDirectory = launchAgentsDirectory
        self.userID = userID
        self.fileManager = fileManager
        self.launchctl = launchctl
    }

    func install() throws -> URL {
        guard isRegularFile(executableURL) else { throw Error.missingExecutable }
        guard bundleURL.isFileURL, !bundleURL.path.isEmpty else { throw Error.invalidBundlePath }
        try fileManager.createDirectory(at: launchAgentsDirectory, withIntermediateDirectories: true)
        let plistURL = launchAgentsDirectory.appendingPathComponent(Self.label + ".plist")
        let plist = try PropertyListSerialization.data(
            fromPropertyList: propertyList(), format: .xml, options: 0
        )
        try plist.write(to: plistURL, options: .atomic)

        let domain = "gui/\(userID)"
        try? launchctl(["bootout", "\(domain)/\(Self.label)"])
        try launchctl(["bootstrap", domain, plistURL.path])
        return plistURL
    }

    func propertyList() -> [String: Any] {
        [
            "Label": Self.label,
            "ProgramArguments": [executableURL.path, BackgroundRuntimePreparer.argument],
            "RunAtLoad": true,
            "WatchPaths": [bundleURL.path],
            "StartInterval": Self.safetyRunInterval,
            "ProcessType": "Background",
            "ThrottleInterval": 10
        ]
    }

    private func isRegularFile(_ url: URL) -> Bool {
        var isDirectory: ObjCBool = false
        return fileManager.fileExists(atPath: url.path, isDirectory: &isDirectory) && !isDirectory.boolValue
    }

    private static func runLaunchctl(_ arguments: [String]) throws {
        let process = Process()
        process.executableURL = URL(fileURLWithPath: "/bin/launchctl")
        process.arguments = arguments
        try process.run()
        process.waitUntilExit()
        guard process.terminationStatus == 0 else { throw Error.launchctlFailed(process.terminationStatus) }
    }
}
