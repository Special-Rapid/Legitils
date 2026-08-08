package com.snkisk.hypixellegitils.nick;

import java.util.regex.Pattern;

/** Strictly recognizes the visible sender prefix of a normal player-chat line. */
public final class NickChatSignal {
    public static final String GAME_START = "The game starts in 1 second!";
    private static final Pattern GAME_START_COUNTDOWN = Pattern.compile("The game starts in (?:[1-9]|10) seconds?!");

    private NickChatSignal() {
    }

    public static boolean isGameStart(String message) {
        return GAME_START.equals(message == null ? null : message.trim());
    }

    /** Accepts the visible Bed Wars countdown so Stats can start before the transition resets the client world. */
    public static boolean isGameStartCountdown(String message) {
        return message != null && GAME_START_COUNTDOWN.matcher(message.trim()).matches();
    }

    /**
     * Hypixel's formatted player chat puts the sender immediately before the
     * first colon. This deliberately rejects later name mentions in a message.
     */
    public static boolean isMessageFrom(String message, String playerName) {
        if (!validPlayerName(playerName) || message == null) return false;
        int firstColon = message.indexOf(':');
        if (firstColon != playerName.length() && firstColon < playerName.length()) return false;
        int nameStart = firstColon - playerName.length();
        if (nameStart < 0 || !message.regionMatches(nameStart, playerName, 0, playerName.length())) return false;
        return nameStart == 0 || !isPlayerNameCharacter(message.charAt(nameStart - 1));
    }

    private static boolean validPlayerName(String playerName) {
        return playerName != null && playerName.matches("[A-Za-z0-9_]{1,16}");
    }

    private static boolean isPlayerNameCharacter(char value) {
        return value >= 'A' && value <= 'Z'
            || value >= 'a' && value <= 'z'
            || value >= '0' && value <= '9'
            || value == '_';
    }
}
