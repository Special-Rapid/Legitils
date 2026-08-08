package com.snkisk.hypixellegitils;

import com.snkisk.hypixellegitils.alert.ChatFormat;
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
    public void versionOneNickProfilesKeepTheirSessionOnlyNametagMarker() {
        java.util.UUID nick = java.util.UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        assertTrue(HypixelLegitilsBootstrap.shouldShowNickedSessionMarker(nick));
        assertEquals(" §c[NICK]", HypixelLegitilsBootstrap.playerNametagSuffix("Nick", nick));
    }
}
