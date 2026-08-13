package com.snkisk.hypixellegitils.detection;

/**
 * Conservative Bed visibility test for the BedNuke adapter.
 *
 * A valid Minecraft break can target a small exposed edge of the Bed even when
 * its block centre is hidden. A centre-only ray would incorrectly call that a
 * wall break, so this checks only the player's side of the Bed at 1/32-block
 * spacing. A blocker at either the very end of a ray or within the bounded
 * remote-position uncertainty near the player is ambiguous. This intentionally
 * favors false negatives over a false flag without treating an opposite-side
 * hole as visible to the breaking player.
 */
public final class BedLineOfSight {
    private static final double BED_TOP = 0.5625D;
    private static final double EDGE_OFFSET = 0.015625D;
    private static final double BED_EDGE_MARGIN = 0.25D;
    // A moving player at up to 250 ms latency can be roughly 1.5 blocks from
    // their last locally observed eye position. Do not flag that uncertainty.
    private static final double PLAYER_MOTION_MARGIN = 1.5D;
    private static final double[] HORIZONTAL_SAMPLES = samples(32);
    // Minecraft 1.8 Beds are shorter than a full block. Keep samples inside
    // the actual top volume rather than treating air above the Bed as visible.
    private static final double[] VERTICAL_SAMPLES = samples(18);

    private BedLineOfSight() {
    }

    /** True when an actual Bed face visible from the player's side is unobstructed. */
    public static boolean canSeeAnyBedPoint(Point eye, BlockPosition bed, RayTester rayTester) {
        if (eye == null || bed == null || rayTester == null) return false;
        double xFace = eye.x <= bed.x + 0.5D ? EDGE_OFFSET : 1D - EDGE_OFFSET;
        double zFace = eye.z <= bed.z + 0.5D ? EDGE_OFFSET : 1D - EDGE_OFFSET;

        for (double y : VERTICAL_SAMPLES) {
            for (double z : HORIZONTAL_SAMPLES) {
                if (rayTester.reachesBed(eye, new Point(bed.x + xFace, bed.y + y, bed.z + z))) return true;
            }
        }
        for (double y : VERTICAL_SAMPLES) {
            for (double x : HORIZONTAL_SAMPLES) {
                if (rayTester.reachesBed(eye, new Point(bed.x + x, bed.y + y, bed.z + zFace))) return true;
            }
        }
        if (eye.y >= bed.y + BED_TOP) {
            for (double x : HORIZONTAL_SAMPLES) {
                for (double z : HORIZONTAL_SAMPLES) {
                    if (rayTester.reachesBed(eye, new Point(bed.x + x, bed.y + BED_TOP - EDGE_OFFSET, bed.z + z))) return true;
                }
            }
        }
        return false;
    }

    /** A near-eye latency uncertainty or a final Bed-edge clip is too ambiguous to flag. */
    public static boolean isAmbiguousBlocker(double fullRayDistance, double blockerDistance) {
        return fullRayDistance > 0D && blockerDistance >= 0D
            && (blockerDistance <= PLAYER_MOTION_MARGIN || blockerDistance >= fullRayDistance - BED_EDGE_MARGIN);
    }

    private static double[] samples(int count) {
        double[] samples = new double[count];
        for (int index = 0; index < count; index++) samples[index] = EDGE_OFFSET + index * 0.03125D;
        return samples;
    }

    public interface RayTester {
        boolean reachesBed(Point eye, Point bedPoint);
    }

    public static final class Point {
        public final double x;
        public final double y;
        public final double z;

        public Point(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static final class BlockPosition {
        public final int x;
        public final int y;
        public final int z;

        public BlockPosition(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
