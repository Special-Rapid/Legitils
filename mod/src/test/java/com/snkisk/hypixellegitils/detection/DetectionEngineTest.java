package com.snkisk.hypixellegitils.detection;

import com.snkisk.hypixellegitils.config.DetectorId;
import com.snkisk.hypixellegitils.config.LegitilsConfig;
import com.snkisk.hypixellegitils.evidence.Evidence;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Positive, normal, missing-state and reset traces for every Phase 2 detector. */
public class DetectionEngineTest {
    @Test
    public void autoBlockCompatibilityTraceRequiresElevenConsecutiveTicks() {
        DetectionEngine engine = engine();
        UUID player = UUID.randomUUID();
        for (int tick = 0; tick < 10; tick++) assertEmpty(engine.observe(autoBlock(player, tick * 50L, true, true)));
        assertDetector(engine.observe(autoBlock(player, 500L, true, true)), DetectorId.AUTO_BLOCK);
    }

    @Test
    public void autoBlockInterruptedTraceRestartsTheElevenTickCount() {
        DetectionEngine engine = engine();
        UUID player = UUID.randomUUID();
        for (int tick = 0; tick < 10; tick++) assertEmpty(engine.observe(autoBlock(player, tick * 50L, true, true)));
        assertEmpty(engine.observe(autoBlock(player, 500L, false, true)));
        for (int tick = 0; tick < 10; tick++) assertEmpty(engine.observe(autoBlock(player, 550L + tick * 50L, true, true)));
        assertDetector(engine.observe(autoBlock(player, 1050L, true, true)), DetectorId.AUTO_BLOCK);
    }

    @Test
    public void autoBlockEmitsAgainAfterEachIndependentElevenTickSequence() {
        DetectionEngine engine = engine();
        UUID player = UUID.randomUUID();
        for (int tick = 0; tick < 10; tick++) assertEmpty(engine.observe(autoBlock(player, tick * 50L, true, true)));
        assertDetector(engine.observe(autoBlock(player, 500L, true, true)), DetectorId.AUTO_BLOCK);
        for (int tick = 0; tick < 10; tick++) assertEmpty(engine.observe(autoBlock(player, 550L + tick * 50L, true, true)));
        assertDetector(engine.observe(autoBlock(player, 1050L, true, true)), DetectorId.AUTO_BLOCK);
    }

    @Test
    public void autoBlockObservationGapResetsTheCompatibilitySequence() {
        DetectionEngine engine = engine();
        UUID player = UUID.randomUUID();
        for (int tick = 0; tick < 5; tick++) assertEmpty(engine.observe(autoBlock(player, tick * 50L, true, true)));
        assertEmpty(engine.observe(autoBlock(player, 500L, true, true)));
        for (int tick = 1; tick < 10; tick++) assertEmpty(engine.observe(autoBlock(player, 500L + tick * 50L, true, true)));
        assertDetector(engine.observe(autoBlock(player, 1000L, true, true)), DetectorId.AUTO_BLOCK);
    }

    @Test
    public void autoBlockUnreliableSampleResetsTheCompatibilitySequence() {
        DetectionEngine engine = engine();
        UUID player = UUID.randomUUID();
        for (int tick = 0; tick < 5; tick++) assertEmpty(engine.observe(autoBlock(player, tick * 50L, true, true)));
        assertEmpty(engine.observe(unreliable(player, 250L)));
        for (int tick = 0; tick < 10; tick++) assertEmpty(engine.observe(autoBlock(player, 300L + tick * 50L, true, true)));
        assertDetector(engine.observe(autoBlock(player, 800L, true, true)), DetectorId.AUTO_BLOCK);
    }

    @Test
    public void autoBlockWorldResetTraceClearsPartialPattern() {
        DetectionEngine engine = engine();
        UUID player = UUID.randomUUID();
        for (int tick = 0; tick < 10; tick++) assertEmpty(engine.observe(autoBlock(player, tick * 50L, true, true)));
        engine.reset();
        for (int tick = 0; tick < 10; tick++) assertEmpty(engine.observe(autoBlock(player, 500L + tick * 50L, true, true)));
        assertDetector(engine.observe(autoBlock(player, 1000L, true, true)), DetectorId.AUTO_BLOCK);
    }

