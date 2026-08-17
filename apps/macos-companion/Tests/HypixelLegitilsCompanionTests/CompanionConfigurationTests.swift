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

    func testSchemaFiveConfigurationAddsDisabledNametagFKDR() throws {
        let configuration = try JSONDecoder().decode(CompanionConfiguration.self, from: schemaFiveFixture)

        XCTAssertEqual(configuration.schemaVersion, CompanionConfiguration.schemaVersion)
        XCTAssertFalse(configuration.stats.nametag)
        XCTAssertEqual(configuration.stats.nametagFkdrThreshold, 1)
    }

    func testSchemaSixConfigurationAddsDisabledTabSorting() throws {
        let configuration = try JSONDecoder().decode(CompanionConfiguration.self, from: schemaSixFixture)

        XCTAssertEqual(configuration.schemaVersion, CompanionConfiguration.schemaVersion)
        XCTAssertFalse(configuration.stats.tabTeamSorting)
        XCTAssertFalse(configuration.stats.tabPlayerSorting)
    }

    func testSchemaSevenConfigurationKeepsAutomaticWhoEnabled() throws {
        let configuration = try JSONDecoder().decode(CompanionConfiguration.self, from: schemaSevenFixture)

        XCTAssertEqual(configuration.schemaVersion, CompanionConfiguration.schemaVersion)
        XCTAssertTrue(configuration.stats.autoWho)
    }

    func testSchemaFourConfigurationWritesCurrentSchemaAfterSaving() throws {
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
        XCTAssertTrue(StatsProvider.seraph.requiresAPIKey)
    }

    func testSignedKeychainNamespaceIsStableAndDoesNotReuseLegacyEntries() {
        XCTAssertEqual(KeychainStore.service, "com.snkisk.hypixellegitils.companion.v2")
        XCTAssertEqual(KeychainStore.legacyService, "com.snkisk.hypixellegitils.companion")
        XCTAssertNotEqual(KeychainStore.service, KeychainStore.legacyService)
    }

    func testLegacyKeyReentryAppearsOnlyUntilEachNewSignedNamespaceKeyIsSaved() {
        XCTAssertTrue(KeychainStore.needsLegacyReentry(for: .hypixel, hasCurrent: false, hasLegacy: true))
        XCTAssertTrue(KeychainStore.needsLegacyReentry(for: .urchin, hasCurrent: false, hasLegacy: true))
        XCTAssertFalse(KeychainStore.needsLegacyReentry(for: .hypixel, hasCurrent: true, hasLegacy: true))
        XCTAssertFalse(KeychainStore.needsLegacyReentry(for: .urchin, hasCurrent: false, hasLegacy: false))
        XCTAssertTrue(KeychainStore.needsLegacyReentry(for: .seraph, hasCurrent: false, hasLegacy: true))
    }

    func testProviderKeyChangeEventContainsOnlyBoundedProviderAndSequence() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
        let url = directory.appendingPathComponent("provider-key-change-events.json")
        defer { try? FileManager.default.removeItem(at: directory) }

        let store = ProviderKeyChangeEventStore(url: url)
        for index in 1...17 {
            try store.recordSavedKey(for: index.isMultiple(of: 2) ? .urchin : .hypixel)
        }
        try store.recordSavedKey(for: .seraph)

        let object = try JSONSerialization.jsonObject(with: Data(contentsOf: url)) as? [String: Any]
        let events = object?["events"] as? [[String: Any]]
        XCTAssertEqual(object?["schemaVersion"] as? Int, 1)
        XCTAssertEqual(events?.count, 16)
        XCTAssertEqual(events?.map { $0["sequence"] as? Int }, Array(2...17))
        XCTAssertEqual(events?.first?["provider"] as? String, "urchin")
        XCTAssertEqual(events?.last?["provider"] as? String, "hypixel")
        XCTAssertEqual(Set(object?.keys ?? Dictionary<String, Any>().keys), Set(["schemaVersion", "events"]))
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

    func testStatsBridgeRefreshesAnExpiredDescriptorWhileItsListenerRemainsLive() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
        let descriptorURL = directory.appendingPathComponent("stats-bridge.json")
        defer { try? FileManager.default.removeItem(at: directory) }
        let server = StatsBridgeServer(descriptorURL: descriptorURL, descriptorLifetime: 0.03)
        let initialStarted = expectation(description: "initial bridge started")
        var initial: StatsBridgeDescriptor?
        server.start { result in
            initial = try? result.get()
            initialStarted.fulfill()
        }
        wait(for: [initialStarted], timeout: 2)
        Thread.sleep(forTimeInterval: 0.06)

        let refreshedStarted = expectation(description: "expired descriptor refreshed")
        var refreshed: StatsBridgeDescriptor?
        server.start { result in
            refreshed = try? result.get()
            refreshedStarted.fulfill()
        }
        wait(for: [refreshedStarted], timeout: 2)

        XCTAssertNotNil(initial)
        XCTAssertTrue(refreshed?.isUsable() == true)
        XCTAssertNotEqual(initial?.capability, refreshed?.capability)
        server.stop()
    }

    func testHypixelKeyValidationUsesOneFixedRequestAndReturnsOnlySafeStatus() {
        let valid = HypixelKeyValidationTransport(statusCode: 200, body: Data("{\"success\":true}".utf8))
        let invalid = HypixelKeyValidationTransport(statusCode: 403, body: Data("{\"cause\":\"expired\"}".utf8))
        let keyStore = FakeStatsKeyStore([StatsProvider.hypixel.keychainAccount: "hypixel-secret"])
        let validLookup = StatsProviderLookup(keychainStore: keyStore, transport: valid)
        let invalidLookup = StatsProviderLookup(keychainStore: keyStore, transport: invalid)
        let validResult = expectation(description: "valid result")
        let invalidResult = expectation(description: "invalid result")

        validLookup.validateHypixelAPIKey {
            XCTAssertEqual($0, .valid)
            validResult.fulfill()
        }
        invalidLookup.validateHypixelAPIKey {
            XCTAssertEqual($0, .invalid)
            invalidResult.fulfill()
        }
        wait(for: [validResult, invalidResult], timeout: 1)
        XCTAssertEqual(valid.requests.count, 1)
        XCTAssertEqual(valid.requests.first?.url?.absoluteString, "https://api.hypixel.net/v2/counts")
        XCTAssertEqual(valid.requests.first?.value(forHTTPHeaderField: "API-Key"), "hypixel-secret")
        XCTAssertFalse(valid.requests.first?.httpBody?.isEmpty == false)
    }

    func testStatsBridgeKeyValidationReturnsOnlySafeStatusBehindCapability() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
        let descriptorURL = directory.appendingPathComponent("stats-bridge.json")
        defer { try? FileManager.default.removeItem(at: directory) }
        let server = StatsBridgeServer(
            descriptorURL: descriptorURL,
            hypixelKeyValidation: { completion in completion(.invalid) }
        )
        let started = expectation(description: "bridge started")
        var descriptor: StatsBridgeDescriptor?
        server.start { result in
            descriptor = try? result.get()
            started.fulfill()
        }
        wait(for: [started], timeout: 2)
        guard let descriptor else {
            XCTFail("missing descriptor")
            return
        }

        var request = URLRequest(url: URL(string: "http://127.0.0.1:\(descriptor.port)/v1/hypixel-key-validation")!)
        request.httpMethod = "POST"
        request.httpBody = Data("{\"schemaVersion\":1}".utf8)
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(descriptor.capability, forHTTPHeaderField: "X-Legitils-Capability")
        let completed = expectation(description: "key validation response")
        URLSession.shared.dataTask(with: request) { data, response, error in
            XCTAssertNil(error)
            XCTAssertEqual((response as? HTTPURLResponse)?.statusCode, 200)
            XCTAssertEqual(
                try? JSONDecoder().decode(HypixelAPIKeyValidationResponse.self, from: data ?? Data()),
                HypixelAPIKeyValidationResponse(schemaVersion: 1, status: .invalid)
            )
            completed.fulfill()
        }.resume()
        wait(for: [completed], timeout: 2)
        server.stop()
    }

    func testHypixelKeyValidationDoesNotRequestOrReportMissingKey() {
        let transport = HypixelKeyValidationTransport(statusCode: 403, body: Data())
        let lookup = StatsProviderLookup(keychainStore: FakeStatsKeyStore([:]), transport: transport)
        let completed = expectation(description: "missing key")

        lookup.validateHypixelAPIKey {
            XCTAssertEqual($0, .unavailable)
            completed.fulfill()
        }
        wait(for: [completed], timeout: 1)
        XCTAssertTrue(transport.requests.isEmpty)
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
        {"players":{"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa":[{"tag_type":"blatantcheater","reason":"vape v4\\n- Added by @hexze 4 months ago"},{"tag_type":"  ","reason":"ignored"},{"tag_type":"unknown_provider_tag","reason":"discarded"}]}}
        """.utf8)
        XCTAssertEqual(StatsProviderLookup.parseUrchinTags(urchin), [
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa": [
                StatsProviderLookup.ProviderTag(label: "Blatant Cheater", tooltip: "vape v4\n- Added by @hexze 4 months ago")
            ]
        ])

        let listBasedUrchin = Data("""
        {"players":[
          {"uuid":"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb","tags":[{"tag":"legitsniper","tooltip":"queued on stream"}]},
          {"id":"cccccccccccccccccccccccccccccccc","data":[{"type":"caution"}]}
        ]}
        """.utf8)
        XCTAssertEqual(StatsProviderLookup.parseUrchinTags(listBasedUrchin), [
            "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb": [
                StatsProviderLookup.ProviderTag(label: "Legit Sniper", tooltip: "queued on stream")
            ],
            "cccccccccccccccccccccccccccccccc": [
                StatsProviderLookup.ProviderTag(label: "Caution", tooltip: nil)
            ]
        ])

        let seraph = Data("""
        {"data":{"blacklist":{"report_type":"cheating_closet","tooltip":"vape v4 (legitscaff)\\n- Added by @hexze 4 months ago"},"bot":{"tagged":true,"tooltip":"automated account"}}}
        """.utf8)
        XCTAssertEqual(StatsProviderLookup.parseSeraphTags(seraph), [
            StatsProviderLookup.ProviderTag(label: "Bot", tooltip: "automated account"),
            StatsProviderLookup.ProviderTag(label: "Closet Cheating", tooltip: "vape v4 (legitscaff)\n- Added by @hexze 4 months ago")
        ])
        let publicDeveloperEnvelope = Data("""
        {"id":"opaque","type":"player","payload":{"blacklist":{"report_type":"cheating_blatant","tooltip":"autoblock"}}}
        """.utf8)
        XCTAssertEqual(StatsProviderLookup.parseSeraphTags(publicDeveloperEnvelope), [
            StatsProviderLookup.ProviderTag(label: "Blatant Cheating", tooltip: "autoblock")
        ])
        let verifiedSeraph = Data("""
        {"data":{"verified":true,"blacklist":{"report_type":"cheating_closet","tooltip":"confirmed by Seraph"},"bot":{"tagged":true,"tooltip":"ignored while confirmed"}}}
        """.utf8)
        XCTAssertEqual(StatsProviderLookup.parseSeraphTags(verifiedSeraph), [
            StatsProviderLookup.ProviderTag(label: "Confirmed Closet Cheating", tooltip: "confirmed by Seraph")
        ])
        let unverifiedSeraph = Data("""
        {"data":{"verified":false,"blacklist":{"report_type":"cheating_closet","tooltip":"ordinary tag"}}}
        """.utf8)
        XCTAssertEqual(StatsProviderLookup.parseSeraphTags(unverifiedSeraph), [
            StatsProviderLookup.ProviderTag(label: "Closet Cheating", tooltip: "ordinary tag")
        ])
        XCTAssertEqual(StatsProviderLookup.parseSeraphTags(Data("{\"player\":{\"blacklist\":null}}".utf8)), [])
    }

    func testUnknownSuccessfulProviderEnvelopesAreNotCacheableNoTagResults() {
        XCTAssertNil(StatsProviderLookup.parseUrchinTagsIfValid(Data("{\"players\":42}".utf8)))
        XCTAssertNil(StatsProviderLookup.parseUrchinTagsIfValid(Data("{\"unexpected\":[]}".utf8)))
        XCTAssertNil(StatsProviderLookup.parseSeraphTagsIfValid(Data("{\"payload\":{\"unexpected\":true}}".utf8)))
        XCTAssertNil(StatsProviderLookup.parseSeraphTagsIfValid(Data("{\"unexpected\":true}".utf8)))
        XCTAssertEqual(StatsProviderLookup.parseUrchinTagsIfValid(Data("{\"players\":{}}".utf8)), [:])
        XCTAssertEqual(StatsProviderLookup.parseSeraphTagsIfValid(Data("{\"data\":{\"blacklist\":null}}".utf8)), [])
    }

    func testProviderTooltipIsBoundedByUtf16CodeUnitsBeforeCrossingTheBridgeOrCache() {
        let emoji = String(repeating: "😀", count: 193)
        let urchin = Data("""
        {"players":{"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa":[{"tag_type":"legitsniper","tooltip":"\(emoji)"}]}}
        """.utf8)
        let tag = StatsProviderLookup.parseUrchinTagsIfValid(urchin)?["aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"]?.first
        XCTAssertEqual(tag?.tooltip, String(repeating: "😀", count: 192))
        XCTAssertEqual(tag?.tooltip?.utf16.count, 384)

        let cache = temporaryCommunityTagCache()
        cache.store([StatsProviderLookup.ProviderTag(label: "Legit Sniper", tooltip: emoji)], for: .urchin, uuid: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
        XCTAssertNil(cache.tags(for: .urchin, uuid: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"))
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

        let seraph = StatsProviderLookup.seraphRequest(uuid: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", apiKey: "seraph-secret")
        XCTAssertEqual(seraph.url?.host, "api.seraph.si")
        XCTAssertEqual(seraph.url?.path, "/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa/blacklist")
        XCTAssertEqual(URLComponents(url: seraph.url!, resolvingAgainstBaseURL: false)?.queryItems, [
            URLQueryItem(name: "key", value: "seraph-secret")
        ])
        XCTAssertNil(seraph.value(forHTTPHeaderField: "X-API-Key"))
        XCTAssertNil(seraph.value(forHTTPHeaderField: "Authorization"))
        XCTAssertNil(seraph.httpBody)
    }

    func testProviderLookupCachesOneNormalizedResultPerMatch() throws {
        let transport = FakeStatsTransport()
        let lookup = StatsProviderLookup(
            keychainStore: FakeStatsKeyStore([
                StatsProvider.hypixel.keychainAccount: "hypixel-secret",
                StatsProvider.urchin.keychainAccount: "urchin-secret",
                StatsProvider.seraph.keychainAccount: "seraph-secret"
            ]),
            transport: transport,
            hypixelCache: HypixelStatsCache(url: FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)),
            communityTagCache: temporaryCommunityTagCache()
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
            StatsBridgeCommunityTag(source: "seraph", label: "Closet Cheating", tooltip: "vape v4 (legitscaff)\n- Added by @hexze 4 months ago"),
            StatsBridgeCommunityTag(source: "urchin", label: "Legit Sniper", tooltip: "queued on stream")
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
        XCTAssertEqual(CompanionPaths.communityTagCacheURL.lastPathComponent, "community-tag-cache.json")
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
        XCTAssertEqual(installed.modFingerprint.count, 64)

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

    func testLunarBakeInvalidationRunsOnlyForANewModAndDefersWhileMinecraftGameWindowExists() throws {
        let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
        defer { try? FileManager.default.removeItem(at: root) }
        let archive = root.appendingPathComponent("hash/bake.zip")
        let fingerprint = root.appendingPathComponent("state/fingerprint.txt")
        try FileManager.default.createDirectory(at: archive.deletingLastPathComponent(), withIntermediateDirectories: true)
        try Data("first archive".utf8).write(to: archive)

        var moved: [URL] = []
        let cache = LunarBakeCacheService(cacheRoot: root) { url in
            moved.append(url)
            try FileManager.default.removeItem(at: url)
        }
        let invalidator = LunarBakeCacheInvalidator(fingerprintURL: fingerprint, cache: cache, minecraftGameWindowExists: { false })
        XCTAssertEqual(try invalidator.invalidateIfNeeded(for: "mod-one"), .movedToTrash(1))
        XCTAssertEqual(moved.count, 1)
        XCTAssertEqual(moved.first?.lastPathComponent, "bake.zip")
        XCTAssertFalse(FileManager.default.fileExists(atPath: archive.path))
        XCTAssertEqual(try String(contentsOf: fingerprint), "mod-one")

        XCTAssertEqual(try invalidator.invalidateIfNeeded(for: "mod-empty"), .movedToTrash(0))
        XCTAssertEqual(try String(contentsOf: fingerprint), "mod-empty")

        try Data("new archive without MOD update".utf8).write(to: archive)
        XCTAssertEqual(try invalidator.invalidateIfNeeded(for: "mod-empty"), .unchanged)
        XCTAssertTrue(FileManager.default.fileExists(atPath: archive.path))

        let deferredFingerprint = root.appendingPathComponent("state/deferred.txt")
        let deferred = LunarBakeCacheInvalidator(fingerprintURL: deferredFingerprint, cache: cache, minecraftGameWindowExists: { true })
        XCTAssertEqual(try deferred.invalidateIfNeeded(for: "mod-two"), .deferredWhileMinecraftGameWindowExists)
        XCTAssertFalse(FileManager.default.fileExists(atPath: deferredFingerprint.path))
        XCTAssertTrue(FileManager.default.fileExists(atPath: archive.path))
    }

    func testMinecraftGameWindowClassifierIgnoresLunarLauncherHomeButProtectsGames() {
        XCTAssertFalse(LunarBakeCacheInvalidator.isMinecraftGameWindow(ownerName: "Lunar Client", title: "Home - Lunar Client"))
        XCTAssertFalse(LunarBakeCacheInvalidator.isMinecraftGameWindow(ownerName: "Lunar Client", title: ""))
        XCTAssertFalse(LunarBakeCacheInvalidator.isMinecraftGameWindow(ownerName: "Lunar Client", title: "Lunar Client"))
        XCTAssertTrue(LunarBakeCacheInvalidator.isMinecraftGameWindow(ownerName: "Lunar Client", title: "Lunar Client 1.8.9 (dev)"))
        XCTAssertTrue(LunarBakeCacheInvalidator.isMinecraftGameWindow(ownerName: "Minecraft", title: ""))
        XCTAssertTrue(LunarBakeCacheInvalidator.isMinecraftGameWindow(ownerName: "Badlion Client", title: ""))
        XCTAssertFalse(LunarBakeCacheInvalidator.isMinecraftGameWindow(ownerName: "Safari", title: "Safari"))
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

    func testCommunityTagCachePersistsNormalizedProviderTagsForTwentyFourHours() {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
        let url = directory.appendingPathComponent("community-tag-cache.json")
        defer { try? FileManager.default.removeItem(at: directory) }
        var current = Date(timeIntervalSince1970: 1_700_000_000)
        let seraph = [StatsProviderLookup.ProviderTag(label: "Closet Cheating", tooltip: "safe reason")]
        let urchin = [StatsProviderLookup.ProviderTag(label: "Legit Sniper", tooltip: nil)]

        let cache = CommunityTagCache(url: url, now: { current })
        cache.store(seraph, for: .seraph, uuid: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        cache.store(urchin, for: .urchin, uuid: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
        XCTAssertEqual(
            CommunityTagCache(url: url, now: { current }).tags(for: .seraph, uuid: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"),
            seraph
        )
        XCTAssertEqual(
            CommunityTagCache(url: url, now: { current }).tags(for: .urchin, uuid: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"),
            urchin
        )

        current = current.addingTimeInterval(CommunityTagCache.lifetime + 1)
        XCTAssertNil(CommunityTagCache(url: url, now: { current }).tags(for: .seraph, uuid: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"))
    }

    func testUrchinKeyReplacementInvalidatesOnlyUrchinNormalizedTags() {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
        let url = directory.appendingPathComponent("community-tag-cache.json")
        defer { try? FileManager.default.removeItem(at: directory) }
        let cache = CommunityTagCache(url: url)
        let uuid = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        cache.store([StatsProviderLookup.ProviderTag(label: "Closet Cheating", tooltip: "safe")], for: .seraph, uuid: uuid)
        cache.store([StatsProviderLookup.ProviderTag(label: "Legit Sniper", tooltip: "safe")], for: .urchin, uuid: uuid)

        cache.removeAll(for: .urchin)

        XCTAssertEqual(cache.tags(for: .seraph, uuid: uuid), [StatsProviderLookup.ProviderTag(label: "Closet Cheating", tooltip: "safe")])
        XCTAssertNil(cache.tags(for: .urchin, uuid: uuid))
    }

    func testCommunityTagCacheIgnoresMalformedOrUnsafeStoredData() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
        let url = directory.appendingPathComponent("community-tag-cache.json")
        defer { try? FileManager.default.removeItem(at: directory) }
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        try Data("""
        {"schemaVersion":1,"entries":{"seraph:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa":{"fetchedAtMillis":1700000000000,"tags":[{"label":"arbitrary","tooltip":"unsafe"}]}}}
        """.utf8).write(to: url)

        XCTAssertNil(CommunityTagCache(url: url, now: { Date(timeIntervalSince1970: 1_700_000_001) })
            .tags(for: .seraph, uuid: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"))
    }

    func testCommunityTagCacheInvalidatesUnauthenticatedSeraphEntriesAfterAuthenticatedEndpointMigration() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
        let url = directory.appendingPathComponent("community-tag-cache.json")
        defer { try? FileManager.default.removeItem(at: directory) }
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        try Data("""
        {"schemaVersion":2,"entries":{
          "seraph:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa":{"fetchedAtMillis":1700000000000,"tags":[]},
          "urchin:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa":{"fetchedAtMillis":1700000000000,"tags":[{"label":"Legit Sniper"}]}
        }}
        """.utf8).write(to: url)

        let cache = CommunityTagCache(url: url, now: { Date(timeIntervalSince1970: 1_700_000_001) })
        XCTAssertNil(cache.tags(for: .seraph, uuid: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"))
        XCTAssertEqual(cache.tags(for: .urchin, uuid: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"), [
            StatsProviderLookup.ProviderTag(label: "Legit Sniper", tooltip: nil)
        ])
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
            hypixelCache: cache,
            communityTagCache: temporaryCommunityTagCache()
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

        XCTAssertEqual(transport.requestCount, 0)
        XCTAssertEqual(result?.players.first?.stars, 130)
        XCTAssertEqual(result?.players.first?.finalKillDeathRatio, 6.5)
        XCTAssertEqual(result?.players.first?.modeWinStreak, 11)
    }

    func testWhoRefreshRevalidatesHypixelWithTheCurrentKeyButKeepsCommunityTagCaches() {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let cache = HypixelStatsCache(url: directory.appendingPathComponent("hypixel-stats-cache.json"))
        let tagCache = CommunityTagCache(url: directory.appendingPathComponent("community-tag-cache.json"))
        cache.store(
            StatsProviderLookup.HypixelStats(stars: 130, finalKillDeathRatio: 6.5, modeWinStreak: 11),
            for: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            gameMode: .fours
        )
        tagCache.store(
            [StatsProviderLookup.ProviderTag(label: "Closet Cheating", tooltip: "cached Seraph")],
            for: .seraph,
            uuid: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        )
        tagCache.store(
            [StatsProviderLookup.ProviderTag(label: "Legit Sniper", tooltip: "cached Urchin")],
            for: .urchin,
            uuid: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        )
        XCTAssertEqual(tagCache.tags(for: .seraph, uuid: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"), [
            StatsProviderLookup.ProviderTag(label: "Closet Cheating", tooltip: "cached Seraph")
        ])
        XCTAssertEqual(tagCache.tags(for: .urchin, uuid: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"), [
            StatsProviderLookup.ProviderTag(label: "Legit Sniper", tooltip: "cached Urchin")
        ])
        let transport = FakeStatsTransport()
        let lookup = StatsProviderLookup(
            keychainStore: FakeStatsKeyStore([
                StatsProvider.hypixel.keychainAccount: "hypixel-secret",
                StatsProvider.urchin.keychainAccount: "urchin-secret",
                StatsProvider.seraph.keychainAccount: "seraph-secret"
            ]),
            transport: transport,
            hypixelCache: cache,
            communityTagCache: tagCache
        )
        let response = expectation(description: "who refresh response")
        var result: StatsBridgeRosterResponse?

        lookup.lookup(StatsBridgeRosterRequest(
            schemaVersion: 2,
            matchID: "who_1_1",
            gameMode: .fours,
            players: [StatsBridgeRosterMember(name: "PlayerOne", uuid: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")]
        )) {
            result = $0
            response.fulfill()
        }
        wait(for: [response], timeout: 2)

        XCTAssertEqual(transport.requestCount, 1)
        XCTAssertEqual(result?.players.first?.stars, 100)
        XCTAssertEqual(result?.players.first?.communityTags, [
            StatsBridgeCommunityTag(source: "seraph", label: "Closet Cheating", tooltip: "cached Seraph"),
            StatsBridgeCommunityTag(source: "urchin", label: "Legit Sniper", tooltip: "cached Urchin")
        ])
    }

    func testProviderKeyReplacementDropsOnlyTheAffectedNormalizedCaches() {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let uuid = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        let hypixelCache = HypixelStatsCache(url: directory.appendingPathComponent("hypixel-stats-cache.json"))
        let tagCache = CommunityTagCache(url: directory.appendingPathComponent("community-tag-cache.json"))
        hypixelCache.store(StatsProviderLookup.HypixelStats(stars: 100, finalKillDeathRatio: 2, modeWinStreak: 3), for: uuid, gameMode: .fours)
        tagCache.store([StatsProviderLookup.ProviderTag(label: "Closet Cheating", tooltip: "safe")], for: .seraph, uuid: uuid)
        tagCache.store([StatsProviderLookup.ProviderTag(label: "Legit Sniper", tooltip: "safe")], for: .urchin, uuid: uuid)
        let lookup = StatsProviderLookup(
            keychainStore: FakeStatsKeyStore([:]),
            hypixelCache: hypixelCache,
            communityTagCache: tagCache
        )

        lookup.invalidateCachedResults(for: .hypixel)
        XCTAssertNil(hypixelCache.stats(for: uuid, gameMode: .fours))
        XCTAssertNotNil(tagCache.tags(for: .seraph, uuid: uuid))
        XCTAssertNotNil(tagCache.tags(for: .urchin, uuid: uuid))

        lookup.invalidateCachedResults(for: .urchin)
        XCTAssertNotNil(tagCache.tags(for: .seraph, uuid: uuid))
        XCTAssertNil(tagCache.tags(for: .urchin, uuid: uuid))

        lookup.invalidateCachedResults(for: .seraph)
        XCTAssertNil(tagCache.tags(for: .seraph, uuid: uuid))
    }

    func testProviderLookupResolvesVisiblePregameChatterBeforeFetchingStats() {
        let transport = FakeStatsTransport()
        let lookup = StatsProviderLookup(
            keychainStore: FakeStatsKeyStore([StatsProvider.hypixel.keychainAccount: "hypixel-secret"]),
            transport: transport,
            hypixelCache: HypixelStatsCache(url: FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)),
            communityTagCache: temporaryCommunityTagCache()
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

        XCTAssertEqual(transport.requestCount, 2)
        XCTAssertEqual(result?.players.first?.nickStatus, .known)
        XCTAssertEqual(result?.players.first?.stars, 100)
    }

    func testPregameChatterWithARealMojangProfileButNoHypixelProfileIsNicked() {
        let transport = PregameNickTransport(mojangStatusCode: 200)
        let lookup = StatsProviderLookup(
            keychainStore: FakeStatsKeyStore([StatsProvider.hypixel.keychainAccount: "hypixel-secret"]),
            transport: transport,
            hypixelCache: HypixelStatsCache(url: FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)),
            communityTagCache: temporaryCommunityTagCache()
        )
        let response = expectation(description: "pregame Nick response")
        var result: StatsBridgeRosterResponse?

        lookup.lookup(StatsBridgeRosterRequest(
            schemaVersion: 2,
            matchID: "pregame_NickAlias",
            gameMode: .fours,
            players: [StatsBridgeRosterMember(name: "NickAlias", uuid: nil)]
        )) {
            result = $0
            response.fulfill()
        }
        wait(for: [response], timeout: 2)

        XCTAssertEqual(transport.requestCount, 2)
        XCTAssertEqual(result?.players.first?.nickStatus, .nicked)
        XCTAssertNil(result?.players.first?.stars)
    }

    func testPregameChatterMissingFromMojangIsNickedWithoutProviderLookups() {
        let transport = PregameNickTransport(mojangStatusCode: 404)
        let lookup = StatsProviderLookup(
            keychainStore: FakeStatsKeyStore([StatsProvider.hypixel.keychainAccount: "hypixel-secret"]),
            transport: transport,
            hypixelCache: HypixelStatsCache(url: FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)),
            communityTagCache: temporaryCommunityTagCache()
        )
        let response = expectation(description: "missing pregame Nick response")
        var result: StatsBridgeRosterResponse?

        lookup.lookup(StatsBridgeRosterRequest(
            schemaVersion: 2,
            matchID: "pregame_MissingAlias",
            gameMode: .fours,
            players: [StatsBridgeRosterMember(name: "MissingAlias", uuid: nil)]
        )) {
            result = $0
            response.fulfill()
        }
        wait(for: [response], timeout: 2)

        XCTAssertEqual(transport.requestCount, 1)
        XCTAssertEqual(result?.players.first?.nickStatus, .nicked)
    }

    func testPregameHypixelFailuresNeverClassifyAResolvedNameAsNicked() {
        for failure in PregameHypixelFailure.allCases {
            let transport = PregameHypixelFailureTransport(failure: failure)
            let lookup = StatsProviderLookup(
                keychainStore: FakeStatsKeyStore([StatsProvider.hypixel.keychainAccount: "hypixel-secret"]),
                transport: transport,
                hypixelCache: HypixelStatsCache(url: FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)),
                communityTagCache: temporaryCommunityTagCache()
            )
            let response = expectation(description: "pregame non-Nick \(failure)")
            var result: StatsBridgeRosterResponse?

            lookup.lookup(StatsBridgeRosterRequest(
                schemaVersion: 2,
                matchID: "pregame_\(failure)",
                gameMode: .fours,
                players: [StatsBridgeRosterMember(name: "KnownAlias", uuid: nil)]
            )) {
                result = $0
                response.fulfill()
            }
            wait(for: [response], timeout: 2)

            XCTAssertEqual(transport.requestCount, 2)
            XCTAssertNotEqual(result?.players.first?.nickStatus, .nicked)
        }
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
            hypixelCache: cache,
            communityTagCache: temporaryCommunityTagCache()
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

        XCTAssertEqual(transport.requestCount, 2)
        XCTAssertNil(result?.players.first?.stars)
        XCTAssertEqual(result?.players.first?.communityTags, [
            StatsBridgeCommunityTag(source: "diagnostic", label: "Hypixel: authorization failed"),
            StatsBridgeCommunityTag(source: "diagnostic", label: "Seraph: API key unavailable"),
            StatsBridgeCommunityTag(source: "diagnostic", label: "Urchin: API key unavailable")
        ])
    }

    func testManualStatsLookupReportsEachSuccessfulProviderSeparately() {
        let lookup = StatsProviderLookup(
            keychainStore: FakeStatsKeyStore([
                StatsProvider.hypixel.keychainAccount: "hypixel-secret",
                StatsProvider.urchin.keychainAccount: "urchin-secret",
                StatsProvider.seraph.keychainAccount: "seraph-secret"
            ]),
            transport: FakeStatsTransport(),
            hypixelCache: HypixelStatsCache(url: FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)),
            communityTagCache: temporaryCommunityTagCache()
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
            StatsBridgeCommunityTag(source: "provider", label: "Seraph: Closet Cheating"),
            StatsBridgeCommunityTag(source: "provider", label: "Urchin: Legit Sniper"),
            StatsBridgeCommunityTag(source: "seraph", label: "Closet Cheating", tooltip: "vape v4 (legitscaff)\n- Added by @hexze 4 months ago"),
            StatsBridgeCommunityTag(source: "urchin", label: "Legit Sniper", tooltip: "queued on stream")
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

private let schemaFiveFixture = Data("""
{
  "schemaVersion": 5,
  "revision": 14,
  "enabledDetectors": [],
  "sensitivity": "balanced",
  "notifications": {"chat": true, "overlay": false, "sound": false},
  "cooldowns": {"normalMillis": 1000, "airStallMillis": 30000},
  "debug": false,
  "markers": {"enabled": true, "threshold": 2},
  "nickDetection": {"enabled": true},
  "partyDetection": {"enabled": true},
  "stats": {"enabled": true, "tab": true, "stars": true, "fkdr": true, "winStreak": true, "chat": true}
}
""".utf8)

private let schemaSixFixture = Data("""
{
  "schemaVersion": 6,
  "revision": 15,
  "enabledDetectors": [],
  "sensitivity": "balanced",
  "notifications": {"chat": true, "overlay": false, "sound": false},
  "cooldowns": {"normalMillis": 1000, "airStallMillis": 30000},
  "debug": false,
  "markers": {"enabled": true, "threshold": 2},
  "nickDetection": {"enabled": true},
  "partyDetection": {"enabled": true},
  "stats": {"enabled": true, "tab": true, "stars": true, "fkdr": true, "winStreak": true, "chat": true, "nametag": false, "nametagFkdrThreshold": 1}
}
""".utf8)

private let schemaSevenFixture = Data("""
{
  "schemaVersion": 7,
  "revision": 16,
  "enabledDetectors": [],
  "sensitivity": "balanced",
  "notifications": {"chat": true, "overlay": false, "sound": false},
  "cooldowns": {"normalMillis": 1000, "airStallMillis": 30000},
  "debug": false,
  "markers": {"enabled": true, "threshold": 2},
  "nickDetection": {"enabled": true},
  "partyDetection": {"enabled": true},
  "stats": {"enabled": true, "tab": true, "stars": true, "fkdr": true, "winStreak": true, "chat": true, "nametag": false, "nametagFkdrThreshold": 1, "tabTeamSorting": false, "tabPlayerSorting": false}
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

private func temporaryCommunityTagCache() -> CommunityTagCache {
    CommunityTagCache(url: FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString))
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
            {"players":{"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa":[{"tag_type":"legitsniper","tooltip":"queued on stream"}]}}
            """.utf8)
        case "api.seraph.si":
            body = Data("""
            {"player":{"blacklist":{"report_type":"cheating_closet","tooltip":"vape v4 (legitscaff)\\n- Added by @hexze 4 months ago"}}}
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

private final class PregameNickTransport: StatsHTTPTransport {
    private let mojangStatusCode: Int
    private(set) var requestCount = 0

    init(mojangStatusCode: Int) {
        self.mojangStatusCode = mojangStatusCode
    }

    func load(_ request: URLRequest, completion: @escaping (Result<(Data, HTTPURLResponse), Error>) -> Void) {
        requestCount += 1
        let statusCode: Int
        let body: Data
        switch request.url?.host {
        case "api.mojang.com":
            statusCode = mojangStatusCode
            body = Data("{\"id\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\",\"name\":\"NickAlias\"}".utf8)
        case "api.hypixel.net":
            statusCode = 200
            body = Data("{\"success\":true,\"player\":null}".utf8)
        default:
            statusCode = 500
            body = Data()
        }
        completion(.success((body, HTTPURLResponse(
            url: request.url!,
            statusCode: statusCode,
            httpVersion: "HTTP/1.1",
            headerFields: nil
        )!)))
    }
}

private enum PregameHypixelFailure: CaseIterable {
    case transport
    case authorization
    case rateLimited
    case malformed
}

private final class PregameHypixelFailureTransport: StatsHTTPTransport {
    let failure: PregameHypixelFailure
    private(set) var requestCount = 0

    init(failure: PregameHypixelFailure) {
        self.failure = failure
    }

    func load(_ request: URLRequest, completion: @escaping (Result<(Data, HTTPURLResponse), Error>) -> Void) {
        requestCount += 1
        if request.url?.host == "api.hypixel.net", failure == .transport {
            completion(.failure(URLError(.notConnectedToInternet)))
            return
        }
        let statusCode: Int
        let body: Data
        switch request.url?.host {
        case "api.mojang.com":
            statusCode = 200
            body = Data("{\"id\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\",\"name\":\"KnownAlias\"}".utf8)
        case "api.hypixel.net":
            switch failure {
            case .authorization:
                statusCode = 403
                body = Data("{\"success\":false}".utf8)
            case .rateLimited:
                statusCode = 429
                body = Data("{\"success\":false}".utf8)
            case .malformed:
                statusCode = 200
                body = Data("not-json".utf8)
            case .transport:
                statusCode = 500
                body = Data()
            }
        default:
            statusCode = 500
            body = Data()
        }
        completion(.success((body, HTTPURLResponse(
            url: request.url!,
            statusCode: statusCode,
            httpVersion: "HTTP/1.1",
            headerFields: nil
        )!)))
    }
}

private final class HypixelKeyValidationTransport: StatsHTTPTransport {
    let statusCode: Int
    let body: Data
    private(set) var requests: [URLRequest] = []

    init(statusCode: Int, body: Data) {
        self.statusCode = statusCode
        self.body = body
    }

    func load(_ request: URLRequest, completion: @escaping (Result<(Data, HTTPURLResponse), Error>) -> Void) {
        requests.append(request)
        completion(.success((body, HTTPURLResponse(
            url: request.url!,
            statusCode: statusCode,
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
