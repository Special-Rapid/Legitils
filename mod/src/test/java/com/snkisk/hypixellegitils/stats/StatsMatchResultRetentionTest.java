package com.snkisk.hypixellegitils.stats;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public final class StatsMatchResultRetentionTest {
    @Test
    public void whoRefreshRetainsEliminatedPlayersAndReplacesReturnedPlayers() {
        StatsBridgePlayerResult eliminated = player("Eliminated", 12);
        StatsBridgePlayerResult activeBefore = player("Active", 20);
        StatsBridgePlayerResult activeAfter = player("Active", 21);

        StatsBridgeLookupResult merged = StatsMatchResultRetention.mergeWhoRefresh(
            StatsBridgeLookupResult.ready(Arrays.asList(eliminated, activeBefore)),
            StatsBridgeLookupResult.ready(Collections.singletonList(activeAfter))
        );

        assertEquals(2, merged.players.size());
        assertSame(eliminated, merged.players.get(0));
        assertSame(activeAfter, merged.players.get(1));
    }

    @Test
    public void unavailableWhoRefreshDoesNotEraseAlreadyDisplayedMatchStats() {
        StatsBridgeLookupResult current = StatsBridgeLookupResult.ready(Collections.singletonList(player("Eliminated", 12)));
        assertSame(current, StatsMatchResultRetention.mergeWhoRefresh(current, StatsBridgeLookupResult.unavailable()));
    }

    private static StatsBridgePlayerResult player(String name, int stars) {
        return new StatsBridgePlayerResult(
            name, StatsBridgePlayerResult.NickStatus.KNOWN, Integer.valueOf(stars), Double.valueOf(1D), null,
            Collections.<StatsBridgePlayerResult.CommunityTag>emptyList()
        );
    }
}
