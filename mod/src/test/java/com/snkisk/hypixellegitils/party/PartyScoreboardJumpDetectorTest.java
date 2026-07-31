package com.snkisk.hypixellegitils.party;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class PartyScoreboardJumpDetectorTest {
    @Test
    public void reportsOnlySimultaneousPregamePlayerCountChanges() {
        PartyScoreboardJumpDetector detector = new PartyScoreboardJumpDetector();

        assertEquals(0, detector.observe(preGameCount(7, 16)));
        assertEquals(4, detector.observe(preGameCount(11, 16)));
        assertEquals(0, detector.observe(preGameCount(12, 16)));
        assertEquals(2, detector.observe(preGameCount(14, 16)));
        assertEquals(-3, detector.observe(preGameCount(11, 16)));
        assertEquals(0, detector.observe(preGameCount(10, 16)));
    }

    @Test
    public void resetsWhenTheSidebarLeavesPregameOrItsMaximumChanges() {
        PartyScoreboardJumpDetector detector = new PartyScoreboardJumpDetector();

        assertEquals(0, detector.observe(preGameCount(8, 16)));
        assertEquals(0, detector.observe(BedwarsPreGameState.playerCount("Bed Wars", java.util.Arrays.asList("Mode: 4v4v4v4"))));
        assertEquals(0, detector.observe(preGameCount(12, 16)));
        assertEquals(0, detector.observe(preGameCount(8, 12)));
        assertEquals(3, detector.observe(preGameCount(11, 12)));
    }

    private static BedwarsPreGameState.PlayerCount preGameCount(int current, int maximum) {
        return BedwarsPreGameState.playerCount(
            "Bed Wars",
            java.util.Arrays.asList("Players: " + current + "/" + maximum, "Starting in 10s")
        );
    }
}
