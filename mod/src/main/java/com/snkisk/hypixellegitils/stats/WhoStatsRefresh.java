package com.snkisk.hypixellegitils.stats;

import java.util.Locale;
import java.util.ArrayDeque;

/** Pure parsing and opaque-ID policy for a player-requested or post-start `/who` refresh. */
public final class WhoStatsRefresh {
    private WhoStatsRefresh() {
    }

    public static boolean isWhoCommand(String message) {
        if (message == null) return false;
        String trimmed = message.trim();
        if (trimmed.length() < 4 || !trimmed.regionMatches(true, 0, "/who", 0, 4)) return false;
        return trimmed.length() == 4 || Character.isWhitespace(trimmed.charAt(4));
    }

    /** Keeps the user-entered outbound command byte-for-byte intact while identifying the local refresh side effect. */
    public static Submission submissionFor(String message) {
        return new Submission(message, isWhoCommand(message));
    }

    public static String matchId(long sessionGeneration, long sequence) {
        return "who_" + Long.toString(sessionGeneration, 36).toLowerCase(Locale.ROOT)
            + "_" + Long.toString(sequence, 36).toLowerCase(Locale.ROOT);
    }

    /** Converts the one consumed post-start gate ID into exactly one server command plus one forced-refresh ID. */
    public static PostStartAction postStartAction(String postStartMatchId, String whoRefreshMatchId) {
        if (postStartMatchId == null || postStartMatchId.trim().isEmpty()
            || whoRefreshMatchId == null || !whoRefreshMatchId.startsWith("who_")) return null;
        return new PostStartAction("/who", whoRefreshMatchId);
    }

    /** A bounded, single-consumer local queue; server command delivery is intentionally outside this class. */
    public static final class PendingRequests {
        private final int maximumPending;
        private final ArrayDeque<String> matchIds = new ArrayDeque<String>();
        private long nextSequence;

        public PendingRequests(int maximumPending) {
            this.maximumPending = maximumPending;
        }

        public synchronized String enqueue(Submission submission, long sessionGeneration) {
            if (submission == null || !submission.shouldRefresh || matchIds.size() >= maximumPending) return null;
            String matchId = matchId(sessionGeneration, ++nextSequence);
            matchIds.addLast(matchId);
            return matchId;
        }

        public synchronized String nextAutomaticMatchId(long sessionGeneration) {
            return matchId(sessionGeneration, ++nextSequence);
        }

        public synchronized String consume() {
            return matchIds.pollFirst();
        }

        public synchronized void clear() {
            matchIds.clear();
        }
    }

    public static final class Submission {
        public final String outboundMessage;
        public final boolean shouldRefresh;

        private Submission(String outboundMessage, boolean shouldRefresh) {
            this.outboundMessage = outboundMessage;
            this.shouldRefresh = shouldRefresh;
        }
    }

    public static final class PostStartAction {
        public final String outboundCommand;
        public final String refreshMatchId;

        private PostStartAction(String outboundCommand, String refreshMatchId) {
            this.outboundCommand = outboundCommand;
            this.refreshMatchId = refreshMatchId;
        }
    }
}
