package com.snkisk.hypixellegitils.command;

import com.snkisk.hypixellegitils.config.DetectorId;
import com.snkisk.hypixellegitils.config.NotificationChannel;
import com.snkisk.hypixellegitils.alert.ChatFormat;
import com.snkisk.hypixellegitils.party.PartyDetectionMethod;
import java.util.Locale;

/** Parses the intentionally tiny, user-entered local diagnostic namespace. */
public final class LocalCommand {
    private static final String PREFIX = ".legitils";
    private static final String SHORT_PREFIX = ".l";
    private static final String STATUS = PREFIX + " status";
    private static final String HELP = PREFIX + " help";
    private static final String ANTICHEAT = PREFIX + " anticheat";
    private static final String DEV_PARTY_METHOD_HELP = ChatFormat.continuation(
        "§6.l partydetect method <chat|scoreboard> §8— §fdeveloper experiment"
    );
    private static final String DEV_LOG_HELP = ChatFormat.continuation(
        "§6.l dev log on/off/dump §8— §fshow developer chat diagnostics"
    );
    private static final String[] HELP_LINES = new String[] {
        ChatFormat.line("§fCommands"),
        ChatFormat.continuation("§7Alias: §b.l <command>"),
        ChatFormat.continuation("§a.l status §8— §fshow all feature status"),
        ChatFormat.continuation("§e.l anticheat list §8— §fshow detector settings"),
        ChatFormat.continuation("§b.l anticheat on <detector|all> §8— §fenable now"),
        ChatFormat.continuation("§c.l anticheat off <detector|all> §8— §fdisable now"),
        ChatFormat.continuation("§7Detectors: §fAutoBlock §8| §fNoSlow §8| §fKillAura §8| §fLegitScaffold"),
        ChatFormat.continuation("§fBedNuke §8| §fBlink §8| §fTimer §8| §fNoBreakDelay §8| §fall"),
        ChatFormat.continuation("§c.l nickdetect on/off §8— §ftoggle Nick detection"),
        ChatFormat.continuation("§b.l partydetect on/off §8— §ftoggle Party Detector"),
        ChatFormat.continuation("§6.l dev on/off §8— §finclude yourself in anti-cheat checks"),
        ChatFormat.continuation("§d.l notify <chat|actionbar|sound> on/off §8— §falert delivery"),
        ChatFormat.continuation("§e.l blacklist on/off §8— §fenable auto Blacklist"),
        ChatFormat.continuation("§e.l blacklist threshold <2-10> §8— §fauto-add threshold"),
        ChatFormat.continuation("§e.l blacklist clear all §8— §fclear stored entries"),
        ChatFormat.continuation("§c.l blacklist add/remove <player> §8— §fmanual edit"),
        ChatFormat.continuation("§8If not visible, resolves the name through Mojang."),
        ChatFormat.continuation("§e.l blacklist status/list [page] §8— §fshow entries")
    };

    private LocalCommand() {
    }

    /** Only manually entered chat text may enter the local diagnostic namespace. */
    public static String responseForUserInput(String input, boolean addToChat, String statusText) {
        return addToChat ? responseFor(input, statusText) : null;
    }

    /** Returns null when the input must pass through untouched to normal chat handling. */
    public static String responseFor(String input, String statusText) {
        Request request = parse(input);
        if (request == null) return null;
        return request.kind == Kind.STATUS
            ? statusText == null ? ChatFormat.line("§cStatus unavailable.") : statusText
            : ChatFormat.line("§eUse .l help for local commands.");
    }

    public static String[] helpLines() {
        return HELP_LINES.clone();
    }

    /** The experimental Party Detector selector is intentionally visible only in developer mode. */
    public static String[] helpLines(boolean developerMode) {
        if (!developerMode) return helpLines();
        String[] lines = new String[HELP_LINES.length + 2];
        System.arraycopy(HELP_LINES, 0, lines, 0, 10);
        lines[10] = DEV_PARTY_METHOD_HELP;
        lines[11] = HELP_LINES[10];
        lines[12] = DEV_LOG_HELP;
        System.arraycopy(HELP_LINES, 11, lines, 13, HELP_LINES.length - 11);
        return lines;
    }

    public static String[] invalidLocalCommandLines() {
        String[] lines = new String[HELP_LINES.length + 1];
        lines[0] = ChatFormat.line("§cUnknown command. §7Use §b.l help");
        System.arraycopy(HELP_LINES, 0, lines, 1, HELP_LINES.length);
        return lines;
    }

