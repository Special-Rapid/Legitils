package com.snkisk.hypixellegitils.evidence;

import com.snkisk.hypixellegitils.config.DetectorId;
import com.snkisk.hypixellegitils.config.LegitilsConfig;
import com.snkisk.hypixellegitils.config.NotificationSettings;
import com.snkisk.hypixellegitils.config.SensitivityPreset;
import java.util.EnumSet;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EvidencePolicyTest {
    @Test
    public void normalEvidenceUsesOneSecondCooldown() {
        EvidencePolicy policy = new EvidencePolicy();
        Evidence evidence = evidence(DetectorId.NO_SLOW);
        LegitilsConfig config = configFor(DetectorId.NO_SLOW);
        assertTrue(policy.evaluate(evidence, context(0L, false, false, true), config).shouldAlert);
        assertFalse(policy.evaluate(evidence, context(999L, false, false, true), config).shouldAlert);
        assertTrue(policy.evaluate(evidence, context(1000L, false, false, true), config).shouldAlert);
    }

    @Test
    public void airStallUsesThirtySecondCooldown() {
        EvidencePolicy policy = new EvidencePolicy();
        Evidence evidence = evidence(DetectorId.AIR_STALL);
        LegitilsConfig config = configFor(DetectorId.AIR_STALL);
        assertTrue(policy.evaluate(evidence, context(0L, false, false, true), config).shouldAlert);
        assertFalse(policy.evaluate(evidence, context(29999L, false, false, true), config).shouldAlert);
        assertTrue(policy.evaluate(evidence, context(30000L, false, false, true), config).shouldAlert);
    }

    @Test
    public void unassignedBedNukeEvidenceUsesItsOwnSharedCooldownKey() {
        EvidencePolicy policy = new EvidencePolicy();
        Evidence evidence = new Evidence(DetectorId.BED_NUKE, null, Confidence.HIGH, 0L, "unassigned blocked-bed break anomaly");
        LegitilsConfig config = configFor(DetectorId.BED_NUKE);
        assertTrue(policy.evaluate(evidence, context(0L, false, false, true), config).shouldAlert);
        assertFalse(policy.evaluate(evidence, context(999L, false, false, true), config).shouldAlert);
        assertTrue(policy.evaluate(evidence, context(1000L, false, false, true), config).shouldAlert);
    }

    @Test
    public void unreliableConditionsAlwaysSuppressEvidence() {
        EvidencePolicy policy = new EvidencePolicy();
        Evidence evidence = evidence(DetectorId.KILL_AURA);
        LegitilsConfig config = configFor(DetectorId.values());
        assertFalse(policy.evaluate(evidence, context(1L, true, false, true), config).shouldAlert);
        assertFalse(policy.evaluate(evidence, context(2L, false, true, true), config).shouldAlert);
        assertFalse(policy.evaluate(evidence, context(3L, false, false, false), config).shouldAlert);
    }

    @Test
    public void cooldownsSurviveTheMaximumObservedPlayerDetectorCombination() {
        EvidencePolicy policy = new EvidencePolicy();
        LegitilsConfig config = configFor(DetectorId.values());
        Evidence first = evidence(DetectorId.AUTO_BLOCK, new UUID(0L, 1L));
        assertTrue(policy.evaluate(first, context(0L, false, false, true), config).shouldAlert);
        for (DetectorId detector : DetectorId.values()) {
            if (detector != DetectorId.AUTO_BLOCK) {
                assertTrue(policy.evaluate(evidence(detector, first.playerId), context(1L, false, false, true), config).shouldAlert);
            }
        }
        for (int player = 1; player < 256; player++) {
            for (DetectorId detector : DetectorId.values()) {
                Evidence next = evidence(detector, new UUID(0L, 1000L + player));
                assertTrue(policy.evaluate(next, context(1L, false, false, true), config).shouldAlert);
            }
        }
        PolicyDecision repeated = policy.evaluate(first, context(999L, false, false, true), config);
        assertFalse(repeated.shouldAlert);
        assertEquals("cooldown", repeated.reason);
    }

    private static Evidence evidence(DetectorId detector) {
        return evidence(detector, UUID.fromString("00000000-0000-0000-0000-000000000001"));
    }

    private static Evidence evidence(DetectorId detector, UUID playerId) {
        return new Evidence(detector, playerId, Confidence.MEDIUM, 0L, "test observation");
    }

    private static EvidencePolicyContext context(long now, boolean lag, boolean transition, boolean history) {
        return new EvidencePolicyContext(now, lag, transition, history);
    }

    private static LegitilsConfig configFor(DetectorId... detectors) {
        return new LegitilsConfig(1, 0L, EnumSet.copyOf(java.util.Arrays.asList(detectors)), SensitivityPreset.BALANCED, new NotificationSettings(true, true, false), 1000L, 30000L, false);
    }
}
