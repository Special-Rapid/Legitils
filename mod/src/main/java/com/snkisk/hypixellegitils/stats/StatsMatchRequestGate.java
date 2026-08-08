package com.snkisk.hypixellegitils.stats;

import java.util.UUID;

/** Schedules one non-persistent Bridge request after Bed Wars team assignment has settled. */
public final class StatsMatchRequestGate {
    private static final long ROSTER_SETTLE_DELAY_MILLIS = 1200L;
    private long dueAtMillis = -1L;
    private String pendingMatchId;
    private boolean requestScheduledForWorld;

    public synchronized void onBedwarsGameStart(long nowMillis) {
        if (requestScheduledForWorld) return;
        requestScheduledForWorld = true;
        dueAtMillis = nowMillis + ROSTER_SETTLE_DELAY_MILLIS;
        pendingMatchId = UUID.randomUUID().toString().replace("-", "");
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
        requestScheduledForWorld = false;
    }
}