    /** Returns a parsed local request, or null when the input must pass through unchanged. */
    public static Request requestForUserInput(String input, boolean addToChat) {
        return addToChat ? parse(input) : null;
    }

    private static Request parse(String input) {
        input = expandShortPrefix(input);
        if (input == null || (!PREFIX.equals(input) && !input.startsWith(PREFIX + " "))) return null;
        if (STATUS.equals(input)) return new Request(Kind.STATUS, null, false, false);
        if (HELP.equals(input)) return new Request(Kind.HELP, null, false, false);
        if (input.startsWith(ANTICHEAT + " ")) {
            String[] parts = input.split(" ");
            if (parts.length == 3 && "list".equals(parts[2])) return new Request(Kind.ANTICHEAT_LIST, null, false, false);
            if (parts.length == 4 && ("on".equals(parts[2]) || "off".equals(parts[2]))) {
                boolean all = "all".equals(parts[3]);
                DetectorId detector = all ? null : detectorFor(parts[3]);
                if (all || detector != null) return new Request(Kind.ANTICHEAT_SET, detector, all, "on".equals(parts[2]));
            }
        }
        if (input.startsWith(PREFIX + " nickdetect ")) {
            String[] parts = input.split(" ");
            if (parts.length == 3 && ("on".equals(parts[2]) || "off".equals(parts[2]))) {
                return new Request(Kind.NICK_DETECT_SET_ENABLED, null, false, "on".equals(parts[2]));
            }
        }
        if (input.startsWith(PREFIX + " partydetect ")) {
            String[] parts = input.split(" ");
            if (parts.length == 3 && ("on".equals(parts[2]) || "off".equals(parts[2]))) {
                return new Request(Kind.PARTY_DETECT_SET_ENABLED, null, false, "on".equals(parts[2]));
            }
            if (parts.length == 4 && "method".equals(parts[2])) {
                PartyDetectionMethod method = PartyDetectionMethod.forCommand(parts[3]);
                if (method != null) return new Request(Kind.PARTY_DETECT_SET_METHOD, null, false, false, -1, null, null, method);
            }
        }
        if (input.startsWith(PREFIX + " dev ")) {
            String[] parts = input.split(" ");
            if (parts.length == 3 && ("on".equals(parts[2]) || "off".equals(parts[2]))) {
                return new Request(Kind.DEV_SET_ENABLED, null, false, "on".equals(parts[2]));
            }
            if (parts.length == 4 && "log".equals(parts[2]) && ("on".equals(parts[3]) || "off".equals(parts[3]))) {
                return new Request(Kind.DEV_LOG_SET_ENABLED, null, false, "on".equals(parts[3]));
            }
            if (parts.length == 4 && "log".equals(parts[2]) && "dump".equals(parts[3])) {
                return new Request(Kind.DEV_LOG_DUMP, null, false, false);
            }
        }
        if (input.startsWith(PREFIX + " notify ")) {
            String[] parts = input.split(" ");
            if (parts.length == 4 && ("on".equals(parts[3]) || "off".equals(parts[3]))) {
                NotificationChannel channel = NotificationChannel.forCommand(parts[2]);
                if (channel != null) return new Request(Kind.NOTIFICATION_SET_ENABLED, null, false, "on".equals(parts[3]), -1, null, channel);
            }
        }
        if (input.startsWith(PREFIX + " marker ") || input.startsWith(PREFIX + " blacklist ")) {
            String[] parts = input.split(" ");
            if (parts.length == 3 && ("on".equals(parts[2]) || "off".equals(parts[2]))) {
                return new Request(Kind.MARKER_SET_ENABLED, null, false, "on".equals(parts[2]));
            }
            if (parts.length == 4 && "threshold".equals(parts[2])) {
                try {
                    int threshold = Integer.parseInt(parts[3]);
                    return new Request(Kind.MARKER_SET_THRESHOLD, null, false, false, threshold);
                } catch (NumberFormatException ignored) {
                    return new Request(Kind.USAGE, null, false, false);
                }
            }
            if (parts.length == 3 && ("status".equals(parts[2]) || "list".equals(parts[2]))) {
                return new Request("list".equals(parts[2]) ? Kind.BLACKLIST_LIST : Kind.MARKER_STATUS, null, false, false);
            }
            if (parts.length == 4 && "list".equals(parts[2])) {
                try {
                    int page = Integer.parseInt(parts[3]);
                    if (page >= 1) return new Request(Kind.BLACKLIST_LIST, null, false, false, page);
                } catch (NumberFormatException ignored) {
                    // Falls through to local usage help.
                }
            }
            if (parts.length == 4 && "clear".equals(parts[2]) && "all".equals(parts[3])) {
                return new Request(Kind.MARKER_CLEAR_ALL, null, false, false);
            }
            if (parts.length == 4 && ("add".equals(parts[2]) || "remove".equals(parts[2])) && isValidPlayerName(parts[3])) {
                return new Request(
                    "add".equals(parts[2]) ? Kind.BLACKLIST_ADD : Kind.BLACKLIST_REMOVE,
                    null,
                    false,
                    false,
                    -1,
                    parts[3]
                );
            }
        }
        return new Request(Kind.USAGE, null, false, false);
    }

