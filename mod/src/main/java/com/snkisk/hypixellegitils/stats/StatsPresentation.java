package com.snkisk.hypixellegitils.stats;

import com.snkisk.hypixellegitils.config.StatsSettings;
import com.snkisk.hypixellegitils.alert.FlagMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Pure local presentation policy for normalized Bridge results; it never starts lookup work. */
public final class StatsPresentation {
    private static final int TAG_TOOLTIP_LINE_WIDTH = 42;
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

    /** A local Chat line with an optional provider explanation for vanilla's hover event. */
    public static final class ChatNotice {
        public final String text;
        public final String tooltip;
        public final String tagCode;
        public final List<TagHover> tagHovers;

        public ChatNotice(String text, String tooltip) {
            this(text, tooltip, null);
        }

        public ChatNotice(String text, String tooltip, String tagCode) {
            this(text, tagCode == null || tooltip == null ? Collections.<TagHover>emptyList()
                : Collections.singletonList(new TagHover(tagCode, tooltip)));
        }

        public ChatNotice(String text, List<TagHover> tagHovers) {
            this.text = text;
            this.tagHovers = tagHovers == null || tagHovers.isEmpty() ? Collections.<TagHover>emptyList()
                : Collections.unmodifiableList(new ArrayList<TagHover>(tagHovers));
            TagHover first = this.tagHovers.isEmpty() ? null : this.tagHovers.get(0);
            this.tooltip = first == null ? null : first.tooltip;
            this.tagCode = first == null ? null : first.code;
        }
    }

    /** One bounded provider tag code and its independently hoverable local explanation. */
    public static final class TagHover {
        public final String code;
        public final String tooltip;

        private TagHover(String code, String tooltip) {
            this.code = code;
            this.tooltip = tooltip;
        }
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
        return tabSuffix(player, settings, "");
    }

    /** Adds per-roster Star-column padding after the Star value and before the following Tab field. */
    public static String tabSuffix(StatsBridgePlayerResult player, StatsSettings settings, String starPadding) {
        if (settings == null || !settings.enabled || !settings.tabEnabled) return "";
        if (player == null || player.nickStatus != StatsBridgePlayerResult.NickStatus.KNOWN) return "";
        List<String> values = new ArrayList<String>();
        String star = tabStar(player, settings);
        if (!star.isEmpty()) values.add(star + (starPadding == null ? "" : starPadding));
        if (settings.fkdrEnabled && player.finalKillDeathRatio != null) values.add(fkdr(player.finalKillDeathRatio.doubleValue()) + decimal(player.finalKillDeathRatio.doubleValue()) + " FKDR");
        if (settings.winStreakEnabled && player.modeWinStreak != null) values.add("§aWS " + player.modeWinStreak.intValue());
        for (StatsBridgePlayerResult.CommunityTag tag : advisoryTags(player)) values.add(formattedTagCode(tag));
        if (values.isEmpty()) return "";
        StringBuilder suffix = new StringBuilder();
        for (String value : values) suffix.append(" §8| ").append(value);
        return suffix.toString();
    }

    /** Returns the exact Star text used in Tab when the local Tab Stars setting permits it. */
    public static String tabStar(StatsBridgePlayerResult player, StatsSettings settings) {
        if (settings == null || !settings.enabled || !settings.tabEnabled || !settings.starsEnabled) return "";
        if (player == null || player.nickStatus != StatsBridgePlayerResult.NickStatus.KNOWN || player.stars == null) return "";
        return stars(player.stars.intValue());
    }

    /** Returns the exact Star text used in Chat summaries, independent of the optional Tab Stars setting. */
    public static String chatStar(StatsBridgePlayerResult player) {
        return player == null || player.stars == null ? "" : stars(player.stars.intValue());
    }

