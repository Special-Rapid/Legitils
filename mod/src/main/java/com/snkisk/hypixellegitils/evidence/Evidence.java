package com.snkisk.hypixellegitils.evidence;

import com.snkisk.hypixellegitils.config.DetectorId;
import java.util.UUID;

/** Immutable advisory observation. It is never a cheat verdict. */
public final class Evidence {
    public final DetectorId detector;
    public final UUID playerId;
    public final Confidence confidence;
    public final long observedAtMillis;
    public final String observation;

    public Evidence(DetectorId detector, UUID playerId, Confidence confidence, long observedAtMillis, String observation) {
        if (detector == null || confidence == null || observation == null || observation.trim().isEmpty()) {
            throw new IllegalArgumentException("Evidence requires detector, confidence, and observation text");
        }
        if (observedAtMillis < 0L) throw new IllegalArgumentException("Evidence time must be non-negative");
        this.detector = detector;
        // Null is an explicitly unassigned local-world observation. It must not
        // be treated as a player identity or shown as one by an alert sink.
        this.playerId = playerId;
        this.confidence = confidence;
        this.observedAtMillis = observedAtMillis;
        this.observation = observation;
    }

    public String advisoryText() {
        return "[HypixelLegitils] " + confidence.name().toLowerCase(java.util.Locale.ROOT) + " confidence: " + observation;
    }
}
