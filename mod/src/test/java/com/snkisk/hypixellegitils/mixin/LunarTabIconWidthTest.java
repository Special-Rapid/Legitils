package com.snkisk.hypixellegitils.mixin;

import java.util.UUID;
import java.lang.reflect.Modifier;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class LunarTabIconWidthTest {
    @Test
    public void exposesTheHelperToTheTransformedMinecraftMixinTarget() throws Exception {
        assertTrue(Modifier.isPublic(LunarTabIconWidth.class.getModifiers()));
        assertTrue(Modifier.isPublic(LunarTabIconWidth.class.getMethod(
            "measuredWidth", Object.class, UUID.class, net.minecraft.client.gui.FontRenderer.class, String.class, Integer.TYPE
        ).getModifiers()));
    }

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