    /** Returns only the optional compact FKDR nametag suffix for a returned real profile. */
    public static String nametagFkdrSuffix(StatsBridgePlayerResult player, StatsSettings settings) {
        if (settings == null || !settings.enabled || !settings.nametagEnabled) return "";
        if (player == null || player.nickStatus != StatsBridgePlayerResult.NickStatus.KNOWN || player.finalKillDeathRatio == null) return "";
        double value = player.finalKillDeathRatio.doubleValue();
        if (value < settings.nametagFkdrThreshold) return "";
        return " " + fkdr(value) + decimal(value) + " FKDR";
    }

    /** Provider tags are advisory data from the two named sources, never local detection conclusions. */
    public static boolean hasCommunityAdvisoryTag(StatsBridgePlayerResult player) {
        return !advisoryTags(player).isEmpty();
    }

    /** Keeps provider tag abbreviations compact on the always-visible 3D name surface. */
    public static String nametagTagSuffix(StatsBridgePlayerResult player) {
        if (player == null || player.nickStatus != StatsBridgePlayerResult.NickStatus.KNOWN) return "";
        StringBuilder suffix = new StringBuilder();
        for (StatsBridgePlayerResult.CommunityTag tag : advisoryTags(player)) suffix.append(" ").append(formattedTagCode(tag));
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
        return textLines(chatNotices(result, teamFormattedNames));
    }

    /** Uses the team-formatted Tab name captured at roster time and preserves each tag's safe hover text. */
    public static List<ChatNotice> chatNotices(StatsBridgeLookupResult result, Map<String, String> teamFormattedNames) {
        return chatNotices(result, teamFormattedNames, Collections.<String, String>emptyMap());
    }

    /** Uses the same measured Star field as Tab when the current roster provided one. */
    public static List<ChatNotice> chatNotices(
        StatsBridgeLookupResult result,
        Map<String, String> teamFormattedNames,
        Map<String, String> starPaddings
    ) {
        return chatNotices(
            result, teamFormattedNames, starPaddings, Collections.<String, String>emptyMap(), StatsSettings.defaults()
        );
    }

    /** Post-start and /who Chat use the same team order as Tab, with Nick rows first and one line per player. */
    public static List<ChatNotice> chatNotices(
        StatsBridgeLookupResult result,
        Map<String, String> teamFormattedNames,
        Map<String, String> starPaddings,
        Map<String, String> fkdrPaddings,
        StatsSettings settings
    ) {
        if (result == null || result.status != StatsBridgeLookupResult.Status.READY) return Collections.emptyList();
        List<StatsTabSorter.Entry<ChatEntry>> entries = new ArrayList<StatsTabSorter.Entry<ChatEntry>>();
        int originalIndex = 0;
        for (StatsBridgePlayerResult player : result.players) {
            if (player == null || !visibleInRosterCard(player)) {
                originalIndex++;
                continue;
            }
            String formattedName = teamFormattedName(player.name, teamFormattedNames);
            String teamKey = FlagMessage.bedWarsTeamKey(formattedName);
            boolean nicked = player.nickStatus == StatsBridgePlayerResult.NickStatus.NICKED;
            Double fkdr = nicked ? null : player.finalKillDeathRatio;
            entries.add(new StatsTabSorter.Entry<ChatEntry>(
                new ChatEntry(player, formattedName, teamKey), teamKey, nicked, fkdr, originalIndex
            ));
            originalIndex++;
        }
        List<ChatNotice> lines = new ArrayList<ChatNotice>();
        String currentTeam = null;
        for (ChatEntry entry : StatsTabSorter.sortForChat(entries, settings)) {
            if (entry.teamKey != null && !entry.teamKey.equals(currentTeam)) {
                lines.add(new ChatNotice(teamHeader(entry.teamKey), (String) null));
                currentTeam = entry.teamKey;
            } else if (entry.teamKey == null) {
                currentTeam = null;
            }
            lines.add(rosterCardNotice(entry, starPadding(entry.player.name, starPaddings), fkdrPadding(entry.player.name, fkdrPaddings)));
        }
        return Collections.unmodifiableList(lines);
    }