    @Test
    public void noSlowCompatibilityTraceRequiresTwentyOneConsecutiveWorldTicks() {
        DetectionEngine engine = engine();
        UUID player = UUID.randomUUID();
        assertEmpty(engine.observe(noSlow(player, 100L, 0.0D, true, true, false, true, -1, true)));
        for (int tick = 1; tick <= 20; tick++) {
            assertEmpty(engine.observe(noSlow(player, 100L + tick, tick * 0.06D, true, true, false, true, -1, true)));
        }
        assertDetector(engine.observe(noSlow(player, 121L, 21D * 0.06D, true, true, false, true, -1, true)), DetectorId.NO_SLOW);
    }

    @Test
    public void noSlowRejectsExactThresholdAndResetsOutsideTheExpectedState() {
        DetectionEngine engine = engine();
        UUID player = UUID.randomUUID();
        assertEmpty(engine.observe(noSlow(player, 100L, 0.0D, true, true, false, true, -1, true)));
        for (int tick = 1; tick <= 21; tick++) {
            double exactThresholdPosition = tick % 2 == 0 ? 0.0D : 0.05D;
            assertEmpty(engine.observe(noSlow(player, 100L + tick, exactThresholdPosition, true, true, false, true, -1, true)));
        }
        for (int tick = 1; tick <= 10; tick++) {
            assertEmpty(engine.observe(noSlow(player, 121L + tick, 2.0D + tick * 0.06D, true, true, false, true, -1, true)));
        }
        assertEmpty(engine.observe(noSlow(player, 132L, 2.66D, false, true, false, true, -1, true)));
        for (int tick = 1; tick <= 20; tick++) {
            assertEmpty(engine.observe(noSlow(player, 132L + tick, 2.66D + tick * 0.06D, true, true, false, true, -1, true)));
        }
    }

    @Test
    public void noSlowAppliesSpeedBoundariesAndDoesNotRequireGround() {
        DetectionEngine engine = engine();
        UUID player = UUID.randomUUID();
        assertEmpty(engine.observe(noSlow(player, 100L, 0.0D, true, true, false, false, 0, true)));
        for (int tick = 1; tick <= 21; tick++) {
            double speedOneThreshold = 0.05D * 1.20D;
            double exactThresholdPosition = tick % 2 == 0 ? 0.0D : speedOneThreshold;
            assertEmpty(engine.observe(noSlow(player, 100L + tick, exactThresholdPosition, true, true, false, false, 0, true)));
        }
        engine.reset();
        assertEmpty(engine.observe(noSlow(player, 200L, 0.0D, true, true, false, false, 1, true)));
        for (int tick = 1; tick <= 20; tick++) {
            assertEmpty(engine.observe(noSlow(player, 200L + tick, tick * 0.071D, true, true, false, false, 1, true)));
        }
        assertDetector(engine.observe(noSlow(player, 221L, 21D * 0.071D, true, true, false, false, 1, true)), DetectorId.NO_SLOW);
    }

    @Test
    public void noSlowUpdatesPreviousPositionInsteadOfMeasuringFromTheOrigin() {
        DetectionEngine engine = engine();
        UUID player = UUID.randomUUID();
        assertEmpty(engine.observe(noSlow(player, 100L, 100.0D, true, true, false, true, -1, true)));
        for (int tick = 1; tick <= 30; tick++) {
            assertEmpty(engine.observe(noSlow(player, 100L + tick, 100.0D + tick * 0.01D, true, true, false, true, -1, true)));
        }
    }

