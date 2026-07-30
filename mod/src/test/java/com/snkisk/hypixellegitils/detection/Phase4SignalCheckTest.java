package com.snkisk.hypixellegitils.detection;

import com.snkisk.hypixellegitils.config.DetectorId;
import com.snkisk.hypixellegitils.config.LegitilsConfig;
import com.snkisk.hypixellegitils.evidence.Evidence;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class Phase4SignalCheckTest {
    private static final UUID PLAYER = new UUID(0L, 44L);

    @Test
    public void combatDesyncRequiresTwoCombatStallAndSnapEpisodes() {
        CombatDesyncSignalCheck check = new CombatDesyncSignalCheck();
        CombatDesyncSignalCheck.State state = new CombatDesyncSignalCheck.State();
        assertNull(check.observe(combat(0L, 0.0D, true, 0.10D, 1), state));
        for (long tick = 1L; tick <= 8L; tick++) assertNull(check.observe(combat(tick, 0.0D, true, 0.10D, 1), state));
        assertNull(check.observe(combat(9L, 4.0D, true, 0.10D, 1), state));
        for (long tick = 10L; tick <= 17L; tick++) assertNull(check.observe(combat(tick, 4.0D, true, 0.10D, 1), state));
        Evidence evidence = check.observe(combat(18L, 8.0D, true, 0.10D, 1), state);
        assertEquals(DetectorId.COMBAT_DESYNC, evidence.detector);
        assertEquals("combat-correlated desync anomaly", evidence.observation);
    }

    @Test
    public void combatDesyncDoesNotTreatNormalStationaryCombatOrNonCombatBaselineAsEvidence() {
        CombatDesyncSignalCheck check = new CombatDesyncSignalCheck();
        CombatDesyncSignalCheck.State state = new CombatDesyncSignalCheck.State();
        assertNull(check.observe(combat(0L, 0.0D, false, 0.10D, 1), state));
        for (long tick = 1L; tick <= 8L; tick++) assertNull(check.observe(combat(tick, 0.0D, false, 0.10D, 1), state));
        assertNull(check.observe(combat(9L, 4.0D, false, 0.10D, 1), state));
        for (long tick = 10L; tick <= 17L; tick++) assertNull(check.observe(combat(tick, 4.0D, true, 0.10D, 1), state));
        assertNull(check.observe(combat(18L, 8.0D, true, 0.10D, 1), state));
        for (long tick = 19L; tick <= 26L; tick++) assertNull(check.observe(combat(tick, 8.0D, true, 0.10D, 1), state));
        assertNull(check.observe(combat(27L, 8.5D, true, 0.10D, 1), state));
    }

    @Test
    public void combatDesyncResetsForMissingNearbyComparisonAndTickDiscontinuity() {
        CombatDesyncSignalCheck check = new CombatDesyncSignalCheck();
        CombatDesyncSignalCheck.State state = new CombatDesyncSignalCheck.State();
        assertNull(check.observe(combat(0L, 0.0D, true, 0.0D, 0), state));
        for (long tick = 1L; tick <= 8L; tick++) assertNull(check.observe(combat(tick, 0.0D, true, 0.0D, 0), state));
        assertNull(check.observe(combat(9L, 4.0D, true, 0.0D, 0), state));
        assertNull(check.observe(combat(20L, 4.0D, true, 0.10D, 1), state));
    }

    @Test
    public void combatDesyncNeverStitchesAcrossAnAmbiguousStallAndSnap() {
        CombatDesyncSignalCheck check = new CombatDesyncSignalCheck();
        CombatDesyncSignalCheck.State state = new CombatDesyncSignalCheck.State();
        completeCombatEpisode(check, state, 0L, 0.0D, 0.10D, 1);
        completeCombatEpisode(check, state, 10L, 4.0D, 0.0D, 0);
        assertNull(completeCombatEpisode(check, state, 20L, 8.0D, 0.10D, 1));
    }

    @Test
    public void airStallReportsSustainedUnsupportedStationarityIncludingReloadLikePattern() {
        AirStallSignalCheck check = new AirStallSignalCheck();
        AirStallSignalCheck.State state = new AirStallSignalCheck.State();
        assertNull(check.observe(air(0L, 0.0D, true, false, false, false, false, false), state));
        for (long tick = 1L; tick < 40L; tick++) assertNull(check.observe(air(tick, 0.0D, true, false, false, false, false, false), state));
        Evidence evidence = check.observe(air(40L, 0.0D, true, false, false, false, false, false), state);
        assertEquals(DetectorId.AIR_STALL, evidence.detector);
        assertEquals("air-stall anomaly", evidence.observation);
    }

    @Test
    public void airStallSuppressesSupportLiquidLadderVehicleAndIncompleteState() {
        assertAirStallSuppressed(air(0L, 0.0D, true, true, false, false, false, false));
        assertAirStallSuppressed(air(0L, 0.0D, true, false, true, false, false, false));
        assertAirStallSuppressed(air(0L, 0.0D, true, false, false, true, false, false));
        assertAirStallSuppressed(air(0L, 0.0D, true, false, false, false, true, false));
        assertAirStallSuppressed(air(0L, 0.0D, false, false, false, false, false, false));
    }

    @Test
    public void airStallSuppressesBroadRemoteUpdateStallWithoutNearbyMovement() {
        AirStallSignalCheck check = new AirStallSignalCheck();
        AirStallSignalCheck.State state = new AirStallSignalCheck.State();
        for (long tick = 0L; tick <= 45L; tick++) {
            assertNull(check.observe(airWithNearby(tick, 0.0D, true, false, false, false, false, false, 0.0D, 0), state));
        }
    }

    @Test
    public void airStallResetsOnObservationDiscontinuity() {
        AirStallSignalCheck check = new AirStallSignalCheck();
        AirStallSignalCheck.State state = new AirStallSignalCheck.State();
        assertNull(check.observe(air(0L, 0.0D, true, false, false, false, false, false), state));
        for (long tick = 1L; tick < 40L; tick++) assertNull(check.observe(air(tick, 0.0D, true, false, false, false, false, false), state));
        assertNull(check.observe(air(41L, 0.0D, true, false, false, false, false, false), state));
    }

    @Test
    public void detectionEngineRoutesBothPhaseFourSignals() {
        DetectionEngine combatEngine = new DetectionEngine(LegitilsConfig.defaults());
        combatEngine.observe(combat(0L, 0.0D, true, 0.10D, 1));
        for (long tick = 1L; tick <= 8L; tick++) combatEngine.observe(combat(tick, 0.0D, true, 0.10D, 1));
        combatEngine.observe(combat(9L, 4.0D, true, 0.10D, 1));
        for (long tick = 10L; tick <= 17L; tick++) combatEngine.observe(combat(tick, 4.0D, true, 0.10D, 1));
        assertContains(combatEngine.observe(combat(18L, 8.0D, true, 0.10D, 1)), DetectorId.COMBAT_DESYNC);

        DetectionEngine airEngine = new DetectionEngine(LegitilsConfig.defaults());
        airEngine.observe(air(0L, 0.0D, true, false, false, false, false, false));
        for (long tick = 1L; tick < 40L; tick++) airEngine.observe(air(tick, 0.0D, true, false, false, false, false, false));
        assertContains(airEngine.observe(air(40L, 0.0D, true, false, false, false, false, false)), DetectorId.AIR_STALL);
    }

    private static void assertAirStallSuppressed(PlayerSample first) {
        AirStallSignalCheck check = new AirStallSignalCheck();
        AirStallSignalCheck.State state = new AirStallSignalCheck.State();
        for (long tick = 0L; tick <= 45L; tick++) {
            PlayerSample sample = air(tick, first.x, first.supportStateComplete, first.supportPresent, first.inLiquid,
                first.onClimbable, first.riding, first.onGround);
            assertNull(check.observe(sample, state));
        }
    }

    private static Evidence completeCombatEpisode(
        CombatDesyncSignalCheck check,
        CombatDesyncSignalCheck.State state,
        long startTick,
        double startX,
        double nearbyMedian,
        int nearbyCount
    ) {
        assertNull(check.observe(combat(startTick, startX, true, nearbyMedian, nearbyCount), state));
        for (long tick = startTick + 1L; tick <= startTick + 8L; tick++) {
            assertNull(check.observe(combat(tick, startX, true, nearbyMedian, nearbyCount), state));
        }
        return check.observe(combat(startTick + 9L, startX + 4.0D, true, nearbyMedian, nearbyCount), state);
    }

    private static PlayerSample combat(long tick, double x, boolean combat, double nearbyMedian, int nearbyCount) {
        return sample(tick, x, 70.0D, combat, nearbyMedian, nearbyCount, false, false, false, false, false, false);
    }

    private static PlayerSample air(
        long tick,
        double x,
        boolean supportComplete,
        boolean supportPresent,
        boolean inLiquid,
        boolean onClimbable,
        boolean riding,
        boolean onGround
    ) {
        return airWithNearby(tick, x, supportComplete, supportPresent, inLiquid, onClimbable, riding, onGround, 0.10D, 1);
    }

    private static PlayerSample airWithNearby(
        long tick,
        double x,
        boolean supportComplete,
        boolean supportPresent,
        boolean inLiquid,
        boolean onClimbable,
        boolean riding,
        boolean onGround,
        double nearbyMedian,
        int nearbyCount
    ) {
        return sample(tick, x, 70.0D, false, nearbyMedian, nearbyCount, supportComplete, supportPresent, inLiquid, onClimbable, riding, onGround);
    }

    private static PlayerSample sample(
        long tick,
        double x,
        double y,
        boolean combat,
        double nearbyMedian,
        int nearbyCount,
        boolean supportComplete,
        boolean supportPresent,
        boolean inLiquid,
        boolean onClimbable,
        boolean riding,
        boolean onGround
    ) {
        return new PlayerSample(
            PLAYER, tick * 50L, tick, x, y, 0.0D,
            false, false, false, false, false, false, false, onGround, riding,
            -1, 0.0F, combat, false, false, true,
            nearbyMedian, nearbyCount, supportComplete, supportPresent, inLiquid, onClimbable
        );
    }

    private static void assertContains(List<Evidence> evidence, DetectorId detector) {
        for (Evidence item : evidence) if (item.detector == detector) return;
        throw new AssertionError("missing " + detector);
    }
}
