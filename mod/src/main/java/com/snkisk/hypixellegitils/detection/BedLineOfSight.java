package com.snkisk.hypixellegitils.detection;

/**
 * Conservative Bed visibility test for the BedNuke adapter.
 *
 * A valid Minecraft break can target a small exposed edge of the Bed even when
 * its block centre is hidden. A centre-only ray would incorrectly call that a
 * wall break, so this checks the player-facing collision surfaces at 1/16
 * block spacing. This intentionally favors false negatives over a false flag.
 */
public final class BedLineOfSight {
    private static final double BED_TOP = 0.5625D;
    private static final double EDGE_OFFSET = 0.03125D;
    private static final double[] HORIZONTAL_SAMPLES = samples(16, 0.0625D);
    // Minecraft 1.8 Beds are shorter than a full block. Keep samples inside
    // the actual top volume rather than treating air above the Bed as visible.
    private static final double[] VERTICAL_SAMPLES = samples(9, 0.0625D);

    private BedLineOfSight() {
    }

    /** True when at least one real Bed collision surface has an unobstructed line from the player eye. */
    public static boolean canSeeAnyBedPoint(Point eye, BlockPosition bed, RayTester rayTester) {
        if (eye == null || bed == null || rayTester == null) return false;
        double xFace = eye.x <= bed.x + 0.5D ? EDGE_OFFSET : 1D - EDGE_OFFSET;
        double zFace = eye.z <= bed.z + 0.5D ? EDGE_OFFSET : 1D - EDGE_OFFSET;
        double yFace = eye.y >= bed.y + BED_TOP ? BED_TOP - EDGE_OFFSET : EDGE_OFFSET;

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
        for (double x : HORIZONTAL_SAMPLES) {
            for (double z : HORIZONTAL_SAMPLES) {
                if (rayTester.reachesBed(eye, new Point(bed.x + x, bed.y + yFace, bed.z + z))) return true;
            }
        }
        return false;
    }

    private static double[] samples(int count, double increment) {
        double[] samples = new double[count];
        for (int index = 0; index < count; index++) samples[index] = EDGE_OFFSET + index * increment;
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
