package com.snkisk.hypixellegitils.detection;

import com.snkisk.hypixellegitils.config.DetectorId;
import com.snkisk.hypixellegitils.evidence.Confidence;
import com.snkisk.hypixellegitils.evidence.Evidence;

/** Clean-room world-tick implementation of the intended Meowtils NoSlow signal. */
final class NoSlowSignalCheck {
    private static final double BASE_MOVEMENT_PER_TICK = 0.05D;
    private static final int REQUIRED_CONSECUTIVE_TICKS = 21;

    Evidence observe(PlayerSample sample, State state) {
        if (!sample.reliable) {
            state.reset();
            return null;
        }
        if (!state.hasPrevious || !state.acceptsTick(sample.worldTick)) {
            state.reset();
            state.setPrevious(sample);
            return null;
        }
        double dx = sample.x - state.previousX;
        double dz = sample.z - state.previousZ;
        double movementPerTick = Math.sqrt(dx * dx + dz * dz);
        boolean expectedSlowState = sample.sprinting && sample.usingItem && !sample.riding;
        double threshold = BASE_MOVEMENT_PER_TICK * (1.0D + Math.max(0, sample.speedPotionAmplifier + 1) * 0.20D);
        state.setPrevious(sample); // Always advance the position sample before evaluating a later tick.
        if (expectedSlowState && movementPerTick > threshold) {
            state.consecutiveFastSamples++;
        } else {
            state.consecutiveFastSamples = 0;
        }
        if (state.consecutiveFastSamples < REQUIRED_CONSECUTIVE_TICKS) return null;
        state.consecutiveFastSamples = 0;
        return new Evidence(DetectorId.NO_SLOW, sample.playerId, Confidence.MEDIUM, sample.observedAtMillis, "NoSlow");
    }

    static final class State {
        private boolean hasPrevious;
        private long previousWorldTick;
        private double previousX;
        private double previousZ;
        private int consecutiveFastSamples;

        private boolean acceptsTick(long worldTick) {
            return worldTick == previousWorldTick + 1L;
        }

        private void setPrevious(PlayerSample sample) {
            hasPrevious = true;
            previousWorldTick = sample.worldTick;
            previousX = sample.x;
            previousZ = sample.z;
        }

        void reset() {
            hasPrevious = false;
            previousWorldTick = -1L;
            previousX = 0.0D;
            previousZ = 0.0D;
            consecutiveFastSamples = 0;
        }
    }
}
