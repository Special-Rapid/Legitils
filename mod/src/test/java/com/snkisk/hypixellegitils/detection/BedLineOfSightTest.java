package com.snkisk.hypixellegitils.detection;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class BedLineOfSightTest {
    @Test
    public void exposedBedEdgeIsEnoughToTreatTheBreakAsVisible() {
        BedLineOfSight.Point eye = new BedLineOfSight.Point(0D, 65.62D, 0D);
        BedLineOfSight.BlockPosition bed = new BedLineOfSight.BlockPosition(4, 64, 6);

        assertTrue(BedLineOfSight.canSeeAnyBedPoint(eye, bed, new BedLineOfSight.RayTester() {
            @Override
            public boolean reachesBed(BedLineOfSight.Point ignoredEye, BedLineOfSight.Point point) {
                // Model the screenshot case: only one small exposed Bed edge is visible.
                return point.x == 4.0625D && point.y == 64.5D && point.z == 6.9375D;
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
    public void samplesStayInsideTheBedCollisionVolume() {
        assertTrue(BedLineOfSight.canSeeAnyBedPoint(
            new BedLineOfSight.Point(0D, 65D, 0D),
            new BedLineOfSight.BlockPosition(4, 64, 6),
            new BedLineOfSight.RayTester() {
                @Override
                public boolean reachesBed(BedLineOfSight.Point eye, BedLineOfSight.Point point) {
                    return point.x > 4D && point.x < 5D
                        && point.y > 64D && point.y <= 64.5D
                        && point.z > 6D && point.z < 7D;
                }
            }
        ));
    }
}
