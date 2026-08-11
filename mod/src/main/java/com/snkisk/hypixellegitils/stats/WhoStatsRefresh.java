package com.snkisk.hypixellegitils.stats;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Pure parsing and opaque-ID policy for a player-requested or post-start `/who` refresh. */
public final class WhoStatsRefresh {
    public static final long ROSTER_SETTLE_DELAY_MILLIS = 1500L;
    private static final long ROSTER_RESPONSE_TIMEOUT_MILLIS = 5000L;
    private static final long RESPONSE_RECOVERY_COOLDOWN_MILLIS = 10000L;
    private static final int MAXIMUM_ROSTER_NAMES = 64;
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

    /**
     * Parses only ordinary visible Minecraft names from Hypixel's own `/who` response.
     * The transient list becomes a fallback roster when Lunar has not repopulated Tab yet.
     */
    public static List<String> rosterNames(String message) {
        if (!isRosterResponse(message)) return Collections.emptyList();
        String names = message.trim().substring("ONLINE:".length()).trim();
        if (names.isEmpty()) return Collections.emptyList();
        List<String> parsed = new ArrayList<String>();
        Set<String> seen = new HashSet<String>();
        for (String candidate : names.split(",")) {
            String name = candidate == null ? "" : candidate.trim();
            if (!new StatsBridgeRosterMember(name, null).isValid()) continue;
            String key = name.toLowerCase(Locale.ROOT);
            if (!seen.add(key)) continue;
            parsed.add(name);
            if (parsed.size() >= MAXIMUM_ROSTER_NAMES) break;
        }
        return parsed.isEmpty() ? Collections.<String>emptyList() : Collections.unmodifiableList(parsed);
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
        private long lastResponseRecoveryAtMillis = Long.MIN_VALUE;

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
            List<String> rosterNames = rosterNames(message);
            if (rosterNames.isEmpty() || nowMillis < 0L) return false;
            PendingRequest request = requests.peekFirst();
            if (request == null || request.responseObserved) return false;
            request.dueAtMillis = nowMillis + ROSTER_SETTLE_DELAY_MILLIS;
            request.responseObserved = true;
            request.rosterNames = rosterNames;
            return true;
        }

        /**
         * Lunar can route a visible `/who` without the normal GuiChat submit hook during a reconnect.
         * When no current Stats result exists, recover once from that server-provided roster instead.
         */
        public synchronized boolean scheduleRecoveryFromRosterResponse(String message, long sessionGeneration, long nowMillis) {
            List<String> rosterNames = rosterNames(message);
            if (rosterNames.isEmpty() || nowMillis < 0L || !requests.isEmpty()
                || lastResponseRecoveryAtMillis != Long.MIN_VALUE
                    && nowMillis >= lastResponseRecoveryAtMillis
                    && nowMillis - lastResponseRecoveryAtMillis < RESPONSE_RECOVERY_COOLDOWN_MILLIS
                || requests.size() >= maximumPending) return false;
            String matchId = matchId(sessionGeneration, ++nextSequence);
            PendingRequest request = new PendingRequest(matchId, nowMillis + ROSTER_SETTLE_DELAY_MILLIS);
            request.responseObserved = true;
            request.rosterNames = rosterNames;
            requests.addLast(request);
            lastResponseRecoveryAtMillis = nowMillis;
            return true;
        }

        public synchronized Refresh consumeDue(long nowMillis) {
            PendingRequest next = requests.peekFirst();
            if (next == null || nowMillis < next.dueAtMillis) return null;
            next = requests.removeFirst();
            return new Refresh(next.matchId, next.rosterNames);
        }

        /** True until the submitted `/who` has either settled or timed out into its one refresh. */
        public synchronized boolean hasPendingRequest() {
            return !requests.isEmpty();
        }

        public synchronized void clear() {
            requests.clear();
            lastResponseRecoveryAtMillis = Long.MIN_VALUE;
        }

        private static final class PendingRequest {
            private final String matchId;
            private long dueAtMillis;
            private boolean responseObserved;
            private List<String> rosterNames = Collections.emptyList();

            private PendingRequest(String matchId, long dueAtMillis) {
                this.matchId = matchId;
                this.dueAtMillis = dueAtMillis;
            }
        }
    }

    /** One due request and, when known, the exact current server roster that settled it. */
    public static final class Refresh {
        public final String matchId;
        public final List<String> rosterNames;

        private Refresh(String matchId, List<String> rosterNames) {
            this.matchId = matchId;
            this.rosterNames = rosterNames == null ? Collections.<String>emptyList() : rosterNames;
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
