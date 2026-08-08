package com.snkisk.hypixellegitils.stats;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public final class StatsPresentationTest {
    @Test
    public void classifiesTargetPlayersWithApprovedThresholds() {
        assertEquals(StatsPresentation.Tier.NONE, StatsPresentation.tierFor(player("Quiet", 99, 0.9D, 2)));
        assertEquals(StatsPresentation.Tier.TARGET, StatsPresentation.tierFor(player("Stars", 100, 0.2D, 0)));
        assertEquals(StatsPresentation.Tier.TARGET, StatsPresentation.tierFor(player("FKDR", null, 1D, null)));
        assertEquals(StatsPresentation.Tier.TARGET, StatsPresentation.tierFor(player("WS", null, null, 3)));
        assertEquals(StatsPresentation.Tier.TARGET, StatsPresentation.tierFor(player("HighStats", 100, 5D, 1)));
        assertEquals(StatsPresentation.Tier.TARGET, StatsPresentation.tierFor(player("Streak", null, null, 10)));
    }

    @Test
    public void tabSuffixOmitsUnknownAndNickResults() {
        assertEquals("", StatsPresentation.tabSuffix(player("NoData", null, null, null)));
        assertEquals(" §8| §f[120✫] §8| §e4.2 FKDR §8| §aWS 7", StatsPresentation.tabSuffix(player("Known", 120, 4.24D, 7)));
        assertEquals("", StatsPresentation.tabSuffix(new StatsBridgePlayerResult(
            "Nick", StatsBridgePlayerResult.NickStatus.NICKED, 120, 4.2D, 7, Collections.<StatsBridgePlayerResult.CommunityTag>emptyList()
        )));
    }

    @Test
    public void starsUseTheProvidedPrestigeTemplatesAndFkdrUsesThresholdColors() {
        assertEquals(" §8| §7[99✫] §8| §70.9 FKDR", StatsPresentation.tabSuffix(player("Gray", 99, 0.9D, null)));
        assertEquals(" §8| §f[100✫] §8| §71.0 FKDR", StatsPresentation.tabSuffix(player("One", 100, 1D, null)));
        assertEquals(" §8| §6[200✫] §8| §f1.9 FKDR", StatsPresentation.tabSuffix(player("White", 200, 1.9D, null)));
        assertEquals(" §8| §c[§61§e0§a3§b4§d✪§5] §8| §a2.0 FKDR", StatsPresentation.tabSuffix(player("Rainbow", 1034, 2D, null)));
        assertEquals(" §8| §8[§72§f04§75⚝§8] §8| §e4.0 FKDR", StatsPresentation.tabSuffix(player("Mirror", 2045, 4D, null)));
        assertEquals(" §8| §9[§b1§f2345§c✭§4] §8| §c15.0 FKDR", StatsPresentation.tabSuffix(player("Prestigious", 12345, 15D, null)));
    }

    @Test
    public void ranksTargetPlayersByFkdrThenStarsAndProvidesOnlyProfileSummaries() {
        assertEquals(Arrays.asList("HighStats", "HigherFKDR", "HigherStars"), names(StatsPresentation.rankedHighStats(Arrays.asList(
            player("HigherStars", 200, 1D, 0),
            player("HigherFKDR", 100, 2D, 0),
            player("HighStats", 100, 5D, 0)
        ))));
        assertEquals("HighStats §8— §f[100✫] §eFKDR 5.0 §aWS 0", StatsPresentation.rankedHighStats(Arrays.asList(
            player("HighStats", 100, 5D, 0)
        )).get(0).chatSummary());
    }

    @Test
    public void chatLinesUseCapturedTeamFormattingWithoutGenericHeadersOrTargetLabels() {
        StatsBridgePlayerResult player = new StatsBridgePlayerResult(
            "HighStats", StatsBridgePlayerResult.NickStatus.KNOWN, 100, 5D, 0,
            Arrays.asList(new StatsBridgePlayerResult.CommunityTag("urchin", "watchlist"))
        );
        assertEquals(Arrays.asList(
            "§cR HighStats §8— §f[100✫] §eFKDR 5.0 §aWS 0",
            "§durchin tag§7: §fHighStats §8— §dwatchlist"
        ), StatsPresentation.chatLines(
            StatsBridgeLookupResult.ready(Arrays.asList(player)),
            Collections.singletonMap("highstats", "§cR HighStats")
        ));
    }

    @Test
    public void pregameChatShowsTheChatterWithoutAPregameHeader() {
        StatsBridgePlayerResult player = new StatsBridgePlayerResult(
            "Quiet", StatsBridgePlayerResult.NickStatus.KNOWN, 12, 0.4D, 0,
            Arrays.asList(new StatsBridgePlayerResult.CommunityTag("urchin", "watchlist"))
        );
        assertEquals(Arrays.asList(
            "Quiet §8— §7[12✫] §7FKDR 0.4 §aWS 0",
            "§durchin tag§7: §fQuiet §8— §dwatchlist"
        ), StatsPresentation.pregameChatLines(StatsBridgeLookupResult.ready(Arrays.asList(player))));
    }

    @Test
    public void manualLookupShowsStatsAndIndividualSafeProviderResults() {
        StatsBridgePlayerResult player = new StatsBridgePlayerResult(
            "Player", StatsBridgePlayerResult.NickStatus.KNOWN, 12, 0.4D, 0,
            Arrays.asList(
                new StatsBridgePlayerResult.CommunityTag("provider", "Hypixel: OK"),
                new StatsBridgePlayerResult.CommunityTag("provider", "Urchin: no active tags"),
                new StatsBridgePlayerResult.CommunityTag("diagnostic", "Seraph: authorization failed")
            )
        );
        assertEquals(Arrays.asList(
            "§bStats§7: §fPlayer §8— §7[12✫] §7FKDR 0.4 §aWS 0",
            "§aAPI§7: §fHypixel: OK",
            "§aAPI§7: §fUrchin: no active tags",
            "§cAPI§7: §fSeraph: authorization failed"
        ), StatsPresentation.manualLookupLines(StatsBridgeLookupResult.ready(Arrays.asList(player))));
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
