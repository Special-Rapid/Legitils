package com.snkisk.hypixellegitils.detection;

/**
 * Conservative Bed visibility test for the BedNuke adapter.
 *
 * A valid Minecraft break can target a small exposed edge of the Bed even when
 * its block centre is hidden. A centre-only ray would incorrectly call that a
 * wall break, so this checks only the player's side of the Bed at 1/32-block
 * spacing, then retries at 1/128 only when the coarse pass found no opening.
 * A blocker at either the very end of a ray or within the bounded
 * remote-position uncertainty near the player is ambiguous. This intentionally
 * favors false negatives over a false flag without treating an opposite-side
 * hole as visible to the breaking player.
 */
public final class BedLineOfSight {
    private static final double BED_TOP = 0.5625D;
    private static final double EDGE_OFFSET = 0.015625D;
    private static final double BED_EDGE_MARGIN = 0.25D;
    // A moving player at up to 250 ms latency can be roughly 1.5 blocks from
    // their last locally observed eye position. Reconstruct only the player's
    // near-side movement envelope, never an eye on the far side of the Bed.
    private static final double PLAYER_FORWARD_MOTION_MARGIN = 1.5D;
    private static final double PLAYER_LATERAL_MOTION_MARGIN = 0.75D;
    private static final double[] COARSE_HORIZONTAL_SAMPLES = samples(32, EDGE_OFFSET, 0.03125D);
    private static final double[] FINE_HORIZONTAL_SAMPLES = samples(128, 0.00390625D, 0.0078125D);
    // Minecraft 1.8 Beds are shorter than a full block. Keep samples inside
    // the actual top volume rather than treating air above the Bed as visible.
    private static final double[] COARSE_VERTICAL_SAMPLES = samples(18, EDGE_OFFSET, 0.03125D);
    private static final double[] FINE_VERTICAL_SAMPLES = samples(72, 0.00390625D, 0.0078125D);

    private BedLineOfSight() {
    }

    /** True when an actual Bed face visible from the player's side is unobstructed. */
    public static boolean canSeeAnyBedPoint(Point eye, BlockPosition bed, RayTester rayTester) {
        if (eye == null || bed == null || rayTester == null) return false;
        for (Point candidateEye : plausibleEyePositions(eye, bed)) {
            if (canSeeAnyBedPointFromEye(candidateEye, bed, rayTester, COARSE_HORIZONTAL_SAMPLES, COARSE_VERTICAL_SAMPLES)) return true;
        }
        // A one-pixel sliver can fall between 1/32 samples. Run this costly
        // pass only after every nearby possible eye position missed the coarse
        // one, preserving ordinary packet-side performance.
        for (Point candidateEye : plausibleEyePositions(eye, bed)) {
            if (canSeeAnyBedPointFromEye(candidateEye, bed, rayTester, FINE_HORIZONTAL_SAMPLES, FINE_VERTICAL_SAMPLES)) return true;
        }
        return false;
    }

    private static boolean canSeeAnyBedPointFromEye(
        Point eye,
        BlockPosition bed,
        RayTester rayTester,
        double[] horizontalSamples,
        double[] verticalSamples
    ) {
        double xFace = eye.x <= bed.x + 0.5D ? EDGE_OFFSET : 1D - EDGE_OFFSET;
        double zFace = eye.z <= bed.z + 0.5D ? EDGE_OFFSET : 1D - EDGE_OFFSET;

        for (double y : verticalSamples) {
            for (double z : horizontalSamples) {
                if (rayTester.reachesBed(eye, new Point(bed.x + xFace, bed.y + y, bed.z + z))) return true;
            }
        }
        for (double y : verticalSamples) {
            for (double x : horizontalSamples) {
                if (rayTester.reachesBed(eye, new Point(bed.x + x, bed.y + y, bed.z + zFace))) return true;
            }
        }
        if (eye.y >= bed.y + BED_TOP) {
            for (double x : horizontalSamples) {
                for (double z : horizontalSamples) {
                    if (rayTester.reachesBed(eye, new Point(bed.x + x, bed.y + BED_TOP - EDGE_OFFSET, bed.z + z))) return true;
                }
            }
        }
        return false;
    }

    /**
     * Builds a small, conservative set of possible recent eye positions. The
     * forward samples stop at the Bed's near face, so a far-side opening never
     * becomes visible merely because the player could have moved.
     */
    private static Point[] plausibleEyePositions(Point eye, BlockPosition bed) {
        double targetX = bed.x + 0.5D;
        double targetZ = bed.z + 0.5D;
        double deltaX = targetX - eye.x;
        double deltaZ = targetZ - eye.z;
        double horizontalLength = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (horizontalLength < 0.0001D) return new Point[] { eye };
        double forwardX = deltaX / horizontalLength;
        double forwardZ = deltaZ / horizontalLength;
        double maximumForward = Math.min(PLAYER_FORWARD_MOTION_MARGIN, Math.max(0D, horizontalLength - 0.5D));
        double sidewaysX = -forwardZ;
        double sidewaysZ = forwardX;
        return new Point[] {
            eye,
            shifted(eye, forwardX, forwardZ, maximumForward, 0D),
            shifted(eye, forwardX, forwardZ, maximumForward * 0.5D, PLAYER_LATERAL_MOTION_MARGIN),
            shifted(eye, forwardX, forwardZ, maximumForward * 0.5D, -PLAYER_LATERAL_MOTION_MARGIN)
        };
    }

    private static Point shifted(Point eye, double forwardX, double forwardZ, double forwardDistance, double lateralDistance) {
        return new Point(
            eye.x + forwardX * forwardDistance - forwardZ * lateralDistance,
            eye.y,
            eye.z + forwardZ * forwardDistance + forwardX * lateralDistance
        );
    }

    /** A final Bed-edge clip is too ambiguous to flag. Eye uncertainty is sampled separately. */
    public static boolean isAmbiguousBlocker(double fullRayDistance, double blockerDistance) {
        return fullRayDistance > 0D && blockerDistance >= 0D
            && blockerDistance >= fullRayDistance - BED_EDGE_MARGIN;
    }

    private static double[] samples(int count, double start, double step) {
        double[] samples = new double[count];
        for (int index = 0; index < count; index++) samples[index] = start + index * step;
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
