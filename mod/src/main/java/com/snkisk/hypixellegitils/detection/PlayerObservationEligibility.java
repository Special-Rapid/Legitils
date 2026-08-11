package com.snkisk.hypixellegitils.detection;

/** One conservative boundary for every per-player local anti-cheat observation. */
public final class PlayerObservationEligibility {
    private PlayerObservationEligibility() {
    }

    /** Either client-visible spectator signal excludes the player from all detector inputs. */
    public static boolean shouldObserve(boolean entitySpectator, boolean networkSpectator) {
        return !entitySpectator && !networkSpectator;
    }
}
