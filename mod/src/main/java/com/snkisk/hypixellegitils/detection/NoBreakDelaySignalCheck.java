package com.snkisk.hypixellegitils.detection;

import com.snkisk.hypixellegitils.config.DetectorId;
import com.snkisk.hypixellegitils.evidence.Confidence;
import com.snkisk.hypixellegitils.evidence.Evidence;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Conservative remote mining-cadence signal.
 *
 * <p>It accepts only a resolved remote player progress sequence that reaches
 * the final animation stage and is immediately matched to a server-applied
 * air transition at the same position. One confirmed next-break start
 * within one world tick of the previous confirmed break is required.</p>
 */
public final class NoBreakDelaySignalCheck {
    private static final int REQUIRED_IMMEDIATE_RESTARTS = 1;
    private static final long MAXIMUM_CONFIRMATION_TICKS = 1L;
    private static final long EXPECTED_LOCAL_POST_BREAK_DELAY_TICKS = 5L;
    private static final int MAXIMUM_PLAYERS = 256;
    private static final int MAXIMUM_PENDING_POSITIONS = 256;
    private final Map<UUID, PlayerState> players = new HashMap<UUID, PlayerState>();
    private final Map<BlockPosition, PendingBreak> pendingByPosition = new HashMap<BlockPosition, PendingBreak>();
    private final Map<UUID, Long> localPostBreakTicks = new HashMap<UUID, Long>();

    /** Records one actor-resolved, eligible S25 block-break animation progress value. */
    public Evidence observeProgress(Progress progress) {
        if (progress == null || !progress.isEligible()) return null;
        PlayerState state = players.get(progress.playerId);
        if (state == null) {
            if (players.size() >= MAXIMUM_PLAYERS) reset();
            state = new PlayerState();
            players.put(progress.playerId, state);
        }
        if (progress.stage == 0) {
            state.currentPosition = progress.position;
            state.currentStartTick = progress.worldTick;
            state.reachedFinalStage = false;
            state.immediateRestart = state.lastConfirmedBreakTick >= 0L
                && progress.worldTick >= state.lastConfirmedBreakTick
                && progress.worldTick - state.lastConfirmedBreakTick <= MAXIMUM_CONFIRMATION_TICKS;
            return null;
        }
        if (state.currentPosition == null || !state.currentPosition.equals(progress.position)
            || progress.worldTick < state.currentStartTick) {
            state.resetCurrent();
            return null;
        }
        if (progress.stage < 9) return null;
        state.reachedFinalStage = true;
        PendingBreak existing = pendingByPosition.get(progress.position);
        if (existing != null && (existing.ambiguous || !existing.playerId.equals(progress.playerId))) {
            invalidatePosition(progress.position);
            pendingByPosition.put(progress.position, PendingBreak.ambiguous());
            return null;
        }
        if (pendingByPosition.size() >= MAXIMUM_PENDING_POSITIONS && existing == null) {
            reset();
            return null;
        }
        pendingByPosition.put(progress.position, new PendingBreak(progress.playerId, progress.worldTick, state.immediateRestart));
        return null;
    }

    /** Matches a pending final animation to its actual server-applied air state. */
    public Evidence observeBlockRemoval(BlockPosition position, long worldTick, boolean completeContext) {
        if (position == null || worldTick < 0L || !completeContext) return null;
        PendingBreak pending = pendingByPosition.remove(position);
        if (pending == null || pending.ambiguous || worldTick < pending.finalStageTick
            || worldTick - pending.finalStageTick > MAXIMUM_CONFIRMATION_TICKS) return null;
        PlayerState state = players.get(pending.playerId);
        if (state == null || !state.reachedFinalStage || state.currentPosition == null || !state.currentPosition.equals(position)) return null;
        state.lastConfirmedBreakTick = worldTick;
        state.resetCurrent();
        if (!pending.immediateRestart) {
            state.immediateRestartStreak = 0;
            return null;
        }
        state.immediateRestartStreak++;
        if (state.immediateRestartStreak < REQUIRED_IMMEDIATE_RESTARTS) return null;
        state.immediateRestartStreak = 0;
        return new Evidence(
            DetectorId.NO_BREAK_DELAY,
            pending.playerId,
            Confidence.HIGH,
            worldTick * 50L,
            "mining cadence anomaly"
        );
    }

