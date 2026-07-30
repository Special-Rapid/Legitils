package com.snkisk.hypixellegitils.observation;

import java.util.UUID;

/** Bounded, local-only per-player lifecycle state. */
public final class PlayerObservation {
    public final UUID playerId;
    public final long lastObservedAtMillis;
    public final int sampleCount;

    public PlayerObservation(UUID playerId, long lastObservedAtMillis, int sampleCount) {
        this.playerId = playerId;
        this.lastObservedAtMillis = lastObservedAtMillis;
        this.sampleCount = sampleCount;
    }
}
