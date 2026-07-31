package com.snkisk.hypixellegitils.party;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class PartyJoinBurstDetectorTest {
    @Test
    public void emitsEveryQuietBurstOfTwoOrMoreAcrossModes() {
        PartyJoinBurstDetector detector = new PartyJoinBurstDetector();

        assertEquals(0, detector.observeChat("one has joined (7/16)!", 100L, true));
        assertEquals(0, detector.observeChat("two has joined (8/16)!", 200L, true));
        assertEquals(2, detector.onTick(1200L, true));

        assertEquals(0, detector.observeChat("one has joined (6/12)!", 2000L, true));
        assertEquals(0, detector.observeChat("two has joined (7/12)!", 2100L, true));
        assertEquals(0, detector.observeChat("three has joined (8/12)!", 2200L, true));
        assertEquals(3, detector.onTick(3200L, true));

        assertEquals(0, detector.observeChat("one has joined (9/16)!", 4000L, true));
        assertEquals(0, detector.observeChat("two has joined (10/16)!", 4100L, true));
        assertEquals(0, detector.observeChat("three has joined (11/16)!", 4200L, true));
        assertEquals(0, detector.observeChat("four has joined (12/16)!", 4300L, true));
        assertEquals(4, detector.onTick(5300L, true));

        assertEquals(0, detector.observeChat("one has joined (9/16)!", 6000L, true));
        assertEquals(0, detector.observeChat("two has joined (10/16)!", 6100L, true));
        assertEquals(0, detector.observeChat("three has joined (11/16)!", 6200L, true));
        assertEquals(0, detector.observeChat("four has joined (12/16)!", 6300L, true));
        assertEquals(0, detector.observeChat("five has joined (13/16)!", 6400L, true));
        assertEquals(5, detector.onTick(7400L, true));
    }

    @Test
    public void ignoresDiscontinuousQuitAndNonPregameSequences() {
        PartyJoinBurstDetector detector = new PartyJoinBurstDetector();
        assertEquals(0, detector.observeChat("one has joined (7/16)!", 100L, true));
        assertEquals(0, detector.observeChat("three has joined (9/16)!", 200L, true));
        assertEquals(0, detector.onTick(1200L, true));

        assertEquals(0, detector.observeChat("one has joined (7/16)!", 2000L, true));
        assertEquals(0, detector.observeChat("two has joined (8/16)!", 2100L, true));
        assertEquals(0, detector.observeChat("two has quit!", 2200L, true));
        assertEquals(0, detector.onTick(3200L, true));

        assertEquals(0, detector.observeChat("one has joined (7/16)!", 4000L, false));
        assertEquals(0, detector.observeChat("two has joined (8/16)!", 4100L, false));
        assertEquals(0, detector.onTick(5100L, false));
    }

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
    public void stripsNonVanillaSectionCodesBeforeParsingTheVisibleCounter() {
        BedwarsPreGameState.PlayerCount playerCount = BedwarsPreGameState.playerCount(
            "§x§e§f§0§0§0§0BED WARS",
            java.util.Arrays.asList("§x§e§f§0§0§0§0Players: §a12/16", "§eWaiting...")
        );

        assertEquals(true, playerCount.preGame);
        assertEquals(12, playerCount.current);
        assertEquals(16, playerCount.maximum);
    }
}
