package com.snkisk.hypixellegitils.stats;

import com.snkisk.hypixellegitils.config.StatsSettings;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public final class StatsTabSorterTest {
    @Test
    public void disabledSortingStillKeepsEachServerTeamTogether() {
        List<String> actual = StatsTabSorter.sort(entries(), settings(false, false));
        assertEquals(Arrays.asList("BlueLow", "BlueNick", "BlueHigh", "RedFive", "RedNick", "RedUnknown"), actual);
    }

    @Test
    public void playerSortingPlacesNickFirstThenFkdrAndKeepsTiesStable() {
        List<String> actual = StatsTabSorter.sort(entries(), settings(false, true));
        assertEquals(Arrays.asList("BlueNick", "BlueHigh", "BlueLow", "RedNick", "RedFive", "RedUnknown"), actual);
    }

    @Test
    public void teamSortingUsesFkdrSumPlusFiveForEveryNick() {
        List<String> actual = StatsTabSorter.sort(entries(), settings(true, false));
        // Red = 10.0 + 5.0; Blue = 3.0 + 5.0, so Red moves ahead without changing either team internally.
        assertEquals(Arrays.asList("RedFive", "RedNick", "RedUnknown", "BlueLow", "BlueNick", "BlueHigh"), actual);
    }

    @Test
    public void teamThenPlayerSortingUsesBothOptInsAndLeavesUnteamedEntriesFixed() {
        List<StatsTabSorter.Entry<String>> entries = entries();
        entries.add(new StatsTabSorter.Entry<String>("Lobby", null, false, 100D, 6));
        List<String> actual = StatsTabSorter.sort(entries, settings(true, true));
        assertEquals(Arrays.asList("RedNick", "RedFive", "RedUnknown", "BlueNick", "BlueHigh", "BlueLow", "Lobby"), actual);
    }

    @Test
    public void unteamedEntriesAreNotIncludedInTeamScoringOrReorderedAcrossTheirSegment() {
        List<StatsTabSorter.Entry<String>> entries = new java.util.ArrayList<StatsTabSorter.Entry<String>>(Arrays.asList(
            new StatsTabSorter.Entry<String>("BlueOne", "blue", false, 1D, 0),
            new StatsTabSorter.Entry<String>("Lobby", null, false, 999D, 1),
            new StatsTabSorter.Entry<String>("RedOne", "red", false, 10D, 2),
            new StatsTabSorter.Entry<String>("BlueTwo", "blue", false, 2D, 3),
            new StatsTabSorter.Entry<String>("RedTwo", "red", false, 5D, 4)
        ));
        List<String> actual = StatsTabSorter.sort(entries, settings(true, false));
        assertEquals(Arrays.asList("BlueOne", "BlueTwo", "Lobby", "RedOne", "RedTwo"), actual);
    }

    private static List<StatsTabSorter.Entry<String>> entries() {
        return new java.util.ArrayList<StatsTabSorter.Entry<String>>(Arrays.asList(
            new StatsTabSorter.Entry<String>("BlueLow", "blue", false, 1D, 0),
            new StatsTabSorter.Entry<String>("RedFive", "red", false, 10D, 1),
            new StatsTabSorter.Entry<String>("BlueNick", "blue", true, null, 2),
            new StatsTabSorter.Entry<String>("RedNick", "red", true, null, 3),
            new StatsTabSorter.Entry<String>("BlueHigh", "blue", false, 2D, 4),
            new StatsTabSorter.Entry<String>("RedUnknown", "red", false, null, 5)
        ));
    }

    private static StatsSettings settings(boolean teams, boolean players) {
        return new StatsSettings(true, true, true, true, true, true, false, 1D, teams, players);
    }
}
