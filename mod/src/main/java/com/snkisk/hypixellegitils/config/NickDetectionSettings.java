package com.snkisk.hypixellegitils.config;

/** Local-only controls for the client-visible, session-only nick heuristic. */
public final class NickDetectionSettings {
    public final boolean enabled;

    public NickDetectionSettings(boolean enabled) {
        this.enabled = enabled;
    }

    public static NickDetectionSettings defaults() {
        return new NickDetectionSettings(true);
    }

    public boolean sameAs(NickDetectionSettings other) {
        return other != null && enabled == other.enabled;
    }
}
