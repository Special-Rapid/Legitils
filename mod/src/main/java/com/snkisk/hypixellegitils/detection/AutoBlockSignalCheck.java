package com.snkisk.hypixellegitils.detection;

import com.snkisk.hypixellegitils.config.DetectorId;
import com.snkisk.hypixellegitils.evidence.Confidence;
import com.snkisk.hypixellegitils.evidence.Evidence;

/**
 * Clean-room implementation of the Meowtils-visible AutoBlock signal.
 *
 * <p>Eleven uninterrupted client-tick samples of visible blocking plus swing
 * animation are required. The wall-clock continuity check is a product safety
 * guard: a stalled or missing observation stream never bridges two sequences.</p>
 */
final class AutoBlockSignalCheck {
    private static final int REQUIRED_CONSECUTIVE_TICKS = 11;

    Evidence observe(PlayerSample sample, State state) {
        if (!state.continuous(sample.observedAtMillis) || !sample.reliable) state.clearPattern();
        if (sample.blocking && sample.swinging && sample.reliable) state.consecutiveTicks++;
        else state.clearPattern();
        state.lastObservedAtMillis = sample.observedAtMillis;
        if (state.consecutiveTicks < REQUIRED_CONSECUTIVE_TICKS) return null;
        state.clearPattern();
        return new Evidence(DetectorId.AUTO_BLOCK, sample.playerId, Confidence.MEDIUM, sample.observedAtMillis, "AutoBlock");
    }

    static final class State {
        private long lastObservedAtMillis = -1L;
        private int consecutiveTicks;

        private boolean continuous(long nowMillis) {
            return lastObservedAtMillis < 0L || nowMillis >= lastObservedAtMillis && nowMillis - lastObservedAtMillis <= 150L;
        }

        private void clearPattern() {
            consecutiveTicks = 0;
        }

        void reset() {
            lastObservedAtMillis = -1L;
            clearPattern();
        }
    }
}
