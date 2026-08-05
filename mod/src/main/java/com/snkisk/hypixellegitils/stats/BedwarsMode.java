package com.snkisk.hypixellegitils.stats;

import java.util.Locale;

/** Maps only the visible Bed Wars sidebar mode to the documented stats key suffix. */
public enum BedwarsMode {
    SOLO("8v1", "eight_one_winstreak"),
    DOUBLES("8v2", "eight_two_winstreak"),
    THREES("4v3v3v3", "four_three_winstreak"),
    FOURS("4v4v4v4", "four_four_winstreak"),
    FOUR_V_FOUR("4v4", "two_four_winstreak"),
    UNKNOWN(null, null);

    public final String sidebarValue;
    public final String statsKey;

    BedwarsMode(String sidebarValue, String statsKey) {
        this.sidebarValue = sidebarValue;
        this.statsKey = statsKey;
    }

    public static BedwarsMode fromSidebarValue(String rawValue) {
        String value = normalized(rawValue);
        for (BedwarsMode mode : values()) {
            if (mode.sidebarValue != null && mode.sidebarValue.equals(value)) return mode;
        }
        return UNKNOWN;
    }

    public static BedwarsMode fromVisibleSidebar(String title, Iterable<String> lines) {
        if (!"bed wars".equals(normalized(title)) || lines == null) return UNKNOWN;
        BedwarsMode found = null;
        for (String line : lines) {
            String value = normalized(line);
            if (!value.startsWith("mode:")) continue;
            BedwarsMode candidate = fromSidebarValue(value.substring("mode:".length()));
            if (candidate == UNKNOWN || found != null && found != candidate) return UNKNOWN;
            found = candidate;
        }
        return found == null ? UNKNOWN : found;
    }

    private static String normalized(String value) {
        return value == null ? "" : value.replaceAll("\\u00a7.", "").toLowerCase(Locale.ROOT).trim();
    }
}
