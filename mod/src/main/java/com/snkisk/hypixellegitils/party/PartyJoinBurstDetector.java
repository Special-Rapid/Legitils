package com.snkisk.hypixellegitils.party;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Local-only parser for Hypixel's pre-game join announcements.
 * It intentionally retains no player identity because pre-game profiles can be obfuscated.
 */
public final class PartyJoinBurstDetector {
    public static final long WINDOW_MILLIS = 1000L;
    private static final Pattern JOIN = Pattern.compile("^.+ has joined \\((\\d+)/(\\d+)\\)!$");
    private static final Pattern QUIT = Pattern.compile("^.+ has quit!$");
    private static final String GAME_START = "The game starts in 1 second!";

    private int joinedCount;
    private int expectedCurrentCount;
    private int maximumPlayers;
    private long lastJoinAtMillis = -1L;

    /** Returns a completed burst size, or zero when there is nothing to display. */
    public synchronized int observeChat(String rawMessage, long nowMillis, boolean bedwarsPreGame) {
        if (!bedwarsPreGame) {
            reset();
            return 0;
        }
        String message = rawMessage == null ? "" : rawMessage.trim();
        if (GAME_START.equals(message) || QUIT.matcher(message).matches()) {
            reset();
            return 0;
        }
        Matcher join = JOIN.matcher(message);
        if (!join.matches()) return 0;

        int completed = flushExpired(nowMillis);
        int current = Integer.parseInt(join.group(1));
        int maximum = Integer.parseInt(join.group(2));
        if (current < 1 || maximum < current) return completed;

        if (joinedCount > 0 && maximum == maximumPlayers && current == expectedCurrentCount + 1
            && nowMillis >= lastJoinAtMillis && nowMillis - lastJoinAtMillis <= WINDOW_MILLIS) {
            joinedCount++;
            expectedCurrentCount = current;
            lastJoinAtMillis = nowMillis;
            return completed;
        }

        reset();
        joinedCount = 1;
        expectedCurrentCount = current;
        maximumPlayers = maximum;
        lastJoinAtMillis = nowMillis;
        return completed;
    }

    /** Flushes a quiet arrival burst after one second. */
    public synchronized int onTick(long nowMillis, boolean bedwarsPreGame) {
        if (!bedwarsPreGame) {
            reset();
            return 0;
        }
        return flushExpired(nowMillis);
    }

    public synchronized void reset() {
        joinedCount = 0;
        expectedCurrentCount = 0;
        maximumPlayers = 0;
        lastJoinAtMillis = -1L;
    }

    private int flushExpired(long nowMillis) {
        if (joinedCount == 0 || nowMillis < lastJoinAtMillis || nowMillis - lastJoinAtMillis < WINDOW_MILLIS) return 0;
        int completed = joinedCount >= 2 ? joinedCount : 0;
        reset();
        return completed;
    }
}
