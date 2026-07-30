package com.snkisk.hypixellegitils.observation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** UUID-keyed store with deterministic expiry and oldest-entry eviction. */
public final class PlayerObservationStore {
    private final int maximumEntries;
    private final long staleAfterMillis;
    private final Map<UUID, PlayerObservation> observations = new HashMap<UUID, PlayerObservation>();

    public PlayerObservationStore(int maximumEntries, long staleAfterMillis) {
        if (maximumEntries < 1 || staleAfterMillis < 1L) throw new IllegalArgumentException("Store bounds must be positive");
        this.maximumEntries = maximumEntries;
        this.staleAfterMillis = staleAfterMillis;
    }

    public synchronized PlayerObservation observe(UUID playerId, long nowMillis) {
        if (playerId == null || nowMillis < 0L) throw new IllegalArgumentException("Observation requires UUID and non-negative time");
        pruneExpired(nowMillis);
        PlayerObservation previous = observations.get(playerId);
        PlayerObservation updated = previous == null
            ? new PlayerObservation(playerId, nowMillis, 1)
            : new PlayerObservation(playerId, Math.max(nowMillis, previous.lastObservedAtMillis), previous.sampleCount + 1);
        if (previous == null && observations.size() >= maximumEntries) evictOldest();
        observations.put(playerId, updated);
        return updated;
    }

    public synchronized PlayerObservation get(UUID playerId) {
        return observations.get(playerId);
    }

    public synchronized void pruneExpired(long nowMillis) {
        Iterator<Map.Entry<UUID, PlayerObservation>> iterator = observations.entrySet().iterator();
        while (iterator.hasNext()) {
            PlayerObservation observation = iterator.next().getValue();
            if (nowMillis >= observation.lastObservedAtMillis && nowMillis - observation.lastObservedAtMillis >= staleAfterMillis) {
                iterator.remove();
            }
        }
    }

    public synchronized int size() {
        return observations.size();
    }

    public synchronized void reset() {
        observations.clear();
    }

    private void evictOldest() {
        UUID oldestId = null;
        long oldestTime = Long.MAX_VALUE;
        for (Map.Entry<UUID, PlayerObservation> entry : observations.entrySet()) {
            if (entry.getValue().lastObservedAtMillis < oldestTime) {
                oldestId = entry.getKey();
                oldestTime = entry.getValue().lastObservedAtMillis;
            }
        }
        if (oldestId != null) observations.remove(oldestId);
    }
}