    /** A chatter explicitly made their visible name available, so show their returned values even below Target thresholds. */
    public static List<String> pregameChatLines(StatsBridgeLookupResult result) {
        return textLines(pregameChatNotices(result));
    }

    /** Pregame has no trustworthy team formatting yet, but keeps the tag hover semantics. */
    public static List<ChatNotice> pregameChatNotices(StatsBridgeLookupResult result) {
        return pregameChatNotices(result, Collections.<String, String>emptyMap());
    }

    /** Reuses a current Tab display field when one is available without requiring pregame team data. */
    public static List<ChatNotice> pregameChatNotices(StatsBridgeLookupResult result, Map<String, String> renderedNames) {
        return pregameChatNotices(result, renderedNames, Collections.<String, String>emptyMap());
    }

    /** Reuses the same measured Star field as Tab when one is available. */
    public static List<ChatNotice> pregameChatNotices(
        StatsBridgeLookupResult result,
        Map<String, String> renderedNames,
        Map<String, String> starPaddings
    ) {
        return pregameChatNotices(result, renderedNames, starPaddings, Collections.<String, String>emptyMap());
    }

    /** Pregame remains compact: one resolved chatter line combines local stats and provider tags. */
    public static List<ChatNotice> pregameChatNotices(
        StatsBridgeLookupResult result,
        Map<String, String> renderedNames,
        Map<String, String> starPaddings,
        Map<String, String> fkdrPaddings
    ) {
        if (result == null || result.status != StatsBridgeLookupResult.Status.READY) return Collections.emptyList();
        List<ChatNotice> lines = new ArrayList<ChatNotice>();
        for (StatsBridgePlayerResult player : result.players) {
            if (player.nickStatus != StatsBridgePlayerResult.NickStatus.KNOWN) continue;
            if (player.stars == null && player.finalKillDeathRatio == null && player.modeWinStreak == null
                && !hasCommunityAdvisoryTag(player)) continue;
            lines.add(rosterCardNotice(
                new ChatEntry(player, displayedName(player.name, renderedNames, "§f" + player.name), null),
                starPadding(player.name, starPaddings), fkdrPadding(player.name, fkdrPaddings)
            ));
        }
        return Collections.unmodifiableList(lines);
    }

    /** Explicit command output: all returned values plus compact provider diagnostics, never raw payloads. */
    public static List<String> manualLookupLines(StatsBridgeLookupResult result) {
        return textLines(manualLookupNotices(result));
    }

    /** Explicit command output preserves the same narrow hover affordance as automatic provider-tag notices. */
    public static List<ChatNotice> manualLookupNotices(StatsBridgeLookupResult result) {
        return manualLookupNotices(result, Collections.<String, String>emptyMap());
    }

    /** Explicit lookup output reuses a current Tab display field when one has already been observed. */
    public static List<ChatNotice> manualLookupNotices(StatsBridgeLookupResult result, Map<String, String> renderedNames) {
        return manualLookupNotices(result, renderedNames, Collections.<String, String>emptyMap());
    }

    /** Explicit lookup output uses the same measured Star field as Tab when one is available. */
    public static List<ChatNotice> manualLookupNotices(
        StatsBridgeLookupResult result,
        Map<String, String> renderedNames,
        Map<String, String> starPaddings
    ) {
        return manualLookupNotices(result, renderedNames, starPaddings, Collections.<String, String>emptyMap());
    }

