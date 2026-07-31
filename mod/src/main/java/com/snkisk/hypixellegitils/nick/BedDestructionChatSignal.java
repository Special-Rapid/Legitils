package com.snkisk.hypixellegitils.nick;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses only Hypixel-style Bed destruction announcements; it never infers an actor from nearby entities. */
public final class BedDestructionChatSignal {
    private static final Pattern DESTROYED_BY = Pattern.compile(
        "^(?:your|[a-z]+) bed was destroyed by ([A-Za-z0-9_]{1,16})!$",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern BED_DESTRUCTION_PREFIX = Pattern.compile("^bed destruction\\s*>\\s*", Pattern.CASE_INSENSITIVE);

    private BedDestructionChatSignal() {
    }

    /** Returns the server-announced actor name, or null for unrelated chat. */
    public static String destroyedBy(String rawMessage) {
        String normalized = BED_DESTRUCTION_PREFIX.matcher(stripFormatting(rawMessage).trim()).replaceFirst("");
        Matcher matcher = DESTROYED_BY.matcher(normalized);
        return matcher.matches() ? matcher.group(1) : null;
    }

    private static String stripFormatting(String value) {
        return value == null ? "" : value.replaceAll("(?i)\\u00a7[0-9a-fk-or]", "");
    }
}
