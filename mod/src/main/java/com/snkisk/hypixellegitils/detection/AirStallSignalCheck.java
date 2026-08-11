package com.snkisk.hypixellegitils.detection;

import com.snkisk.hypixellegitils.config.DetectorId;
import com.snkisk.hypixellegitils.evidence.Confidence;
import com.snkisk.hypixellegitils.evidence.Evidence;

/**
 * Reports sustained visible unsupported air stationarity.  A remote F3+T-like
 * reload may intentionally satisfy this advisory signal; local/global freezes
 * are reset before this check receives a continuous frame.
 */
final class AirStallSignalCheck {
    private static final int MIN_STATIONARY_TICKS = 40;
    private static final double STATIONARY_DISTANCE = 0.015D;
    private static final double MIN_WORLD_MEDIAN_MOVEMENT = 0.02D;

    Evidence observe(PlayerSample sample, State state) {
        if (!isEligible(sample) || state.hasPrevious && sample.worldTick != state.previousWorldTick + 1L) {
            state.reset();
            if (sample.reliable) state.setPrevious(sample);
            return null;
        }
        if (!state.hasPrevious) {
            state.setPrevious(sample);
            return null;
        }

        double distance = distance(sample.x, sample.y, sample.z, state.previousX, state.previousY, state.previousZ);
        state.stationaryTicks = distance <= STATIONARY_DISTANCE ? state.stationaryTicks + 1 : 0;
        state.setPrevious(sample);
        if (state.stationaryTicks < MIN_STATIONARY_TICKS) return null;
        state.stationaryTicks = 0;
        return new Evidence(DetectorId.AIR_STALL, sample.playerId, Confidence.LOW, sample.observedAtMillis, "air-stall anomaly");
    }

    private static boolean isEligible(PlayerSample sample) {
        return sample.reliable
            && sample.supportStateComplete
            && !sample.supportPresent
            && !sample.onGround
            && !sample.riding
            && !sample.inLiquid
            && !sample.onClimbable
            // Any other loaded visible player moving proves this client is
            // still receiving world updates; no arbitrary distance cutoff is
            // needed for that global-freeze guard.
            && sample.worldMovementCount > 0
            && sample.worldMovementMedian >= MIN_WORLD_MEDIAN_MOVEMENT;
    }

    private static double distance(double x, double y, double z, double otherX, double otherY, double otherZ) {
        double dx = x - otherX;
        double dy = y - otherY;
        double dz = z - otherZ;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    static final class State {
        private boolean hasPrevious;
        private long previousWorldTick;
        private double previousX;
        private double previousY;
        private double previousZ;
        private int stationaryTicks;

        void reset() {
            hasPrevious = false;
            stationaryTicks = 0;
        }

        private void setPrevious(PlayerSample sample) {
            hasPrevious = true;
            previousWorldTick = sample.worldTick;
            previousX = sample.x;
            previousY = sample.y;
            previousZ = sample.z;
        }
    }
}