    private static String expandShortPrefix(String input) {
        if (SHORT_PREFIX.equals(input)) return PREFIX;
        if (input != null && input.startsWith(SHORT_PREFIX + " ")) return PREFIX + input.substring(SHORT_PREFIX.length());
        return input;
    }

    private static boolean isValidPlayerName(String value) {
        return value != null && value.matches("[A-Za-z0-9_]{1,16}");
    }

    private static DetectorId detectorFor(String value) {
        String normalized = value.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
        if ("autoblock".equals(normalized)) return DetectorId.AUTO_BLOCK;
        if ("noslow".equals(normalized)) return DetectorId.NO_SLOW;
        if ("killaura".equals(normalized)) return DetectorId.KILL_AURA;
        if ("legitscaffold".equals(normalized)) return DetectorId.LEGIT_SCAFFOLD;
        if ("bednuke".equals(normalized)) return DetectorId.BED_NUKE;
        if ("blink".equals(normalized) || "combatdesync".equals(normalized)) return DetectorId.COMBAT_DESYNC;
        if ("timer".equals(normalized) || "airstall".equals(normalized)) return DetectorId.AIR_STALL;
        if ("nobreakdelay".equals(normalized)) return DetectorId.NO_BREAK_DELAY;
        return null;
    }

    public enum Kind {
        STATUS,
        HELP,
        ANTICHEAT_LIST,
        ANTICHEAT_SET,
        NICK_DETECT_SET_ENABLED,
        PARTY_DETECT_SET_ENABLED,
        PARTY_DETECT_SET_METHOD,
        DEV_SET_ENABLED,
        DEV_LOG_SET_ENABLED,
        DEV_LOG_DUMP,
        NOTIFICATION_SET_ENABLED,
        MARKER_STATUS,
        MARKER_SET_ENABLED,
        MARKER_SET_THRESHOLD,
        MARKER_CLEAR_ALL,
        BLACKLIST_ADD,
        BLACKLIST_REMOVE,
        BLACKLIST_LIST,
        USAGE
    }

    public static final class Request {
        public final Kind kind;
        public final DetectorId detector;
        public final boolean all;
        public final boolean enabled;
        public final int threshold;
        public final String playerName;
        public final NotificationChannel notificationChannel;
        public final PartyDetectionMethod partyDetectionMethod;

        private Request(Kind kind, DetectorId detector, boolean all, boolean enabled) {
            this(kind, detector, all, enabled, -1);
        }

        private Request(Kind kind, DetectorId detector, boolean all, boolean enabled, int threshold) {
            this(kind, detector, all, enabled, threshold, null);
        }

        private Request(Kind kind, DetectorId detector, boolean all, boolean enabled, int threshold, String playerName) {
            this(kind, detector, all, enabled, threshold, playerName, null);
        }

        private Request(
            Kind kind,
            DetectorId detector,
            boolean all,
            boolean enabled,
            int threshold,
            String playerName,
            NotificationChannel notificationChannel
        ) {
            this(kind, detector, all, enabled, threshold, playerName, notificationChannel, null);
        }

        private Request(
            Kind kind,
            DetectorId detector,
            boolean all,
            boolean enabled,
            int threshold,
            String playerName,
            NotificationChannel notificationChannel,
            PartyDetectionMethod partyDetectionMethod
        ) {
            this.kind = kind;
            this.detector = detector;
            this.all = all;
            this.enabled = enabled;
            this.threshold = threshold;
            this.playerName = playerName;
            this.notificationChannel = notificationChannel;
            this.partyDetectionMethod = partyDetectionMethod;
        }
    }
}
