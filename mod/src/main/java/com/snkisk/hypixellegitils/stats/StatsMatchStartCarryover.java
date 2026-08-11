package com.snkisk.hypixellegitils.stats;

/** Carries only already-normalized pre-game match data through the confirmed game-start world transition. */
public final class StatsMatchStartCarryover {
    private StatsMatchStartCarryover() {
    }

    public static StatsBridgeLookupResult forConfirmedGameStart(
        boolean postStartRosterScheduled,
        StatsBridgeLookupResult pregameResult
    ) {
        if (postStartRosterScheduled && pregameResult != null
            && pregameResult.status == StatsBridgeLookupResult.Status.READY) return pregameResult;
        return StatsBridgeLookupResult.unavailable();
    }
}
