package com.snkisk.hypixellegitils.nick;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracts only the visible sender name from an ordinary pre-game player chat line. */
public final class PregameChatSender {
    private static final Pattern PLAYER_CHAT = Pattern.compile("^(?:\\[[^\\]]{1,32}\\]\\s*)?([A-Za-z0-9_]{1,16}):\\s+\\S.*$");

    private PregameChatSender() {
    }

    /** Returns null for server/status lines; this never attempts to identify a Nick. */
    public static String visibleName(String message) {
        if (message == null) return null;
        Matcher matcher = PLAYER_CHAT.matcher(message.trim());
        return matcher.matches() ? matcher.group(1) : null;
    }
}
