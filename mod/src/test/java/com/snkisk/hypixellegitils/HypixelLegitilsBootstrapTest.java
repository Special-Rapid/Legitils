package com.snkisk.hypixellegitils;

import com.snkisk.hypixellegitils.alert.ChatFormat;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Queue;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.LegitilsTestTextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class HypixelLegitilsBootstrapTest {
    @Test
    public void companionConfigurationNoticeIsDeliveredOnceToTheMinecraftChatQueue() {
        HypixelLegitilsBootstrap.drainPendingConfigurationNotices();

        HypixelLegitilsBootstrap.enqueueCompanionSettingsApplied(42L);

        assertArrayEquals(new String[] {
            ChatFormat.line("§aCompanion settings applied. §7Revision §f42")
        }, HypixelLegitilsBootstrap.drainPendingConfigurationNotices());
        assertArrayEquals(new String[0], HypixelLegitilsBootstrap.drainPendingConfigurationNotices());
    }

    @Test
    public void pregameNickNoticeUsesTheCapturedTeamFormattedName() {
        assertEquals(
            ChatFormat.line("§c§lR §cRedteamplayer§5 is nicked."),
            HypixelLegitilsBootstrap.pregameNickNotice("Redteamplayer", "§cR Redteamplayer")
        );
    }

    @Test
    public void pregameNickChatNoticeIsImmediateWithoutInventingATeamColour() {
        assertEquals(
            ChatFormat.line("§fPregameNick§5 is nicked."),
            HypixelLegitilsBootstrap.pregameNickChatNotice("PregameNick")
        );
    }

    @Test
    public void currentNickAliasPromotesOnlyThatVisibleRosterEntryToNicked() {
        com.snkisk.hypixellegitils.stats.StatsBridgePlayerResult visible =
            new com.snkisk.hypixellegitils.stats.StatsBridgePlayerResult(
                "PregameNick", com.snkisk.hypixellegitils.stats.StatsBridgePlayerResult.NickStatus.UNAVAILABLE,
                null, null, null, Collections.<com.snkisk.hypixellegitils.stats.StatsBridgePlayerResult.CommunityTag>emptyList()
            );
        com.snkisk.hypixellegitils.stats.StatsBridgeLookupResult result =
            com.snkisk.hypixellegitils.stats.StatsBridgeLookupResult.ready(Collections.singletonList(visible));

        assertEquals(
            com.snkisk.hypixellegitils.stats.StatsBridgePlayerResult.NickStatus.NICKED,
            HypixelLegitilsBootstrap.withSessionNickStatuses(result, Collections.singleton("pregamenick")).players.get(0).nickStatus
        );
    }

    @Test
    public void sessionNickAliasPreventsASecondAlertWhenTheUuidProfileArrivesLater() {
        assertTrue(HypixelLegitilsBootstrap.hasSessionNickAlias(Collections.singleton("pregamenick"), "PregameNick"));
    }

    @Test
    public void companionPregameNickResultClaimsOnlyTheVisibleAliasOnce() {
        com.snkisk.hypixellegitils.stats.StatsBridgePlayerResult nicked =
            new com.snkisk.hypixellegitils.stats.StatsBridgePlayerResult(
                "PregameNick", com.snkisk.hypixellegitils.stats.StatsBridgePlayerResult.NickStatus.NICKED,
                null, null, null, Collections.<com.snkisk.hypixellegitils.stats.StatsBridgePlayerResult.CommunityTag>emptyList()
            );
        java.util.Set<String> aliases = new java.util.HashSet<String>();

        assertTrue(HypixelLegitilsBootstrap.claimPregameBridgeNick(aliases, nicked));
        assertTrue(HypixelLegitilsBootstrap.hasSessionNickAlias(aliases, "pregamenick"));
        assertTrue(!HypixelLegitilsBootstrap.claimPregameBridgeNick(aliases, nicked));
    }

    @Test
    public void pendingStatsAndNickChatAreDisplayedOnlyWhileTheirFeaturesRemainEnabled() {
        com.snkisk.hypixellegitils.config.StatsSettings defaults =
            com.snkisk.hypixellegitils.config.StatsSettings.defaults();
        assertTrue(HypixelLegitilsBootstrap.shouldDisplayPendingStatsChat(defaults));
        assertTrue(!HypixelLegitilsBootstrap.shouldDisplayPendingStatsChat(
            new com.snkisk.hypixellegitils.config.StatsSettings(false, true, true, true, true, true)
        ));
        assertTrue(!HypixelLegitilsBootstrap.shouldDisplayPendingStatsChat(
            new com.snkisk.hypixellegitils.config.StatsSettings(true, true, true, true, true, false)
        ));
        assertTrue(HypixelLegitilsBootstrap.shouldDisplayPendingNickNotices(true));
        assertTrue(!HypixelLegitilsBootstrap.shouldDisplayPendingNickNotices(false));
    }

    @Test
    public void disablingFeaturesDropsAlreadyQueuedAutomaticAndPregameChatOutput() throws Exception {
        com.snkisk.hypixellegitils.config.StatsSettings originalStatsSettings =
            (com.snkisk.hypixellegitils.config.StatsSettings) staticField("statsSettings").get(null);
        boolean originalNickDetectionEnabled = staticField("nickDetectionEnabled").getBoolean(null);
        Queue<Object> statsNotices = queue("PENDING_STATS_NOTICES");
        Queue<Object> statsPublications = queue("PENDING_STATS_PUBLICATIONS");
        Queue<Object> nickNotices = queue("PENDING_NICK_NOTICES");
        Queue<Object> teamNickNotices = queue("PENDING_TEAM_NICK_NOTICES");
        try {
            statsNotices.add(newPendingStatsNotice("pregame Stats"));
            statsNotices.add(newPendingStatsNotice("automatic Stats"));
            statsPublications.add(newPendingStatsPublication());
            staticField("statsSettings").set(null,
                new com.snkisk.hypixellegitils.config.StatsSettings(false, true, true, true, true, true));

            assertEquals(0, HypixelLegitilsBootstrap.drainPendingStatsNotices().length);
            assertTrue(statsNotices.isEmpty());
            assertTrue(statsPublications.isEmpty());

            statsNotices.add(newPendingStatsNotice("chat disabled Stats"));
            staticField("statsSettings").set(null,
                new com.snkisk.hypixellegitils.config.StatsSettings(true, true, true, true, true, false));

            assertEquals(0, HypixelLegitilsBootstrap.drainPendingStatsNotices().length);
            assertTrue(statsNotices.isEmpty());

            nickNotices.add("pregame Nick");
            teamNickNotices.add(newPendingTeamNickNotice());
            staticField("nickDetectionEnabled").setBoolean(null, false);

            assertEquals(0, HypixelLegitilsBootstrap.drainPendingNickNotices().length);
            assertEquals(0, HypixelLegitilsBootstrap.drainPendingTeamNickNotices(System.currentTimeMillis()).length);
            assertTrue(nickNotices.isEmpty());
            assertTrue(teamNickNotices.isEmpty());
        } finally {
            statsNotices.clear();
            statsPublications.clear();
            nickNotices.clear();
            teamNickNotices.clear();
            staticField("statsSettings").set(null, originalStatsSettings);
            staticField("nickDetectionEnabled").setBoolean(null, originalNickDetectionEnabled);
        }
    }

    @SuppressWarnings("unchecked")
    private static Queue<Object> queue(String fieldName) throws Exception {
        return (Queue<Object>) staticField(fieldName).get(null);
    }

    private static Field staticField(String fieldName) throws Exception {
        Field field = HypixelLegitilsBootstrap.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field;
    }

    private static Object newPendingStatsNotice(String text) throws Exception {
        Class<?> type = Class.forName(HypixelLegitilsBootstrap.class.getName() + "$PendingStatsNotice");
        Constructor<?> constructor = type.getDeclaredConstructor(String.class, java.util.List.class);
        constructor.setAccessible(true);
        return constructor.newInstance(text, Collections.emptyList());
    }

    private static Object newPendingStatsPublication() throws Exception {
        Class<?> type = Class.forName(HypixelLegitilsBootstrap.class.getName() + "$PendingStatsPublication");
        Constructor<?> constructor = type.getDeclaredConstructor(
            com.snkisk.hypixellegitils.stats.StatsBridgeLookupResult.class,
            java.util.Map.class,
            Long.TYPE,
            Long.TYPE
        );
        constructor.setAccessible(true);
        return constructor.newInstance(
            com.snkisk.hypixellegitils.stats.StatsBridgeLookupResult.unavailable(),
            Collections.emptyMap(),
            0L,
            System.currentTimeMillis()
        );
    }

    private static Object newPendingTeamNickNotice() throws Exception {
        Class<?> type = Class.forName(HypixelLegitilsBootstrap.class.getName() + "$PendingTeamNickNotice");
        Constructor<?> constructor = type.getDeclaredConstructor(UUID.class, String.class, Long.TYPE);
        constructor.setAccessible(true);
        return constructor.newInstance(UUID.randomUUID(), "PregameNick", 0L);
    }

    @Test
    public void chatColumnsFindOneExactlyReachablePixelEndForVariableGlyphWidths() {
        assertEquals(38, HypixelLegitilsBootstrap.alignedChatColumnEnd(33, java.util.Arrays.asList(33, 30), 4, 5));
        assertEquals("§r§l §r", HypixelLegitilsBootstrap.chatPixelPadding(5, 4, 5));
        assertEquals("§r  §r", HypixelLegitilsBootstrap.chatPixelPadding(8, 4, 5));
    }

    @Test
    public void versionOneNickProfilesKeepTheirSessionOnlyNametagMarker() {
        java.util.UUID nick = java.util.UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        assertTrue(HypixelLegitilsBootstrap.shouldShowNickedSessionMarker(nick));
        assertEquals(" §c[NICK]", HypixelLegitilsBootstrap.playerNametagSuffix("Nick", nick));
        assertEquals("§c[NICK]", HypixelLegitilsBootstrap.lunarNickedLevelText("37", nick));
    }

    @Test
    public void lunarLevelHeadKeepsRealProfilesAndEmptyValuesUntouched() {
        java.util.UUID real = java.util.UUID.fromString("123e4567-e89b-42d3-a456-426655440000");

        assertEquals("37", HypixelLegitilsBootstrap.lunarNickedLevelText("37", real));
        assertEquals("", HypixelLegitilsBootstrap.lunarNickedLevelText("", real));
        assertEquals(null, HypixelLegitilsBootstrap.lunarNickedLevelText(null, real));
    }

    @Test
    public void lunarLevelHeadRemovesOnlyTheSelectedLevelSourcePrefix() {
        assertEquals("", HypixelLegitilsBootstrap.lunarLevelHeadPrefixText("Level"));
        assertEquals("", HypixelLegitilsBootstrap.lunarLevelHeadPrefixText("§fBedWars Level:"));
        assertEquals("", HypixelLegitilsBootstrap.lunarLevelHeadPrefixText("SkyWars Level"));
        assertEquals("Prestige", HypixelLegitilsBootstrap.lunarLevelHeadPrefixText("Prestige"));
        assertEquals("37", HypixelLegitilsBootstrap.lunarLevelHeadPrefixText("37"));
        assertEquals("§a427✫", HypixelLegitilsBootstrap.lunarLevelHeadPrefixText("§a427✫"));
    }

    @Test
    public void allLocalMarkersMoveFromTheNameLineToAnActiveLunarLevelHead() {
        java.util.UUID nick = java.util.UUID.fromString("223e4567-e89b-12d3-a456-426655440000");

        assertEquals(" §c[NICK]", HypixelLegitilsBootstrap.playerNametagSuffix("Nick", nick));
        assertEquals("", HypixelLegitilsBootstrap.lunarLevelHeadSuffix("Nick", nick));
        HypixelLegitilsBootstrap.onLunarLevelHeadRendered(nick);
        assertEquals("", HypixelLegitilsBootstrap.playerNametagSuffix("Nick", nick));
    }

    @Test
    public void lunarNametagComponentKeepsExistingContentAndAppendsTheLegacySuffixAsAChild() {
        java.util.UUID nick = java.util.UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        LegitilsTestTextComponent original = new LegitilsTestTextComponent("Nick", Collections.<Object>emptyList());

        LegitilsTestTextComponent updated = (LegitilsTestTextComponent)
            HypixelLegitilsBootstrap.appendLunarNametagComponentSuffix(original, "Nick", nick);

        assertEquals("Nick", updated.content);
        assertEquals(1, updated.children().size());
        assertEquals(" §c[NICK]", ((LegacyComponentSerializer.LegacyText) updated.children().get(0)).text);
    }
}
