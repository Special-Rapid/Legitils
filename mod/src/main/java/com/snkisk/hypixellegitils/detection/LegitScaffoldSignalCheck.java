package com.snkisk.hypixellegitils.detection;

import com.snkisk.hypixellegitils.config.DetectorId;
import com.snkisk.hypixellegitils.evidence.Confidence;
import com.snkisk.hypixellegitils.evidence.Evidence;

/** Clean-room world-tick implementation of the Meowtils-visible LegitScaffold signal. */
final class LegitScaffoldSignalCheck {
    private static final int MAXIMUM_CROUCH_DURATIONS = 5;
    private static final long COOLDOWN_TICKS = 60L;

    Evidence observe(PlayerSample sample, State state) {
        return observe(sample, state, false);
    }

    Evidence observe(PlayerSample sample, State state, boolean bypassCooldown) {
        if (!sample.reliable) {
            state.resetPattern();
            return null;
        }
        if (!state.acceptsTick(sample.worldTick)) {
            state.resetPattern();
            state.lastWorldTick = sample.worldTick;
            return null;
        }
        state.trackCrouch(sample);
        if (sample.swingStartedThisTick) state.lastSwingTick = sample.worldTick;
        state.lastWorldTick = sample.worldTick;
        if (!isScaffoldContext(sample) || !state.matches(sample.worldTick)) return null;
        if (!bypassCooldown && state.lastFlagTick >= 0L && sample.worldTick - state.lastFlagTick < COOLDOWN_TICKS) return null;
        state.lastFlagTick = sample.worldTick;
        return new Evidence(DetectorId.LEGIT_SCAFFOLD, sample.playerId, Confidence.LOW, sample.observedAtMillis, "LegitScaffold");
    }

    private static boolean isScaffoldContext(PlayerSample sample) {
        return sample.holdingBlock && sample.onGround && sample.pitch >= 60.0F;
    }

    static final class State {
        private boolean hasSneakState;
        private boolean lastSneaking;
        private long crouchStartTick = -1L;
        private long lastCrouchEndTick = -1L;
        private long lastSwingTick = -1L;
        private long lastFlagTick = -1L;
        private long lastWorldTick = -1L;
        private final java.util.ArrayDeque<Integer> crouchDurations = new java.util.ArrayDeque<Integer>();

        private boolean acceptsTick(long worldTick) {
            return lastWorldTick < 0L || worldTick == lastWorldTick + 1L;
        }

        private void trackCrouch(PlayerSample sample) {
            if (!hasSneakState) {
                hasSneakState = true;
                lastSneaking = sample.sneaking;
                if (sample.sneaking) crouchStartTick = sample.worldTick;
                return;
            }
            if (sample.sneaking && !lastSneaking) {
                crouchStartTick = sample.worldTick;
            } else if (!sample.sneaking && lastSneaking) {
                long start = crouchStartTick >= 0L ? crouchStartTick : sample.worldTick - 1L;
                long duration = sample.worldTick - start;
                if (duration >= Integer.MIN_VALUE && duration <= Integer.MAX_VALUE) {
                    crouchDurations.addFirst(Integer.valueOf((int) duration));
                    while (crouchDurations.size() > MAXIMUM_CROUCH_DURATIONS) crouchDurations.removeLast();
                }
                lastCrouchEndTick = sample.worldTick;
                crouchStartTick = -1L;
            }
            lastSneaking = sample.sneaking;
        }

        private boolean matches(long worldTick) {
            if (lastCrouchEndTick < 0L || crouchDurations.size() < 3) return false;
            int latestDuration = crouchDurations.peekFirst().intValue();
            if (latestDuration < 1 || latestDuration > 2) return false;
            int checked = 0;
            for (Integer duration : crouchDurations) {
                if (checked++ == 3) break;
                if (duration.intValue() > 3) return false;
            }
            return lastSwingTick >= lastCrouchEndTick
                && lastSwingTick <= lastCrouchEndTick + 3L
                && worldTick - lastSwingTick <= 10L;
        }

        void reset() {
            resetPattern();
            lastFlagTick = -1L;
        }

        void resetPattern() {
            hasSneakState = false;
            lastSneaking = false;
            crouchStartTick = -1L;
            lastCrouchEndTick = -1L;
            lastSwingTick = -1L;
            lastWorldTick = -1L;
            crouchDurations.clear();
        }
    }
}
