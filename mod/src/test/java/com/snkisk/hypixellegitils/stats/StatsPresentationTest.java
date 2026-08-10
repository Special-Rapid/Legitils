package com.snkisk.hypixellegitils.stats;

import com.snkisk.hypixellegitils.config.StatsSettings;
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
    public void nametagFKDRIsOptInAndFiltersByTheConfiguredThreshold() {
        StatsSettings disabled = StatsSettings.defaults();
        StatsSettings enabled = new StatsSettings(true, true, true, true, true, true, true, 3.5D);
        assertEquals("", StatsPresentation.nametagFkdrSuffix(player("Low", 10, 3.4D, null), enabled));
        assertEquals(" §a3.5 FKDR", StatsPresentation.nametagFkdrSuffix(player("Match", 10, 3.5D, null), enabled));
        assertEquals("", StatsPresentation.nametagFkdrSuffix(player("High", 10, 9D, null), disabled));
        assertEquals("", StatsPresentation.nametagFkdrSuffix(new StatsBridgePlayerResult(
            "Nick", StatsBridgePlayerResult.NickStatus.NICKED, 10, 9D, null, Collections.<StatsBridgePlayerResult.CommunityTag>emptyList()
        ), enabled));
    }

    @Test
    public void providerTagsUseOneSharedAbbreviationVocabularyAcrossTabAndNametag() {
        StatsBridgePlayerResult player = new StatsBridgePlayerResult(
            "Tagged", StatsBridgePlayerResult.NickStatus.KNOWN, null, null, null,
            Arrays.asList(
                new StatsBridgePlayerResult.CommunityTag("seraph", "Blatant Cheating", "vape v4"),
                new StatsBridgePlayerResult.CommunityTag("urchin", "Legit Sniper", "queued repeatedly")
            )
        );
        assertEquals(" §8| §6[BC] §8| §c[LS]", StatsPresentation.tabSuffix(player));
        assertEquals(" §6[BC] §c[LS]", StatsPresentation.nametagTagSuffix(player));
        assertEquals(true, StatsPresentation.hasCommunityAdvisoryTag(player));
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
            Arrays.asList(new StatsBridgePlayerResult.CommunityTag("urchin", "Legit Sniper"))
        );
        assertEquals(Arrays.asList(
            "§cR HighStats §8— §f[100✫] §eFKDR 5.0 §aWS 0",
            "§c[LS] §8— §cR HighStats"
        ), StatsPresentation.chatLines(
            StatsBridgeLookupResult.ready(Arrays.asList(player)),
            Collections.singletonMap("highstats", "§cR HighStats")
        ));
    }

    @Test
    public void renderedTabNamesUseTheCurrentRosterPixelWidthAndPreserveThirdPartySuffixes() {
        StatsDisplayNameColumns columns = new StatsDisplayNameColumns();
        String beeStar = "§f[100✫]";
        columns.beginTabRender();
        assertEquals("", columns.observeTabName("Short", "§cR Short [190]", 64, 4, 5, "§c[12345✭]", 40));
        assertEquals("", columns.observeTabName("Bee", "§aG Bee", 33, 4, 5, beeStar, 28));
        columns.finishTabRender();

        String paddedBee = "§aG Bee§r" + spaces(4) + "§l" + spaces(3) + "§r";
        String paddedBeeStar = beeStar + "§r" + spaces(3) + "§r";
        assertEquals("§cR Short [190]", columns.nameForChat("Short", "§cR Short"));
        assertEquals(paddedBee, columns.nameForChat("Bee", "§aG Bee"));
        assertEquals("", columns.starPadding("Short", "§c[12345✭]"));
        assertEquals("§r" + spaces(3) + "§r", columns.starPadding("Bee", beeStar));
        assertEquals("§r" + spaces(3) + "§r", columns.starPadding("Bee", "§f[101✫]", 28));

        StatsBridgePlayerResult player = player("Bee", 100, 2D, null);
        assertEquals(paddedBee + " §8— " + paddedBeeStar + " §aFKDR 2.0", StatsPresentation.chatNotices(
            StatsBridgeLookupResult.ready(Collections.singletonList(player)),
            Collections.singletonMap("bee", paddedBee),
            Collections.singletonMap("bee", columns.starPadding("Bee", beeStar))
        ).get(0).text);

        columns.beginTabRender();
        columns.observeTabName("Jo", "§cR Jo", 21, 4, 5, "", 0);
        columns.observeTabName("Ava", "§aG Ava", 24, 4, 5, "", 0);
        columns.finishTabRender();
        assertEquals("§cR Jo§r" + spaces(1) + "§r", columns.nameForChat("Jo", "§cR Jo"));
        assertEquals("§aG Ava", columns.nameForChat("Ava", "§aG Ava"));
    }

    @Test
    public void communityTagChatNoticeCarriesTheBoundedTooltipButNotTheDisplayLine() {
        StatsBridgePlayerResult player = new StatsBridgePlayerResult(
            "Tagged", StatsBridgePlayerResult.NickStatus.KNOWN, null, null, null,
            Collections.singletonList(new StatsBridgePlayerResult.CommunityTag("urchin", "Closet Cheater", "vape v4\n- Added by @hexze"))
        );
        StatsPresentation.ChatNotice notice = StatsPresentation.chatNotices(
            StatsBridgeLookupResult.ready(Collections.singletonList(player)), Collections.<String, String>emptyMap()
        ).get(0);
        assertEquals("§6[CC] §8— §fTagged", notice.text);
        assertEquals("§6§lCloset Cheater\n§7vape v4\n§7- Added by @hexze", notice.tooltip);
    }

    @Test
    public void pregameChatShowsTheChatterWithoutAPregameHeader() {
        StatsBridgePlayerResult player = new StatsBridgePlayerResult(
            "Quiet", StatsBridgePlayerResult.NickStatus.KNOWN, 12, 0.4D, 0,
            Arrays.asList(new StatsBridgePlayerResult.CommunityTag("urchin", "Legit Sniper"))
        );
        assertEquals(Arrays.asList(
            "Quiet §8— §7[12✫] §7FKDR 0.4 §aWS 0",
            "§c[LS] §8— §fQuiet"
        ), StatsPresentation.pregameChatLines(StatsBridgeLookupResult.ready(Arrays.asList(player))));
    }

    @Test
    public void pregameStatsAndTagReuseTheCachedRenderedTabField() {
        StatsBridgePlayerResult player = new StatsBridgePlayerResult(
            "Quiet", StatsBridgePlayerResult.NickStatus.KNOWN, 12, 0.4D, null,
            Collections.singletonList(new StatsBridgePlayerResult.CommunityTag("urchin", "Legit Sniper", "queue pattern"))
        );
        String rendered = "§cR Quiet [190]";
        java.util.List<StatsPresentation.ChatNotice> notices = StatsPresentation.pregameChatNotices(
            StatsBridgeLookupResult.ready(Collections.singletonList(player)),
            Collections.singletonMap("quiet", rendered)
        );
        assertEquals(rendered + " §8— §7[12✫] §7FKDR 0.4", notices.get(0).text);
        assertEquals("§c[LS] §8— " + rendered, notices.get(1).text);
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

    @Test
    public void manualLookupKeepsTagHoverOnTheAbbreviation() {
        StatsBridgePlayerResult player = new StatsBridgePlayerResult(
            "Player", StatsBridgePlayerResult.NickStatus.KNOWN, null, null, null,
            Collections.singletonList(new StatsBridgePlayerResult.CommunityTag("seraph", "Blatant Cheating", "vape v4"))
        );
        StatsPresentation.ChatNotice notice = StatsPresentation.manualLookupNotices(
            StatsBridgeLookupResult.ready(Collections.singletonList(player))
        ).get(1);
        assertEquals("§6[BC] §8— §fPlayer", notice.text);
        assertEquals("§6[BC]", notice.tagCode);
        assertEquals("§6§lBlatant Cheating\n§7vape v4", notice.tooltip);
    }

    @Test
    public void manualStatsAndTagReuseTheCachedRenderedTabField() {
        StatsBridgePlayerResult player = new StatsBridgePlayerResult(
            "Player", StatsBridgePlayerResult.NickStatus.KNOWN, 12, 0.4D, null,
            Collections.singletonList(new StatsBridgePlayerResult.CommunityTag("seraph", "Blatant Cheating", "vape v4"))
        );
        String rendered = "§aG Player [190]";
        java.util.List<StatsPresentation.ChatNotice> notices = StatsPresentation.manualLookupNotices(
            StatsBridgeLookupResult.ready(Collections.singletonList(player)),
            Collections.singletonMap("player", rendered)
        );
        assertEquals("§bStats§7: " + rendered + " §8— §7[12✫] §7FKDR 0.4", notices.get(0).text);
        assertEquals("§6[BC] §8— " + rendered, notices.get(1).text);
    }

    @Test
    public void tagHoverUsesAColoredTitleAndWrapsTheProviderExplanation() {
        StatsBridgePlayerResult player = new StatsBridgePlayerResult(
            "Player", StatsBridgePlayerResult.NickStatus.KNOWN, null, null, null,
            Collections.singletonList(new StatsBridgePlayerResult.CommunityTag(
                "urchin", "Confirmed Cheater",
                "vape v4 (legitscaff, aa + ac, hitselect, autoblockhit, visuals)"
            ))
        );
        StatsPresentation.ChatNotice notice = StatsPresentation.manualLookupNotices(
            StatsBridgeLookupResult.ready(Collections.singletonList(player))
        ).get(1);
        assertEquals("§5[CF] §8— §fPlayer", notice.text);
        assertEquals("§5§lConfirmed Cheater\n§7vape v4 (legitscaff, aa + ac, hitselect,\n§7autoblockhit, visuals)", notice.tooltip);
    }

    private static StatsBridgePlayerResult player(String name, Integer stars, Double fkdr, Integer streak) {
        return new StatsBridgePlayerResult(
            name, StatsBridgePlayerResult.NickStatus.KNOWN, stars, fkdr, streak,
            Collections.<StatsBridgePlayerResult.CommunityTag>emptyList()
        );
    }

    private static String spaces(int count) {
        StringBuilder spaces = new StringBuilder(count);
        for (int index = 0; index < count; index++) spaces.append(' ');
        return spaces.toString();
    }

    private static java.util.List<String> names(java.util.List<StatsPresentation.Profile> profiles) {
        java.util.List<String> names = new java.util.ArrayList<String>();
        for (StatsPresentation.Profile profile : profiles) names.add(profile.player.name);
        return names;
    }
}
