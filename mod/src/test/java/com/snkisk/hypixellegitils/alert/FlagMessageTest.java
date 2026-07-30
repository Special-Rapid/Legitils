package com.snkisk.hypixellegitils.alert;

import com.snkisk.hypixellegitils.config.DetectorId;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FlagMessageTest {
    private static final String PREFIX = ChatFormat.PREFIX + " ";

    @Test
    public void mockAutoBlockFormatUsesOnlyTheServerProvidedDisplayName() {
        FlagMessage message = FlagMessage.attributed(DetectorId.AUTO_BLOCK, "\u00a7eY Yellowteamplayer", "Yellowteamplayer");
        assertEquals(
            PREFIX + "\u00a7eY Yellowteamplayer \u00a7cflagged \u00a76AutoBlock \u00a77| \u00a74[WDR]",
            message.completeChatText()
        );
        assertEquals("Yellowteamplayer", message.wdrTarget);
        assertFalse(message.actionBarText.contains("WDR"));
    }

    @Test
    public void currentDetectorColoursFollowTheMockAndReservedMapping() {
        assertTrue(FlagMessage.attributed(DetectorId.NO_SLOW, "Name", "Name").completeChatText().contains("\u00a7bNoSlow"));
        assertTrue(FlagMessage.attributed(DetectorId.KILL_AURA, "Name", "Name").completeChatText().contains("\u00a7cKillAura"));
        assertTrue(FlagMessage.attributed(DetectorId.LEGIT_SCAFFOLD, "Name", "Name").completeChatText().contains("\u00a75LegitScaffold"));
        assertTrue(FlagMessage.attributed(DetectorId.COMBAT_DESYNC, "Name", "Name").completeChatText().contains("\u00a7dBlink"));
        assertTrue(FlagMessage.attributed(DetectorId.AIR_STALL, "Name", "Name").completeChatText().contains("\u00a7fTimer"));
        assertTrue(FlagMessage.attributed(DetectorId.NO_BREAK_DELAY, "Name", "Name").completeChatText().contains("\u00a7fNoBreakDelay"));
    }

    @Test
    public void grayWhiteTeamPrefixIsNormalizedToWhiteForAFlag() {
        FlagMessage message = FlagMessage.attributed(DetectorId.NO_SLOW, "§7W Whiteteamplayer", "Whiteteamplayer");
        assertTrue(message.completeChatText().contains("§fW Whiteteamplayer"));
        assertFalse(message.completeChatText().contains("§7W Whiteteamplayer"));
    }

    @Test
    public void teamFormattedNameKeepsTheVisibleTeamPrefixForNickNotices() {
        assertEquals("§cR Flaming", FlagMessage.teamFormattedName("§cR Flaming", "Flaming"));
        assertEquals("§fW White", FlagMessage.teamFormattedName("§7W White", "White"));
        assertEquals("Flaming", FlagMessage.teamFormattedName(null, "Flaming"));
    }

    @Test
    public void invalidOrMissingIdentityCannotCreateACommandTarget() {
        FlagMessage invalid = FlagMessage.attributed(DetectorId.AUTO_BLOCK, "\u00a7aG Example", "Example;op");
        FlagMessage missing = FlagMessage.attributed(DetectorId.AUTO_BLOCK, "", "Example");
        assertNull(invalid.wdrTarget);
        assertNull(missing.wdrTarget);
        assertEquals(PREFIX + "\u00a7cflagged \u00a76AutoBlock", invalid.completeChatText());
        assertFalse(FlagMessage.isValidPlayerName("with space"));
        assertTrue(FlagMessage.isValidPlayerName("Valid_Name16"));
    }

    @Test
    public void developmentSampleCanKeepItsNameWithoutOfferingWdr() {
        FlagMessage message = FlagMessage.attributed(DetectorId.NO_SLOW, "Self", "Self", false);
        assertNull(message.wdrTarget);
        assertFalse(message.completeChatText().contains("WDR"));
        assertTrue(message.completeChatText().contains("Self"));
    }

    @Test
    public void bedNukeIsAlwaysAnonymousAndNeverContainsWdr() {
        FlagMessage message = FlagMessage.attributed(DetectorId.BED_NUKE, "\u00a79B Blueteamplayer", "Blueteamplayer");
        assertEquals(PREFIX + "\u00a7cflagged \u00a74BedNuke", message.completeChatText());
        assertNull(message.wdrTarget);
    }
}
