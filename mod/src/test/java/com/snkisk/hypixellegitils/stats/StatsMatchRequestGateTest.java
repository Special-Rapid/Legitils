package com.snkisk.hypixellegitils.stats;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class StatsMatchRequestGateTest {
    @Test
    public void waitsForTheGameWorldThenDelaysAndConsumesOneEphemeralMatchId() {
        StatsMatchRequestGate gate = new StatsMatchRequestGate();
        gate.onBedwarsGameStart(1000L);
        assertNull(gate.consumeDueMatchId(3000L));
        assertTrue(gate.onWorldLoading(4000L));
        assertNull(gate.consumeDueMatchId(5499L));
        String matchId = gate.consumeDueMatchId(5500L);
        assertNotNull(matchId);
        assertNull(gate.consumeDueMatchId(5500L));
    }

    @Test
    public void resetDiscardsAPendingRequest() {
        StatsMatchRequestGate gate = new StatsMatchRequestGate();
        gate.onBedwarsGameStart(1000L);
        assertTrue(gate.onWorldLoading(2000L));
        gate.reset();
        assertNull(gate.consumeDueMatchId(3000L));
    }

    @Test
    public void keepsTheFirstCountdownUntilTheExpectedGameWorldTransition() {
        StatsMatchRequestGate gate = new StatsMatchRequestGate();
        gate.onBedwarsGameStart(1000L);
        gate.onBedwarsGameStart(1500L);
        assertTrue(gate.onWorldLoading(2000L));
        assertNotNull(gate.consumeDueMatchId(3500L));
    }

    @Test
    public void ignoresFurtherCountdownsAfterThePostStartRequestUntilWorldReset() {
        StatsMatchRequestGate gate = new StatsMatchRequestGate();
        gate.onBedwarsGameStart(1000L);
        assertTrue(gate.onWorldLoading(2000L));
        assertNotNull(gate.consumeDueMatchId(3500L));
        gate.onBedwarsGameStart(3000L);
        assertNull(gate.consumeDueMatchId(5000L));
        gate.reset();
        gate.onBedwarsGameStart(6000L);
        assertTrue(gate.onWorldLoading(7000L));
        assertNotNull(gate.consumeDueMatchId(8500L));
    }

    @Test
    public void ignoresUnrelatedOrLateWorldLoads() {
        StatsMatchRequestGate gate = new StatsMatchRequestGate();
        assertFalse(gate.onWorldLoading(1000L));
        gate.onBedwarsGameStart(2000L);
        assertFalse(gate.onWorldLoading(17001L));
        assertNull(gate.consumeDueMatchId(20000L));
    }
}
