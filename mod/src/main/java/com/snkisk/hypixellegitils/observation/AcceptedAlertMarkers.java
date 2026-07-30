package com.snkisk.hypixellegitils.observation;

import com.snkisk.hypixellegitils.config.MarkerHistoryEntry;
import com.snkisk.hypixellegitils.config.MarkerHistoryStore;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Bounded local UUID history for accepted alerts and manual local blacklisting. */
final class AcceptedAlertMarkers {
    private final Map<UUID, State> states = new HashMap<UUID, State>();

    void restore(Map<UUID, MarkerHistoryEntry> history) {
        states.clear();
        if (history == null) return;
        for (Map.Entry<UUID, MarkerHistoryEntry> entry : history.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            if (states.size() >= MarkerHistoryStore.MAXIMUM_ENTRIES) break;
            MarkerHistoryEntry value = entry.getValue();
            states.put(entry.getKey(), new State(
                value.acceptedCount,
                value.blacklisted,
                value.updatedAtEpochMillis,
                value.mojangResolvedName,
                value.mojangResolvedAtEpochMillis,
                value.observedServerName,
                value.observedServerNameAtEpochMillis
            ));
        }
    }

    void observe(UUID playerId, long nowMillis) {
        if (playerId == null || nowMillis < 0L) return;
        State state = states.get(playerId);
        if (state != null) state.lastObservedAtMillis = nowMillis;
    }

    boolean recordAccepted(UUID playerId, long nowMillis, int autoBlacklistThreshold) {
        if (playerId == null || nowMillis < 0L) return false;
        State state = stateForMutation(playerId, nowMillis);
        if (state == null) return false;
        if (state.count < 1000000) state.count++;
        if (state.count >= autoBlacklistThreshold) state.blacklisted = true;
        state.lastObservedAtMillis = nowMillis;
        state.updatedAtEpochMillis = nowMillis;
        return true;
    }

    boolean blacklist(UUID playerId, long nowMillis) {
        if (playerId == null || nowMillis < 0L) return false;
        State state = stateForMutation(playerId, nowMillis);
        if (state == null || state.blacklisted) return false;
        state.blacklisted = true;
        state.lastObservedAtMillis = nowMillis;
        state.updatedAtEpochMillis = nowMillis;
        return true;
    }

    boolean remove(UUID playerId) {
        return playerId != null && states.remove(playerId) != null;
    }

    boolean clearAll() {
        if (states.isEmpty()) return false;
        states.clear();
        return true;
    }

    boolean isBlacklisted(UUID playerId, long nowMillis) {
        if (playerId == null || nowMillis < 0L) return false;
        State state = states.get(playerId);
        return state != null && state.blacklisted;
    }

    boolean promoteEligible(int autoBlacklistThreshold, long nowMillis) {
        boolean changed = false;
        for (State state : states.values()) {
            if (!state.blacklisted && state.count >= autoBlacklistThreshold) {
                state.blacklisted = true;
                state.updatedAtEpochMillis = nowMillis;
                changed = true;
            }
        }
        return changed;
    }

    int size() {
        return states.size();
    }

    int blacklistedCount() {
        int count = 0;
        for (State state : states.values()) if (state.blacklisted) count++;
        return count;
    }

    boolean recordMojangResolvedName(UUID playerId, String name, long nowMillis) {
        State state = states.get(playerId);
        if (state == null || !state.blacklisted || !validName(name) || name.equals(state.mojangResolvedName)) return false;
        state.mojangResolvedName = name;
        state.mojangResolvedAtEpochMillis = nowMillis;
        return true;
    }

    boolean recordObservedServerName(UUID playerId, String name, long nowMillis) {
        State state = states.get(playerId);
        if (state == null || !state.blacklisted || !validName(name) || name.equals(state.observedServerName)) return false;
        state.observedServerName = name;
        state.observedServerNameAtEpochMillis = nowMillis;
        return true;
    }

    Map<UUID, MarkerHistoryEntry> snapshot() {
        Map<UUID, MarkerHistoryEntry> snapshot = new HashMap<UUID, MarkerHistoryEntry>();
        for (Map.Entry<UUID, State> entry : states.entrySet()) {
            State state = entry.getValue();
            snapshot.put(entry.getKey(), new MarkerHistoryEntry(
                state.count,
                state.blacklisted,
                state.updatedAtEpochMillis,
                state.mojangResolvedName,
                state.mojangResolvedAtEpochMillis,
                state.observedServerName,
                state.observedServerNameAtEpochMillis
            ));
        }
        return snapshot;
    }

    private State stateForMutation(UUID playerId, long nowMillis) {
        State state = states.get(playerId);
        if (state != null) return state;
        if (states.size() >= MarkerHistoryStore.MAXIMUM_ENTRIES) evictOldest();
        state = new State(0, false, nowMillis, null, 0L, null, 0L);
        states.put(playerId, state);
        return state;
    }

    private void evictOldest() {
        UUID oldest = null;
        long oldestTime = Long.MAX_VALUE;
        for (Map.Entry<UUID, State> entry : states.entrySet()) {
            if (oldest == null || entry.getValue().updatedAtEpochMillis < oldestTime) {
                oldest = entry.getKey();
                oldestTime = entry.getValue().updatedAtEpochMillis;
            }
        }
        if (oldest != null) states.remove(oldest);
    }

    private static final class State {
        private int count;
        private boolean blacklisted;
        private long lastObservedAtMillis;
        private long updatedAtEpochMillis;
        private String mojangResolvedName;
        private long mojangResolvedAtEpochMillis;
        private String observedServerName;
        private long observedServerNameAtEpochMillis;

        private State(
            int count,
            boolean blacklisted,
            long updatedAtEpochMillis,
            String mojangResolvedName,
            long mojangResolvedAtEpochMillis,
            String observedServerName,
            long observedServerNameAtEpochMillis
        ) {
            this.count = count;
            this.blacklisted = blacklisted;
            this.updatedAtEpochMillis = updatedAtEpochMillis;
            this.mojangResolvedName = mojangResolvedName;
            this.mojangResolvedAtEpochMillis = mojangResolvedAtEpochMillis;
            this.observedServerName = observedServerName;
            this.observedServerNameAtEpochMillis = observedServerNameAtEpochMillis;
        }
    }

    private static boolean validName(String value) {
        return value != null && value.matches("[A-Za-z0-9_]{1,16}");
    }
}
