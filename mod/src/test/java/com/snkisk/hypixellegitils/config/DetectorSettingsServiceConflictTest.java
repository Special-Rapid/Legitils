package com.snkisk.hypixellegitils.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DetectorSettingsServiceConflictTest {
    @Test
    public void externalRevisionChangeIsNotOverwritten() throws Exception {
        Path directory = Files.createTempDirectory("legitils-detector-command-conflict");
        Path path = directory.resolve("config.json");
        try {
            LegitilsConfigStore store = new LegitilsConfigStore();
            LegitilsConfig startup = new LegitilsConfig(
                1, 5L, EnumSet.of(DetectorId.NO_SLOW), SensitivityPreset.BALANCED,
                new NotificationSettings(true, false, false), 1000L, 30000L, false
            );
            store.writeAtomically(path, startup);
            DetectorSettingsService service = new DetectorSettingsService(store, path, startup);
            LegitilsConfig external = new LegitilsConfig(
                1, 6L, EnumSet.of(DetectorId.KILL_AURA), SensitivityPreset.CONSERVATIVE,
                new NotificationSettings(false, true, false), 1200L, 45000L, true
            );
            store.writeAtomically(path, external);
            assertWriteRefused(service, DetectorId.AUTO_BLOCK, true);
            ConfigLoadResult preserved = store.load(path);
            assertEquals(6L, preserved.config.revision);
            assertTrue(preserved.config.isDetectorEnabled(DetectorId.KILL_AURA));
            assertEquals(SensitivityPreset.CONSERVATIVE, preserved.config.sensitivity);
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void sameRevisionExternalEditIsNotOverwrittenOrReportedAsSaved() throws Exception {
        Path directory = Files.createTempDirectory("legitils-detector-command-same-revision");
        Path path = directory.resolve("config.json");
        try {
            LegitilsConfigStore store = new LegitilsConfigStore();
            LegitilsConfig startup = new LegitilsConfig(
                1, 5L, EnumSet.of(DetectorId.NO_SLOW), SensitivityPreset.BALANCED,
                new NotificationSettings(true, false, false), 1000L, 30000L, false
            );
            store.writeAtomically(path, startup);
            DetectorSettingsService service = new DetectorSettingsService(store, path, startup);
            LegitilsConfig external = new LegitilsConfig(
                1, 5L, EnumSet.noneOf(DetectorId.class), SensitivityPreset.CONSERVATIVE,
                new NotificationSettings(false, true, true), 1200L, 45000L, true
            );
            store.writeAtomically(path, external);
            assertWriteRefused(service, DetectorId.NO_SLOW, true);
            assertWriteRefused(service, DetectorId.AUTO_BLOCK, true);
            ConfigLoadResult preserved = store.load(path);
            assertEquals(5L, preserved.config.revision);
            assertEquals(SensitivityPreset.CONSERVATIVE, preserved.config.sensitivity);
            assertTrue(!preserved.config.isDetectorEnabled(DetectorId.NO_SLOW));
            assertEquals(1200L, preserved.config.normalCooldownMillis);
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void invalidExternalConfigurationIsNotOverwritten() throws Exception {
        Path directory = Files.createTempDirectory("legitils-detector-command-conflict");
        Path path = directory.resolve("config.json");
        try {
            LegitilsConfigStore store = new LegitilsConfigStore();
            LegitilsConfig startup = LegitilsConfig.defaults();
            DetectorSettingsService service = new DetectorSettingsService(store, path, startup);
            Files.write(path, "{\"schemaVersion\":2}".getBytes("UTF-8"));
            assertWriteRefused(service, DetectorId.AUTO_BLOCK, true);
            assertEquals("{\"schemaVersion\":2}", new String(Files.readAllBytes(path), "UTF-8"));
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void sameRevisionExternalNickDetectionChangeIsNotOverwritten() throws Exception {
        Path directory = Files.createTempDirectory("legitils-nick-detect-conflict");
        Path path = directory.resolve("config.json");
        try {
            LegitilsConfigStore store = new LegitilsConfigStore();
            LegitilsConfig startup = LegitilsConfig.defaults();
            store.writeAtomically(path, startup);
            DetectorSettingsService service = new DetectorSettingsService(store, path, startup);
            LegitilsConfig external = new LegitilsConfig(
                startup.schemaVersion, startup.revision, startup.enabledDetectors, startup.sensitivity,
                startup.notifications, startup.normalCooldownMillis, startup.airStallCooldownMillis,
                startup.debugEnabled, startup.markerSettings, new NickDetectionSettings(false)
            );
            store.writeAtomically(path, external);
            assertNickWriteRefused(service, false);
            assertTrue(!store.load(path).config.nickDetectionSettings.enabled);
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(directory);
        }
    }

    private static void assertWriteRefused(DetectorSettingsService service, DetectorId detector, boolean enabled) throws Exception {
        try {
            service.setEnabled(detector, enabled);
            fail("expected external config conflict");
        } catch (DetectorSettingsService.ConfigWriteRefusedException expected) {
            // Expected: current on-disk configuration remains the source of truth.
        }
    }

    private static void assertNickWriteRefused(DetectorSettingsService service, boolean enabled) throws Exception {
        try {
            service.setNickDetectionEnabled(enabled);
            fail("expected external config conflict");
        } catch (DetectorSettingsService.ConfigWriteRefusedException expected) {
            // Expected: current on-disk configuration remains the source of truth.
        }
    }
}
