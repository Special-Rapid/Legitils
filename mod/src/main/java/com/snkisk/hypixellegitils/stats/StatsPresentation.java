package com.snkisk.hypixellegitils.stats;

import com.snkisk.hypixellegitils.config.StatsSettings;
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
        TARGET
    }

    /** Identifies one locally presented target tier; it does not imply a violation or an alert. */
    public static Tier tierFor(StatsBridgePlayerResult player) {
        if (player == null) return Tier.NONE;
        boolean target = atLeast(player.stars, 100) || atLeast(player.finalKillDeathRatio, 1D)
            || atLeast(player.modeWinStreak, 3);
        return target ? Tier.TARGET : Tier.NONE;
    }

    /** Appends only known, compact values after the existing Tab text and local markers. */
    public static String tabSuffix(StatsBridgePlayerResult player) {
        return tabSuffix(player, StatsSettings.defaults());
    }

    public static String tabSuffix(StatsBridgePlayerResult player, StatsSettings settings) {
        if (settings == null || !settings.enabled || !settings.tabEnabled) return "";
        if (player == null || player.nickStatus != StatsBridgePlayerResult.NickStatus.KNOWN) return "";
        List<String> values = new ArrayList<String>();
        if (settings.starsEnabled && player.stars != null) values.add("§b✫" + player.stars.intValue());
        if (settings.fkdrEnabled && player.finalKillDeathRatio != null) values.add("§e" + decimal(player.finalKillDeathRatio.doubleValue()) + " FKDR");
        if (settings.winStreakEnabled && player.modeWinStreak != null) values.add("§aWS " + player.modeWinStreak.intValue());
        if (values.isEmpty()) return "";
        StringBuilder suffix = new StringBuilder();
        for (String value : values) suffix.append(" §8| ").append(value);
        return suffix.toString();
    }

    /** Orders local target profiles by FKDR, stars, and case-insensitive name; no alert semantics are attached. */
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
                int fkdr = Double.compare(value(right.player.finalKillDeathRatio), value(left.player.finalKillDeathRatio));
                if (fkdr != 0) return fkdr;
                int stars = Integer.compare(value(right.player.stars), value(left.player.stars));
                if (stars != 0) return stars;
                return left.player.name.compareToIgnoreCase(right.player.name);
            }
        });
        return Collections.unmodifiableList(ranked);
    }

    /** One neutral per-match header followed by every Target Player profile and source-labelled tags. */
    public static List<String> chatLines(StatsBridgeLookupResult result) {
        if (result == null || result.status != StatsBridgeLookupResult.Status.READY) return Collections.emptyList();
        List<String> lines = new ArrayList<String>();
        lines.add("§fBed Wars stats: §a" + result.players.size() + " §fprofiles loaded.");
        for (Profile profile : rankedHighStats(result.players)) {
            lines.add("§eTarget Player§7: §f" + profile.chatSummary());
        }
        for (StatsBridgePlayerResult player : result.players) {
            for (StatsBridgePlayerResult.CommunityTag tag : player.communityTags) {
                lines.add("§d" + tag.source + " tag§7: §f" + player.name + " §8— §d" + tag.label);
            }
        }
        return Collections.unmodifiableList(lines);
    }

    /** A chatter explicitly made their visible name available, so show their returned values even below Target thresholds. */
    public static List<String> pregameChatLines(StatsBridgeLookupResult result) {
        if (result == null || result.status != StatsBridgeLookupResult.Status.READY) return Collections.emptyList();
        List<String> lines = new ArrayList<String>();
        for (StatsBridgePlayerResult player : result.players) {
            if (player.nickStatus != StatsBridgePlayerResult.NickStatus.KNOWN) continue;
            if (player.stars != null || player.finalKillDeathRatio != null || player.modeWinStreak != null) {
                lines.add("§bPregame stats§7: §f" + new Profile(player, Tier.NONE).chatSummary());
            }
            for (StatsBridgePlayerResult.CommunityTag tag : player.communityTags) {
                lines.add("§d" + tag.source + " tag§7: §f" + player.name + " §8— §d" + tag.label);
            }
        }
        return Collections.unmodifiableList(lines);
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
