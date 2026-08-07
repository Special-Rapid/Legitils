package com.snkisk.hypixellegitils.config;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DetectorSettingsServiceTest {
    @Test
    public void savesOneDetectorForTheNextStartWithoutChangingStartupConfig() throws Exception {
        Path directory = Files.createTempDirectory("legitils-detector-command-test");
        Path path = directory.resolve("config.json");
        try {
            LegitilsConfig startup = LegitilsConfig.defaults();
            DetectorSettingsService service = new DetectorSettingsService(new LegitilsConfigStore(), path, startup);
            DetectorSettingsService.Update update = service.setEnabled(DetectorId.NO_SLOW, true);
            assertTrue(update.changed);
            assertFalse(startup.isDetectorEnabled(DetectorId.NO_SLOW));
            assertTrue(update.config.isDetectorEnabled(DetectorId.NO_SLOW));
            assertEquals(1L, update.config.revision);
            assertTrue(new LegitilsConfigStore().load(path).config.isDetectorEnabled(DetectorId.NO_SLOW));
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void allToggleUsesOnlyImplementedDetectorsAndNoOpDoesNotIncreaseRevision() throws Exception {
        Path directory = Files.createTempDirectory("legitils-detector-command-test");
        Path path = directory.resolve("config.json");
        try {
            DetectorSettingsService service = new DetectorSettingsService(new LegitilsConfigStore(), path, LegitilsConfig.defaults());
            DetectorSettingsService.Update enabled = service.setAllEnabled(true);
            assertEquals(8, enabled.config.enabledDetectors.size());
            assertTrue(enabled.config.enabledDetectors.contains(DetectorId.COMBAT_DESYNC));
            assertTrue(enabled.config.enabledDetectors.contains(DetectorId.AIR_STALL));
            assertTrue(enabled.config.enabledDetectors.contains(DetectorId.NO_BREAK_DELAY));
            DetectorSettingsService.Update unchanged = service.setAllEnabled(true);
            assertFalse(unchanged.changed);
            assertEquals(enabled.config.revision, unchanged.config.revision);
            DetectorSettingsService.Update disabled = service.setAllEnabled(false);
            assertTrue(disabled.changed);
            assertEquals(0, disabled.config.enabledDetectors.size());
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void phaseFourDetectorCanBeSavedForTheNextStart() throws Exception {
        Path directory = Files.createTempDirectory("legitils-detector-command-test");
        try {
            DetectorSettingsService service = new DetectorSettingsService(new LegitilsConfigStore(), directory.resolve("config.json"), LegitilsConfig.defaults());
            DetectorSettingsService.Update update = service.setEnabled(DetectorId.AIR_STALL, true);
            assertTrue(update.changed);
            assertTrue(update.config.isDetectorEnabled(DetectorId.AIR_STALL));
        } finally {
            Files.deleteIfExists(directory.resolve("config.json"));
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void markerSettingsPersistAsCurrentSchemaWithoutChangingDetectorSet() throws Exception {
        Path directory = Files.createTempDirectory("legitils-marker-command-test");
        Path path = directory.resolve("config.json");
        try {
            DetectorSettingsService service = new DetectorSettingsService(new LegitilsConfigStore(), path, LegitilsConfig.defaults());
            DetectorSettingsService.Update enabled = service.setMarkerEnabled(true);
            assertTrue(enabled.changed);
            assertTrue(enabled.config.markerSettings.enabled);
            DetectorSettingsService.Update threshold = service.setMarkerThreshold(4);
            assertTrue(threshold.changed);
            assertEquals(4, threshold.config.markerSettings.threshold);
            assertEquals(0, threshold.config.enabledDetectors.size());
            assertEquals(4, new LegitilsConfigStore().load(path).config.markerSettings.threshold);
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void markerUpdateMigratesLegacyConfigurationToCurrentSchema() throws Exception {
        Path directory = Files.createTempDirectory("legitils-legacy-marker-test");
        Path path = directory.resolve("config.json");
        try {
            LegitilsConfig defaults = LegitilsConfig.defaults();
            LegitilsConfig legacy = new LegitilsConfig(
                LegitilsConfig.LEGACY_SCHEMA_VERSION, 0L, defaults.enabledDetectors, defaults.sensitivity,
                defaults.notifications, defaults.normalCooldownMillis, defaults.airStallCooldownMillis,
                defaults.debugEnabled, defaults.markerSettings
            );
            DetectorSettingsService service = new DetectorSettingsService(new LegitilsConfigStore(), path, legacy);
            DetectorSettingsService.Update update = service.setMarkerEnabled(true);
            assertEquals(LegitilsConfig.SCHEMA_VERSION, update.config.schemaVersion);
            assertTrue(update.config.markerSettings.enabled);
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void nickDetectionTogglePersistsAndMigratesMarkerConfigurationToSchemaThree() throws Exception {
        Path directory = Files.createTempDirectory("legitils-nick-detect-command-test");
        Path path = directory.resolve("config.json");
        try {
            LegitilsConfig defaults = LegitilsConfig.defaults();
            LegitilsConfig schemaTwo = new LegitilsConfig(
                LegitilsConfig.MARKER_SCHEMA_VERSION, 3L, defaults.enabledDetectors, defaults.sensitivity,
                defaults.notifications, defaults.normalCooldownMillis, defaults.airStallCooldownMillis,
                defaults.debugEnabled, defaults.markerSettings
            );
            LegitilsConfigStore store = new LegitilsConfigStore();
            store.writeAtomically(path, schemaTwo);
            DetectorSettingsService service = new DetectorSettingsService(store, path, schemaTwo);
            DetectorSettingsService.Update disabled = service.setNickDetectionEnabled(false);
            assertTrue(disabled.changed);
            assertEquals(LegitilsConfig.SCHEMA_VERSION, disabled.config.schemaVersion);
            assertFalse(disabled.config.nickDetectionSettings.enabled);
            assertEquals(4L, disabled.config.revision);
            assertFalse(store.load(path).config.nickDetectionSettings.enabled);
            assertFalse(service.setNickDetectionEnabled(false).changed);
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void nickDetectionToggleMigratesSchemaOneConfigurationToSchemaThree() throws Exception {
        Path directory = Files.createTempDirectory("legitils-legacy-nick-detect-command-test");
        Path path = directory.resolve("config.json");
        try {
            LegitilsConfig defaults = LegitilsConfig.defaults();
            LegitilsConfig legacy = new LegitilsConfig(
                LegitilsConfig.LEGACY_SCHEMA_VERSION, 8L, defaults.enabledDetectors, defaults.sensitivity,
                defaults.notifications, defaults.normalCooldownMillis, defaults.airStallCooldownMillis,
                defaults.debugEnabled
            );
            LegitilsConfigStore store = new LegitilsConfigStore();
            store.writeAtomically(path, legacy);
            DetectorSettingsService service = new DetectorSettingsService(store, path, legacy);
            DetectorSettingsService.Update disabled = service.setNickDetectionEnabled(false);
            assertEquals(LegitilsConfig.SCHEMA_VERSION, disabled.config.schemaVersion);
            assertEquals(9L, disabled.config.revision);
            assertFalse(store.load(path).config.nickDetectionSettings.enabled);
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void notificationChannelTogglePersistsWithoutChangingOtherChannels() throws Exception {
        Path directory = Files.createTempDirectory("legitils-notify-command-test");
        Path path = directory.resolve("config.json");
        try {
            DetectorSettingsService service = new DetectorSettingsService(new LegitilsConfigStore(), path, LegitilsConfig.defaults());
            DetectorSettingsService.Update actionBarEnabled = service.setNotificationEnabled(NotificationChannel.ACTION_BAR, true);
            assertTrue(actionBarEnabled.changed);
            assertTrue(actionBarEnabled.config.notifications.chatEnabled);
            assertTrue(actionBarEnabled.config.notifications.overlayEnabled);
            assertFalse(actionBarEnabled.config.notifications.soundEnabled);
            assertTrue(new LegitilsConfigStore().load(path).config.notifications.overlayEnabled);
            assertFalse(service.setNotificationEnabled(NotificationChannel.ACTION_BAR, true).changed);
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void developerSelfDetectionTogglePersistsAsTheLocalDebugSetting() throws Exception {
        Path directory = Files.createTempDirectory("legitils-dev-command-test");
        Path path = directory.resolve("config.json");
        try {
            DetectorSettingsService service = new DetectorSettingsService(new LegitilsConfigStore(), path, LegitilsConfig.defaults());
            DetectorSettingsService.Update enabled = service.setDebugEnabled(true);
            assertTrue(enabled.changed);
            assertTrue(enabled.config.debugEnabled);
            assertTrue(new LegitilsConfigStore().load(path).config.debugEnabled);
            assertFalse(service.setDebugEnabled(true).changed);
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void statsPresentationSettingsPersistWithoutResettingOtherFields() throws Exception {
        Path directory = Files.createTempDirectory("legitils-stats-command-test");
        Path path = directory.resolve("config.json");
        try {
            LegitilsConfig defaults = LegitilsConfig.defaults();
            DetectorSettingsService service = new DetectorSettingsService(new LegitilsConfigStore(), path, defaults);
            StatsSettings changed = new StatsSettings(true, false, true, false, true, false);
            DetectorSettingsService.Update update = service.setStatsSettings(changed);
            assertTrue(update.changed);
            assertFalse(update.config.statsSettings.tabEnabled);
            assertFalse(update.config.statsSettings.fkdrEnabled);
            assertFalse(update.config.statsSettings.chatEnabled);
            assertEquals(defaults.enabledDetectors, update.config.enabledDetectors);
            assertFalse(service.setStatsSettings(changed).changed);
            assertFalse(new LegitilsConfigStore().load(path).config.statsSettings.tabEnabled);

            DetectorSettingsService.Update detectorUpdate = service.setEnabled(DetectorId.NO_SLOW, true);
            assertFalse(detectorUpdate.config.statsSettings.tabEnabled);
            assertFalse(detectorUpdate.config.statsSettings.chatEnabled);
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void acceptsOnlyNewerExternalConfigurationAndKeepsKnownGoodStateOnInvalidInput() throws Exception {
        Path directory = Files.createTempDirectory("legitils-external-config-reload-test");
        Path path = directory.resolve("config.json");
        try {
            LegitilsConfigStore store = new LegitilsConfigStore();
            LegitilsConfig startup = LegitilsConfig.defaults();
            DetectorSettingsService service = new DetectorSettingsService(store, path, startup);
            LegitilsConfig external = new LegitilsConfig(
                LegitilsConfig.SCHEMA_VERSION, 1L, java.util.EnumSet.of(DetectorId.NO_BREAK_DELAY), startup.sensitivity,
                startup.notifications, startup.normalCooldownMillis, startup.airStallCooldownMillis,
                startup.debugEnabled, startup.markerSettings, startup.nickDetectionSettings, startup.partyDetectionSettings
            );
            store.writeAtomically(path, external);

            DetectorSettingsService.ExternalReload applied = service.reloadFromDiskIfNewer();
            assertTrue(applied.applied);
            assertTrue(applied.config.isDetectorEnabled(DetectorId.NO_BREAK_DELAY));
            assertEquals(1L, service.savedConfig().revision);
            assertFalse(service.reloadFromDiskIfNewer().applied);

            Files.write(path, "{invalid".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            DetectorSettingsService.ExternalReload invalid = service.reloadFromDiskIfNewer();
            assertFalse(invalid.applied);
            assertTrue(invalid.invalidOrMissing);
            assertEquals(1L, service.savedConfig().revision);
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(directory);
        }
    }
}
