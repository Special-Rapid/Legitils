import XCTest
@testable import HypixelLegitilsCompanion

final class CompanionConfigurationTests: XCTestCase {
    func testCurrentSchemaConfigurationRoundTrips() throws {
        var configuration = CompanionConfiguration.default
        configuration.revision = 7
        configuration.enabledDetectors = [.combatDesync, .airStall]

        let data = try JSONEncoder().encode(configuration)
        XCTAssertEqual(try JSONDecoder().decode(CompanionConfiguration.self, from: data), configuration)
    }

    func testSchemaFourConfigurationAddsDefaultStatsAndNormalizesToCurrentSchema() throws {
        let configuration = try JSONDecoder().decode(CompanionConfiguration.self, from: schemaFourFixture)

        XCTAssertEqual(configuration.schemaVersion, CompanionConfiguration.schemaVersion)
        XCTAssertEqual(configuration.revision, 13)
        XCTAssertEqual(configuration.enabledDetectors, [.noBreakDelay])
        XCTAssertEqual(configuration.stats, CompanionConfiguration.defaultStats)
    }

    func testSchemaFourConfigurationWritesSchemaFiveAfterSaving() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
        let url = directory.appendingPathComponent("config.json")
        defer { try? FileManager.default.removeItem(at: directory) }
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        try schemaFourFixture.write(to: url)

        let store = ConfigurationStore()
        let loaded = try store.load(at: url)
        let saved = try store.replace(loaded, expectedRevision: 13, to: url)
        let storedJSON = try JSONSerialization.jsonObject(with: Data(contentsOf: url)) as? [String: Any]

