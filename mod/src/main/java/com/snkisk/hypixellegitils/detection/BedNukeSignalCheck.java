package com.snkisk.hypixellegitils.detection;

import com.snkisk.hypixellegitils.config.DetectorId;
import com.snkisk.hypixellegitils.evidence.Confidence;
import com.snkisk.hypixellegitils.evidence.Evidence;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Pure-Java detector for an unassigned, fully-observed blocked-bed break.
 *
 * <p>It makes no claim about which player acted. The source adapter must first
 * register every block of a small loaded cuboid before the first bed-half state
 * is applied. Evidence is possible only after both halves have disappeared and
 * flood-fill finds no open path from the cuboid exterior to the former bed.
 * Partial volumes, delayed half changes, world resets and global-lag policy
 * suppression all produce no notification.</p>
 */
public final class BedNukeSignalCheck {
    private static final int MAXIMUM_BEDS = 64;
    private static final int MAXIMUM_VOLUME_BLOCKS = 729;
    private static final long BED_HALF_WINDOW_MILLIS = 350L;
    private static final long SETTLING_WINDOW_MILLIS = 250L;
    private final Map<BedKey, State> beds = new HashMap<BedKey, State>();

    /** Registers a complete current-state snapshot for a small, loaded cuboid. */
    public void register(BedStructure structure, long observedAtMillis, boolean completeHistory) {
        if (structure == null || observedAtMillis < 0L) return;
        BedKey key = new BedKey(structure.firstHalf, structure.secondHalf);
        if (!beds.containsKey(key) && beds.size() >= MAXIMUM_BEDS) evictOldest();
        beds.put(key, new State(structure, observedAtMillis, completeHistory));
    }

    /** Records a server-applied block state inside any registered volume. */
    public void observeBlockState(BlockPosition position, BlockKind state, long observedAtMillis) {
        if (position == null || state == null || observedAtMillis < 0L) return;
        for (State bed : beds.values()) bed.observeBlockState(position, state, observedAtMillis);
    }

    /** Evaluates any completed bed removal after a short state-settling window. */
    public Evidence evaluate(long nowMillis) {
        if (nowMillis < 0L) return null;
        Evidence result = null;
        for (State bed : beds.values()) {
            Evidence candidate = bed.evaluate(nowMillis);
            if (candidate != null) result = candidate;
        }
        return result;
    }

    /** Invalidates all observations on world or chunk ambiguity. */
    public void reset() {
        beds.clear();
    }

    public int size() {
        return beds.size();
    }

    private void evictOldest() {
        BedKey oldestKey = null;
        long oldestTime = Long.MAX_VALUE;
        for (Map.Entry<BedKey, State> entry : beds.entrySet()) {
            if (entry.getValue().registeredAtMillis < oldestTime) {
                oldestTime = entry.getValue().registeredAtMillis;
                oldestKey = entry.getKey();
            }
        }
        if (oldestKey != null) beds.remove(oldestKey);
    }

    public enum BlockKind {
        BED,
        SOLID,
        OPEN
    }

    /** Immutable integer block coordinate, independent from Minecraft classes. */
    public static final class BlockPosition {
        public final int x;
        public final int y;
        public final int z;

