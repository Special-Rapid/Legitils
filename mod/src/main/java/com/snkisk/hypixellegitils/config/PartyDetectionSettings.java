package com.snkisk.hypixellegitils.config;

/** Local-only control for the Bed Wars pre-game Party Detector. */
public final class PartyDetectionSettings {
    public final boolean enabled;

    public PartyDetectionSettings(boolean enabled) {
        this.enabled = enabled;
    }

    public static PartyDetectionSettings defaults() {
        return new PartyDetectionSettings(true);
    }

    public boolean sameAs(PartyDetectionSettings other) {
        return other != null && enabled == other.enabled;
    }
}
