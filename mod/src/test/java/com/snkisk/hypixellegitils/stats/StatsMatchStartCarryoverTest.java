package com.snkisk.hypixellegitils.stats;

import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public final class StatsMatchStartCarryoverTest {
    @Test
    public void retainsResolvedPregamePlayerForTheConfirmedGameStartOnly() {
        StatsBridgeLookupResult ready = StatsBridgeLookupResult.ready(Collections.singletonList(new StatsBridgePlayerResult(
            "Disconnected", StatsBridgePlayerResult.NickStatus.KNOWN, Integer.valueOf(10), Double.valueOf(1D), null,
            Collections.singletonList(new StatsBridgePlayerResult.CommunityTag("seraph", "Closet Cheating", "visible before start"))
        )));
        assertSame(ready, StatsMatchStartCarryover.forConfirmedGameStart(true, ready));
        assertEquals(StatsBridgeLookupResult.Status.UNAVAILABLE, StatsMatchStartCarryover.forConfirmedGameStart(false, ready).status);
        assertEquals(StatsBridgeLookupResult.Status.UNAVAILABLE,
            StatsMatchStartCarryover.forConfirmedGameStart(true, StatsBridgeLookupResult.unavailable()).status);
    }
}
