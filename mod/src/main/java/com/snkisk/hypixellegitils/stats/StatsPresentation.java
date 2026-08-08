package com.snkisk.hypixellegitils.stats;

import com.snkisk.hypixellegitils.config.StatsSettings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Pure local presentation policy for normalized Bridge results; it never starts lookup work. */
public final class StatsPresentation {
    /**
     * Bed Wars prestige templates from sample/star-color-code/color-code.md.
     * Each entry keeps the document's colour codes and symbol; its visible digits are
     * replaced with the player's current level so a prestige applies through its
     * whole 100-level band rather than only at the exact milestone.
     */
    private static final String[] STAR_PRESTIGE_TEMPLATES = {
        null,
        "§f[100✫]", "§6[200✫]", "§b[300✫]", "§2[400✫]", "§3[500✫]", "§4[600✫]", "§d[700✫]", "§9[800✫]", "§5[900✫]",
        "§c[§61§e0§a0§b0§d✪§5]", "§7[§f1100§7✪]", "§7[§e1200§6✪§7]", "§7[§b1300§3✪§7]", "§7[§a1400§2✪§7]", "§7[§31500§9✪§7]", "§7[§c1600§4✪§7]", "§7[§d1700§5✪§7]", "§7[§91800§1✪§7]", "§7[§51900§8✪§7]",
        "§8[§72§f00§70⚝§8]", "§f[2§e10§60⚝]", "§6[2§f20§b0§3⚝]", "§5[2§d30§60§e⚝]", "§b[2§f40§70⚝§8]", "§f[2§a50§20⚝]", "§4[2§c60§d0⚝§5]", "§e[2§f70§80⚝]", "§a[2§280§60⚝§e]", "§b[2§390§90⚝§1]",
        "§e[3§600§c0✥§4]", "§9[3§310§60✥§e]", "§c[§43§720§40§c✥]", "§9[33§d0§c0✥§4]", "§2[§a3§d40§50✥§2]", "§c[3§450§20§a✥]", "§a[36§b0§90✥§1]", "§4[3§c70§b0§3✥]", "§1[3§98§500§d✥§1]", "§c[3§a90§30§9✥]",
        "§5[4§c00§60✭§e]", "§e[4§61§c0§d0✭§5]", "§1[§94§32§b0§f0§7✭]", "§0[§54§830§50✭§0]", "§2[4§a4§e0§60§5✭§d]", "§f[4§b50§30✭]", "§3[§b4§e6§600§d✭§5]", "§f[§44§c70§90§1✭§9]", "§5[4§c8§600§b✭§3]", "§2[§a4§f900§a✭§2]",
        "§4[5§50§900§1✭§0]", "§4[§c51§60§e0§f✭§4]", "§1[§95§32§b0§f0§e✭§1]", "§5[§d5§e3§f0§e0§d✭§5]", "§3[§a5§24§80§20§a✭§3]", "§2[§a5§e5§f0§b0§d✭§5]", "§4[§c5§e6§f0§e0§c✭§4]", "§4[§65§27§30§90§5✭§8]", "§5[§c5§68§f0§b0§3✭§9]", "§7[§05§89§70§f0✭§7]",
        "§c[§f6000§c✭§f]", "§6[§e6§f100§b✭§3]", "§e[§f6§e2§600§f✭§e]", "§a[§e6300§a✭§2]", "§b[6§c400§a✭]", "§3[6§a50§f0§a✭§3]", "§9[§d6600§b✭§9]", "§5[§d6700§f✭§5]", "§0[§668§e00§f✭]", "§a[690§20✭§8]",
        "§3[§b7000§f✭§3]", "§4[§c7§61§e0§c0§6✭§e]", "§2[§a7§f2§20§a0§f✭§8]", "§2[§373§b00§a✭§2]", "§8[7400§d✭§8]", "§6[7§250§f0✭]", "§f[76§700§c✭§8]", "§d[§c7700§6✭§d]", "§8[§77§f800§e✭§8]", "§6[§f7§29§60§20§f✭§6]",
        "§2[§a800§c0§4✭§2]", "§8[§78§f1§b0§30§9✭§1]", "§f[8200§a✭§f]", "§8[8§430§c0✭§8]", "§f[§d840§a0✭§f]", "§3[§68500§e✭§3]", "§d[§f8600§e✭§d]", "§8[§68700✭§8]", "§4[88§c00§f✭]", "§9[§b890§30✭§9]",
        "§d[9000§5✭§8]", "§0[§c9§610§c0✭§4]", "§2[§d9200§a✭§2]", "§f[§89300§f✭]", "§e[§69§44§800✭]", "§0[9§850§70✭§f]", "§e[96§000§e✭§0]", "§d[97§e00§b✭§e]", "§0[§89800✭§0]", "§8[§79§f900§e✭§f]",
        "§9[§b1§f0000§c✭§4]"
    };

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
        if (settings.starsEnabled && player.stars != null) values.add(stars(player.stars.intValue()));
        if (settings.fkdrEnabled && player.finalKillDeathRatio != null) values.add(fkdr(player.finalKillDeathRatio.doubleValue()) + decimal(player.finalKillDeathRatio.doubleValue()) + " FKDR");
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