        XCTAssertEqual(saved.schemaVersion, CompanionConfiguration.schemaVersion)
        XCTAssertEqual(saved.revision, 14)
        XCTAssertNotNil(storedJSON?["stats"])
        XCTAssertEqual(storedJSON?["schemaVersion"] as? Int, CompanionConfiguration.schemaVersion)
    }

    func testBridgeDescriptorRejectsExpiredOrWrongSchemaValues() {
        XCTAssertFalse(StatsBridgeDescriptor(
            schemaVersion: 1,
            port: 43123,
            capability: "capability",
            expiresAt: Date.now.addingTimeInterval(60)
        ).isUsable())
        XCTAssertFalse(StatsBridgeDescriptor(
            schemaVersion: 2,
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
        XCTAssertTrue(StatsProvider.hypixel.requiresAPIKey)
        XCTAssertTrue(StatsProvider.urchin.requiresAPIKey)
        XCTAssertFalse(StatsProvider.seraph.requiresAPIKey)
    }

    func testStatsBridgeRejectsInvalidOrOversizedRosterRequests() {
        let valid = StatsBridgeRosterRequest(
            schemaVersion: StatsBridgeRosterRequest.schemaVersion,
            matchID: "bedwars-match_1",
            gameMode: .fours,
            players: [StatsBridgeRosterMember(name: "Player_1", uuid: nil)]
        )
        XCTAssertTrue(valid.isValid)
        XCTAssertTrue(StatsBridgeRosterRequest(
            schemaVersion: StatsBridgeRosterRequest.schemaVersion,
            matchID: "bedwars-without-visible-mode",
            gameMode: nil,
            players: valid.players
        ).isValid)
        let modeOmittedOnWire = try! JSONDecoder().decode(StatsBridgeRosterRequest.self, from: Data("""
        {"schemaVersion":2,"matchID":"bedwars-without-visible-mode","players":[{"name":"Player_1","uuid":null}]}
        """.utf8))
        XCTAssertNil(modeOmittedOnWire.gameMode)
        XCTAssertTrue(modeOmittedOnWire.isValid)
        XCTAssertFalse(StatsBridgeRosterRequest(
            schemaVersion: 1,
            matchID: "bedwars-match_1",
            gameMode: .fours,
            players: valid.players
        ).isValid)
        XCTAssertFalse(StatsBridgeRosterRequest(
            schemaVersion: 2,
            matchID: "not allowed!",
            gameMode: .fours,
            players: valid.players
        ).isValid)
        XCTAssertFalse(StatsBridgeRosterRequest(
            schemaVersion: 2,
            matchID: "bedwars-match_1",
            gameMode: .fours,
            players: Array(repeating: valid.players[0], count: StatsBridgeRosterRequest.maximumMembers + 1)
        ).isValid)
    }

    func testStatsBridgeWritesOnlyEphemeralLocalDescriptor() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
        let descriptorURL = directory.appendingPathComponent("stats-bridge.json")
        defer { try? FileManager.default.removeItem(at: directory) }
        let server = StatsBridgeServer(descriptorURL: descriptorURL)
        let started = expectation(description: "bridge started")
        var result: Result<StatsBridgeDescriptor, Error>?

        server.start {
            result = $0
            started.fulfill()
        }
        wait(for: [started], timeout: 2)

        let descriptor = try result?.get()
        XCTAssertNotNil(descriptor)
        XCTAssertTrue(descriptor?.isUsable() == true)
        XCTAssertFalse(descriptor?.capability.isEmpty == true)
        XCTAssertTrue(FileManager.default.fileExists(atPath: descriptorURL.path))
        let encoded = try String(contentsOf: descriptorURL, encoding: .utf8)
        XCTAssertFalse(encoded.lowercased().contains("api-key"))
        server.stop()
    }

    func testProviderNormalizersReturnOnlyDisplaySafeStatsAndTags() throws {
        let hypixel = Data("""
        {"success":true,"player":{"achievements":{"bedwars_level":120},"stats":{"Bedwars":{"final_kills_bedwars":44,"final_deaths_bedwars":11}}}}
        """.utf8)
        let stats = StatsProviderLookup.parseHypixelStats(hypixel, gameMode: .fours)
        XCTAssertEqual(stats?.stars, 120)
        XCTAssertEqual(stats?.finalKillDeathRatio, 4)
        XCTAssertNil(stats?.modeWinStreak)
        XCTAssertNil(StatsProviderLookup.parseHypixelStats(hypixel, gameMode: nil)?.modeWinStreak)

        let urchin = Data("""
        {"players":{"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa":[{"tag_type":"cheating","reason":"not returned"},{"tag_type":"  ","reason":"ignored"}]}}
        """.utf8)
        XCTAssertEqual(StatsProviderLookup.parseUrchinTags(urchin), [
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa": ["cheating"]
        ])

        let listBasedUrchin = Data("""
        {"players":[
          {"uuid":"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb","tags":[{"tag":"watchlist"}]},
          {"id":"cccccccccccccccccccccccccccccccc","data":[{"type":"suspicious"}]}
        ]}
        """.utf8)
        XCTAssertEqual(StatsProviderLookup.parseUrchinTags(listBasedUrchin), [
            "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb": ["watchlist"],
            "cccccccccccccccccccccccccccccccc": ["suspicious"]
        ])

        let seraph = Data("""
        {"player":{"blacklist":{"reason":"not returned"}}}
        """.utf8)
        XCTAssertEqual(StatsProviderLookup.parseSeraphTags(seraph), ["blacklist"])
        XCTAssertEqual(StatsProviderLookup.parseSeraphTags(Data("{\"player\":{\"blacklist\":null}}".utf8)), [])
    }

    func testProviderRequestsUseFixedHostsAndKeepSecretsOutOfPayloads() throws {
        let hypixel = StatsProviderLookup.hypixelRequest(uuid: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", apiKey: "hypixel-secret")
        XCTAssertEqual(hypixel.url?.host, "api.hypixel.net")
        XCTAssertEqual(hypixel.value(forHTTPHeaderField: "API-Key"), "hypixel-secret")
        XCTAssertFalse(hypixel.url?.absoluteString.contains("hypixel-secret") == true)

        let mojang = StatsProviderLookup.mojangProfileRequest(name: "Player_1")
        XCTAssertEqual(mojang.url?.absoluteString, "https://api.mojang.com/users/profiles/minecraft/Player_1")
        XCTAssertNil(mojang.value(forHTTPHeaderField: "ApiKey"))

        let urchin = StatsProviderLookup.urchinRequest(uuids: ["aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"], apiKey: "urchin-secret")
        XCTAssertEqual(urchin.url?.host, "api.urchin.gg")
        XCTAssertEqual(urchin.httpMethod, "POST")
        XCTAssertEqual(urchin.value(forHTTPHeaderField: "X-API-Key"), "urchin-secret")
        XCTAssertEqual(try JSONSerialization.jsonObject(with: urchin.httpBody ?? Data()) as? [String: [String]], [
            "uuids": ["aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"]
        ])
        XCTAssertFalse(String(data: urchin.httpBody ?? Data(), encoding: .utf8)?.contains("urchin-secret") == true)

        let seraph = StatsProviderLookup.seraphRequest(uuid: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
        XCTAssertEqual(seraph.url?.absoluteString, "https://developer-api.seraph.si/player/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
        XCTAssertNil(seraph.value(forHTTPHeaderField: "X-API-Key"))
        XCTAssertNil(seraph.value(forHTTPHeaderField: "Authorization"))
    }

    func testProviderLookupCachesOneNormalizedResultPerMatch() throws {
        let transport = FakeStatsTransport()
        let lookup = StatsProviderLookup(
            keychainStore: FakeStatsKeyStore([
                StatsProvider.hypixel.keychainAccount: "hypixel-secret",
                StatsProvider.urchin.keychainAccount: "urchin-secret"
            ]),
            transport: transport,
            hypixelCache: HypixelStatsCache(url: FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString))
        )
        let roster = StatsBridgeRosterRequest(
            schemaVersion: 2,
            matchID: "match_cache_test",
            gameMode: .fours,
            players: [StatsBridgeRosterMember(name: "PlayerOne", uuid: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")]
        )
        let first = expectation(description: "first response")
        var firstResponse: StatsBridgeRosterResponse?
        lookup.lookup(roster) {
            firstResponse = $0
            first.fulfill()
        }
        wait(for: [first], timeout: 2)
        XCTAssertEqual(firstResponse?.availability, .ready)
        XCTAssertEqual(firstResponse?.players.first?.stars, 100)
        XCTAssertEqual(firstResponse?.players.first?.modeWinStreak, 9)
        XCTAssertEqual(firstResponse?.players.first?.communityTags, [
            StatsBridgeCommunityTag(source: "seraph", label: "blacklist"),
            StatsBridgeCommunityTag(source: "urchin", label: "watchlist")
        ])
        XCTAssertEqual(transport.requestCount, 3)

        let second = expectation(description: "cached response")
        lookup.lookup(roster) { _ in second.fulfill() }
        wait(for: [second], timeout: 2)
        XCTAssertEqual(transport.requestCount, 3)
    }

    func testCompanionUsesTheSameApplicationSupportDirectoryAsTheMod() {
        XCTAssertEqual(CompanionPaths.applicationSupportDirectory.lastPathComponent, "HypixelLegitils")
        XCTAssertEqual(CompanionPaths.configurationURL.lastPathComponent, "config.json")
        XCTAssertEqual(CompanionPaths.runtimeStatusURL.lastPathComponent, "runtime-status.json")
        XCTAssertEqual(CompanionPaths.hypixelStatsCacheURL.lastPathComponent, "hypixel-stats-cache.json")
        XCTAssertEqual(CompanionPaths.loaderRuntimeDirectory.lastPathComponent, "runtime")
    }

    func testRuntimeInstallerCopiesBundledArtifactsAndGeneratesARealArgument() throws {
        let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
        defer { try? FileManager.default.removeItem(at: root) }
        let bundledLoader = root.appendingPathComponent("bundled-loader.jar")
        let bundledMod = root.appendingPathComponent("bundled-mod.jar")
        let runtimeDirectory = root.appendingPathComponent("installed/runtime", isDirectory: true)
        try FileManager.default.createDirectory(at: root, withIntermediateDirectories: true)
        try Data("loader".utf8).write(to: bundledLoader)
        try Data("mod".utf8).write(to: bundledMod)

        let installer = RuntimeInstaller(
            bundledLoaderURL: bundledLoader,
            bundledModURL: bundledMod,
            runtimeDirectory: runtimeDirectory
        )
        let installed = try installer.prepare()

        XCTAssertEqual(try Data(contentsOf: installed.loaderURL), Data("loader".utf8))
        XCTAssertEqual(try Data(contentsOf: installed.modURL), Data("mod".utf8))
        XCTAssertTrue(installed.loaderURL.path.hasPrefix(runtimeDirectory.path))
        XCTAssertFalse(installed.jvmArgument.contains("/absolute/path"))
        XCTAssertEqual(installed.jvmArgument, "-javaagent:\(installed.loaderURL.path)=\(installed.configurationURL.path)")

        let configuration = try JSONSerialization.jsonObject(with: Data(contentsOf: installed.configurationURL)) as? [String: Any]
        XCTAssertEqual(configuration?["modJar"] as? String, installed.modURL.path)
        XCTAssertEqual(configuration?["mixinConfig"] as? String, "mixins.hypixellegitils.json")

        try Data("updated loader".utf8).write(to: bundledLoader)
        let updated = try installer.prepare()
        XCTAssertEqual(updated, installed)
        XCTAssertEqual(try Data(contentsOf: updated.loaderURL), Data("updated loader".utf8))
    }

    func testRuntimeInstallerFailsClearlyWhenTheBundledLoaderIsMissing() {
        let installer = RuntimeInstaller(
            bundledLoaderURL: nil,
            bundledModURL: URL(fileURLWithPath: "/missing/mod.jar"),
            runtimeDirectory: FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        )
        XCTAssertThrowsError(try installer.prepare()) { error in
            XCTAssertEqual(error as? RuntimeInstallerError, .missingBundledLoader)
        }
    }

    func testLunarBakeCacheFindsAndMovesAllNestedBakeArchivesWithoutHashAssumptions() throws {
        let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
        defer { try? FileManager.default.removeItem(at: root) }
        let first = root.appendingPathComponent("one/two/bake.zip")
        let second = root.appendingPathComponent("different-hash/bake.zip")
        try FileManager.default.createDirectory(at: first.deletingLastPathComponent(), withIntermediateDirectories: true)
        try FileManager.default.createDirectory(at: second.deletingLastPathComponent(), withIntermediateDirectories: true)
        try Data(repeating: 1, count: 12).write(to: first)
        try Data(repeating: 2, count: 34).write(to: second)
        try Data("not a bake archive".utf8).write(to: root.appendingPathComponent("ignore.zip"))

        var moved: [URL] = []
        let service = LunarBakeCacheService(cacheRoot: root) { url in
            moved.append(url)
            try FileManager.default.removeItem(at: url)
        }
        let found = try service.scan()
        XCTAssertEqual(found.count, 2)
        XCTAssertEqual(try service.moveToTrash(found), 2)
        XCTAssertEqual(moved.count, 2)
        XCTAssertTrue(moved.allSatisfy { $0.lastPathComponent == "bake.zip" })
        XCTAssertTrue(try service.scan().isEmpty)
    }

    func testHypixelStatsCachePersistsNormalizedValuesForTwentyFourHours() {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
        let url = directory.appendingPathComponent("hypixel-stats-cache.json")
        defer { try? FileManager.default.removeItem(at: directory) }
        var current = Date(timeIntervalSince1970: 1_700_000_000)
        let stats = StatsProviderLookup.HypixelStats(stars: 120, finalKillDeathRatio: 3.5, modeWinStreak: 7)

        HypixelStatsCache(url: url, now: { current }).store(stats, for: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", gameMode: .fours)
        XCTAssertEqual(
            HypixelStatsCache(url: url, now: { current }).stats(for: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", gameMode: .fours),
            stats
        )

        XCTAssertNil(HypixelStatsCache(url: url, now: { current }).stats(for: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", gameMode: nil))

        current = current.addingTimeInterval(HypixelStatsCache.lifetime + 1)
        XCTAssertNil(HypixelStatsCache(url: url, now: { current }).stats(for: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", gameMode: .fours))
    }

    func testProviderLookupUsesFreshPersistentHypixelCacheBeforeNetwork() {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let current = Date(timeIntervalSince1970: 1_700_000_000)
        let cache = HypixelStatsCache(
            url: directory.appendingPathComponent("hypixel-stats-cache.json"),
            now: { current }
        )
        cache.store(
            StatsProviderLookup.HypixelStats(stars: 130, finalKillDeathRatio: 6.5, modeWinStreak: 11),
            for: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            gameMode: .fours
        )
        let transport = FakeStatsTransport()
        let lookup = StatsProviderLookup(
            keychainStore: FakeStatsKeyStore([StatsProvider.hypixel.keychainAccount: "hypixel-secret"]),
            transport: transport,
            hypixelCache: cache
        )
        let response = expectation(description: "cached response")
        var result: StatsBridgeRosterResponse?

        lookup.lookup(StatsBridgeRosterRequest(
            schemaVersion: 2,
            matchID: "persistent_cache_test",
            gameMode: .fours,
            players: [StatsBridgeRosterMember(name: "PlayerOne", uuid: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")]
        )) {
            result = $0
            response.fulfill()
        }
        wait(for: [response], timeout: 2)

        XCTAssertEqual(transport.requestCount, 1)
        XCTAssertEqual(result?.players.first?.stars, 130)
        XCTAssertEqual(result?.players.first?.finalKillDeathRatio, 6.5)
        XCTAssertEqual(result?.players.first?.modeWinStreak, 11)
    }

    func testProviderLookupResolvesVisiblePregameChatterBeforeFetchingStats() {
        let transport = FakeStatsTransport()
        let lookup = StatsProviderLookup(
            keychainStore: FakeStatsKeyStore([StatsProvider.hypixel.keychainAccount: "hypixel-secret"]),
            transport: transport,
            hypixelCache: HypixelStatsCache(url: FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString))
        )
        let response = expectation(description: "pregame chatter response")
        var result: StatsBridgeRosterResponse?

        lookup.lookup(StatsBridgeRosterRequest(
            schemaVersion: 2,
            matchID: "pregame_PlayerOne",
            gameMode: .fours,
            players: [StatsBridgeRosterMember(name: "PlayerOne", uuid: nil)]
        )) {
            result = $0
            response.fulfill()
        }
        wait(for: [response], timeout: 2)

        XCTAssertEqual(transport.requestCount, 3)
        XCTAssertEqual(result?.players.first?.nickStatus, .known)
        XCTAssertEqual(result?.players.first?.stars, 100)
    }

    func testManualStatsLookupForcesAProviderRequestAndReturnsOnlySafeDiagnostics() {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let cache = HypixelStatsCache(url: directory.appendingPathComponent("hypixel-stats-cache.json"))
        cache.store(
            StatsProviderLookup.HypixelStats(stars: 999, finalKillDeathRatio: 99, modeWinStreak: 99),
            for: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            gameMode: .fours
        )
        let transport = ManualFailureTransport()
        let lookup = StatsProviderLookup(
            keychainStore: FakeStatsKeyStore([StatsProvider.hypixel.keychainAccount: "hypixel-secret"]),
            transport: transport,
            hypixelCache: cache
        )
        let response = expectation(description: "manual failure response")
        var result: StatsBridgeRosterResponse?

        lookup.lookup(StatsBridgeRosterRequest(
            schemaVersion: 2,
            matchID: "manual_1_1",
            gameMode: .fours,
            players: [StatsBridgeRosterMember(name: "PlayerOne", uuid: nil)]
        )) {
            result = $0
            response.fulfill()
        }
        wait(for: [response], timeout: 2)

        XCTAssertEqual(transport.requestCount, 3)
        XCTAssertNil(result?.players.first?.stars)
        XCTAssertEqual(result?.players.first?.communityTags, [
            StatsBridgeCommunityTag(source: "diagnostic", label: "Hypixel: authorization failed"),
            StatsBridgeCommunityTag(source: "diagnostic", label: "Urchin: API key unavailable"),
            StatsBridgeCommunityTag(source: "provider", label: "Seraph: no blacklist")
        ])
    }

    func testManualStatsLookupReportsEachSuccessfulProviderSeparately() {
        let lookup = StatsProviderLookup(
            keychainStore: FakeStatsKeyStore([
                StatsProvider.hypixel.keychainAccount: "hypixel-secret",
                StatsProvider.urchin.keychainAccount: "urchin-secret"
            ]),
            transport: FakeStatsTransport(),
            hypixelCache: HypixelStatsCache(url: FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString))
        )
        let response = expectation(description: "manual provider status response")
        var result: StatsBridgeRosterResponse?

        lookup.lookup(StatsBridgeRosterRequest(
            schemaVersion: 2,
            matchID: "manual_1_2",
            gameMode: .fours,
            players: [StatsBridgeRosterMember(name: "PlayerOne", uuid: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")]
        )) {
            result = $0
            response.fulfill()
        }
        wait(for: [response], timeout: 2)

        XCTAssertEqual(result?.players.first?.communityTags, [
            StatsBridgeCommunityTag(source: "provider", label: "Hypixel: OK"),
            StatsBridgeCommunityTag(source: "provider", label: "Seraph: blacklist"),
            StatsBridgeCommunityTag(source: "provider", label: "Urchin: watchlist"),
            StatsBridgeCommunityTag(source: "seraph", label: "blacklist"),
            StatsBridgeCommunityTag(source: "urchin", label: "watchlist")
        ])
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

private let schemaFourFixture = Data("""
{
  "schemaVersion": 4,
  "revision": 13,
  "enabledDetectors": ["NO_BREAK_DELAY"],
  "sensitivity": "balanced",
  "notifications": {"chat": true, "overlay": true, "sound": true},
  "cooldowns": {"normalMillis": 1000, "airStallMillis": 30000},
  "debug": false,
  "markers": {"enabled": true, "threshold": 2},
  "nickDetection": {"enabled": true},
  "partyDetection": {"enabled": true}
}
""".utf8)

private struct FakeStatsKeyStore: StatsProviderKeyReading {
    let values: [String: String]

    init(_ values: [String: String]) {
        self.values = values
    }

    func readSecret(account: String) throws -> String? {
        values[account]
    }
}

private final class FakeStatsTransport: StatsHTTPTransport {
    private(set) var requestCount = 0

    func load(_ request: URLRequest, completion: @escaping (Result<(Data, HTTPURLResponse), Error>) -> Void) {
        requestCount += 1
        let body: Data
        switch request.url?.host {
        case "api.mojang.com":
            body = Data("{\"id\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\",\"name\":\"PlayerOne\"}".utf8)
        case "api.hypixel.net":
            body = Data("""
            {"success":true,"player":{"achievements":{"bedwars_level":100},"stats":{"Bedwars":{"final_kills_bedwars":6,"final_deaths_bedwars":2,"four_four_winstreak":9}}}}
            """.utf8)
        case "api.urchin.gg":
            body = Data("""
            {"players":{"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa":[{"tag_type":"watchlist"}]}}
            """.utf8)
        case "developer-api.seraph.si":
            body = Data("""
            {"player":{"blacklist":{"reason":"not returned"}}}
            """.utf8)
        default:
            body = Data("{\"success\":true}".utf8)
        }
        completion(.success((body, HTTPURLResponse(
            url: request.url!,
            statusCode: 200,
            httpVersion: "HTTP/1.1",
            headerFields: nil
        )!)))
    }
}

private final class ManualFailureTransport: StatsHTTPTransport {
    private(set) var requestCount = 0

    func load(_ request: URLRequest, completion: @escaping (Result<(Data, HTTPURLResponse), Error>) -> Void) {
        requestCount += 1
        let status = request.url?.host == "api.hypixel.net" ? 401 : 200
        let body = request.url?.host == "api.mojang.com"
            ? Data("{\"id\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\",\"name\":\"PlayerOne\"}".utf8)
            : Data("{}".utf8)
        completion(.success((body, HTTPURLResponse(
            url: request.url!,
            statusCode: status,
            httpVersion: "HTTP/1.1",
            headerFields: nil
        )!)))
    }
}
