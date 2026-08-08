package com.snkisk.hypixellegitils.stats;

import java.util.UUID;

/** Carries one visible Bed Wars countdown across its game-world transition, then settles the roster once. */
public final class StatsMatchRequestGate {
    private static final long POST_START_ROSTER_SETTLE_DELAY_MILLIS = 1500L;
    private static final long GAME_WORLD_TRANSITION_TIMEOUT_MILLIS = 15000L;
    private long dueAtMillis = -1L;
    private String pendingMatchId;
    private long gameStartCountdownAtMillis = -1L;
    private boolean postStartRequestScheduled;

    public synchronized void onBedwarsGameStart(long nowMillis) {
        if (gameStartCountdownAtMillis >= 0L || postStartRequestScheduled) return;
        gameStartCountdownAtMillis = nowMillis;
    }

    /** Arms one post-start roster request only when the expected game-world transition arrives. */
    public synchronized boolean onWorldLoading(long nowMillis) {
        if (gameStartCountdownAtMillis < 0L
            || nowMillis < gameStartCountdownAtMillis
            || nowMillis - gameStartCountdownAtMillis > GAME_WORLD_TRANSITION_TIMEOUT_MILLIS) {
            reset();
            return false;
        }
        gameStartCountdownAtMillis = -1L;
        postStartRequestScheduled = true;
        dueAtMillis = nowMillis + POST_START_ROSTER_SETTLE_DELAY_MILLIS;
        pendingMatchId = UUID.randomUUID().toString().replace("-", "");
        return true;
    }

    /** Returns the ephemeral match ID once; null means there is no due request. */
    public synchronized String consumeDueMatchId(long nowMillis) {
        if (!isDue(nowMillis)) return null;
        String matchId = pendingMatchId;
        pendingMatchId = null;
        dueAtMillis = -1L;
        return matchId;
    }

    /** Returns whether the one pre-game roster request is ready to collect visible members. */
    public synchronized boolean isDue(long nowMillis) {
        return pendingMatchId != null && nowMillis >= dueAtMillis;
    }

    public synchronized void reset() {
        pendingMatchId = null;
        dueAtMillis = -1L;
        gameStartCountdownAtMillis = -1L;
        postStartRequestScheduled = false;
    }
}