    @Test
    public void noSlowMissingAndWorldResetDiscardPartialStreaks() {
        DetectionEngine engine = engine();
        UUID player = UUID.randomUUID();
        assertEmpty(engine.observe(noSlow(player, 100L, 0.0D, true, true, false, true, -1, true)));
        for (int tick = 1; tick <= 10; tick++) assertEmpty(engine.observe(noSlow(player, 100L + tick, tick * 0.06D, true, true, false, true, -1, true)));
        assertEmpty(engine.observe(unreliable(player, 111L * 50L)));
        for (int tick = 0; tick <= 20; tick++) assertEmpty(engine.observe(noSlow(player, 112L + tick, 2.0D + tick * 0.06D, true, true, false, true, -1, true)));
        assertEmpty(engine.observe(noSlow(player, 200L, 5.0D, true, true, false, true, -1, true)));
        for (int tick = 1; tick <= 10; tick++) assertEmpty(engine.observe(noSlow(player, 200L + tick, 5.0D + tick * 0.06D, true, true, false, true, -1, true)));
        engine.reset();
        for (int tick = 0; tick <= 20; tick++) assertEmpty(engine.observe(noSlow(player, 300L + tick, 8.0D + tick * 0.06D, true, true, false, true, -1, true)));
    }

    @Test
    public void noSlowDuplicateAndSkippedWorldTicksDiscardPartialStreaks() {
        DetectionEngine engine = engine();
        UUID player = UUID.randomUUID();
        assertEmpty(engine.observe(noSlow(player, 100L, 0.0D, true, true, false, true, -1, true)));
        for (int tick = 1; tick <= 10; tick++) assertEmpty(engine.observe(noSlow(player, 100L + tick, tick * 0.06D, true, true, false, true, -1, true)));
        assertEmpty(engine.observe(noSlow(player, 110L, 1.0D, true, true, false, true, -1, true)));
        for (int tick = 1; tick <= 20; tick++) assertEmpty(engine.observe(noSlow(player, 110L + tick, 1.0D + tick * 0.06D, true, true, false, true, -1, true)));
        assertDetector(engine.observe(noSlow(player, 131L, 2.26D, true, true, false, true, -1, true)), DetectorId.NO_SLOW);

        engine.reset();
        assertEmpty(engine.observe(noSlow(player, 200L, 0.0D, true, true, false, true, -1, true)));
        for (int tick = 1; tick <= 10; tick++) assertEmpty(engine.observe(noSlow(player, 200L + tick, tick * 0.06D, true, true, false, true, -1, true)));
        assertEmpty(engine.observe(noSlow(player, 212L, 1.0D, true, true, false, true, -1, true)));
        for (int tick = 1; tick <= 20; tick++) assertEmpty(engine.observe(noSlow(player, 212L + tick, 1.0D + tick * 0.06D, true, true, false, true, -1, true)));
        assertDetector(engine.observe(noSlow(player, 233L, 2.26D, true, true, false, true, -1, true)), DetectorId.NO_SLOW);
    }

    @Test
    public void killAuraCompatibilityTraceRequiresACompletedConsumableUseAndEightViolatingTicks() {
        DetectionEngine engine = engine();
        UUID player = UUID.randomUUID();
        establishCompletedConsumableUse(engine, player, 100L);
        for (long tick = 108L; tick < 121L; tick++) {
            assertEmpty(engine.observe(killAura(player, tick, true, true, true, false, true)));
        }
        assertDetector(engine.observe(killAura(player, 121L, true, true, true, false, true)), DetectorId.KILL_AURA);
    }

    @Test
    public void killAuraRejectsSixTicksNonConsumablesAndTheExactRecentUseBoundary() {
        DetectionEngine engine = engine();
        UUID player = UUID.randomUUID();
        establishCompletedConsumableUse(engine, player, 100L);
        for (long tick = 108L; tick <= 113L; tick++) {
            assertEmpty(engine.observe(killAura(player, tick, true, true, true, false, true)));
        }
        assertEmpty(engine.observe(killAura(player, 114L, true, false, true, false, true)));
        assertEmpty(engine.observe(killAura(player, 115L, true, true, true, true, true)));

        engine.reset();
        establishCompletedConsumableUse(engine, player, 100L);
        for (long tick = 134L; tick <= 147L; tick++) {
            assertEmpty(engine.observe(killAura(player, tick, true, true, true, false, true)));
        }
    }

