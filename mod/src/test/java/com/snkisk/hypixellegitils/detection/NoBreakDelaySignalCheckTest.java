package com.snkisk.hypixellegitils.detection;

import com.snkisk.hypixellegitils.config.DetectorId;
import com.snkisk.hypixellegitils.evidence.Evidence;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class NoBreakDelaySignalCheckTest {
    @Test
    public void emitsAfterTwoConsecutiveConfirmedBreaksWithoutTheNormalDelay() {
        NoBreakDelaySignalCheck check = new NoBreakDelaySignalCheck();
        UUID player = UUID.randomUUID();
        assertNull(complete(check, player, 0L, 4L, 4L, 0));
        Evidence evidence = complete(check, player, 5L, 8L, 8L, 1);
        assertEquals(DetectorId.NO_BREAK_DELAY, evidence.detector);
        assertEquals(player, evidence.playerId);
    }

    @Test
    public void normalPostBreakDelayAndMissingConfirmationNeverEmit() {
        NoBreakDelaySignalCheck check = new NoBreakDelaySignalCheck();
        UUID player = UUID.randomUUID();
        assertNull(complete(check, player, 0L, 4L, 4L, 0));
        assertNull(complete(check, player, 9L, 13L, 13L, 1));
        assertNull(complete(check, player, 18L, 22L, 22L, 2));
        assertNull(complete(check, player, 27L, 31L, 31L, 3));

        NoBreakDelaySignalCheck missing = new NoBreakDelaySignalCheck();
        NoBreakDelaySignalCheck.BlockPosition position = new NoBreakDelaySignalCheck.BlockPosition(2, 64, 2);
        assertNull(missing.observeProgress(progress(player, position, 0L, 0)));
        assertNull(missing.observeProgress(progress(player, position, 4L, 9)));
        assertNull(missing.observeBlockRemoval(position, 6L, true));
    }

    @Test
    public void incompleteContextAndResetDiscardAllPartialHistory() {
        NoBreakDelaySignalCheck check = new NoBreakDelaySignalCheck();
        UUID player = UUID.randomUUID();
        NoBreakDelaySignalCheck.BlockPosition position = new NoBreakDelaySignalCheck.BlockPosition(1, 64, 1);
        assertNull(check.observeProgress(progress(player, position, 0L, 0)));
        assertNull(check.observeProgress(progress(player, position, 4L, 9)));
        assertNull(check.observeBlockRemoval(position, 4L, false));
        check.reset();
        assertNull(complete(check, player, 5L, 8L, 8L, 1));
    }

    @Test
    public void twoActorsAtTheSamePositionMakeTheRemovalUnassigned() {
        NoBreakDelaySignalCheck check = new NoBreakDelaySignalCheck();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        NoBreakDelaySignalCheck.BlockPosition position = new NoBreakDelaySignalCheck.BlockPosition(0, 64, 0);
        assertNull(check.observeProgress(progress(first, position, 0L, 0)));
        assertNull(check.observeProgress(progress(first, position, 4L, 9)));
        assertNull(check.observeProgress(progress(second, position, 4L, 0)));
        assertNull(check.observeProgress(progress(second, position, 4L, 9)));
        assertNull(check.observeBlockRemoval(position, 4L, true));
    }

    @Test
    public void developmentObserverDetectsTheActualPostBreakDelayBypass() {
        NoBreakDelaySignalCheck check = new NoBreakDelaySignalCheck();
        UUID player = UUID.randomUUID();
        assertNull(check.observeLocalPostBreakDelay(player, 100L, 5, true));
        Evidence evidence = check.observeLocalPostBreakDelay(player, 101L, 0, false);
        assertEquals(DetectorId.NO_BREAK_DELAY, evidence.detector);
        assertEquals(player, evidence.playerId);
    }

    @Test
    public void developmentObserverDoesNotFlagTheNormalFiveTickDelay() {
        NoBreakDelaySignalCheck check = new NoBreakDelaySignalCheck();
        UUID player = UUID.randomUUID();
        assertNull(check.observeLocalPostBreakDelay(player, 100L, 5, true));
        assertNull(check.observeLocalPostBreakDelay(player, 101L, 5, false));
        assertNull(check.observeLocalPostBreakDelay(player, 102L, 4, false));
        assertNull(check.observeLocalPostBreakDelay(player, 103L, 3, false));
        assertNull(check.observeLocalPostBreakDelay(player, 104L, 2, false));
        assertNull(check.observeLocalPostBreakDelay(player, 105L, 1, false));
        assertNull(check.observeLocalPostBreakDelay(player, 106L, 0, false));
    }

    @Test
    public void developmentObserverRequiresAnActualPostBreakCompletionBeforeItCanFlag() {
        NoBreakDelaySignalCheck check = new NoBreakDelaySignalCheck();
        UUID player = UUID.randomUUID();
        assertNull(check.observeLocalPostBreakDelay(player, 100L, 0, false));
        assertNull(check.observeLocalPostBreakDelay(player, 101L, 0, false));
    }

    private static Evidence complete(NoBreakDelaySignalCheck check, UUID player, long startTick, long finalTick, long removalTick, int position) {
        NoBreakDelaySignalCheck.BlockPosition block = new NoBreakDelaySignalCheck.BlockPosition(position, 64, 0);
        assertNull(check.observeProgress(progress(player, block, startTick, 0)));
        assertNull(check.observeProgress(progress(player, block, finalTick, 9)));
        return check.observeBlockRemoval(block, removalTick, true);
    }

    private static NoBreakDelaySignalCheck.Progress progress(UUID player, NoBreakDelaySignalCheck.BlockPosition position, long tick, int stage) {
        return new NoBreakDelaySignalCheck.Progress(player, position, tick, stage, true, true);
    }
}
