package com.snkisk.hypixellegitils.stats;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public final class BedwarsModeTrackerTest {
    @Test
    public void retainsOnlyTheCurrentWorldsVisibleModeWhenThePostStartSidebarOmitsMode() {
        BedwarsModeTracker tracker = new BedwarsModeTracker();
        tracker.observe(BedwarsMode.DOUBLES);
        assertEquals(BedwarsMode.DOUBLES, tracker.resolve(BedwarsMode.UNKNOWN));
        assertEquals(BedwarsMode.SOLO, tracker.resolve(BedwarsMode.SOLO));
    }

    @Test
    public void resetPreventsAStaleWorldFromSupplyingAWinStreakMode() {
        BedwarsModeTracker tracker = new BedwarsModeTracker();
        tracker.observe(BedwarsMode.FOURS);
        tracker.reset();
        assertEquals(BedwarsMode.UNKNOWN, tracker.resolve(BedwarsMode.UNKNOWN));
    }
}
