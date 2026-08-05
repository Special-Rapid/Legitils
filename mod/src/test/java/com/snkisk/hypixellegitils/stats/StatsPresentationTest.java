package com.snkisk.hypixellegitils.stats;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public final class StatsPresentationTest {
    @Test
    public void classifiesStrongBeforeEliteWithApprovedThresholds() {
        assertEquals(StatsPresentation.Tier.NONE, StatsPresentation.tierFor(player("Quiet", 99, 0.9D, 2)));
        assertEquals(StatsPresentation.Tier.ELITE, StatsPresentation.tierFor(player("Elite", 100, 0.2D, 0)));
        assertEquals(StatsPresentation.Tier.ELITE, StatsPresentation.tierFor(player("FKDR", null, 1D, null)));
        assertEquals(StatsPresentation.Tier.ELITE, StatsPresentation.tierFor(player("WS", null, null, 3)));
        assertEquals(StatsPresentation.Tier.STRONG, StatsPresentation.tierFor(player("Strong", 100, 5D, 1)));
        assertEquals(StatsPresentation.Tier.STRONG, StatsPresentation.tierFor(player("Streak", null, null, 10)));
    }

    @Test
    public void tabSuffixOmitsUnknownAndNickResults() {
        assertEquals("", StatsPresentation.tabSuffix(player("NoData", null, null, null)));
        assertEquals(" §8| §b✫120 §8| §e4.2 FKDR §8| §aWS 7", StatsPresentation.tabSuffix(player("Known", 120, 4.24D, 7)));
        assertEquals("", StatsPresentation.tabSuffix(new StatsBridgePlayerResult(
            "Nick", StatsBridgePlayerResult.NickStatus.NICKED, 120, 4.2D, 7, Collections.<StatsBridgePlayerResult.CommunityTag>emptyList()
        )));
    }

    @Test
    public void ranksStrongThenFkdrThenStarsAndProvidesOnlyProfileSummaries() {
        assertEquals(Arrays.asList("Strong", "HigherFKDR", "HigherStars"), names(StatsPresentation.rankedHighStats(Arrays.asList(
            player("HigherStars", 200, 1D, 0),
            player("HigherFKDR", 100, 2D, 0),
            player("Strong", 100, 5D, 0)
        ))));
        assertEquals("Strong §8— §b✫100 §eFKDR 5.0 §aWS 0", StatsPresentation.rankedHighStats(Arrays.asList(
            player("Strong", 100, 5D, 0)
        )).get(0).chatSummary());
    }

    @Test
    public void chatLinesAreNeutralAndSourceLabelCommunityTags() {
        StatsBridgePlayerResult player = new StatsBridgePlayerResult(
            "Strong", StatsBridgePlayerResult.NickStatus.KNOWN, 100, 5D, 0,
            Arrays.asList(new StatsBridgePlayerResult.CommunityTag("urchin", "watchlist"))
        );
        assertEquals(Arrays.asList(
            "§fBed Wars stats: §a1 §fprofiles loaded.",
            "§cStrong§7: §fStrong §8— §b✫100 §eFKDR 5.0 §aWS 0",
            "§durchin tag§7: §fStrong §8— §dwatchlist"
        ), StatsPresentation.chatLines(StatsBridgeLookupResult.ready(Arrays.asList(player))));
    }

    private static StatsBridgePlayerResult player(String name, Integer stars, Double fkdr, Integer streak) {
        return new StatsBridgePlayerResult(
            name, StatsBridgePlayerResult.NickStatus.KNOWN, stars, fkdr, streak,
            Collections.<StatsBridgePlayerResult.CommunityTag>emptyList()
        );
    }

    private static java.util.List<String> names(java.util.List<StatsPresentation.Profile> profiles) {
        java.util.List<String> names = new java.util.ArrayList<String>();
        for (StatsPresentation.Profile profile : profiles) names.add(profile.player.name);
        return names;
    }
}