    /** Explicit lookup keeps diagnostics separate but combines Stats and advisory tags for each returned profile. */
    public static List<ChatNotice> manualLookupNotices(
        StatsBridgeLookupResult result,
        Map<String, String> renderedNames,
        Map<String, String> starPaddings,
        Map<String, String> fkdrPaddings
    ) {
        if (result == null || result.status == StatsBridgeLookupResult.Status.UNAVAILABLE) {
            return Collections.singletonList(new ChatNotice("§cStats Bridge unavailable. §7Start Companion and check API keys.", (String) null));
        }
        if (result.status != StatsBridgeLookupResult.Status.READY || result.players.isEmpty()) {
            return Collections.singletonList(new ChatNotice("§eStats lookup did not return a profile.", (String) null));
        }
        List<ChatNotice> lines = new ArrayList<ChatNotice>();
        for (StatsBridgePlayerResult player : result.players) {
            if (player.nickStatus != StatsBridgePlayerResult.NickStatus.KNOWN) {
                lines.add(new ChatNotice("§eStats§7: " + displayedName(player.name, renderedNames, "§f" + player.name)
                    + " §8— §eprofile unavailable", (String) null));
            } else {
                ChatNotice playerNotice = rosterCardNotice(
                    new ChatEntry(player, displayedName(player.name, renderedNames, "§f" + player.name), null),
                    starPadding(player.name, starPaddings), fkdrPadding(player.name, fkdrPaddings)
                );
                lines.add(new ChatNotice("§bStats§7: " + playerNotice.text, playerNotice.tagHovers));
            }
            for (StatsBridgePlayerResult.CommunityTag tag : player.communityTags) {
                if ("diagnostic".equals(tag.source)) {
                    lines.add(new ChatNotice("§cAPI§7: §f" + tag.label, (String) null));
                } else if ("provider".equals(tag.source)) {
                    lines.add(new ChatNotice("§aAPI§7: §f" + tag.label, (String) null));
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
            return statsSummary("");
        }

        public String statsSummary(String starPadding) {
            StringBuilder line = new StringBuilder();
            if (player.stars != null) line.append(' ').append(stars(player.stars.intValue())).append(starPadding == null ? "" : starPadding);
            if (player.finalKillDeathRatio != null) line.append(' ').append(fkdr(player.finalKillDeathRatio.doubleValue())).append("FKDR ").append(decimal(player.finalKillDeathRatio.doubleValue()));
            if (player.modeWinStreak != null) line.append(" §aWS ").append(player.modeWinStreak.intValue());
            return line.length() == 0 ? "§7no stats" : line.substring(1);
        }
    }

    /** Exact FKDR field text used by the Chat roster card and measured during Tab rendering. */
    public static String chatFkdr(StatsBridgePlayerResult player) {
        if (player == null || player.nickStatus != StatsBridgePlayerResult.NickStatus.KNOWN
            || player.finalKillDeathRatio == null) return "";
        return fkdr(player.finalKillDeathRatio.doubleValue()) + decimal(player.finalKillDeathRatio.doubleValue()) + " FKDR";
    }

    private static boolean visibleInRosterCard(StatsBridgePlayerResult player) {
        return player.nickStatus == StatsBridgePlayerResult.NickStatus.NICKED
            || tierFor(player) != Tier.NONE || hasCommunityAdvisoryTag(player);
    }

    private static ChatNotice rosterCardNotice(ChatEntry entry, String starPadding, String fkdrPadding) {
        StatsBridgePlayerResult player = entry.player;
        StringBuilder text = new StringBuilder(entry.formattedName);
        if (player.nickStatus == StatsBridgePlayerResult.NickStatus.NICKED) text.append(" §c[NICK]");
        String stats = rosterCardStats(player, starPadding, fkdrPadding);
        if (!stats.isEmpty()) text.append(" §8— ").append(stats);
        List<TagHover> hovers = new ArrayList<TagHover>();
        for (StatsBridgePlayerResult.CommunityTag tag : advisoryTags(player)) {
            text.append(" §8| ").append(formattedTagCode(tag));
            hovers.add(tagHover(tag));
        }
        return new ChatNotice(text.toString(), hovers);
    }

    private static String rosterCardStats(StatsBridgePlayerResult player, String starPadding, String fkdrPadding) {
        if (player == null || player.nickStatus != StatsBridgePlayerResult.NickStatus.KNOWN) return "";
        StringBuilder fields = new StringBuilder();
        String star = chatStar(player);
        if (!star.isEmpty()) fields.append(star).append(starPadding == null ? "" : starPadding);
        String fkdr = chatFkdr(player);
        if (!fkdr.isEmpty()) {
            if (fields.length() > 0) fields.append(" §8— ");
            fields.append(fkdr).append(fkdrPadding == null ? "" : fkdrPadding);
        }
        if (player.modeWinStreak != null) {
            if (fields.length() > 0) fields.append(" §8— ");
            fields.append("§aWS ").append(player.modeWinStreak.intValue());
        }
        return fields.toString();
    }

    private static String teamHeader(String key) {
        String color = teamColor(key);
        return color + "§l" + key + " " + color + teamName(key);
    }

    private static String teamColor(String key) {
        if ("R".equals(key)) return "§c";
        if ("B".equals(key)) return "§9";
        if ("G".equals(key)) return "§a";
        if ("Y".equals(key)) return "§e";
        if ("A".equals(key)) return "§b";
        if ("W".equals(key)) return "§f";
        if ("P".equals(key)) return "§d";
        return "§8";
    }

    private static String teamName(String key) {
        if ("R".equals(key)) return "Red";
        if ("B".equals(key)) return "Blue";
        if ("G".equals(key)) return "Green";
        if ("Y".equals(key)) return "Yellow";
        if ("A".equals(key)) return "Aqua";
        if ("W".equals(key)) return "White";
        if ("P".equals(key)) return "Pink";
        return "Gray";
    }

    private static final class ChatEntry {
        private final StatsBridgePlayerResult player;
        private final String formattedName;
        private final String teamKey;

        private ChatEntry(StatsBridgePlayerResult player, String formattedName, String teamKey) {
            this.player = player;
            this.formattedName = formattedName;
            this.teamKey = teamKey;
        }
    }

    private static String teamFormattedName(String name, Map<String, String> teamFormattedNames) {
        String fallback = name == null ? "Unknown" : name;
        return displayedName(name, teamFormattedNames, "§f" + fallback);
    }

    private static String displayedName(String name, Map<String, String> renderedNames, String fallback) {
        if (name != null && renderedNames != null) {
            String formatted = renderedNames.get(name.toLowerCase(Locale.ROOT));
            if (formatted != null && !formatted.trim().isEmpty()) return formatted;
        }
        return fallback;
    }

    private static String starPadding(String name, Map<String, String> starPaddings) {
        if (name == null || starPaddings == null) return "";
        String padding = starPaddings.get(name.toLowerCase(Locale.ROOT));
        return padding == null ? "" : padding;
    }

    private static String fkdrPadding(String name, Map<String, String> fkdrPaddings) {
        if (name == null || fkdrPaddings == null) return "";
        String padding = fkdrPaddings.get(name.toLowerCase(Locale.ROOT));
        return padding == null ? "" : padding;
    }

    private static List<StatsBridgePlayerResult.CommunityTag> advisoryTags(StatsBridgePlayerResult player) {
        if (player == null || player.communityTags == null || player.communityTags.isEmpty()) return Collections.emptyList();
        List<StatsBridgePlayerResult.CommunityTag> tags = new ArrayList<StatsBridgePlayerResult.CommunityTag>();
        for (StatsBridgePlayerResult.CommunityTag tag : player.communityTags) {
            if (isAdvisoryTag(tag)) tags.add(tag);
        }
        return tags;
    }

    private static boolean isAdvisoryTag(StatsBridgePlayerResult.CommunityTag tag) {
        return tag != null && ("seraph".equals(tag.source) || "urchin".equals(tag.source));
    }

    private static String tagAbbreviation(StatsBridgePlayerResult.CommunityTag tag) {
        String label = tag.label == null ? "" : tag.label;
        if ("Blatant Cheating".equals(label) || "Blatant Cheater".equals(label)) return "[BC]";
        if ("Closet Cheating".equals(label) || "Closet Cheater".equals(label)) return "[CC]";
        if (label.startsWith("Confirmed ")) return "[CF]";
        if ("Sniping".equals(label) || "Sniper".equals(label)) return "[S]";
        if ("Possible Sniper".equals(label) || "Potential Sniper".equals(label)) return "[PS]";
        if ("Legit Sniper".equals(label)) return "[LS]";
        if ("Alt Account".equals(label) || "Account".equals(label)) return "[A]";
        if ("Bot".equals(label)) return "[B]";
        if ("Annoying".equals(label)) return "[AN]";
        return "[CA]";
    }

    /** One source-independent palette, matched to the supplied Seraph/Urchin tag references. */
    private static String tagColor(StatsBridgePlayerResult.CommunityTag tag) {
        String label = tag.label == null ? "" : tag.label;
        if ("Blatant Cheating".equals(label) || "Blatant Cheater".equals(label)) return "§6";
        if ("Closet Cheating".equals(label) || "Closet Cheater".equals(label)) return "§6";
        if (label.startsWith("Confirmed ")) return "§5";
        if ("Sniping".equals(label) || "Sniper".equals(label)) return "§c";
        if ("Possible Sniper".equals(label) || "Potential Sniper".equals(label)) return "§c";
        if ("Legit Sniper".equals(label)) return "§c";
        if ("Alt Account".equals(label) || "Account".equals(label)) return "§6";
        if ("Bot".equals(label)) return "§7";
        if ("Annoying".equals(label)) return "§e";
        return "§6";
    }

    private static String formattedTagCode(StatsBridgePlayerResult.CommunityTag tag) {
        return tagColor(tag) + tagAbbreviation(tag);
    }

    private static ChatNotice tagChatNotice(StatsBridgePlayerResult.CommunityTag tag, String formattedName) {
        String code = formattedTagCode(tag);
        return new ChatNotice(code + " §8— " + formattedName, Collections.singletonList(tagHover(tag)));
    }

    private static TagHover tagHover(StatsBridgePlayerResult.CommunityTag tag) {
        return new TagHover(formattedTagCode(tag), tagTooltip(tag));
    }

    /** Keeps Minecraft's hover card narrow while making the provider's canonical category explicit. */
    private static String tagTooltip(StatsBridgePlayerResult.CommunityTag tag) {
        String title = tagColor(tag) + "§l" + tag.label;
        if (tag.tooltip == null || tag.tooltip.trim().isEmpty()) return title;
        return title + "\n§7" + wrapTooltip(tag.tooltip).replace("\n", "\n§7");
    }

    private static String wrapTooltip(String tooltip) {
        String[] sourceLines = tooltip.replace('\r', '\n').split("\\n", -1);
        List<String> lines = new ArrayList<String>();
        for (String sourceLine : sourceLines) {
            String remaining = sourceLine.trim();
            if (remaining.isEmpty()) {
                if (!lines.isEmpty()) lines.add("");
                continue;
            }
            while (remaining.length() > TAG_TOOLTIP_LINE_WIDTH) {
                int split = remaining.lastIndexOf(' ', TAG_TOOLTIP_LINE_WIDTH);
                if (split <= 0) split = TAG_TOOLTIP_LINE_WIDTH;
                lines.add(remaining.substring(0, split));
                remaining = remaining.substring(split).trim();
            }
            if (!remaining.isEmpty()) lines.add(remaining);
        }
        return lines.isEmpty() ? "" : joinLines(lines);
    }

    private static String joinLines(List<String> lines) {
        StringBuilder joined = new StringBuilder();
        for (String line : lines) {
            if (joined.length() > 0) joined.append('\n');
            joined.append(line);
        }
        return joined.toString();
    }

    private static List<String> textLines(List<ChatNotice> notices) {
        if (notices == null || notices.isEmpty()) return Collections.emptyList();
        List<String> lines = new ArrayList<String>();
        for (ChatNotice notice : notices) lines.add(notice.text);
        return Collections.unmodifiableList(lines);
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