    @Test
    public void killAuraDecaysOnNormalTicksAndClearsItsPatternAfterAnAlert() {
        DetectionEngine engine = engine();
        UUID player = UUID.randomUUID();
        establishCompletedConsumableUse(engine, player, 100L);
        for (long tick = 108L; tick <= 117L; tick++) {
            assertEmpty(engine.observe(killAura(player, tick, true, true, tick >= 114L, false, true)));
        }
        for (long tick = 118L; tick <= 121L; tick++) {
            assertEmpty(engine.observe(killAura(player, tick, true, true, false, false, true)));
        }
        for (long tick = 122L; tick < 129L; tick++) {
            assertEmpty(engine.observe(killAura(player, tick, true, true, true, false, true)));
        }
        assertDetector(engine.observe(killAura(player, 129L, true, true, true, false, true)), DetectorId.KILL_AURA);

        assertEmpty(engine.observe(killAura(player, 130L, false, false, false, false, true)));
        for (long tick = 131L; tick <= 137L; tick++) {
            assertEmpty(engine.observe(killAura(player, tick, true, true, false, false, true)));
        }
        assertEmpty(engine.observe(killAura(player, 138L, false, true, false, false, true)));
        for (long tick = 139L; tick < 152L; tick++) {
            assertEmpty(engine.observe(killAura(player, tick, true, true, true, false, true)));
        }
        assertDetector(engine.observe(killAura(player, 152L, true, true, true, false, true)), DetectorId.KILL_AURA);
    }

    @Test
    public void killAuraDiscontinuityAndUnreliableObservationDiscardPartialState() {
        DetectionEngine engine = engine();
        UUID player = UUID.randomUUID();
        establishCompletedConsumableUse(engine, player, 100L);
        for (long tick = 108L; tick <= 117L; tick++) {
            assertEmpty(engine.observe(killAura(player, tick, true, true, true, false, true)));
        }
        assertEmpty(engine.observe(killAura(player, 117L, true, true, true, false, true)));
        for (long tick = 118L; tick <= 140L; tick++) {
            assertEmpty(engine.observe(killAura(player, tick, true, true, true, false, true)));
        }

        engine.reset();
        establishCompletedConsumableUse(engine, player, 200L);
        for (long tick = 208L; tick <= 217L; tick++) {
            assertEmpty(engine.observe(killAura(player, tick, true, true, true, false, true)));
        }
        assertEmpty(engine.observe(unreliable(player, 218L * 50L)));
        for (long tick = 219L; tick <= 241L; tick++) {
            assertEmpty(engine.observe(killAura(player, tick, true, true, true, false, true)));
        }
    }

    @Test
    public void killAuraRidingObservationIsIneligibleAndClearsPartialState() {
        DetectionEngine engine = engine();
        UUID player = UUID.randomUUID();
        establishCompletedConsumableUse(engine, player, 100L);
        for (long tick = 108L; tick <= 117L; tick++) {
            assertEmpty(engine.observe(killAura(player, tick, true, true, true, false, true)));
        }
        assertEmpty(engine.observe(killAura(player, 118L, true, true, true, true, true)));
        for (long tick = 119L; tick <= 141L; tick++) {
            assertEmpty(engine.observe(killAura(player, tick, true, true, true, false, true)));
        }
    }

    @Test
    public void legitScaffoldCompatibilityTraceMatchesThreeShortCrouchesAndTimedSwing() {
        DetectionEngine engine = engine();
        UUID player = UUID.randomUUID();
        assertEmpty(engine.observe(scaffold(player, 100L, false, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 101L, true, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 102L, false, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 103L, true, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 104L, false, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 105L, true, false, true, true, 70.0F, true)));
        assertDetector(engine.observe(scaffold(player, 106L, false, true, true, true, 70.0F, true)), DetectorId.LEGIT_SCAFFOLD);
    }

