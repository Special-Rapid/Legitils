package com.snkisk.hypixellegitils.detection;

import com.snkisk.hypixellegitils.config.DetectorId;
import com.snkisk.hypixellegitils.evidence.Confidence;
import com.snkisk.hypixellegitils.evidence.Evidence;

/**
 * Conservative visible stall-and-snap signal.  It reports only an observed
 * combat-correlated desync pattern; it cannot identify Blink, packet intent,
 * or a resource reload.
 */
final class CombatDesyncSignalCheck {
    private static final int MIN_STALL_TICKS = 8;
    private static final double STATIONARY_DISTANCE = 0.015D;
    private static final double MIN_RESUME_DISTANCE = 3.0D;
    private static final double MIN_NEARBY_MEDIAN_MOVEMENT = 0.02D;
    private static final int REQUIRED_COMBAT_EPISODES = 2;
    private static final long EPISODE_WINDOW_TICKS = 600L;

    Evidence observe(PlayerSample sample, State state) {
        if (!sample.reliable || state.hasPrevious && sample.worldTick != state.previousWorldTick + 1L) {
            state.reset();
            state.setPrevious(sample);
            return null;
        }
        if (!state.hasPrevious) {
            state.setPrevious(sample);
            return null;
        }

        double distance = distance(sample.x, sample.y, sample.z, state.previousX, state.previousY, state.previousZ);
        if (state.windowStartedAtTick >= 0L && sample.worldTick - state.windowStartedAtTick > EPISODE_WINDOW_TICKS) {
            state.resetEpisodes(sample.worldTick);
        }
        if (distance <= STATIONARY_DISTANCE) {
            state.stationaryTicks++;
            state.combatDuringStall |= sample.combatContext;
            state.setPrevious(sample);
            return null;
        }

        Evidence evidence = null;
        if (state.stationaryTicks >= MIN_STALL_TICKS && distance >= MIN_RESUME_DISTANCE) {
            boolean nearbyPlayersUpdated = sample.nearbyMovementCount > 0
                && sample.nearbyMovementMedian >= MIN_NEARBY_MEDIAN_MOVEMENT;
            if (!nearbyPlayersUpdated) {
                // Never stitch a final alert across a broad/ambiguous remote
                // update stall where the required comparison is absent.
                state.resetEpisodes(-1L);
            } else if (state.combatDuringStall) {
                if (state.windowStartedAtTick < 0L) state.windowStartedAtTick = sample.worldTick;
                state.combatEpisodes++;
                if (state.combatEpisodes >= REQUIRED_COMBAT_EPISODES
                    && state.combatEpisodes >= state.nonCombatEpisodes + REQUIRED_COMBAT_EPISODES) {
                    evidence = new Evidence(
                        DetectorId.COMBAT_DESYNC,
                        sample.playerId,
                        Confidence.LOW,
                        sample.observedAtMillis,
                        "combat-correlated desync anomaly"
                    );
                    state.resetEpisodes(sample.worldTick);
                }
            } else {
                if (state.windowStartedAtTick < 0L) state.windowStartedAtTick = sample.worldTick;
                state.nonCombatEpisodes++;
            }
        }
        state.stationaryTicks = 0;
        state.combatDuringStall = false;
        state.setPrevious(sample);
        return evidence;
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
        private boolean combatDuringStall;
        private int combatEpisodes;
        private int nonCombatEpisodes;
        private long windowStartedAtTick = -1L;

        void reset() {
            hasPrevious = false;
            stationaryTicks = 0;
            combatDuringStall = false;
            resetEpisodes(-1L);
        }

        void resetPattern() {
            stationaryTicks = 0;
            combatDuringStall = false;
            resetEpisodes(-1L);
        }

        private void resetEpisodes(long newWindowStartedAtTick) {
            combatEpisodes = 0;
            nonCombatEpisodes = 0;
            windowStartedAtTick = newWindowStartedAtTick;
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
