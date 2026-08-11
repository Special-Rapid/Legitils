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
        assertEquals("Active", merged.players.get(1).name);
        assertEquals(Integer.valueOf(21), merged.players.get(1).stars);
    }

    @Test
    public void unavailableWhoRefreshDoesNotEraseAlreadyDisplayedMatchStats() {
        StatsBridgeLookupResult current = StatsBridgeLookupResult.ready(Collections.singletonList(player("Eliminated", 12)));
        assertSame(current, StatsMatchResultRetention.mergeWhoRefresh(current, StatsBridgeLookupResult.unavailable()));
    }

    @Test
    public void emptyFollowupDoesNotEraseAnAlreadyShownCommunityTag() {
        StatsBridgePlayerResult tagged = player("xoyf", 1250, new StatsBridgePlayerResult.CommunityTag(
            "urchin", "Closet Cheater", "first observed tooltip"
        ));
        StatsBridgePlayerResult followup = player("xoyf", 1250);

        StatsBridgeLookupResult merged = StatsMatchResultRetention.mergeWhoRefresh(
            StatsBridgeLookupResult.ready(Collections.singletonList(tagged)),
            StatsBridgeLookupResult.ready(Collections.singletonList(followup))
        );

        assertEquals(1, merged.players.get(0).communityTags.size());
        assertEquals("Closet Cheater", merged.players.get(0).communityTags.get(0).label);
    }

    @Test
    public void returnedMemberDisplayOmitsPregamePlayerMissingFromTheCurrentRoster() {
        StatsBridgePlayerResult disconnected = player("Disconnected", 12, new StatsBridgePlayerResult.CommunityTag(
            "seraph", "Closet Cheating", "pregame only"
        ));
        StatsBridgePlayerResult active = player("Active", 20, new StatsBridgePlayerResult.CommunityTag(
            "urchin", "Caution", "known in pregame"
        ));
        StatsBridgePlayerResult activeFollowup = player("Active", 21);

        StatsBridgeLookupResult displayed = StatsMatchResultRetention.returnedMembersWithRetainedTags(
            StatsBridgeLookupResult.ready(Arrays.asList(disconnected, active)),
            StatsBridgeLookupResult.ready(Collections.singletonList(activeFollowup))
        );

        assertEquals(1, displayed.players.size());
        assertEquals("Active", displayed.players.get(0).name);
        assertEquals(1, displayed.players.get(0).communityTags.size());
        assertEquals("Caution", displayed.players.get(0).communityTags.get(0).label);
    }

    private static StatsBridgePlayerResult player(String name, int stars) {
        return player(name, stars, new StatsBridgePlayerResult.CommunityTag[0]);
    }

    private static StatsBridgePlayerResult player(
        String name,
        int stars,
        StatsBridgePlayerResult.CommunityTag... tags
    ) {
        return new StatsBridgePlayerResult(
            name, StatsBridgePlayerResult.NickStatus.KNOWN, Integer.valueOf(stars), Double.valueOf(1D), null,
            Arrays.asList(tags)
        );
    }
}
