import XCTest
@testable import HypixelLegitilsCompanion

final class CompanionConfigurationTests: XCTestCase {
    func testSchemaFourConfigurationRoundTrips() throws {
        var configuration = CompanionConfiguration.default
        configuration.revision = 7
        configuration.enabledDetectors = [.combatDesync, .airStall]

        let data = try JSONEncoder().encode(configuration)
        XCTAssertEqual(try JSONDecoder().decode(CompanionConfiguration.self, from: data), configuration)
    }

    func testBridgeDescriptorRejectsExpiredOrWrongSchemaValues() {
        XCTAssertFalse(StatsBridgeDescriptor(
            schemaVersion: 2,
            port: 43123,
            capability: "capability",
            expiresAt: Date.now.addingTimeInterval(60)
        ).isUsable())
        XCTAssertFalse(StatsBridgeDescriptor(
            schemaVersion: 1,
            port: 43123,
            capability: "capability",
            expiresAt: Date.now.addingTimeInterval(-1)
        ).isUsable())
    }

    func testStatsProvidersUseStableDistinctKeychainAccounts() {
        XCTAssertEqual(StatsProvider.hypixel.keychainAccount, "hypixel-api-key")
        XCTAssertEqual(StatsProvider.urchin.keychainAccount, "urchin-api-key")
        XCTAssertEqual(StatsProvider.seraph.keychainAccount, "seraph-api-key")
        XCTAssertEqual(Set(StatsProvider.allCases.map(\.keychainAccount)).count, StatsProvider.allCases.count)
    }

    func testCompanionUsesTheSameApplicationSupportDirectoryAsTheMod() {
        XCTAssertEqual(CompanionPaths.applicationSupportDirectory.lastPathComponent, "HypixelLegitils")
        XCTAssertEqual(CompanionPaths.configurationURL.lastPathComponent, "config.json")
        XCTAssertEqual(CompanionPaths.runtimeStatusURL.lastPathComponent, "runtime-status.json")
    }

    func testConfigurationStoreReplacesAndReloadsAConfiguration() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
        let url = directory.appendingPathComponent("config.json")
        defer { try? FileManager.default.removeItem(at: directory) }

        var expected = CompanionConfiguration.default
        expected.enabledDetectors = [.noBreakDelay]
        let store = ConfigurationStore()
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        try JSONEncoder().encode(CompanionConfiguration.default).write(to: url)

        let saved = try store.replace(expected, expectedRevision: 0, to: url)
        expected.revision = 1
        XCTAssertEqual(saved, expected)
        XCTAssertEqual(try store.load(at: url), expected)
    }

    func testConfigurationStoreRejectsAStaleRevision() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
        let url = directory.appendingPathComponent("config.json")
        defer { try? FileManager.default.removeItem(at: directory) }
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        var current = CompanionConfiguration.default
        current.revision = 4
        try JSONEncoder().encode(current).write(to: url)

        XCTAssertThrowsError(try ConfigurationStore().replace(CompanionConfiguration.default, expectedRevision: 3, to: url)) { error in
            XCTAssertEqual(error as? ConfigurationStoreError, .revisionConflict)
        }
    }
}
