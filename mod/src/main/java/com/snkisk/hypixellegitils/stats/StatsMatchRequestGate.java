package com.snkisk.hypixellegitils.stats;

import java.util.UUID;

/** Carries one visible Bed Wars countdown across its post-start transition, then settles the roster once. */
public final class StatsMatchRequestGate {
    private static final long POST_START_ROSTER_SETTLE_DELAY_MILLIS = 1500L;
    private static final long GAME_WORLD_TRANSITION_TIMEOUT_MILLIS = 15000L;
    private long dueAtMillis = -1L;
    private String pendingMatchId;
    private long gameStartCountdownAtMillis = -1L;
    private boolean postStartRequestScheduled;
    private boolean pregameWasActive;

    public synchronized void onBedwarsGameStart(long nowMillis) {
        if (gameStartCountdownAtMillis >= 0L || postStartRequestScheduled) return;
        gameStartCountdownAtMillis = nowMillis;
    }

    /** Arms one post-start roster request after either expected client-side start transition. */
    public synchronized boolean onWorldLoading(long nowMillis) {
        return schedulePostStartRoster(nowMillis);
    }

    /** Some Lunar transitions keep the WorldClient; only an observed active-to-inactive edge starts the roster delay. */
    public synchronized boolean onPregameState(boolean active, long nowMillis) {
        boolean ended = pregameWasActive && !active;
        pregameWasActive = active;
        return ended && schedulePostStartRoster(nowMillis);
    }

    private boolean schedulePostStartRoster(long nowMillis) {
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

    /** True while the one post-start `/who` is still deliberately waiting to run. */
    public synchronized boolean isPending() {
        return pendingMatchId != null;
    }

    public synchronized void reset() {
        pendingMatchId = null;
        dueAtMillis = -1L;
        gameStartCountdownAtMillis = -1L;
        postStartRequestScheduled = false;
        pregameWasActive = false;
    }
}
