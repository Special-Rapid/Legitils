package com.snkisk.hypixellegitils.stats;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class WhoStatsRefreshTest {
    @Test
    public void recognizesOnlyTheWhoCommandAndKeepsArgumentsServerOwned() {
        assertTrue(WhoStatsRefresh.isWhoCommand("/who"));
        assertTrue(WhoStatsRefresh.isWhoCommand("  /WHO party  "));
        assertFalse(WhoStatsRefresh.isWhoCommand("/whom"));
        assertFalse(WhoStatsRefresh.isWhoCommand("who"));
        assertFalse(WhoStatsRefresh.isWhoCommand(".l who"));
    }

    @Test
    public void preservesTheOutgoingServerCommandAndQueuesOneSettledFullRepublish() {
        WhoStatsRefresh.Submission submission = WhoStatsRefresh.submissionFor("  /WHO party  ");
        assertEquals("  /WHO party  ", submission.outboundMessage);
        assertTrue(submission.shouldRefresh);

        WhoStatsRefresh.PendingRequests requests = new WhoStatsRefresh.PendingRequests(1);
        assertEquals("who_2_1", requests.enqueue(submission, 2L, 1000L));
        assertEquals(null, requests.enqueue(submission, 2L, 1000L));
        assertEquals(null, requests.consumeDue(1000L + WhoStatsRefresh.ROSTER_SETTLE_DELAY_MILLIS - 1L));
        assertEquals("who_2_1", requests.consumeDue(1000L + WhoStatsRefresh.ROSTER_SETTLE_DELAY_MILLIS));
        assertEquals(null, requests.consumeDue(3000L));
    }

    @Test
    public void producesBoundedOpaqueIdsThatForceTheExistingCompanionRefreshPath() {
        assertEquals("who_2_7", WhoStatsRefresh.matchId(2L, 7L));
        assertEquals("who_z_10", WhoStatsRefresh.matchId(35L, 36L));
    }

    @Test
    public void postStartGateProducesOneAutomaticWhoRefreshThenOneSettledFullRepublish() {
        StatsMatchRequestGate gate = new StatsMatchRequestGate();
        gate.onBedwarsGameStart(1000L);
        assertTrue(gate.onWorldLoading(2000L));
        String postStart = gate.consumeDueMatchId(3500L);
        assertTrue(postStart != null);
        assertEquals(null, gate.consumeDueMatchId(3500L));

        WhoStatsRefresh.PendingRequests requests = new WhoStatsRefresh.PendingRequests(1);
        WhoStatsRefresh.PostStartAction action = WhoStatsRefresh.postStartAction(postStart, requests.nextAutomaticMatchId(3L));
        assertEquals("/who", action.outboundCommand);
        assertEquals("who_3_1", action.refreshMatchId);
        assertFalse(action.refreshMatchId.equals(postStart));
        assertTrue(requests.enqueue(action.refreshMatchId, 3500L));
        assertEquals(null, requests.consumeDue(3500L + WhoStatsRefresh.ROSTER_SETTLE_DELAY_MILLIS - 1L));
        assertEquals(action.refreshMatchId, requests.consumeDue(3500L + WhoStatsRefresh.ROSTER_SETTLE_DELAY_MILLIS));
        assertEquals(null, WhoStatsRefresh.postStartAction(null, action.refreshMatchId));
    }
}
