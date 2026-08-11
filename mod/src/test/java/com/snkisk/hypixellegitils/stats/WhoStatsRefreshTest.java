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
        assertTrue(WhoStatsRefresh.isRosterResponse("ONLINE: Alpha, Beta"));
        assertTrue(WhoStatsRefresh.isRosterResponse("  online: Alpha  "));
        assertFalse(WhoStatsRefresh.isRosterResponse("ONLINE:"));
        assertFalse(WhoStatsRefresh.isRosterResponse("Player is ONLINE: Alpha"));
    }

    @Test
    public void preservesTheOutgoingServerCommandAndQueuesOneSettledFullRepublish() {
        WhoStatsRefresh.Submission submission = WhoStatsRefresh.submissionFor("  /WHO party  ");
        assertEquals("  /WHO party  ", submission.outboundMessage);
        assertTrue(submission.shouldRefresh);

        WhoStatsRefresh.PendingRequests requests = new WhoStatsRefresh.PendingRequests(1);
        assertEquals("who_2_1", requests.enqueue(submission, 2L, 1000L));
        assertEquals(null, requests.enqueue(submission, 2L, 1000L));
        assertEquals(null, requests.consumeDue(1000L + WhoStatsRefresh.ROSTER_SETTLE_DELAY_MILLIS));
        assertTrue(requests.observeRosterResponse("ONLINE: Alpha, Beta", 1400L));
        assertEquals(null, requests.consumeDue(1400L + WhoStatsRefresh.ROSTER_SETTLE_DELAY_MILLIS - 1L));
        WhoStatsRefresh.Refresh refresh = requests.consumeDue(1400L + WhoStatsRefresh.ROSTER_SETTLE_DELAY_MILLIS);
        assertEquals("who_2_1", refresh.matchId);
        assertEquals("Alpha", refresh.rosterNames.get(0));
        assertEquals("Beta", refresh.rosterNames.get(1));
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
        assertTrue(requests.observeRosterResponse("ONLINE: Alpha", 3600L));
        assertEquals(null, requests.consumeDue(3600L + WhoStatsRefresh.ROSTER_SETTLE_DELAY_MILLIS - 1L));
        assertEquals(action.refreshMatchId, requests.consumeDue(3600L + WhoStatsRefresh.ROSTER_SETTLE_DELAY_MILLIS).matchId);
        assertEquals(null, WhoStatsRefresh.postStartAction(null, action.refreshMatchId));
    }

    @Test
    public void fallsBackToTheLatestRosterWhenHypixelDoesNotReplyToWho() {
        WhoStatsRefresh.PendingRequests requests = new WhoStatsRefresh.PendingRequests(1);
        assertEquals("who_1_1", requests.enqueue(WhoStatsRefresh.submissionFor("/who"), 1L, 1000L));
        assertEquals(null, requests.consumeDue(5999L));
        assertEquals("who_1_1", requests.consumeDue(6000L).matchId);
    }

    @Test
    public void fallsBackToTheServerRosterWhenReconnectSkippedTheGuiChatSubmitHook() {
        assertEquals(2, WhoStatsRefresh.rosterNames("ONLINE: Alpha, Beta, alpha, !bad").size());
        WhoStatsRefresh.PendingRequests requests = new WhoStatsRefresh.PendingRequests(2);
        assertTrue(requests.scheduleRecoveryFromRosterResponse("ONLINE: Alpha, Beta", 4L, 1000L));
        assertFalse(requests.scheduleRecoveryFromRosterResponse("ONLINE: Alpha, Beta", 4L, 1001L));
        assertEquals(null, requests.consumeDue(2499L));
        WhoStatsRefresh.Refresh refresh = requests.consumeDue(2500L);
        assertEquals("who_4_1", refresh.matchId);
        assertEquals("Alpha", refresh.rosterNames.get(0));
        assertEquals("Beta", refresh.rosterNames.get(1));
    }
}
