package com.snkisk.hypixellegitils.detection;

import com.snkisk.hypixellegitils.config.DetectorId;
import com.snkisk.hypixellegitils.detection.BedNukeSignalCheck.BedStructure;
import com.snkisk.hypixellegitils.detection.BedNukeSignalCheck.BlockKind;
import com.snkisk.hypixellegitils.detection.BedNukeSignalCheck.BlockPosition;
import com.snkisk.hypixellegitils.evidence.Evidence;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/** Trace tests for the strict, bounded 3D BedNuke contract. */
public class BedNukeSignalCheckTest {
    private static final BlockPosition BED_HEAD = new BlockPosition(0, 64, 0);
    private static final BlockPosition BED_FOOT = new BlockPosition(0, 64, 1);
    private static final BlockPosition OUTER_DEFENSE = new BlockPosition(-2, 64, 0);
    private static final BlockPosition INNER_DEFENSE = new BlockPosition(-1, 64, 0);

    @Test
    public void emitsUnassignedEvidenceWhenTheFullyObservedVolumeHasNoOpenRoute() {
        BedNukeSignalCheck check = registered(true);
        check.observeBlockState(OUTER_DEFENSE, BlockKind.OPEN, 120L);
        removeBed(check, 150L);
        assertNull(check.evaluate(419L));
        Evidence evidence = check.evaluate(420L);
        assertEquals(DetectorId.BED_NUKE, evidence.detector);
        assertNull(evidence.playerId);
    }

    @Test
    public void normalOpeningOnAnyObservedSideCreatesA3dRouteAndDoesNotEmit() {
        BedNukeSignalCheck check = registered(true);
        check.observeBlockState(OUTER_DEFENSE, BlockKind.OPEN, 120L);
        check.observeBlockState(INNER_DEFENSE, BlockKind.OPEN, 140L);
        removeBed(check, 150L);
        assertNull(check.evaluate(420L));
    }

    @Test
    public void verticalRouteFromTheTopBoundaryDoesNotEmit() {
        BedNukeSignalCheck check = registered(true);
        check.observeBlockState(new BlockPosition(0, 65, 0), BlockKind.OPEN, 120L);
        removeBed(check, 150L);
        assertNull(check.evaluate(420L));
    }

    @Test
    public void incompleteHistoryNeverEmits() {
        BedNukeSignalCheck check = registered(false);
        removeBed(check, 150L);
        assertNull(check.evaluate(420L));
    }

    @Test
    public void delayedBedHalvesNeverEmit() {
        BedNukeSignalCheck check = registered(true);
        check.observeBlockState(BED_HEAD, BlockKind.OPEN, 100L);
        check.observeBlockState(BED_FOOT, BlockKind.OPEN, 1000L);
        assertNull(check.evaluate(1300L));
    }

    @Test
    public void worldResetNeverEmits() {
        BedNukeSignalCheck check = registered(true);
        check.reset();
        removeBed(check, 150L);
        assertNull(check.evaluate(420L));
    }

    private static void removeBed(BedNukeSignalCheck check, long atMillis) {
        check.observeBlockState(BED_HEAD, BlockKind.OPEN, atMillis);
        check.observeBlockState(BED_FOOT, BlockKind.OPEN, atMillis + 20L);
    }

    private static BedNukeSignalCheck registered(boolean completeHistory) {
        BedNukeSignalCheck check = new BedNukeSignalCheck();
        BlockPosition minimum = new BlockPosition(-3, 63, -1);
        BlockPosition maximum = new BlockPosition(1, 65, 2);
        Map<BlockPosition, BlockKind> states = completeVolume(minimum, maximum);
        states.put(BED_HEAD, BlockKind.BED);
        states.put(BED_FOOT, BlockKind.BED);
        states.put(OUTER_DEFENSE, BlockKind.SOLID);
        states.put(INNER_DEFENSE, BlockKind.SOLID);
        check.register(new BedStructure(BED_HEAD, BED_FOOT, minimum, maximum, states), 0L, completeHistory);
        return check;
    }

    private static Map<BlockPosition, BlockKind> completeVolume(BlockPosition minimum, BlockPosition maximum) {
        Map<BlockPosition, BlockKind> states = new HashMap<BlockPosition, BlockKind>();
        for (int x = minimum.x; x <= maximum.x; x++) {
            for (int y = minimum.y; y <= maximum.y; y++) {
                for (int z = minimum.z; z <= maximum.z; z++) states.put(new BlockPosition(x, y, z), BlockKind.SOLID);
            }
        }
        states.put(new BlockPosition(-3, 64, 0), BlockKind.OPEN);
        return states;
    }
}
