package com.snkisk.hypixellegitils.stats;

import java.util.UUID;

/** Schedules one non-persistent Bridge request after Bed Wars team assignment has settled. */
public final class StatsMatchRequestGate {
    private static final long ROSTER_SETTLE_DELAY_MILLIS = 1200L;
    private long dueAtMillis = -1L;
    private String pendingMatchId;

    public synchronized void onBedwarsGameStart(long nowMillis) {
        dueAtMillis = nowMillis + ROSTER_SETTLE_DELAY_MILLIS;
        pendingMatchId = UUID.randomUUID().toString().replace("-", "");
    }

    /** Returns the ephemeral match ID once; null means there is no due request. */
    public synchronized String consumeDueMatchId(long nowMillis) {
        if (pendingMatchId == null || nowMillis < dueAtMillis) return null;
        String matchId = pendingMatchId;
        pendingMatchId = null;
        dueAtMillis = -1L;
        return matchId;
    }

    public synchronized void reset() {
        pendingMatchId = null;
        dueAtMillis = -1L;
    }
}