    @Test
    public void legitScaffoldRejectsLongLatestCrouchAndInconsistentHistory() {
        DetectionEngine engine = engine();
        UUID player = UUID.randomUUID();
        assertEmpty(engine.observe(scaffold(player, 100L, false, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 101L, true, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 102L, false, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 103L, true, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 104L, false, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 105L, true, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 108L, false, true, true, true, 70.0F, true)));

        engine.reset();
        assertEmpty(engine.observe(scaffold(player, 120L, false, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 121L, true, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 125L, false, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 126L, true, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 127L, false, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 128L, true, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 129L, false, true, true, true, 70.0F, true)));
    }

    @Test
    public void legitScaffoldRejectsEarlyLateAndMissingScaffoldContext() {
        DetectionEngine engine = engine();
        UUID player = UUID.randomUUID();
        assertEmpty(engine.observe(scaffold(player, 100L, false, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 101L, true, true, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 102L, false, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 103L, true, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 104L, false, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 105L, true, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 106L, false, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 110L, false, true, true, true, 70.0F, true)));

        engine.reset();
        assertEmpty(engine.observe(scaffold(player, 120L, false, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 121L, true, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 122L, false, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 123L, true, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 124L, false, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 125L, true, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 126L, false, true, false, true, 70.0F, true)));
    }