        public BlockPosition(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof BlockPosition)) return false;
            BlockPosition that = (BlockPosition) other;
            return x == that.x && y == that.y && z == that.z;
        }

        @Override
        public int hashCode() {
            int result = x;
            result = 31 * result + y;
            result = 31 * result + z;
            return result;
        }
    }

    /** Complete, bounded cuboid containing both bed halves. */
    public static final class BedStructure {
        public final BlockPosition firstHalf;
        public final BlockPosition secondHalf;
        private final BlockPosition minimum;
        private final BlockPosition maximum;
        private final Map<BlockPosition, BlockKind> initialStates;

        public BedStructure(
            BlockPosition firstHalf,
            BlockPosition secondHalf,
            BlockPosition minimum,
            BlockPosition maximum,
            Map<BlockPosition, BlockKind> initialStates
        ) {
            if (firstHalf == null || secondHalf == null || firstHalf.equals(secondHalf)
                || minimum == null || maximum == null || initialStates == null
                || minimum.x > maximum.x || minimum.y > maximum.y || minimum.z > maximum.z
                || !contains(minimum, maximum, firstHalf) || !contains(minimum, maximum, secondHalf)) {
                throw new IllegalArgumentException("Bed structure requires two halves inside a complete volume");
            }
            long size = (long) (maximum.x - minimum.x + 1)
                * (long) (maximum.y - minimum.y + 1)
                * (long) (maximum.z - minimum.z + 1);
            if (size > MAXIMUM_VOLUME_BLOCKS) throw new IllegalArgumentException("Bed volume is too large");
            Map<BlockPosition, BlockKind> copy = new HashMap<BlockPosition, BlockKind>();
            for (int x = minimum.x; x <= maximum.x; x++) {
                for (int y = minimum.y; y <= maximum.y; y++) {
                    for (int z = minimum.z; z <= maximum.z; z++) {
                        BlockPosition position = new BlockPosition(x, y, z);
                        BlockKind kind = initialStates.get(position);
                        if (kind == null) throw new IllegalArgumentException("Every volume block requires an initial state");
                        copy.put(position, kind);
                    }
                }
            }
            if (copy.get(firstHalf) != BlockKind.BED || copy.get(secondHalf) != BlockKind.BED) {
                throw new IllegalArgumentException("Both initial bed halves must be BED");
            }
            this.firstHalf = firstHalf;
            this.secondHalf = secondHalf;
            this.minimum = minimum;
            this.maximum = maximum;
            this.initialStates = java.util.Collections.unmodifiableMap(copy);
        }
    }

    private static final class BedKey {
        private final BlockPosition first;
        private final BlockPosition second;

        private BedKey(BlockPosition first, BlockPosition second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof BedKey)) return false;
            BedKey that = (BedKey) other;
            return (first.equals(that.first) && second.equals(that.second))
                || (first.equals(that.second) && second.equals(that.first));
        }

        @Override
        public int hashCode() {
            return first.hashCode() ^ second.hashCode();
        }
    }

    private static final class State {
        private final BedStructure structure;
        private final long registeredAtMillis;
        private final Map<BlockPosition, BlockKind> states;
        private boolean completeHistory;
        private long firstBedRemovalAtMillis = -1L;
        private long fullyRemovedAtMillis = -1L;
        private boolean emitted;

        private State(BedStructure structure, long registeredAtMillis, boolean completeHistory) {
            this.structure = structure;
            this.registeredAtMillis = registeredAtMillis;
            this.completeHistory = completeHistory;
            this.states = new HashMap<BlockPosition, BlockKind>(structure.initialStates);
        }

        private void observeBlockState(BlockPosition position, BlockKind state, long observedAtMillis) {
            if (!states.containsKey(position) || emitted || !completeHistory) return;
            if (observedAtMillis < registeredAtMillis) {
                completeHistory = false;
                return;
            }
            states.put(position, state);
            if (!isBedHalf(position) || state == BlockKind.BED) return;
            if (firstBedRemovalAtMillis < 0L) firstBedRemovalAtMillis = observedAtMillis;
            if (states.get(structure.firstHalf) != BlockKind.BED && states.get(structure.secondHalf) != BlockKind.BED) {
                fullyRemovedAtMillis = observedAtMillis;
            }
        }

        private Evidence evaluate(long nowMillis) {
            if (emitted || !completeHistory || fullyRemovedAtMillis < 0L
                || nowMillis < fullyRemovedAtMillis + SETTLING_WINDOW_MILLIS
                || fullyRemovedAtMillis - firstBedRemovalAtMillis > BED_HALF_WINDOW_MILLIS
                || hasOpenRouteToBed()) return null;
            emitted = true;
            return new Evidence(
                DetectorId.BED_NUKE,
                null,
                Confidence.HIGH,
                nowMillis,
                "unassigned blocked-bed break anomaly: no route through the fully observed defense volume reached the bed"
            );
        }

        private boolean isBedHalf(BlockPosition position) {
            return structure.firstHalf.equals(position) || structure.secondHalf.equals(position);
        }

        private boolean hasOpenRouteToBed() {
            Queue<BlockPosition> queue = new ArrayDeque<BlockPosition>();
            Set<BlockPosition> visited = new HashSet<BlockPosition>();
            for (int x = structure.minimum.x; x <= structure.maximum.x; x++) {
                for (int y = structure.minimum.y; y <= structure.maximum.y; y++) {
                    enqueueBoundary(new BlockPosition(x, y, structure.minimum.z), queue, visited);
                    enqueueBoundary(new BlockPosition(x, y, structure.maximum.z), queue, visited);
                }
            }
            for (int x = structure.minimum.x; x <= structure.maximum.x; x++) {
                for (int z = structure.minimum.z; z <= structure.maximum.z; z++) {
                    enqueueBoundary(new BlockPosition(x, structure.minimum.y, z), queue, visited);
                    enqueueBoundary(new BlockPosition(x, structure.maximum.y, z), queue, visited);
                }
            }
            for (int y = structure.minimum.y; y <= structure.maximum.y; y++) {
                for (int z = structure.minimum.z; z <= structure.maximum.z; z++) {
                    enqueueBoundary(new BlockPosition(structure.minimum.x, y, z), queue, visited);
                    enqueueBoundary(new BlockPosition(structure.maximum.x, y, z), queue, visited);
                }
            }
            while (!queue.isEmpty()) {
                BlockPosition position = queue.remove();
                if (isBedHalf(position)) return true;
                enqueueOpen(position.x + 1, position.y, position.z, queue, visited);
                enqueueOpen(position.x - 1, position.y, position.z, queue, visited);
                enqueueOpen(position.x, position.y + 1, position.z, queue, visited);
                enqueueOpen(position.x, position.y - 1, position.z, queue, visited);
                enqueueOpen(position.x, position.y, position.z + 1, queue, visited);
                enqueueOpen(position.x, position.y, position.z - 1, queue, visited);
            }
            return false;
        }

        private void enqueueBoundary(BlockPosition position, Queue<BlockPosition> queue, Set<BlockPosition> visited) {
            if (states.get(position) == BlockKind.OPEN && visited.add(position)) queue.add(position);
        }

        private void enqueueOpen(int x, int y, int z, Queue<BlockPosition> queue, Set<BlockPosition> visited) {
            BlockPosition position = new BlockPosition(x, y, z);
            if (states.containsKey(position) && states.get(position) == BlockKind.OPEN && visited.add(position)) queue.add(position);
        }
    }

    private static boolean contains(BlockPosition minimum, BlockPosition maximum, BlockPosition position) {
        return position.x >= minimum.x && position.x <= maximum.x
            && position.y >= minimum.y && position.y <= maximum.y
            && position.z >= minimum.z && position.z <= maximum.z;
    }
}
