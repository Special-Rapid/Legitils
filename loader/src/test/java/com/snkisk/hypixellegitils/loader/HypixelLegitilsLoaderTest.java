package com.snkisk.hypixellegitils.loader;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class HypixelLegitilsLoaderTest {
    @Test
    public void acceptsOnlyTheLunarIchorMixinHostClassLoader() {
        assertTrue(HypixelLegitilsLoader.isIchorClassLoaderName(
            "com.moonsworth.lunar.ichor.IchorClassLoader"
        ));
        assertFalse(HypixelLegitilsLoader.isIchorClassLoaderName(
            "org.spongepowered.asm.launch.MixinClassLoader"
        ));
        assertFalse(HypixelLegitilsLoader.isIchorClassLoaderName(null));
    }
}
