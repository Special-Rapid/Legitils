package com.snkisk.hypixellegitils.stats;

import java.util.Locale;
import java.util.ArrayDeque;

/** Pure parsing and opaque-ID policy for a player-requested or post-start `/who` refresh. */
public final class WhoStatsRefresh {
    public static final long ROSTER_SETTLE_DELAY_MILLIS = 1500L;
    private static final long ROSTER_RESPONSE_TIMEOUT_MILLIS = 5000L;
    private WhoStatsRefresh() {
    }

    public static boolean isWhoCommand(String message) {
        if (message == null) return false;
        String trimmed = message.trim();
        if (trimmed.length() < 4 || !trimmed.regionMatches(true, 0, "/who", 0, 4)) return false;
        return trimmed.length() == 4 || Character.isWhitespace(trimmed.charAt(4));
    }

    /** Hypixel's ordinary `/who` reply. It is used only to settle an already user- or auto-requested refresh. */
    public static boolean isRosterResponse(String message) {
        if (message == null) return false;
        String trimmed = message.trim();
        return trimmed.regionMatches(true, 0, "ONLINE:", 0, "ONLINE:".length())
            && trimmed.length() > "ONLINE:".length();
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

    /** A bounded, single-consumer queue that waits for the server's `/who` reply before collecting Tab. */
    public static final class PendingRequests {
        private final int maximumPending;
        private final ArrayDeque<PendingRequest> requests = new ArrayDeque<PendingRequest>();
        private long nextSequence;

        public PendingRequests(int maximumPending) {
            this.maximumPending = maximumPending;
        }

        public synchronized String enqueue(Submission submission, long sessionGeneration, long nowMillis) {
            if (submission == null || !submission.shouldRefresh) return null;
            String matchId = matchId(sessionGeneration, ++nextSequence);
            return enqueue(matchId, nowMillis) ? matchId : null;
        }

        public synchronized String nextAutomaticMatchId(long sessionGeneration) {
            return matchId(sessionGeneration, ++nextSequence);
        }

        /** Adds the already-issued automatic `/who` refresh; it must not trigger a second server command. */
        public synchronized boolean enqueue(String matchId, long nowMillis) {
            if (matchId == null || !matchId.startsWith("who_") || nowMillis < 0L || requests.size() >= maximumPending) return false;
            requests.addLast(new PendingRequest(matchId, nowMillis + ROSTER_RESPONSE_TIMEOUT_MILLIS));
            return true;
        }

        /** Arms the oldest outstanding `/who` only after its server roster reply was observed. */
        public synchronized boolean observeRosterResponse(String message, long nowMillis) {
            if (!isRosterResponse(message) || nowMillis < 0L) return false;
            PendingRequest request = requests.peekFirst();
            if (request == null || request.responseObserved) return false;
            request.dueAtMillis = nowMillis + ROSTER_SETTLE_DELAY_MILLIS;
            request.responseObserved = true;
            return true;
        }

        public synchronized String consumeDue(long nowMillis) {
            PendingRequest next = requests.peekFirst();
            if (next == null || nowMillis < next.dueAtMillis) return null;
            return requests.removeFirst().matchId;
        }

        public synchronized void clear() {
            requests.clear();
        }

        private static final class PendingRequest {
            private final String matchId;
            private long dueAtMillis;
            private boolean responseObserved;

            private PendingRequest(String matchId, long dueAtMillis) {
                this.matchId = matchId;
                this.dueAtMillis = dueAtMillis;
            }
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
