package com.snkisk.hypixellegitils.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Pure local presentation policy for normalized Bridge results; it never starts lookup work. */
public final class StatsPresentation {
    private StatsPresentation() {
    }

    public enum Tier {
        NONE,
        ELITE,
        STRONG
    }

    /** Strong takes precedence, so one profile can appear in the match chat exactly once. */
    public static Tier tierFor(StatsBridgePlayerResult player) {
        if (player == null) return Tier.NONE;
        boolean strong = atLeast(player.stars, 100) && atLeast(player.finalKillDeathRatio, 5D)
            || atLeast(player.modeWinStreak, 10);
        if (strong) return Tier.STRONG;
        boolean elite = atLeast(player.stars, 100) || atLeast(player.finalKillDeathRatio, 1D)
            || atLeast(player.modeWinStreak, 3);
        return elite ? Tier.ELITE : Tier.NONE;
    }

    /** Appends only known, compact values after the existing Tab text and local markers. */
    public static String tabSuffix(StatsBridgePlayerResult player) {
        if (player == null || player.nickStatus != StatsBridgePlayerResult.NickStatus.KNOWN) return "";
        List<String> values = new ArrayList<String>();
        if (player.stars != null) values.add("§b✫" + player.stars.intValue());
        if (player.finalKillDeathRatio != null) values.add("§e" + decimal(player.finalKillDeathRatio.doubleValue()) + " FKDR");
        if (player.modeWinStreak != null) values.add("§aWS " + player.modeWinStreak.intValue());
        if (values.isEmpty()) return "";
        StringBuilder suffix = new StringBuilder();
        for (String value : values) suffix.append(" §8| ").append(value);
        return suffix.toString();
    }

    /** Strong first, then FKDR, stars, and case-insensitive name; no alert semantics are attached. */
    public static List<Profile> rankedHighStats(List<StatsBridgePlayerResult> players) {
        if (players == null || players.isEmpty()) return Collections.emptyList();
        List<Profile> ranked = new ArrayList<Profile>();
        for (StatsBridgePlayerResult player : players) {
            Tier tier = tierFor(player);
            if (tier != Tier.NONE) ranked.add(new Profile(player, tier));
        }
        Collections.sort(ranked, new Comparator<Profile>() {
            @Override
            public int compare(Profile left, Profile right) {
                int tier = right.tier.ordinal() - left.tier.ordinal();
                if (tier != 0) return tier;
                int fkdr = Double.compare(value(right.player.finalKillDeathRatio), value(left.player.finalKillDeathRatio));
                if (fkdr != 0) return fkdr;
                int stars = Integer.compare(value(right.player.stars), value(left.player.stars));
                if (stars != 0) return stars;
                return left.player.name.compareToIgnoreCase(right.player.name);
            }
        });
        return Collections.unmodifiableList(ranked);
    }

    public static final class Profile {
        public final StatsBridgePlayerResult player;
        public final Tier tier;

        private Profile(StatsBridgePlayerResult player, Tier tier) {
            this.player = player;
            this.tier = tier;
        }

        public String chatSummary() {
            StringBuilder line = new StringBuilder(player.name).append(" §8—");
            if (player.stars != null) line.append(" §b✫").append(player.stars.intValue());
            if (player.finalKillDeathRatio != null) line.append(" §eFKDR ").append(decimal(player.finalKillDeathRatio.doubleValue()));
            if (player.modeWinStreak != null) line.append(" §aWS ").append(player.modeWinStreak.intValue());
            return line.toString();
        }
    }

    private static boolean atLeast(Integer value, int threshold) {
        return value != null && value.intValue() >= threshold;
    }

    private static boolean atLeast(Double value, double threshold) {
        return value != null && value.doubleValue() >= threshold;
    }

    private static int value(Integer value) {
        return value == null ? -1 : value.intValue();
    }

    private static double value(Double value) {
        return value == null ? -1D : value.doubleValue();
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
