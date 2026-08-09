package com.snkisk.hypixellegitils.stats;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public final class BedwarsModeTest {
    @Test
    public void readsOnlyKnownVisibleBedwarsModes() {
        assertEquals(BedwarsMode.FOURS, BedwarsMode.fromVisibleSidebar("§eBED WARS", Arrays.asList("§7Mode: §a4v4v4v4")));
        assertEquals(BedwarsMode.SOLO, BedwarsMode.fromVisibleSidebar("Bed Wars", Arrays.asList("Mode: 8v1")));
        assertEquals(BedwarsMode.DOUBLES, BedwarsMode.fromVisibleSidebar("Bed Wars", Arrays.asList("Mode: 8v2")));
        assertEquals(BedwarsMode.THREES, BedwarsMode.fromVisibleSidebar("Bed Wars", Arrays.asList("Mode: 4v3v3v3")));
        assertEquals(BedwarsMode.FOUR_V_FOUR, BedwarsMode.fromVisibleSidebar("Bed Wars", Arrays.asList("Mode: 4v4")));
        assertEquals(BedwarsMode.UNKNOWN, BedwarsMode.fromVisibleSidebar("Bed Wars", Arrays.asList("Mode: Armed")));
        assertEquals(BedwarsMode.UNKNOWN, BedwarsMode.fromVisibleSidebar("Bed Wars", Arrays.asList("Mode: 4v4", "Mode: 4v4v4v4")));
        assertEquals(BedwarsMode.UNKNOWN, BedwarsMode.fromVisibleSidebar("SkyWars", Arrays.asList("Mode: 4v4v4v4")));
    }
}
