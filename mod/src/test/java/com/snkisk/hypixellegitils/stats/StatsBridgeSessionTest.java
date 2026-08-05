package com.snkisk.hypixellegitils.stats;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class StatsBridgeSessionTest {
    @Test
    public void resetInvalidatesAnOldAsynchronousRequestGeneration() {
        StatsBridgeSession session = new StatsBridgeSession();
        long oldGeneration = session.currentGeneration();
        assertTrue(session.isCurrent(oldGeneration));
        session.reset();
        assertFalse(session.isCurrent(oldGeneration));
        assertTrue(session.isCurrent(session.currentGeneration()));
    }
}
