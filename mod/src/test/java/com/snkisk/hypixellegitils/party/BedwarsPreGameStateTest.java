package com.snkisk.hypixellegitils.party;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class BedwarsPreGameStateTest {
    @Test
    public void recognizesBedwarsWaitingAndStartingSidebarOnly() {
        assertEquals(true, BedwarsPreGameState.isActive("§eBED WARS", java.util.Arrays.asList("§fWaiting...")));
        assertEquals(true, BedwarsPreGameState.isActive("Bed Wars", java.util.Arrays.asList("Starting in 20s")));
        assertEquals(false, BedwarsPreGameState.isActive("SkyWars", java.util.Arrays.asList("Starting in 20s")));
        assertEquals(false, BedwarsPreGameState.isActive("Bed Wars", java.util.Arrays.asList("Mode: 4v4v4v4")));
    }

    @Test
    public void exposesOnlyTheVisiblePregamePlayerCounter() {
        BedwarsPreGameState.PlayerCount playerCount = BedwarsPreGameState.playerCount(
            "§eBED WARS",
            java.util.Arrays.asList("§fPlayers: §a12/16", "§fWaiting...")
        );
        assertEquals(true, playerCount.preGame);
        assertEquals(12, playerCount.current);
        assertEquals(16, playerCount.maximum);

        BedwarsPreGameState.PlayerCount notPregame = BedwarsPreGameState.playerCount(
            "§eBED WARS",
            java.util.Arrays.asList("§fPlayers: §a12/16", "§fMode: 4v4v4v4")
        );
        assertEquals(false, notPregame.preGame);
        assertEquals(-1, notPregame.current);
    }

    @Test
    public void stripsNonVanillaSectionCodesAndLunarEntryMarkersBeforeParsingTheVisibleCounter() {
        BedwarsPreGameState.PlayerCount playerCount = BedwarsPreGameState.playerCount(
            "§x§e§f§0§0§0§0BED WARS",
            java.util.Arrays.asList("§x§e§f§0§0§0§0Players: §a12/16🌠", "§eStarting in 6s⚽")
        );

        assertEquals(true, playerCount.preGame);
        assertEquals(12, playerCount.current);
        assertEquals(16, playerCount.maximum);
    }
}
