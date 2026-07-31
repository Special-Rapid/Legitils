package com.snkisk.hypixellegitils.detection;

import com.snkisk.hypixellegitils.config.DetectorId;
import com.snkisk.hypixellegitils.evidence.Confidence;
import com.snkisk.hypixellegitils.evidence.Evidence;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BedNukeAttributionGateTest {
    @Test
    public void attributesOnlyTheMatchedObstructedBedBreak() {
        BedNukeAttributionGate gate = new BedNukeAttributionGate();
        UUID playerId = UUID.randomUUID();

        gate.observeBedAttempt(playerId, "Itzhamke99", true, 100L);
        gate.observeBedDestruction("itzhamke99", 300L);
        gate.observeStructuralAnomaly(structuralAnomaly(350L), 350L);

        Evidence evidence = gate.evaluate(351L);
        assertEquals(DetectorId.BED_NUKE, evidence.detector);
        assertEquals(playerId, evidence.playerId);
    }

    @Test
    public void rejectsAVisibleClearRouteOrMismatchedChatActor() {
        BedNukeAttributionGate clearRoute = new BedNukeAttributionGate();
        clearRoute.observeBedAttempt(UUID.randomUUID(), "Legit", false, 100L);
        clearRoute.observeBedDestruction("Legit", 200L);
        clearRoute.observeStructuralAnomaly(structuralAnomaly(250L), 250L);
        assertNull(clearRoute.evaluate(251L));

        BedNukeAttributionGate mismatchedActor = new BedNukeAttributionGate();
        mismatchedActor.observeBedAttempt(UUID.randomUUID(), "Attacker", true, 100L);
        mismatchedActor.observeBedDestruction("OtherPlayer", 200L);
        mismatchedActor.observeStructuralAnomaly(structuralAnomaly(250L), 250L);
        assertNull(mismatchedActor.evaluate(251L));
    }

    @Test
    public void expiresIncompleteEvidenceInsteadOfGuessingAnActor() {
        BedNukeAttributionGate gate = new BedNukeAttributionGate();
        gate.observeBedAttempt(UUID.randomUUID(), "Attacker", true, 100L);
        gate.observeStructuralAnomaly(structuralAnomaly(100L), 100L);
        gate.observeBedDestruction("Attacker", 2700L);
        assertNull(gate.evaluate(2700L));
    }

    private static Evidence structuralAnomaly(long nowMillis) {
        return new Evidence(DetectorId.BED_NUKE, null, Confidence.HIGH, nowMillis, "sealed defense");
    }
}
