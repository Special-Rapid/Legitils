package com.snkisk.hypixellegitils.stats;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ManualStatsLookupBudgetTest {
    @Test
    public void boundsOutstandingManualWorkAndReleasesAfterDeliveryOrDiscard() {
        ManualStatsLookupBudget budget = new ManualStatsLookupBudget(2);

        assertTrue(budget.tryReserve());
        assertTrue(budget.tryReserve());
        assertFalse(budget.tryReserve());
        assertEquals(2, budget.pendingCount());

        budget.release();
        assertEquals(1, budget.pendingCount());
        assertTrue(budget.tryReserve());
        assertEquals(2, budget.pendingCount());

        budget.release();
        budget.release();
        budget.release();
        assertEquals(0, budget.pendingCount());
    }
}
