package com.snkisk.hypixellegitils.detection;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class BedLineOfSightTest {
    @Test
    public void nearSideFineGridTreatsAGrazingBedEdgeAsVisible() {
        BedLineOfSight.Point eye = new BedLineOfSight.Point(0D, 65.62D, 0D);
        BedLineOfSight.BlockPosition bed = new BedLineOfSight.BlockPosition(4, 64, 6);

        assertTrue(BedLineOfSight.canSeeAnyBedPoint(eye, bed, new BedLineOfSight.RayTester() {
            @Override
            public boolean reachesBed(BedLineOfSight.Point ignoredEye, BedLineOfSight.Point point) {
                // Model the screenshot case: the player can see only a tiny
                // actual Bed edge on the near face, not its centre.
                return point.x == 4.015625D && point.y == 64.546875D && point.z == 6.984375D;
            }
        }));
    }

    @Test
    public void fullyBlockedBedStillHasNoVisibleHitPoint() {
        assertFalse(BedLineOfSight.canSeeAnyBedPoint(
            new BedLineOfSight.Point(0D, 65D, 0D),
            new BedLineOfSight.BlockPosition(4, 64, 6),
            new BedLineOfSight.RayTester() {
                @Override
                public boolean reachesBed(BedLineOfSight.Point eye, BedLineOfSight.Point point) {
                    return false;
                }
            }
        ));
    }

    @Test
    public void oppositeSideOpeningDoesNotCountAsVisibleToThePlayer() {
        assertFalse(BedLineOfSight.canSeeAnyBedPoint(
            new BedLineOfSight.Point(0D, 65D, 0D),
            new BedLineOfSight.BlockPosition(4, 64, 6),
            new BedLineOfSight.RayTester() {
                @Override
                public boolean reachesBed(BedLineOfSight.Point eye, BedLineOfSight.Point point) {
                    return point.x == 5.015625D;
                }
            }
        ));
    }

    @Test
    public void samplesStayOnThePlayersNearFacesOrTheActualTopFace() {
        assertTrue(BedLineOfSight.canSeeAnyBedPoint(
            new BedLineOfSight.Point(0D, 65D, 0D),
            new BedLineOfSight.BlockPosition(4, 64, 6),
            new BedLineOfSight.RayTester() {
                @Override
                public boolean reachesBed(BedLineOfSight.Point eye, BedLineOfSight.Point point) {
                    boolean nearXFace = point.x == 4.015625D && point.y > 64D && point.y < 64.5625D
                        && point.z > 6D && point.z < 7D;
                    boolean nearZFace = point.z == 6.015625D && point.y > 64D && point.y < 64.5625D
                        && point.x > 4D && point.x < 5D;
                    boolean topFace = point.x > 4D && point.x < 5D && point.y == 64.546875D && point.z > 6D && point.z < 7D;
                    return nearXFace || nearZFace || topFace;
                }
            }
        ));
    }

    @Test
    public void finalBedEdgeBlockerIsAmbiguousButMidRayBlockerIsNot() {
        assertTrue(BedLineOfSight.isAmbiguousBlocker(5D, 4.76D));
        assertFalse(BedLineOfSight.isAmbiguousBlocker(5D, 1.49D));
        assertFalse(BedLineOfSight.isAmbiguousBlocker(5D, 4.74D));
    }

    @Test
    public void plausibleMovingNearSideEyePositionCanSeeAGrazingBedEdge() {
        assertTrue(BedLineOfSight.canSeeAnyBedPoint(
            new BedLineOfSight.Point(0D, 65D, 0D),
            new BedLineOfSight.BlockPosition(4, 64, 0),
            new BedLineOfSight.RayTester() {
                @Override
                public boolean reachesBed(BedLineOfSight.Point eye, BedLineOfSight.Point point) {
                    return eye.x > 1.48D && eye.x <= 1.50D
                        && point.x == 4.015625D && point.z == 0.98046875D;
                }
            }
        ));
    }

    @Test
    public void oneOver128FinePassFindsASliverBetweenCoarseSamples() {
        assertTrue(BedLineOfSight.canSeeAnyBedPoint(
            new BedLineOfSight.Point(0D, 65D, 0D),
            new BedLineOfSight.BlockPosition(4, 64, 6),
            new BedLineOfSight.RayTester() {
                @Override
                public boolean reachesBed(BedLineOfSight.Point eye, BedLineOfSight.Point point) {
                    // 1/256 + 16 / 128 lies between 1/32 coarse sample points.
                    return point.x == 4.015625D && point.y == 64.12890625D && point.z == 6.12890625D;
                }
            }
        ));
    }

    @Test
    public void movementEnvelopeNeverSamplesBeyondTheBedsFarSide() {
        assertFalse(BedLineOfSight.canSeeAnyBedPoint(
            new BedLineOfSight.Point(0D, 65D, 0D),
            new BedLineOfSight.BlockPosition(4, 64, 0),
            new BedLineOfSight.RayTester() {
                @Override
                public boolean reachesBed(BedLineOfSight.Point eye, BedLineOfSight.Point point) {
                    return eye.x > 4D;
                }
            }
        ));
    }
}
