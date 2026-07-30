package com.snkisk.hypixellegitils.observation;

import com.snkisk.hypixellegitils.alert.LocalAlertSink;
import com.snkisk.hypixellegitils.config.DetectorId;
import com.snkisk.hypixellegitils.config.LegitilsConfig;
import com.snkisk.hypixellegitils.config.SensitivityPreset;
import com.snkisk.hypixellegitils.config.MarkerSettings;
import com.snkisk.hypixellegitils.config.MarkerHistoryEntry;
import com.snkisk.hypixellegitils.evidence.Confidence;
import com.snkisk.hypixellegitils.evidence.Evidence;
import com.snkisk.hypixellegitils.evidence.PolicyDecision;
import com.snkisk.hypixellegitils.detection.PlayerSample;
import com.snkisk.hypixellegitils.detection.BedNukeSignalCheck;
import com.snkisk.hypixellegitils.detection.NoBreakDelaySignalCheck;
import com.snkisk.hypixellegitils.alert.AlertPresentation;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.io.IOException;
import java.util.EnumSet;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ObservationCoordinatorTest {
    @Test
    public void suppressesEvidenceUntilAWorldTransitionHasAStableObservation() {
        LegitilsConfig config = noSlowEnabledConfig();
        ObservationCoordinator coordinator = new ObservationCoordinator(config, new LocalAlertSink(config.notifications));
        UUID player = UUID.randomUUID();
        Evidence evidence = new Evidence(DetectorId.NO_SLOW, player, Confidence.MEDIUM, 0L, "test observation");
        coordinator.onWorldLoading();
        PolicyDecision duringTransition = coordinator.submit(evidence, 1L, true);
        assertFalse(duringTransition.shouldAlert);
        assertEquals("world-transition", duringTransition.reason);
        coordinator.observePlayer(player, 2L);
        assertTrue(coordinator.submit(evidence, 2L, true).shouldAlert);
    }

    @Test
    public void runtimeDetectorSetAppliesImmediatelyAndResetsTimerProgress() {
        LegitilsConfig timer = timerEnabledConfig();
        ObservationCoordinator coordinator = new ObservationCoordinator(timer, new LocalAlertSink(timer.notifications));
        UUID player = UUID.randomUUID();
        assertTrue(coordinator.statusText().contains("1/8"));
        for (long tick = 0L; tick <= 20L; tick++) {
            coordinator.beginObservationFrame(false);
            coordinator.observe(timerSample(player, tick));
            assertFalse(coordinator.onClientTick(tick * 50L).alert);
        }
        LegitilsConfig timerAndNoSlow = new LegitilsConfig(
            timer.schemaVersion, timer.revision + 1L, EnumSet.of(DetectorId.AIR_STALL, DetectorId.NO_SLOW),
            timer.sensitivity, timer.notifications, timer.normalCooldownMillis, timer.airStallCooldownMillis, timer.debugEnabled
        );
        coordinator.applyRuntimeDetectorConfig(timerAndNoSlow);
        assertTrue(coordinator.statusText().contains("2/8"));
        for (long tick = 21L; tick <= 60L; tick++) {
            coordinator.beginObservationFrame(false);
            coordinator.observe(timerSample(player, tick));
            assertFalse(coordinator.onClientTick(tick * 50L).alert);
        }
        coordinator.beginObservationFrame(false);
        coordinator.observe(timerSample(player, 61L));
        assertTrue(coordinator.onClientTick(3050L).chatText != null);
    }

    @Test
    public void runtimeDetectorSetRejectsNonDetectorChanges() {
        LegitilsConfig timer = timerEnabledConfig();
        ObservationCoordinator coordinator = new ObservationCoordinator(timer, new LocalAlertSink(timer.notifications));
        LegitilsConfig invalid = new LegitilsConfig(
            timer.schemaVersion, timer.revision + 1L, timer.enabledDetectors, SensitivityPreset.CONSERVATIVE,
            timer.notifications, timer.normalCooldownMillis, timer.airStallCooldownMillis, timer.debugEnabled
        );
        try {
            coordinator.applyRuntimeDetectorConfig(invalid);
            fail("expected non-detector runtime update rejection");
        } catch (IllegalArgumentException expected) {
            assertTrue(coordinator.statusText().contains("1/8"));
        }
    }

    @Test
    public void runtimeNotificationSettingsApplyImmediatelyWithoutResettingDetectors() {
        LegitilsConfig config = noSlowEnabledConfig();
        LocalAlertSink sink = new LocalAlertSink(config.notifications);
        ObservationCoordinator coordinator = new ObservationCoordinator(config, sink);
        LegitilsConfig actionBarEnabled = new LegitilsConfig(
            config.schemaVersion, config.revision + 1L, config.enabledDetectors, config.sensitivity,
            new com.snkisk.hypixellegitils.config.NotificationSettings(true, true, false),
            config.normalCooldownMillis, config.airStallCooldownMillis, config.debugEnabled,
            config.markerSettings, config.nickDetectionSettings
        );
        coordinator.applyRuntimeNotificationConfig(actionBarEnabled);
        UUID player = UUID.randomUUID();
        coordinator.observePlayer(player, 0L);
        coordinator.submit(new Evidence(DetectorId.NO_SLOW, player, Confidence.HIGH, 0L, "test"), 0L, true);
        assertTrue(sink.presentation(1L).alert);
        assertTrue(sink.presentation(1L).actionBarText != null);
    }

    @Test
    public void developmentSettingKeepsLaterRuntimeSettingsApplicable() {
        LegitilsConfig config = noSlowEnabledConfig();
        ObservationCoordinator coordinator = new ObservationCoordinator(config, new LocalAlertSink(config.notifications));
        LegitilsConfig nickDisabled = new LegitilsConfig(
            config.schemaVersion, config.revision + 1L, config.enabledDetectors, config.sensitivity,
            config.notifications, config.normalCooldownMillis, config.airStallCooldownMillis, config.debugEnabled,
            config.markerSettings, new com.snkisk.hypixellegitils.config.NickDetectionSettings(false)
        );
        coordinator.applyRuntimeNickDetectionConfig(nickDisabled);
        LegitilsConfig devEnabled = new LegitilsConfig(
            nickDisabled.schemaVersion, nickDisabled.revision + 1L, nickDisabled.enabledDetectors, nickDisabled.sensitivity,
            nickDisabled.notifications, nickDisabled.normalCooldownMillis, nickDisabled.airStallCooldownMillis, true,
            nickDisabled.markerSettings, nickDisabled.nickDetectionSettings
        );
        coordinator.applyRuntimeDevelopmentConfig(devEnabled);

        LegitilsConfig actionBarEnabled = new LegitilsConfig(
            devEnabled.schemaVersion, devEnabled.revision + 1L, devEnabled.enabledDetectors, devEnabled.sensitivity,
            new com.snkisk.hypixellegitils.config.NotificationSettings(true, true, false),
            devEnabled.normalCooldownMillis, devEnabled.airStallCooldownMillis, devEnabled.debugEnabled,
            devEnabled.markerSettings, devEnabled.nickDetectionSettings
        );
        coordinator.applyRuntimeNotificationConfig(actionBarEnabled);

        LegitilsConfig markerEnabled = new LegitilsConfig(
            actionBarEnabled.schemaVersion, actionBarEnabled.revision + 1L, actionBarEnabled.enabledDetectors,
            actionBarEnabled.sensitivity, actionBarEnabled.notifications, actionBarEnabled.normalCooldownMillis,
            actionBarEnabled.airStallCooldownMillis, actionBarEnabled.debugEnabled, new MarkerSettings(true, 2),
            actionBarEnabled.nickDetectionSettings
        );
        coordinator.applyRuntimeMarkerConfig(markerEnabled);

        LegitilsConfig noSlowAndTimer = new LegitilsConfig(
            markerEnabled.schemaVersion, markerEnabled.revision + 1L,
            EnumSet.of(DetectorId.NO_SLOW, DetectorId.AIR_STALL), markerEnabled.sensitivity,
            markerEnabled.notifications, markerEnabled.normalCooldownMillis, markerEnabled.airStallCooldownMillis,
            markerEnabled.debugEnabled, markerEnabled.markerSettings, markerEnabled.nickDetectionSettings
        );
        coordinator.applyRuntimeDetectorConfig(noSlowAndTimer);
        assertTrue(coordinator.statusText().contains("2/8"));
    }

    @Test
    public void automaticBlacklistCountsOnlyAcceptedAttributableAlertsAndPersistsAcrossWorlds() {
        LegitilsConfig defaults = LegitilsConfig.defaults();
        LegitilsConfig config = new LegitilsConfig(
            defaults.schemaVersion, defaults.revision, EnumSet.of(DetectorId.NO_SLOW), defaults.sensitivity,
            defaults.notifications, defaults.normalCooldownMillis, defaults.airStallCooldownMillis,
            defaults.debugEnabled, new MarkerSettings(true, 3)
        );
        ObservationCoordinator coordinator = new ObservationCoordinator(config, new LocalAlertSink(config.notifications));
        UUID player = UUID.randomUUID();
        for (long now = 0L; now <= 2000L; now += 1000L) {
            coordinator.observePlayer(player, now);
            coordinator.submit(new Evidence(DetectorId.NO_SLOW, player, Confidence.HIGH, now, "test"), now, true);
        }
        assertTrue(coordinator.shouldShowAcceptedAlertMarker(player, 2000L));
        coordinator.setGlobalLag(true);
        coordinator.submit(new Evidence(DetectorId.NO_SLOW, player, Confidence.HIGH, 3000L, "test"), 3000L, true);
        assertTrue(coordinator.shouldShowAcceptedAlertMarker(player, 3000L));
        coordinator.onWorldLoading();
        assertTrue(coordinator.shouldShowAcceptedAlertMarker(player, 3001L));
    }

    @Test
    public void developmentSelfSampleCanAlertButNeverCreatesPersistentBlacklistHistory() {
        LegitilsConfig defaults = LegitilsConfig.defaults();
        LegitilsConfig config = new LegitilsConfig(
            defaults.schemaVersion, defaults.revision, EnumSet.of(DetectorId.NO_SLOW), defaults.sensitivity,
            defaults.notifications, defaults.normalCooldownMillis, defaults.airStallCooldownMillis,
            defaults.debugEnabled, new MarkerSettings(true, 2), defaults.nickDetectionSettings
        );
        ObservationCoordinator coordinator = new ObservationCoordinator(config, new LocalAlertSink(config.notifications));
        UUID localPlayer = UUID.randomUUID();
        coordinator.setDevelopmentSelfPlayerId(localPlayer);
        coordinator.observePlayer(localPlayer, 0L);
        assertTrue(coordinator.submit(new Evidence(DetectorId.NO_SLOW, localPlayer, Confidence.HIGH, 0L, "test"), 0L, true).shouldAlert);
        assertEquals(0, coordinator.markerHistoryCount());
        assertFalse(coordinator.shouldShowAcceptedAlertMarker(localPlayer, 1L));
    }

    @Test
    public void globalLagSuppressesTheDevelopmentNoBreakDelayBypass() {
        LegitilsConfig defaults = LegitilsConfig.defaults();
        LegitilsConfig config = new LegitilsConfig(
            defaults.schemaVersion, defaults.revision, EnumSet.of(DetectorId.NO_BREAK_DELAY), defaults.sensitivity,
            defaults.notifications, defaults.normalCooldownMillis, defaults.airStallCooldownMillis,
            defaults.debugEnabled, defaults.markerSettings, defaults.nickDetectionSettings
        );
        LocalAlertSink sink = new LocalAlertSink(config.notifications);
        ObservationCoordinator coordinator = new ObservationCoordinator(config, sink);
        UUID localPlayer = UUID.randomUUID();
        coordinator.setDevelopmentSelfPlayerId(localPlayer);
        coordinator.observeDevelopmentNoBreakDelay(localPlayer, 100L, 5, true);
        coordinator.beginObservationFrame(true);
        coordinator.observeDevelopmentNoBreakDelay(localPlayer, 101L, 0, false);
        assertTrue(sink.presentation(101L).chatText == null);
    }

    @Test
    public void persistedAndManualBlacklistEntriesRenderUntilExplicitlyRemoved() {
        LegitilsConfig defaults = LegitilsConfig.defaults();
        LegitilsConfig config = new LegitilsConfig(
            defaults.schemaVersion, defaults.revision, defaults.enabledDetectors, defaults.sensitivity,
            defaults.notifications, defaults.normalCooldownMillis, defaults.airStallCooldownMillis,
            defaults.debugEnabled, new MarkerSettings(true, 3)
        );
        UUID restored = UUID.randomUUID();
        Map<UUID, MarkerHistoryEntry> history = new HashMap<UUID, MarkerHistoryEntry>();
        history.put(restored, new MarkerHistoryEntry(1, true, 100L));
        ObservationCoordinator coordinator = new ObservationCoordinator(
            config,
            new LocalAlertSink(config.notifications),
            history,
            new MarkerHistoryPersistence() {
                @Override
                public void write(Map<UUID, MarkerHistoryEntry> ignored) {
                }
            }
        );
        assertTrue(coordinator.shouldShowAcceptedAlertMarker(restored, 200L));
        UUID manual = UUID.randomUUID();
        assertTrue(coordinator.blacklistMarker(manual, 201L));
        assertTrue(coordinator.shouldShowAcceptedAlertMarker(manual, 202L));
        assertTrue(coordinator.removeMarker(manual));
        assertFalse(coordinator.shouldShowAcceptedAlertMarker(manual, 203L));
    }

    @Test
    public void failedHistoryWriteRollsBackAManualBlacklistChange() {
        LegitilsConfig defaults = LegitilsConfig.defaults();
        LegitilsConfig config = new LegitilsConfig(
            defaults.schemaVersion, defaults.revision, defaults.enabledDetectors, defaults.sensitivity,
            defaults.notifications, defaults.normalCooldownMillis, defaults.airStallCooldownMillis,
            defaults.debugEnabled, new MarkerSettings(true, 3)
        );
        ObservationCoordinator coordinator = new ObservationCoordinator(
            config,
            new LocalAlertSink(config.notifications),
            new HashMap<UUID, MarkerHistoryEntry>(),
            new MarkerHistoryPersistence() {
                @Override
                public void write(Map<UUID, MarkerHistoryEntry> ignored) throws IOException {
                    throw new IOException("test failure");
                }
            }
        );
        UUID player = UUID.randomUUID();
        assertFalse(coordinator.blacklistMarker(player, 1L));
        assertFalse(coordinator.shouldShowAcceptedAlertMarker(player, 2L));
    }

    @Test
    public void markerNeverCountsAnonymousOrCooldownRejectedEvidence() {
        LegitilsConfig defaults = LegitilsConfig.defaults();
        LegitilsConfig config = new LegitilsConfig(
            defaults.schemaVersion, defaults.revision, EnumSet.of(DetectorId.NO_SLOW, DetectorId.BED_NUKE), defaults.sensitivity,
            defaults.notifications, defaults.normalCooldownMillis, defaults.airStallCooldownMillis,
            defaults.debugEnabled, new MarkerSettings(true, 2)
        );
        ObservationCoordinator coordinator = new ObservationCoordinator(config, new LocalAlertSink(config.notifications));
        UUID player = UUID.randomUUID();
        coordinator.observePlayer(player, 0L);
        coordinator.submit(new Evidence(DetectorId.BED_NUKE, null, Confidence.HIGH, 0L, "anonymous"), 0L, true);
        coordinator.submit(new Evidence(DetectorId.NO_SLOW, player, Confidence.HIGH, 0L, "first"), 0L, true);
        coordinator.submit(new Evidence(DetectorId.NO_SLOW, player, Confidence.HIGH, 100L, "cooldown"), 100L, true);
        assertFalse(coordinator.shouldShowAcceptedAlertMarker(player, 100L));
    }

    @Test
    public void nickedUuidVersionOneEvidenceNeverCreatesPersistentBlacklistHistory() {
        LegitilsConfig defaults = LegitilsConfig.defaults();
        LegitilsConfig config = new LegitilsConfig(
            defaults.schemaVersion, defaults.revision, EnumSet.of(DetectorId.NO_SLOW), defaults.sensitivity,
            defaults.notifications, defaults.normalCooldownMillis, defaults.airStallCooldownMillis,
            defaults.debugEnabled, new MarkerSettings(true, 2)
        );
        ObservationCoordinator coordinator = new ObservationCoordinator(config, new LocalAlertSink(config.notifications));
        UUID nicked = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        coordinator.observePlayer(nicked, 0L);
        assertTrue(coordinator.submit(new Evidence(DetectorId.NO_SLOW, nicked, Confidence.HIGH, 0L, "test"), 0L, true).shouldAlert);
        assertEquals(0, coordinator.markerHistoryCount());
        assertFalse(coordinator.shouldShowAcceptedAlertMarker(nicked, 1L));
    }

    @Test
    public void legacyToCurrentSchemaMarkerUpdateAppliesImmediately() {
        LegitilsConfig defaults = LegitilsConfig.defaults();
        LegitilsConfig legacy = new LegitilsConfig(
            LegitilsConfig.LEGACY_SCHEMA_VERSION, 0L, EnumSet.of(DetectorId.NO_SLOW), defaults.sensitivity,
            defaults.notifications, defaults.normalCooldownMillis, defaults.airStallCooldownMillis,
            defaults.debugEnabled, defaults.markerSettings
        );
        ObservationCoordinator coordinator = new ObservationCoordinator(legacy, new LocalAlertSink(legacy.notifications));
        LegitilsConfig enabled = new LegitilsConfig(
            LegitilsConfig.SCHEMA_VERSION, 1L, legacy.enabledDetectors, legacy.sensitivity,
            legacy.notifications, legacy.normalCooldownMillis, legacy.airStallCooldownMillis,
            legacy.debugEnabled, new MarkerSettings(true, 3)
        );
        coordinator.applyRuntimeMarkerConfig(enabled);
        UUID player = UUID.randomUUID();
        for (long now = 0L; now <= 2000L; now += 1000L) {
            coordinator.observePlayer(player, now);
            coordinator.submit(new Evidence(DetectorId.NO_SLOW, player, Confidence.HIGH, now, "test"), now, true);
        }
        assertTrue(coordinator.shouldShowAcceptedAlertMarker(player, 2000L));
    }

    @Test
    public void globalLagResetsPartialNoBreakDelayCadence() {
        LegitilsConfig defaults = LegitilsConfig.defaults();
        LegitilsConfig config = new LegitilsConfig(
            defaults.schemaVersion, defaults.revision, EnumSet.of(DetectorId.NO_BREAK_DELAY), defaults.sensitivity,
            defaults.notifications, defaults.normalCooldownMillis, defaults.airStallCooldownMillis,
            defaults.debugEnabled, defaults.markerSettings
        );
        ObservationCoordinator coordinator = new ObservationCoordinator(config, new LocalAlertSink(config.notifications));
        UUID player = UUID.randomUUID();
        noBreakComplete(coordinator, player, 0L, 4L, 0);
        coordinator.beginObservationFrame(true);
        coordinator.beginObservationFrame(false);
        noBreakComplete(coordinator, player, 5L, 8L, 1);
        assertTrue(coordinator.onClientTick(1000L).chatText == null);
    }

    @Test
    public void immediateGlobalLagSuppressesQueuedRemoteNoBreakDelayEvents() {
        LegitilsConfig defaults = LegitilsConfig.defaults();
        LegitilsConfig config = new LegitilsConfig(
            defaults.schemaVersion, defaults.revision, EnumSet.of(DetectorId.NO_BREAK_DELAY), defaults.sensitivity,
            defaults.notifications, defaults.normalCooldownMillis, defaults.airStallCooldownMillis,
            defaults.debugEnabled, defaults.markerSettings, defaults.nickDetectionSettings
        );
        LocalAlertSink sink = new LocalAlertSink(config.notifications);
        ObservationCoordinator coordinator = new ObservationCoordinator(config, sink);
        UUID player = UUID.randomUUID();
        noBreakComplete(coordinator, player, 0L, 4L, 0);
        coordinator.onImmediateGlobalLag();
        noBreakComplete(coordinator, player, 5L, 8L, 1);
        coordinator.beginObservationFrame(true);
        coordinator.beginObservationFrame(false);
        noBreakComplete(coordinator, player, 9L, 12L, 2);
        assertTrue(sink.presentation(1000L).chatText == null);
    }

    @Test
    public void runtimeDetectorSetClearsPartialBedNukeHistory() {
        LegitilsConfig disabled = LegitilsConfig.defaults();
        ObservationCoordinator coordinator = new ObservationCoordinator(disabled, new LocalAlertSink(disabled.notifications));
        BedNukeSignalCheck.BlockPosition head = new BedNukeSignalCheck.BlockPosition(0, 64, 0);
        BedNukeSignalCheck.BlockPosition foot = new BedNukeSignalCheck.BlockPosition(0, 64, 1);
        BedNukeSignalCheck.BlockPosition minimum = new BedNukeSignalCheck.BlockPosition(-1, 63, -1);
        BedNukeSignalCheck.BlockPosition maximum = new BedNukeSignalCheck.BlockPosition(1, 65, 1);
        Map<BedNukeSignalCheck.BlockPosition, BedNukeSignalCheck.BlockKind> states = solidVolume(minimum, maximum);
        states.put(head, BedNukeSignalCheck.BlockKind.BED);
        states.put(foot, BedNukeSignalCheck.BlockKind.BED);

        coordinator.observeBedStructure(new BedNukeSignalCheck.BedStructure(head, foot, minimum, maximum, states), 0L);
        coordinator.observeBedBlockState(head, BedNukeSignalCheck.BlockKind.OPEN, 10L);
        LegitilsConfig bedNukeEnabled = new LegitilsConfig(
            disabled.schemaVersion, disabled.revision + 1L, EnumSet.of(DetectorId.BED_NUKE),
            disabled.sensitivity, disabled.notifications, disabled.normalCooldownMillis,
            disabled.airStallCooldownMillis, disabled.debugEnabled
        );
        coordinator.applyRuntimeDetectorConfig(bedNukeEnabled);
        coordinator.observeBedBlockState(foot, BedNukeSignalCheck.BlockKind.OPEN, 20L);
        assertTrue(coordinator.onClientTick(300L).chatText == null);
    }

    @Test
    public void globalLagFrameSuppressesNoSlowEvidenceProducedByTheDetectorEngine() {
        LegitilsConfig config = noSlowEnabledConfig();
        LocalAlertSink sink = new LocalAlertSink(config.notifications);
        ObservationCoordinator coordinator = new ObservationCoordinator(config, sink);
        UUID player = UUID.randomUUID();
        for (int tick = 0; tick <= 20; tick++) {
            coordinator.beginObservationFrame(false);
            coordinator.observe(noSlowSample(player, tick * 50L, tick * 0.14D));
        }
        coordinator.beginObservationFrame(true);
        coordinator.observe(noSlowSample(player, 1050L, 2.94D));
        assertFalse(coordinator.onClientTick(1050L).alert);
        for (int tick = 22; tick <= 42; tick++) {
            coordinator.beginObservationFrame(false);
            coordinator.observe(noSlowSample(player, tick * 50L, 2.94D + (tick - 21L) * 0.14D));
            assertFalse(coordinator.onClientTick(tick * 50L).alert);
        }
        coordinator.beginObservationFrame(false);
        coordinator.observe(noSlowSample(player, 2150L, 6.02D));
        assertTrue(coordinator.onClientTick(2150L).chatText != null);
    }

    @Test
    public void globalLagFrameResetsTimerProgressBeforeItCanAlertAgain() {
        LegitilsConfig config = timerEnabledConfig();
        ObservationCoordinator coordinator = new ObservationCoordinator(config, new LocalAlertSink(config.notifications));
        UUID player = UUID.randomUUID();
        for (long tick = 0L; tick <= 20L; tick++) {
            coordinator.beginObservationFrame(false);
            coordinator.observe(timerSample(player, tick));
            assertFalse(coordinator.onClientTick(tick * 50L).alert);
        }
        coordinator.beginObservationFrame(true);
        coordinator.observe(timerSample(player, 21L));
        assertFalse(coordinator.onClientTick(1050L).alert);
        for (long tick = 22L; tick <= 61L; tick++) {
            coordinator.beginObservationFrame(false);
            coordinator.observe(timerSample(player, tick));
            assertFalse(coordinator.onClientTick(tick * 50L).alert);
        }
        coordinator.beginObservationFrame(false);
        coordinator.observe(timerSample(player, 62L));
        assertTrue(coordinator.onClientTick(3100L).chatText != null);
    }

    @Test
    public void developmentTimerStallAlertsWithoutTurningOffRemoteGlobalLagSafety() {
        LegitilsConfig config = timerEnabledConfig();
        LocalAlertSink sink = new LocalAlertSink(config.notifications);
        ObservationCoordinator coordinator = new ObservationCoordinator(config, sink);
        UUID localPlayer = UUID.randomUUID();
        UUID otherPlayer = UUID.randomUUID();
        coordinator.setDevelopmentSelfPlayerId(localPlayer);
        coordinator.beginObservationFrame(true);
        coordinator.observeDevelopmentTimerStall(otherPlayer, 999L);
        assertEquals(0L, sink.presentation(999L).sequence);
        coordinator.observeDevelopmentTimerStall(localPlayer, 1000L);
        assertTrue(sink.presentation(1000L).chatText != null);
        assertFalse(coordinator.shouldShowAcceptedAlertMarker(localPlayer, 1001L));
        coordinator.observeDevelopmentTimerStall(localPlayer, 1001L);
        assertEquals(1L, sink.presentation(1001L).sequence);
        coordinator.observeDevelopmentTimerStall(localPlayer, 31000L);
        assertEquals(2L, sink.presentation(31000L).sequence);
    }

    @Test
    public void globalLagSuppressesAnUnassignedBedNukeAnomaly() {
        LegitilsConfig config = LegitilsConfig.defaults();
        ObservationCoordinator coordinator = new ObservationCoordinator(config, new LocalAlertSink(config.notifications));
        BedNukeSignalCheck.BlockPosition head = new BedNukeSignalCheck.BlockPosition(0, 64, 0);
        BedNukeSignalCheck.BlockPosition foot = new BedNukeSignalCheck.BlockPosition(0, 64, 1);
        BedNukeSignalCheck.BlockPosition minimum = new BedNukeSignalCheck.BlockPosition(-1, 63, -1);
        BedNukeSignalCheck.BlockPosition maximum = new BedNukeSignalCheck.BlockPosition(1, 65, 1);
        Map<BedNukeSignalCheck.BlockPosition, BedNukeSignalCheck.BlockKind> states = new HashMap<BedNukeSignalCheck.BlockPosition, BedNukeSignalCheck.BlockKind>();
        for (int x = minimum.x; x <= maximum.x; x++) {
            for (int y = minimum.y; y <= maximum.y; y++) {
                for (int z = minimum.z; z <= maximum.z; z++) {
                    states.put(new BedNukeSignalCheck.BlockPosition(x, y, z), BedNukeSignalCheck.BlockKind.SOLID);
                }
            }
        }
        states.put(head, BedNukeSignalCheck.BlockKind.BED);
        states.put(foot, BedNukeSignalCheck.BlockKind.BED);
        coordinator.observeBedStructure(new BedNukeSignalCheck.BedStructure(head, foot, minimum, maximum, states), 0L);
        coordinator.observeBedBlockState(head, BedNukeSignalCheck.BlockKind.OPEN, 10L);
        coordinator.observeBedBlockState(foot, BedNukeSignalCheck.BlockKind.OPEN, 20L);
        coordinator.beginObservationFrame(true);
        AlertPresentation presentation = coordinator.onClientTick(300L);
        assertFalse(presentation.alert);
    }

    @Test
    public void worldTransitionSuppressesBedNukeUntilANormalObservationFrame() {
        LegitilsConfig config = LegitilsConfig.defaults();
        ObservationCoordinator coordinator = new ObservationCoordinator(config, new LocalAlertSink(config.notifications));
        BedNukeSignalCheck.BlockPosition head = new BedNukeSignalCheck.BlockPosition(0, 64, 0);
        BedNukeSignalCheck.BlockPosition foot = new BedNukeSignalCheck.BlockPosition(0, 64, 1);
        BedNukeSignalCheck.BlockPosition minimum = new BedNukeSignalCheck.BlockPosition(-1, 63, -1);
        BedNukeSignalCheck.BlockPosition maximum = new BedNukeSignalCheck.BlockPosition(1, 65, 1);
        Map<BedNukeSignalCheck.BlockPosition, BedNukeSignalCheck.BlockKind> states = solidVolume(minimum, maximum);
        states.put(head, BedNukeSignalCheck.BlockKind.BED);
        states.put(foot, BedNukeSignalCheck.BlockKind.BED);
        coordinator.onWorldLoading();
        coordinator.observeBedStructure(new BedNukeSignalCheck.BedStructure(head, foot, minimum, maximum, states), 0L);
        coordinator.observeBedBlockState(head, BedNukeSignalCheck.BlockKind.OPEN, 10L);
        coordinator.observeBedBlockState(foot, BedNukeSignalCheck.BlockKind.OPEN, 20L);
        assertFalse(coordinator.onClientTick(300L).alert);
    }

    @Test
    public void chunkTransitionInvalidatesAPartialBedNukeTrace() {
        LegitilsConfig config = LegitilsConfig.defaults();
        ObservationCoordinator coordinator = new ObservationCoordinator(config, new LocalAlertSink(config.notifications));
        BedNukeSignalCheck.BlockPosition head = new BedNukeSignalCheck.BlockPosition(0, 64, 0);
        BedNukeSignalCheck.BlockPosition foot = new BedNukeSignalCheck.BlockPosition(0, 64, 1);
        BedNukeSignalCheck.BlockPosition minimum = new BedNukeSignalCheck.BlockPosition(-1, 63, -1);
        BedNukeSignalCheck.BlockPosition maximum = new BedNukeSignalCheck.BlockPosition(1, 65, 1);
        Map<BedNukeSignalCheck.BlockPosition, BedNukeSignalCheck.BlockKind> states = solidVolume(minimum, maximum);
        states.put(head, BedNukeSignalCheck.BlockKind.BED);
        states.put(foot, BedNukeSignalCheck.BlockKind.BED);
        coordinator.observeBedStructure(new BedNukeSignalCheck.BedStructure(head, foot, minimum, maximum, states), 0L);
        coordinator.observeBedBlockState(head, BedNukeSignalCheck.BlockKind.OPEN, 10L);
        coordinator.onChunkTransition();
        coordinator.observeBedBlockState(foot, BedNukeSignalCheck.BlockKind.OPEN, 20L);
        assertFalse(coordinator.onClientTick(300L).alert);
    }

    private static Map<BedNukeSignalCheck.BlockPosition, BedNukeSignalCheck.BlockKind> solidVolume(
        BedNukeSignalCheck.BlockPosition minimum,
        BedNukeSignalCheck.BlockPosition maximum
    ) {
        Map<BedNukeSignalCheck.BlockPosition, BedNukeSignalCheck.BlockKind> states = new HashMap<BedNukeSignalCheck.BlockPosition, BedNukeSignalCheck.BlockKind>();
        for (int x = minimum.x; x <= maximum.x; x++) {
            for (int y = minimum.y; y <= maximum.y; y++) {
                for (int z = minimum.z; z <= maximum.z; z++) {
                    states.put(new BedNukeSignalCheck.BlockPosition(x, y, z), BedNukeSignalCheck.BlockKind.SOLID);
                }
            }
        }
        return states;
    }

    private static PlayerSample noSlowSample(UUID player, long nowMillis, double x) {
        return new PlayerSample(player, nowMillis, x, 64.0D, 0.0D, false, false, true, true, false, false, true, false, -1, 0.0F, false, true);
    }

    private static void noBreakComplete(ObservationCoordinator coordinator, UUID player, long startTick, long finishTick, int x) {
        NoBreakDelaySignalCheck.BlockPosition position = new NoBreakDelaySignalCheck.BlockPosition(x, 64, 0);
        coordinator.observeNoBreakDelayProgress(new NoBreakDelaySignalCheck.Progress(player, position, startTick, 0, true, true));
        coordinator.observeNoBreakDelayProgress(new NoBreakDelaySignalCheck.Progress(player, position, finishTick, 9, true, true));
        coordinator.observeNoBreakDelayBlockRemoval(position, finishTick, true);
    }

    private static PlayerSample timerSample(UUID player, long worldTick) {
        return new PlayerSample(
            player, worldTick * 50L, worldTick, 0.0D, 70.0D, 0.0D,
            false, false, false, false, false, false, false, false, false,
            -1, 0.0F, false, false, false, true,
            0.10D, 1, true, false, false, false
        );
    }

    private static LegitilsConfig noSlowEnabledConfig() {
        LegitilsConfig defaults = LegitilsConfig.defaults();
        return new LegitilsConfig(
            defaults.schemaVersion,
            defaults.revision,
            EnumSet.of(DetectorId.NO_SLOW),
            defaults.sensitivity,
            defaults.notifications,
            defaults.normalCooldownMillis,
            defaults.airStallCooldownMillis,
            defaults.debugEnabled
        );
    }

    private static LegitilsConfig timerEnabledConfig() {
        LegitilsConfig defaults = LegitilsConfig.defaults();
        return new LegitilsConfig(
            defaults.schemaVersion,
            defaults.revision,
            EnumSet.of(DetectorId.AIR_STALL),
            defaults.sensitivity,
            defaults.notifications,
            defaults.normalCooldownMillis,
            defaults.airStallCooldownMillis,
            defaults.debugEnabled
        );
    }
}