    /** Returns compact high-stat lines without a generic per-match header. */
    public static List<String> chatLines(StatsBridgeLookupResult result) {
        return chatLines(result, Collections.<String, String>emptyMap());
    }

    /** Uses the team-formatted Tab name captured at roster time when it is available. */
    public static List<String> chatLines(StatsBridgeLookupResult result, Map<String, String> teamFormattedNames) {
        if (result == null || result.status != StatsBridgeLookupResult.Status.READY) return Collections.emptyList();
        List<String> lines = new ArrayList<String>();
        for (Profile profile : rankedHighStats(result.players)) {
            lines.add(teamFormattedName(profile.player.name, teamFormattedNames) + " §8— " + profile.statsSummary());
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
                lines.add(new Profile(player, Tier.NONE).chatSummary());
            }
            for (StatsBridgePlayerResult.CommunityTag tag : player.communityTags) {
                lines.add("§d" + tag.source + " tag§7: §f" + player.name + " §8— §d" + tag.label);
            }
        }
        return Collections.unmodifiableList(lines);
    }

    /** Explicit command output: all returned values plus compact provider diagnostics, never raw payloads. */
    public static List<String> manualLookupLines(StatsBridgeLookupResult result) {
        if (result == null || result.status == StatsBridgeLookupResult.Status.UNAVAILABLE) {
            return Collections.singletonList("§cStats Bridge unavailable. §7Start Companion and check API keys.");
        }
        if (result.status != StatsBridgeLookupResult.Status.READY || result.players.isEmpty()) {
            return Collections.singletonList("§eStats lookup did not return a profile.");
        }
        List<String> lines = new ArrayList<String>();
        for (StatsBridgePlayerResult player : result.players) {
            if (player.nickStatus != StatsBridgePlayerResult.NickStatus.KNOWN) {
                lines.add("§eStats§7: §f" + player.name + " §8— §eprofile unavailable");
            } else {
                lines.add("§bStats§7: §f" + new Profile(player, Tier.NONE).chatSummary());
            }
            for (StatsBridgePlayerResult.CommunityTag tag : player.communityTags) {
                if ("diagnostic".equals(tag.source)) {
                    lines.add("§cAPI§7: §f" + tag.label);
                } else if ("provider".equals(tag.source)) {
                    lines.add("§aAPI§7: §f" + tag.label);
                } else {
                    lines.add("§d" + tag.source + " tag§7: §f" + player.name + " §8— §d" + tag.label);
                }
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
            StringBuilder line = new StringBuilder(player.name).append(" §8— ");
            return line.append(statsSummary()).toString();
        }

        public String statsSummary() {
            StringBuilder line = new StringBuilder();
            if (player.stars != null) line.append(' ').append(stars(player.stars.intValue()));
            if (player.finalKillDeathRatio != null) line.append(' ').append(fkdr(player.finalKillDeathRatio.doubleValue())).append("FKDR ").append(decimal(player.finalKillDeathRatio.doubleValue()));
            if (player.modeWinStreak != null) line.append(" §aWS ").append(player.modeWinStreak.intValue());
            return line.length() == 0 ? "§7no stats" : line.substring(1);
        }
    }

    private static String teamFormattedName(String name, Map<String, String> teamFormattedNames) {
        if (name != null && teamFormattedNames != null) {
            String formatted = teamFormattedNames.get(name.toLowerCase(Locale.ROOT));
            if (formatted != null && !formatted.trim().isEmpty()) return formatted;
        }
        return "§f" + (name == null ? "Unknown" : name);
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

    private static String stars(int value) {
        if (value < 100) return "§7[" + value + "✫]";
        if (value > 99999) return "§9[" + value + "✭]";
        int prestige = Math.min(value / 100, STAR_PRESTIGE_TEMPLATES.length - 1);
        return replaceVisibleDigits(STAR_PRESTIGE_TEMPLATES[prestige], Integer.toString(value));
    }

    /** Replaces only visible template digits, preserving every Minecraft format-code digit. */
    private static String replaceVisibleDigits(String template, String replacement) {
        StringBuilder formatted = new StringBuilder(template.length());
        int replacementIndex = 0;
        for (int index = 0; index < template.length(); index++) {
            char current = template.charAt(index);
            if (current == '§' && index + 1 < template.length()) {
                formatted.append(current).append(template.charAt(++index));
            } else if (current >= '0' && current <= '9' && replacementIndex < replacement.length()) {
                formatted.append(replacement.charAt(replacementIndex++));
            } else {
                formatted.append(current);
            }
        }
        return formatted.toString();
    }

    private static String fkdr(double value) {
        if (value <= 1D) return "§7";
        if (value < 2D) return "§f";
        if (value < 4D) return "§a";
        if (value < 8D) return "§e";
        if (value < 15D) return "§6";
        return "§c";
    }
}
