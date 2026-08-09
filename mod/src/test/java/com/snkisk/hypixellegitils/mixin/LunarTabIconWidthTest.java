package com.snkisk.hypixellegitils.mixin;

import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class LunarTabIconWidthTest {
    @Test
    public void safelyKeepsTheFontWidthWhenLunarInternalsAreUnavailable() {
        assertEquals(37, LunarTabIconWidth.measuredWidth(new Object(), UUID.randomUUID(), null, "Name", 37));
    }

    @Test
    public void rejectsAChangedConcreteLunarCallbackTypeBeforeProxyConstruction() {
        assertFalse(LunarTabIconWidth.supportsBooleanReference(String.class));
        assertTrue(LunarTabIconWidth.supportsBooleanReference(Runnable.class));
    }
}
