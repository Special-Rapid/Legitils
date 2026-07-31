package com.snkisk.hypixellegitils.command;

import com.snkisk.hypixellegitils.config.DetectorId;
import com.snkisk.hypixellegitils.config.NotificationChannel;
import com.snkisk.hypixellegitils.alert.ChatFormat;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LocalCommandTest {
    @Test
    public void statusCommandReturnsTheSuppliedLocalStatus() {
        assertEquals(
            "Hypixel Legitils: anti-cheat 2/5 detectors active",
            LocalCommand.responseFor(".legitils status", "Hypixel Legitils: anti-cheat 2/5 detectors active")
        );
    }

    @Test
    public void helpCommandUsesSeveralColorFormattedLines() {
        LocalCommand.Request help = LocalCommand.requestForUserInput(".legitils help", true);
        String[] lines = LocalCommand.helpLines();
        assertEquals(LocalCommand.Kind.HELP, help.kind);
        assertTrue(lines.length >= 6);
        assertTrue(lines[0].contains(ChatFormat.PREFIX));
        assertTrue(lines[2].contains(".l status"));
        assertTrue(lines[4].contains("anticheat on"));
        assertTrue(lines[5].contains("anticheat off"));
        assertTrue(lines[1].contains(".l <command>"));
        assertTrue(lines[8].contains(".l nickdetect on/off"));
        assertTrue(lines[9].contains(".l partydetect on/off"));
        assertTrue(lines[10].contains(".l dev on/off"));
        assertTrue(lines[11].contains(".l notify"));
        assertTrue(lines[15].contains(".l blacklist add/remove"));
        assertTrue(lines[16].contains("through Mojang"));
        assertTrue(!contains(lines, "partydetect method"));
        assertTrue(!contains(lines, ".l dev log"));
    }

    @Test
    public void invalidLocalNamespaceReturnsColorFormattedHelpGuidance() {
        LocalCommand.Request invalid = LocalCommand.requestForUserInput(".legitils anticheat on unknown", true);
        String[] lines = LocalCommand.invalidLocalCommandLines();
        assertEquals(LocalCommand.Kind.USAGE, invalid.kind);
        assertEquals(ChatFormat.line("§cUnknown command. §7Use §b.l help"), lines[0]);
        assertTrue(lines.length > LocalCommand.helpLines().length);
    }

    @Test
    public void anticheatCommandsExposeOnlyReleasedDetectorChoices() {
        LocalCommand.Request list = LocalCommand.requestForUserInput(".legitils anticheat list", true);
        LocalCommand.Request enable = LocalCommand.requestForUserInput(".legitils anticheat on no-slow", true);
        LocalCommand.Request desync = LocalCommand.requestForUserInput(".legitils anticheat on Blink", true);
        LocalCommand.Request airStall = LocalCommand.requestForUserInput(".legitils anticheat on Timer", true);
        LocalCommand.Request all = LocalCommand.requestForUserInput(".legitils anticheat off all", true);
        assertEquals(LocalCommand.Kind.ANTICHEAT_LIST, list.kind);
        assertEquals(LocalCommand.Kind.ANTICHEAT_SET, enable.kind);
        assertEquals(DetectorId.NO_SLOW, enable.detector);
        assertEquals(DetectorId.COMBAT_DESYNC, desync.detector);
        assertEquals(DetectorId.AIR_STALL, airStall.detector);
        assertEquals(DetectorId.COMBAT_DESYNC, LocalCommand.requestForUserInput(".legitils anticheat on combat-desync", true).detector);
        assertEquals(DetectorId.AIR_STALL, LocalCommand.requestForUserInput(".legitils anticheat on airstall", true).detector);
        assertTrue(enable.enabled);
        assertTrue(all.all);
        assertTrue(!all.enabled);
        assertEquals(DetectorId.NO_BREAK_DELAY, LocalCommand.requestForUserInput(".legitils anticheat on NoBreakDelay", true).detector);
        assertEquals(LocalCommand.Kind.MARKER_SET_ENABLED, LocalCommand.requestForUserInput(".legitils marker on", true).kind);
        assertEquals(4, LocalCommand.requestForUserInput(".legitils marker threshold 4", true).threshold);
        assertEquals(LocalCommand.Kind.MARKER_SET_THRESHOLD, LocalCommand.requestForUserInput(".legitils marker threshold 1", true).kind);
        assertEquals(LocalCommand.Kind.MARKER_SET_ENABLED, LocalCommand.requestForUserInput(".l blacklist on", true).kind);
        LocalCommand.Request nickOn = LocalCommand.requestForUserInput(".l nickdetect on", true);
        LocalCommand.Request nickOff = LocalCommand.requestForUserInput(".legitils nickdetect off", true);
        assertEquals(LocalCommand.Kind.NICK_DETECT_SET_ENABLED, nickOn.kind);
        assertTrue(nickOn.enabled);
        assertEquals(LocalCommand.Kind.NICK_DETECT_SET_ENABLED, nickOff.kind);
        assertTrue(!nickOff.enabled);
        LocalCommand.Request partyOff = LocalCommand.requestForUserInput(".l partydetect off", true);
        assertEquals(LocalCommand.Kind.PARTY_DETECT_SET_ENABLED, partyOff.kind);
        assertTrue(!partyOff.enabled);
        assertEquals(LocalCommand.Kind.USAGE, LocalCommand.requestForUserInput(".l partydetect method scoreboard", true).kind);
        LocalCommand.Request dev = LocalCommand.requestForUserInput(".l dev on", true);
        assertEquals(LocalCommand.Kind.DEV_SET_ENABLED, dev.kind);
        assertTrue(dev.enabled);
        assertEquals(LocalCommand.Kind.USAGE, LocalCommand.requestForUserInput(".l dev log on", true).kind);
        LocalCommand.Request notify = LocalCommand.requestForUserInput(".l notify actionbar on", true);
        assertEquals(LocalCommand.Kind.NOTIFICATION_SET_ENABLED, notify.kind);
        assertEquals(NotificationChannel.ACTION_BAR, notify.notificationChannel);
        assertTrue(notify.enabled);
        assertEquals(4, LocalCommand.requestForUserInput(".l blacklist threshold 4", true).threshold);
        assertEquals(LocalCommand.Kind.MARKER_CLEAR_ALL, LocalCommand.requestForUserInput(".l blacklist clear all", true).kind);
        LocalCommand.Request add = LocalCommand.requestForUserInput(".l blacklist add Valid_Name", true);
        assertEquals(LocalCommand.Kind.BLACKLIST_ADD, add.kind);
        assertEquals("Valid_Name", add.playerName);
        assertEquals(LocalCommand.Kind.BLACKLIST_REMOVE, LocalCommand.requestForUserInput(".legitils blacklist remove Valid_Name", true).kind);
        assertEquals(LocalCommand.Kind.BLACKLIST_LIST, LocalCommand.requestForUserInput(".l blacklist list", true).kind);
    }

    @Test
    public void allOtherTextPassesThroughUnchanged() {
        assertNull(LocalCommand.responseFor("/legitils status", "ignored"));
        assertNull(LocalCommand.responseFor(".other status", "ignored"));
        assertNull(LocalCommand.responseFor(".local status", "ignored"));
        assertNull(LocalCommand.responseFor("hello team", "ignored"));
        assertNull(LocalCommand.responseFor(null, "ignored"));
    }

    @Test
    public void missingStatusUsesSafeFallback() {
        assertEquals(ChatFormat.line("§cStatus unavailable."), LocalCommand.responseFor(".legitils status", null));
    }

    private static boolean contains(String[] lines, String expected) {
        for (String line : lines) {
            if (line.contains(expected)) return true;
        }
        return false;
    }

    @Test
    public void clickableRunCommandPathCannotEnterTheLocalNamespace() {
        assertNull(LocalCommand.responseForUserInput(".legitils status", false, "ignored"));
        assertNull(LocalCommand.responseForUserInput("/wdr Valid_Name", false, "ignored"));
        assertEquals("status", LocalCommand.responseForUserInput(".legitils status", true, "status"));
        assertNull(LocalCommand.requestForUserInput(".legitils help", false));
        assertNull(LocalCommand.requestForUserInput(".legitils anticheat on all", false));
    }
}