    /**
     * Development-only local controller signal. The normal client sets a five-tick
     * blockHitDelay after a survival block completion; reaching zero before that
     * window is direct evidence that the post-break delay was bypassed.
     */
    public Evidence observeLocalPostBreakDelay(UUID playerId, long worldTick, int blockHitDelay, boolean breakCompleted) {
        if (playerId == null || worldTick < 0L || blockHitDelay < 0) return null;
        if (breakCompleted && blockHitDelay >= EXPECTED_LOCAL_POST_BREAK_DELAY_TICKS) {
            if (localPostBreakTicks.size() >= MAXIMUM_PLAYERS && !localPostBreakTicks.containsKey(playerId)) reset();
            localPostBreakTicks.put(playerId, Long.valueOf(worldTick));
            return null;
        }
        Long completedAt = localPostBreakTicks.get(playerId);
        if (completedAt == null || worldTick < completedAt.longValue()) return null;
        long elapsed = worldTick - completedAt.longValue();
        if (elapsed >= EXPECTED_LOCAL_POST_BREAK_DELAY_TICKS) {
            localPostBreakTicks.remove(playerId);
            return null;
        }
        if (elapsed > 0L && blockHitDelay == 0) {
            localPostBreakTicks.remove(playerId);
            return new Evidence(
                DetectorId.NO_BREAK_DELAY,
                playerId,
                Confidence.HIGH,
                worldTick * 50L,
                "post-break delay bypass"
            );
        }
        return null;
    }

    /** Any world, chunk, lag, or observation ambiguity discards partial traces. */
    public void reset() {
        players.clear();
        pendingByPosition.clear();
        localPostBreakTicks.clear();
    }

    private void invalidatePosition(BlockPosition position) {
        for (PlayerState state : players.values()) {
            if (position.equals(state.currentPosition)) {
                state.resetCurrent();
                state.immediateRestartStreak = 0;
            }
        }
    }

    public static final class Progress {
        public final UUID playerId;
        public final BlockPosition position;
        public final long worldTick;
        public final int stage;
        private final boolean actorVisible;
        private final boolean ordinaryContext;

        public Progress(UUID playerId, BlockPosition position, long worldTick, int stage, boolean actorVisible, boolean ordinaryContext) {
            this.playerId = playerId;
            this.position = position;
            this.worldTick = worldTick;
            this.stage = stage;
            this.actorVisible = actorVisible;
            this.ordinaryContext = ordinaryContext;
        }

        private boolean isEligible() {
            return playerId != null && position != null && worldTick >= 0L && stage >= 0 && stage <= 9
                && actorVisible && ordinaryContext;
        }
    }

    /** Minecraft-independent immutable integer position. */
    public static final class BlockPosition {
        public final int x;
        public final int y;
        public final int z;

        public BlockPosition(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof BlockPosition)) return false;
            BlockPosition that = (BlockPosition) other;
            return x == that.x && y == that.y && z == that.z;
        }

        @Override public int hashCode() {
            int result = x;
            result = 31 * result + y;
            return 31 * result + z;
        }
    }

    private static final class PendingBreak {
        private final UUID playerId;
        private final long finalStageTick;
        private final boolean immediateRestart;
        private final boolean ambiguous;

        private PendingBreak(UUID playerId, long finalStageTick, boolean immediateRestart) {
            this.playerId = playerId;
            this.finalStageTick = finalStageTick;
            this.immediateRestart = immediateRestart;
            this.ambiguous = false;
        }

        private PendingBreak() {
            this.playerId = null;
            this.finalStageTick = -1L;
            this.immediateRestart = false;
            this.ambiguous = true;
        }

        private static PendingBreak ambiguous() {
            return new PendingBreak();
        }
    }

    private static final class PlayerState {
        private BlockPosition currentPosition;
        private long currentStartTick;
        private long lastConfirmedBreakTick = -1L;
        private boolean immediateRestart;
        private boolean reachedFinalStage;
        private int immediateRestartStreak;

        private void resetCurrent() {
            currentPosition = null;
            immediateRestart = false;
            reachedFinalStage = false;
        }
    }
}
