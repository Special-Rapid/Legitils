package com.snkisk.hypixellegitils.config;

/** Local-only visual advisory settings; no player state is persisted. */
public final class MarkerSettings {
    public static final int MINIMUM_THRESHOLD = 2;
    public static final int MAXIMUM_THRESHOLD = 10;
    public static final int DEFAULT_THRESHOLD = 3;

    public final boolean enabled;
    public final int threshold;

    public MarkerSettings(boolean enabled, int threshold) {
        if (threshold < MINIMUM_THRESHOLD || threshold > MAXIMUM_THRESHOLD) {
            throw new IllegalArgumentException("Marker threshold outside allowed range");
        }
        this.enabled = enabled;
        this.threshold = threshold;
    }

    public static MarkerSettings defaults() {
        return new MarkerSettings(false, DEFAULT_THRESHOLD);
    }

    public boolean sameAs(MarkerSettings other) {
        return other != null && enabled == other.enabled && threshold == other.threshold;
    }
}
