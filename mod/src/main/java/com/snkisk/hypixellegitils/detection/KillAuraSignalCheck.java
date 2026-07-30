package com.snkisk.hypixellegitils.detection;

import com.snkisk.hypixellegitils.config.DetectorId;
import com.snkisk.hypixellegitils.evidence.Confidence;
import com.snkisk.hypixellegitils.evidence.Evidence;

/** Clean-room world-tick implementation of the observable Meowtils KillAura signal. */
final class KillAuraSignalCheck {
    private static final int MINIMUM_USE_TICKS = 6;
    private static final int RECENT_USE_WINDOW_TICKS = 33;
    private static final int REQUIRED_VIOLATION_LEVEL = 8;

    Evidence observe(PlayerSample sample, State state) {
        if (!sample.reliable || sample.riding) {
            state.reset();
            return null;
        }
        if (state.hasPreviousTick && sample.worldTick != state.lastWorldTick + 1L) state.reset();
        state.hasPreviousTick = true;
        state.lastWorldTick = sample.worldTick;

        boolean consumableInUse = sample.usingItem && sample.holdingConsumable;
        if (consumableInUse) {
            state.useItemTicks++;
        } else {
            if (state.useItemTicks > 0) {
                state.hasCompletedConsumableUse = true;
                state.lastCompletedConsumableUseTick = sample.worldTick;
            }
            state.useItemTicks = 0;
        }

        boolean recentCompletedUse = state.hasCompletedConsumableUse
            && sample.worldTick - state.lastCompletedConsumableUseTick < RECENT_USE_WINDOW_TICKS;
        boolean violatingTick = sample.attackAnimationActive
            && sample.holdingConsumable
            && state.useItemTicks > MINIMUM_USE_TICKS
            && recentCompletedUse;
        if (violatingTick) {
            state.violationLevel++;
        } else if (state.violationLevel > 0) {
            state.violationLevel--;
        }
        if (state.violationLevel < REQUIRED_VIOLATION_LEVEL) return null;
        state.resetPattern();
        return new Evidence(DetectorId.KILL_AURA, sample.playerId, Confidence.LOW, sample.observedAtMillis, "KillAura");
    }

    static final class State {
        private boolean hasPreviousTick;
        private long lastWorldTick;
        private int useItemTicks;
        private boolean hasCompletedConsumableUse;
        private long lastCompletedConsumableUseTick;
        private int violationLevel;

        private void resetPattern() {
            useItemTicks = 0;
            hasCompletedConsumableUse = false;
            lastCompletedConsumableUseTick = -1L;
            violationLevel = 0;
        }

        void reset() {
            hasPreviousTick = false;
            lastWorldTick = -1L;
            resetPattern();
        }
    }
}