    @Test
    public void legitScaffoldCooldownAndTimingDiscontinuitiesAreFailClosed() {
        DetectionEngine engine = engine();
        UUID player = UUID.randomUUID();
        observeScaffoldPositive(engine, player, 100L);
        for (long tick = 107L; tick < 160L; tick++) assertEmpty(engine.observe(scaffold(player, tick, false, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 160L, true, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 161L, false, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 162L, true, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 163L, false, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 164L, true, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 165L, false, true, true, true, 70.0F, true)));
        assertDetector(engine.observe(scaffold(player, 166L, false, false, true, true, 70.0F, true)), DetectorId.LEGIT_SCAFFOLD);

        engine.reset();
        assertEmpty(engine.observe(scaffold(player, 200L, false, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 201L, true, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 201L, false, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 202L, false, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(unreliable(player, 203L * 50L)));
        assertEmpty(engine.observe(scaffold(player, 204L, false, false, true, true, 70.0F, true)));
    }

    @Test
    public void legitScaffoldCooldownSurvivesAnUnreliableObservation() {
        DetectionEngine engine = engine();
        UUID player = UUID.randomUUID();
        observeScaffoldPositive(engine, player, 100L);
        assertEmpty(engine.observe(unreliable(player, 107L * 50L)));
        assertEmpty(engine.observe(scaffold(player, 108L, false, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 109L, true, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 110L, false, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 111L, true, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 112L, false, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 113L, true, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 114L, false, true, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 160L, false, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 161L, true, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 162L, false, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 163L, true, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 164L, false, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 165L, true, false, true, true, 70.0F, true)));
        assertDetector(engine.observe(scaffold(player, 166L, false, true, true, true, 70.0F, true)), DetectorId.LEGIT_SCAFFOLD);
    }

    @Test
    public void legitScaffoldObservationDiscontinuityClearsOnlyThePartialPattern() {
        DetectionEngine engine = engine();
        UUID player = UUID.randomUUID();
        assertEmpty(engine.observe(scaffold(player, 100L, false, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 101L, true, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 102L, false, false, true, true, 70.0F, true)));
        engine.resetForObservationDiscontinuity();
        assertEmpty(engine.observe(scaffold(player, 103L, true, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 104L, false, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 105L, true, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, 106L, false, true, true, true, 70.0F, true)));
    }

    @Test
    public void missingPlayerFrameDropsEveryDetectorState() {
        DetectionEngine engine = engine();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        assertEmpty(engine.observe(autoBlock(first, 0L, true, true)));
        assertEmpty(engine.observe(noSlow(second, 0L, 0.0D, true, true, false, true, -1, true)));
        assertEmpty(engine.observe(killAura(third, 0L, true, true, false, false, true)));
        assertEquals(3, engine.size());
        engine.retainOnlyPlayers(java.util.Collections.<UUID>emptySet());
        assertEquals(0, engine.size());
    }

    private static DetectionEngine engine() {
        return new DetectionEngine(LegitilsConfig.defaults());
    }

    private static PlayerSample autoBlock(UUID id, long time, boolean swinging, boolean blocking) {
        return sample(id, time, 0.0D, blocking, swinging, false, false, false, false, true, false, 0.0F, false, true);
    }

    private static PlayerSample noSlow(UUID id, long worldTick, double x, boolean sprinting, boolean usingItem, boolean riding, boolean onGround, int amplifier, boolean reliable) {
        return new PlayerSample(
            id,
            worldTick * 50L,
            worldTick,
            x,
            64.0D,
            0.0D,
            false,
            false,
            false,
            sprinting,
            usingItem,
            false,
            false,
            onGround,
            riding,
            amplifier,
            0.0F,
            false,
            false,
            false,
            reliable
        );
    }

    private static void establishCompletedConsumableUse(DetectionEngine engine, UUID player, long startTick) {
        for (long tick = startTick; tick <= startTick + 6L; tick++) {
            assertEmpty(engine.observe(killAura(player, tick, true, true, false, false, true)));
        }
        assertEmpty(engine.observe(killAura(player, startTick + 7L, false, true, false, false, true)));
    }

    private static PlayerSample killAura(UUID id, long worldTick, boolean usingItem, boolean holdingConsumable, boolean attackAnimationActive, boolean riding, boolean reliable) {
        return new PlayerSample(
            id,
            worldTick * 50L,
            worldTick,
            0.0D,
            64.0D,
            0.0D,
            false,
            attackAnimationActive,
            false,
            false,
            usingItem,
            false,
            false,
            true,
            riding,
            -1,
            0.0F,
            false,
            holdingConsumable,
            attackAnimationActive,
            reliable
        );
    }

    private static void observeScaffoldPositive(DetectionEngine engine, UUID player, long startTick) {
        assertEmpty(engine.observe(scaffold(player, startTick, false, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, startTick + 1L, true, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, startTick + 2L, false, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, startTick + 3L, true, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, startTick + 4L, false, false, true, true, 70.0F, true)));
        assertEmpty(engine.observe(scaffold(player, startTick + 5L, true, false, true, true, 70.0F, true)));
        assertDetector(engine.observe(scaffold(player, startTick + 6L, false, true, true, true, 70.0F, true)), DetectorId.LEGIT_SCAFFOLD);
    }

    private static PlayerSample scaffold(UUID id, long worldTick, boolean sneaking, boolean swingStarted, boolean holdingBlock, boolean onGround, float pitch, boolean reliable) {
        return new PlayerSample(
            id,
            worldTick * 50L,
            worldTick,
            0.0D,
            64.0D,
            0.0D,
            false,
            swingStarted,
            swingStarted,
            false,
            false,
            sneaking,
            holdingBlock,
            onGround,
            false,
            -1,
            pitch,
            false,
            false,
            false,
            reliable
        );
    }

    private static PlayerSample unreliable(UUID id, long time) {
        return sample(id, time, 0.0D, false, false, false, false, false, false, false, false, 0.0F, false, false);
    }

    private static PlayerSample sample(UUID id, long time, double x, boolean blocking, boolean swinging, boolean sprinting, boolean usingItem, boolean sneaking, boolean holdingBlock, boolean onGround, boolean riding, float pitch, boolean combatContext, boolean reliable) {
        return new PlayerSample(id, time, x, 64.0D, 0.0D, blocking, swinging, sprinting, usingItem, sneaking, holdingBlock, onGround, riding, -1, pitch, combatContext, reliable);
    }

    private static void assertEmpty(List<Evidence> evidence) {
        assertEquals(0, evidence.size());
    }

    private static void assertDetector(List<Evidence> evidence, DetectorId detector) {
        assertEquals(1, evidence.size());
        assertEquals(detector, evidence.get(0).detector);
        assertTrue(evidence.get(0).observation.length() > 0);
    }
}
