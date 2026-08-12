package com.snkisk.hypixellegitils.detection;

/**
 * Conservative Bed visibility test for the BedNuke adapter.
 *
 * A valid Minecraft break can target a small exposed edge of the Bed even when
 * its block centre is hidden. A centre-only ray would incorrectly call that a
 * wall break, so this checks a bounded grid inside the Bed collision volume.
 */
public final class BedLineOfSight {
    private static final double[] HORIZONTAL_SAMPLES = new double[] { 0.0625D, 0.25D, 0.5D, 0.75D, 0.9375D };
    // Minecraft 1.8 Beds are shorter than a full block. Keep samples inside
    // the actual top volume rather than treating air above the Bed as visible.
    private static final double[] VERTICAL_SAMPLES = new double[] { 0.0625D, 0.25D, 0.5D };

    private BedLineOfSight() {
    }

    /** True when at least one real Bed hit point has an unobstructed line from the player eye. */
    public static boolean canSeeAnyBedPoint(Point eye, BlockPosition bed, RayTester rayTester) {
        if (eye == null || bed == null || rayTester == null) return false;
        for (double x : HORIZONTAL_SAMPLES) {
            for (double y : VERTICAL_SAMPLES) {
                for (double z : HORIZONTAL_SAMPLES) {
                    Point point = new Point(bed.x + x, bed.y + y, bed.z + z);
                    if (rayTester.reachesBed(eye, point)) return true;
                }
            }
        }
        return false;
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
