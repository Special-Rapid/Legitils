package com.snkisk.hypixellegitils.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Schedules bounded lookup retries only for visible Tab profiles that still have no normalized Stats result. */
public final class StatsRosterReconciliation {
    private static final long SCAN_INTERVAL_MILLIS = 1000L;
    private static final long RETRY_DELAY_MILLIS = 6000L;
    private static final int MAXIMUM_PLAYERS = 64;
    private final Map<String, Long> retryAfterByName = new LinkedHashMap<String, Long>();
    private final Map<String, List<String>> pendingNamesByMatchId = new LinkedHashMap<String, List<String>>();
    private final Set<String> inFlightNames = new LinkedHashSet<String>();
    private long nextScanAtMillis;
    private long nextSequence;

    /**
     * Automatic reconciliation needs either a visible Bed Wars mode or the
     * confirmed post-start context. The latter is necessary because the game
     * sidebar commonly stops exposing its mode line after the world changes.
     */
    public static boolean supports(BedwarsMode gameMode, boolean confirmedMatchContext) {
        return confirmedMatchContext || (gameMode != null && gameMode != BedwarsMode.UNKNOWN);
    }

    /** Incremental Tab recovery must not repeat the complete roster's Chat notice. */
    public static boolean isIncrementalRequest(String matchId) {
        return matchId != null && matchId.startsWith("reconcile_");
    }

    /** Returns one missing-profile request at most once per scan and never while an earlier attempt is cooling down. */
    public synchronized Request dueRequest(
        long nowMillis,
        long sessionGeneration,
        List<StatsBridgeRosterMember> visiblePlayers,
        StatsBridgeLookupResult current
    ) {
        if (nowMillis < 0L || nowMillis < nextScanAtMillis || visiblePlayers == null
            || visiblePlayers.isEmpty() || current == null || current.status == StatsBridgeLookupResult.Status.ALREADY_REQUESTED) return null;
        nextScanAtMillis = nowMillis + SCAN_INTERVAL_MILLIS;
        Set<String> resolved = resolvedNames(current);
        Map<String, StatsBridgeRosterMember> missing = new LinkedHashMap<String, StatsBridgeRosterMember>();
        for (StatsBridgeRosterMember member : visiblePlayers) {
            if (member == null || !member.isValid()) continue;
            String key = key(member.name);
            if (resolved.contains(key)) {
                retryAfterByName.remove(key);
                continue;
            }
            if (inFlightNames.contains(key)) continue;
            Long retryAfter = retryAfterByName.get(key);
            if (retryAfter != null && nowMillis < retryAfter.longValue()) continue;
            if (missing.size() < MAXIMUM_PLAYERS) missing.put(key, member);
        }
        if (missing.isEmpty()) return null;
        String matchId = "reconcile_" + Long.toString(sessionGeneration, 36).toLowerCase(Locale.ROOT)
            + "_" + Long.toString(++nextSequence, 36).toLowerCase(Locale.ROOT);
        List<String> pendingNames = new ArrayList<String>(missing.keySet());
        pendingNamesByMatchId.put(matchId, pendingNames);
        inFlightNames.addAll(pendingNames);
        for (String name : pendingNames) retryAfterByName.put(name, Long.valueOf(nowMillis + RETRY_DELAY_MILLIS));
        return new Request(matchId, new ArrayList<StatsBridgeRosterMember>(missing.values()));
    }

    /** Resolves only the submitted names; unavailable or partial replies retry after the bounded cooldown. */
    public synchronized void onResponse(String matchId, StatsBridgeLookupResult result, long nowMillis) {
        List<String> pendingNames = pendingNamesByMatchId.remove(matchId);
        if (pendingNames == null) return;
        Set<String> returned = resolvedNames(result);
        for (String name : pendingNames) {
            inFlightNames.remove(name);
            if (returned.contains(name)) retryAfterByName.remove(name);
            else retryAfterByName.put(name, Long.valueOf(nowMillis + RETRY_DELAY_MILLIS));
        }
    }

    public synchronized void reset() {
        retryAfterByName.clear();
        pendingNamesByMatchId.clear();
        inFlightNames.clear();
        nextScanAtMillis = 0L;
        nextSequence = 0L;
    }

    private static Set<String> resolvedNames(StatsBridgeLookupResult result) {
        Set<String> resolved = new LinkedHashSet<String>();
        if (result == null || result.status != StatsBridgeLookupResult.Status.READY) return resolved;
        for (StatsBridgePlayerResult player : result.players) {
            if (player != null && player.name != null) resolved.add(key(player.name));
        }
        return resolved;
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public static final class Request {
        public final String matchId;
        public final List<StatsBridgeRosterMember> players;

        private Request(String matchId, List<StatsBridgeRosterMember> players) {
            this.matchId = matchId;
            this.players = Collections.unmodifiableList(players);
        }
    }
}
