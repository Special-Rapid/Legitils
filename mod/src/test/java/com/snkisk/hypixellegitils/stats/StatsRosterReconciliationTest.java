package com.snkisk.hypixellegitils.stats;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class StatsRosterReconciliationTest {
    @Test
    public void requestsOnlyVisibleProfilesMissingFromTheCurrentStatsState() {
        StatsRosterReconciliation reconciliation = new StatsRosterReconciliation();
        StatsRosterReconciliation.Request request = reconciliation.dueRequest(
            1000L, 2L, Arrays.asList(member("Resolved"), member("Missing")), result(player("Resolved"))
        );
        assertEquals("reconcile_2_1", request.matchId);
        assertEquals(1, request.players.size());
        assertEquals("Missing", request.players.get(0).name);
        assertNull(reconciliation.dueRequest(1001L, 2L, Collections.singletonList(member("Missing")), result(player("Resolved"))));
        assertNull(reconciliation.dueRequest(8000L, 2L, Collections.singletonList(member("Missing")), result(player("Resolved"))));
    }

    @Test
    public void resolvedResponseStopsFutureLookupsWhileUnavailableResponseRetriesLater() {
        StatsRosterReconciliation reconciliation = new StatsRosterReconciliation();
        StatsRosterReconciliation.Request request = reconciliation.dueRequest(
            1000L, 2L, Collections.singletonList(member("Missing")), result(player("Resolved"))
        );
        reconciliation.onResponse(request.matchId, result(player("Missing")), 1100L);
        assertNull(reconciliation.dueRequest(2500L, 2L, Collections.singletonList(member("Missing")), result(player("Resolved"), player("Missing"))));

        StatsRosterReconciliation.Request retry = reconciliation.dueRequest(
            3500L, 2L, Collections.singletonList(member("Other")), result(player("Resolved"))
        );
        reconciliation.onResponse(retry.matchId, StatsBridgeLookupResult.unavailable(), 3600L);
        assertNull(reconciliation.dueRequest(4500L, 2L, Collections.singletonList(member("Other")), result(player("Resolved"))));
        assertEquals("Other", reconciliation.dueRequest(9700L, 2L, Collections.singletonList(member("Other")), result(player("Resolved"))).players.get(0).name);
    }

    @Test
    public void unavailableCurrentStateRecoversEveryVisibleProfileAfterBridgeOrKeyRecovery() {
        StatsRosterReconciliation reconciliation = new StatsRosterReconciliation();
        StatsRosterReconciliation.Request request = reconciliation.dueRequest(
            1000L, 7L, Arrays.asList(member("First"), member("Second")), StatsBridgeLookupResult.unavailable()
        );

        assertEquals("reconcile_7_1", request.matchId);
        assertEquals(Arrays.asList("First", "Second"), Arrays.asList(request.players.get(0).name, request.players.get(1).name));
    }

    private static StatsBridgeRosterMember member(String name) {
        return new StatsBridgeRosterMember(name, null);
    }

    private static StatsBridgePlayerResult player(String name) {
        return new StatsBridgePlayerResult(
            name, StatsBridgePlayerResult.NickStatus.KNOWN, Integer.valueOf(1), Double.valueOf(1D), null,
            Collections.<StatsBridgePlayerResult.CommunityTag>emptyList()
        );
    }

    private static StatsBridgeLookupResult result(StatsBridgePlayerResult... players) {
        return StatsBridgeLookupResult.ready(Arrays.asList(players));
    }
}
