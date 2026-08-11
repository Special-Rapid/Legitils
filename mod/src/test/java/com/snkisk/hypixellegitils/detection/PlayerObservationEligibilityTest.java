package com.snkisk.hypixellegitils.detection;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class PlayerObservationEligibilityTest {
    @Test
    public void excludesEitherVisibleSpectatorSignalBeforeAnyDetectorReceivesThePlayer() {
        assertTrue(PlayerObservationEligibility.shouldObserve(false, false));
        assertFalse(PlayerObservationEligibility.shouldObserve(true, false));
        assertFalse(PlayerObservationEligibility.shouldObserve(false, true));
        assertFalse(PlayerObservationEligibility.shouldObserve(true, true));
    }
}
