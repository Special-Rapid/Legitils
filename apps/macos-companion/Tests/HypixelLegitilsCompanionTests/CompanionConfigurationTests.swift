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

    func testCompanionUsesTheSameApplicationSupportDirectoryAsTheMod() {
        XCTAssertEqual(CompanionPaths.applicationSupportDirectory.lastPathComponent, "HypixelLegitils")
        XCTAssertEqual(CompanionPaths.configurationURL.lastPathComponent, "config.json")
        XCTAssertEqual(CompanionPaths.runtimeStatusURL.lastPathComponent, "runtime-status.json")
    }

    func testConfigurationStoreWritesAndReloadsAFirstConfiguration() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
        let url = directory.appendingPathComponent("config.json")
        defer { try? FileManager.default.removeItem(at: directory) }

        var expected = CompanionConfiguration.default
        expected.revision = 3
        expected.enabledDetectors = [.noBreakDelay]
        let store = ConfigurationStore()

        try store.write(expected, to: url)
        XCTAssertEqual(try store.load(at: url), expected)
    }
}
