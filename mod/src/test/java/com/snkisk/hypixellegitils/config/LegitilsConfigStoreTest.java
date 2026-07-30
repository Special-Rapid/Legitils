package com.snkisk.hypixellegitils.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LegitilsConfigStoreTest {
    @Test
    public void missingConfigurationUsesSafeDefaults() throws Exception {
        Path directory = Files.createTempDirectory("legitils-config-test");
        try {
            ConfigLoadResult result = new LegitilsConfigStore().load(directory.resolve("config.json"));
            assertTrue(result.usedDefaults);
            assertEquals(0, result.config.enabledDetectors.size());
            assertTrue(!result.config.enabledDetectors.contains(DetectorId.BED_NUKE));
            assertTrue(!result.config.enabledDetectors.contains(DetectorId.AUTO_BLOCK));
            assertTrue(!result.config.enabledDetectors.contains(DetectorId.NO_SLOW));
            assertTrue(!result.config.enabledDetectors.contains(DetectorId.LEGIT_SCAFFOLD));
            assertTrue(!result.config.enabledDetectors.contains(DetectorId.KILL_AURA));
            assertTrue(!result.config.enabledDetectors.contains(DetectorId.COMBAT_DESYNC));
            assertTrue(!result.config.enabledDetectors.contains(DetectorId.AIR_STALL));
            assertEquals(1000L, result.config.normalCooldownMillis);
            assertEquals(30000L, result.config.airStallCooldownMillis);
            assertTrue(result.config.notifications.chatEnabled);
            assertFalse(result.config.notifications.overlayEnabled);
            assertTrue(result.config.partyDetectionSettings.enabled);
        } finally {
            Files.deleteIfExists(directory.resolve("config.json"));
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void atomicRoundTripPreservesValidatedConfiguration() throws Exception {
        Path directory = Files.createTempDirectory("legitils-config-test");
        Path path = directory.resolve("config.json");
        try {
            LegitilsConfig expected = new LegitilsConfig(
                1, 9L, EnumSet.of(DetectorId.NO_SLOW), SensitivityPreset.CONSERVATIVE,
                new NotificationSettings(false, true, false), 1200L, 45000L, true
            );
            LegitilsConfigStore store = new LegitilsConfigStore();
            store.writeAtomically(path, expected);
            ConfigLoadResult actual = store.load(path);
            assertFalse(actual.usedDefaults);
            assertEquals(9L, actual.config.revision);
            assertEquals(EnumSet.of(DetectorId.NO_SLOW), actual.config.enabledDetectors);
            assertEquals(SensitivityPreset.CONSERVATIVE, actual.config.sensitivity);
            assertEquals(1200L, actual.config.normalCooldownMillis);
            assertTrue(actual.config.debugEnabled);
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void malformedConfigurationDoesNotPreventStartup() throws Exception {
        Path directory = Files.createTempDirectory("legitils-config-test");
        Path path = directory.resolve("config.json");
        try {
            Files.write(path, "{\"schemaVersion\":2}".getBytes("UTF-8"));
            ConfigLoadResult result = new LegitilsConfigStore().load(path);
            assertTrue(result.usedDefaults);
            assertEquals(0, result.config.enabledDetectors.size());
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void releasedPhaseFourIdentifiersAreRetained() throws Exception {
        Path directory = Files.createTempDirectory("legitils-config-test");
        Path path = directory.resolve("config.json");
        try {
            Files.write(path, ("{\"schemaVersion\":1,\"revision\":23,\"enabledDetectors\":[\"NO_SLOW\",\"BED_NUKE\",\"COMBAT_DESYNC\",\"AIR_STALL\"],\"sensitivity\":\"conservative\",\"notifications\":{\"chat\":false,\"overlay\":true,\"sound\":false},\"cooldowns\":{\"normalMillis\":1700,\"airStallMillis\":45000},\"debug\":true}").getBytes("UTF-8"));
            ConfigLoadResult result = new LegitilsConfigStore().load(path);
            assertFalse(result.usedDefaults);
            assertEquals(EnumSet.of(DetectorId.NO_SLOW, DetectorId.BED_NUKE, DetectorId.COMBAT_DESYNC, DetectorId.AIR_STALL), result.config.enabledDetectors);
            assertEquals(23L, result.config.revision);
            assertEquals(SensitivityPreset.CONSERVATIVE, result.config.sensitivity);
            assertFalse(result.config.notifications.chatEnabled);
            assertEquals(1700L, result.config.normalCooldownMillis);
            assertTrue(result.config.debugEnabled);
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void schemaTwoConfigurationKeepsNickDetectionEnabledForCompatibility() throws Exception {
        Path directory = Files.createTempDirectory("legitils-schema-two-nick-test");
        Path path = directory.resolve("config.json");
        try {
            Files.write(path, ("{\"schemaVersion\":2,\"revision\":4,\"enabledDetectors\":[],\"sensitivity\":\"balanced\",\"notifications\":{\"chat\":true,\"overlay\":false,\"sound\":false},\"cooldowns\":{\"normalMillis\":1000,\"airStallMillis\":30000},\"debug\":false,\"markers\":{\"enabled\":false,\"threshold\":3}}").getBytes("UTF-8"));
            ConfigLoadResult result = new LegitilsConfigStore().load(path);
            assertFalse(result.usedDefaults);
            assertTrue(result.config.nickDetectionSettings.enabled);
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void schemaThreeConfigurationKeepsPartyDetectionEnabledForCompatibility() throws Exception {
        Path directory = Files.createTempDirectory("legitils-schema-three-party-test");
        Path path = directory.resolve("config.json");
        try {
            Files.write(path, ("{\"schemaVersion\":3,\"revision\":4,\"enabledDetectors\":[],\"sensitivity\":\"balanced\",\"notifications\":{\"chat\":true,\"overlay\":false,\"sound\":false},\"cooldowns\":{\"normalMillis\":1000,\"airStallMillis\":30000},\"debug\":false,\"markers\":{\"enabled\":false,\"threshold\":3},\"nickDetection\":{\"enabled\":true}}").getBytes("UTF-8"));
            ConfigLoadResult result = new LegitilsConfigStore().load(path);
            assertFalse(result.usedDefaults);
            assertTrue(result.config.partyDetectionSettings.enabled);
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void partyDetectionTogglePersistsAsSchemaFourAndMigratesSchemaThree() throws Exception {
        Path directory = Files.createTempDirectory("legitils-party-detect-command-test");
        Path path = directory.resolve("config.json");
        try {
            LegitilsConfig defaults = LegitilsConfig.defaults();
            LegitilsConfig schemaThree = new LegitilsConfig(
                LegitilsConfig.NICK_DETECTION_SCHEMA_VERSION, 3L, defaults.enabledDetectors, defaults.sensitivity,
                defaults.notifications, defaults.normalCooldownMillis, defaults.airStallCooldownMillis,
                defaults.debugEnabled, defaults.markerSettings, defaults.nickDetectionSettings
            );
            LegitilsConfigStore store = new LegitilsConfigStore();
            store.writeAtomically(path, schemaThree);
            DetectorSettingsService service = new DetectorSettingsService(store, path, schemaThree);
            DetectorSettingsService.Update disabled = service.setPartyDetectionEnabled(false);
            assertTrue(disabled.changed);
            assertEquals(LegitilsConfig.SCHEMA_VERSION, disabled.config.schemaVersion);
            assertFalse(disabled.config.partyDetectionSettings.enabled);
            assertFalse(store.load(path).config.partyDetectionSettings.enabled);
            assertFalse(service.setPartyDetectionEnabled(false).changed);
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(directory);
        }
    }
}
