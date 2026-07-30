package com.snkisk.hypixellegitils.config;

/** Immutable local-only marker history entry with optional non-nick name caches for display only. */
public final class MarkerHistoryEntry {
    public final int acceptedCount;
    public final boolean blacklisted;
    public final long updatedAtEpochMillis;
    public final String mojangResolvedName;
    public final long mojangResolvedAtEpochMillis;
    public final String observedServerName;
    public final long observedServerNameAtEpochMillis;

    public MarkerHistoryEntry(int acceptedCount, boolean blacklisted, long updatedAtEpochMillis) {
        this(acceptedCount, blacklisted, updatedAtEpochMillis, null, 0L, null, 0L);
    }

    public MarkerHistoryEntry(
        int acceptedCount,
        boolean blacklisted,
        long updatedAtEpochMillis,
        String mojangResolvedName,
        long mojangResolvedAtEpochMillis,
        String observedServerName,
        long observedServerNameAtEpochMillis
    ) {
        if (acceptedCount < 0 || acceptedCount > 1000000 || updatedAtEpochMillis < 0L) {
            throw new IllegalArgumentException("Invalid marker history entry");
        }
        if (!validName(mojangResolvedName) || !validName(observedServerName)
            || mojangResolvedAtEpochMillis < 0L || observedServerNameAtEpochMillis < 0L
            || (mojangResolvedName == null && mojangResolvedAtEpochMillis != 0L)
            || (observedServerName == null && observedServerNameAtEpochMillis != 0L)) {
            throw new IllegalArgumentException("Invalid marker name cache");
        }
        this.acceptedCount = acceptedCount;
        this.blacklisted = blacklisted;
        this.updatedAtEpochMillis = updatedAtEpochMillis;
        this.mojangResolvedName = mojangResolvedName;
        this.mojangResolvedAtEpochMillis = mojangResolvedAtEpochMillis;
        this.observedServerName = observedServerName;
        this.observedServerNameAtEpochMillis = observedServerNameAtEpochMillis;
    }

    private static boolean validName(String value) {
        return value == null || value.matches("[A-Za-z0-9_]{1,16}");
    }
}
