package com.snkisk.hypixellegitils.stats;

import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public final class StatsMatchRequestGateTest {
    @Test
    public void delaysAndConsumesOneEphemeralMatchId() {
        StatsMatchRequestGate gate = new StatsMatchRequestGate();
        gate.onBedwarsGameStart(1000L);
        assertNull(gate.consumeDueMatchId(2199L));
        String matchId = gate.consumeDueMatchId(2200L);
        assertNotNull(matchId);
        assertNull(gate.consumeDueMatchId(2200L));
    }

    @Test
    public void resetDiscardsAPendingRequest() {
        StatsMatchRequestGate gate = new StatsMatchRequestGate();
        gate.onBedwarsGameStart(1000L);
        gate.reset();
        assertNull(gate.consumeDueMatchId(3000L));
    }

    @Test
    public void keepsTheFirstCountdownScheduleInsteadOfPostponingIt() {
        StatsMatchRequestGate gate = new StatsMatchRequestGate();
        gate.onBedwarsGameStart(1000L);
        gate.onBedwarsGameStart(1500L);
        assertNotNull(gate.consumeDueMatchId(2200L));
    }
}
