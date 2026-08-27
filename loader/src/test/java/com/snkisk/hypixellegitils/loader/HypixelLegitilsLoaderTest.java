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

    @Test
    public void acceptsOnlyTheIchorGameMixinStage() {
        String className = "com.moonsworth.lunar.ichor.IchorClassLoader";
        assertTrue(HypixelLegitilsLoader.isLunarGameMixinClassLoader(
            className, "IchorClassLoader(MIXIN)"
        ));
        assertFalse(HypixelLegitilsLoader.isLunarGameMixinClassLoader(
            className, "IchorClassLoader(META_MIXIN)"
        ));
        assertFalse(HypixelLegitilsLoader.isLunarGameMixinClassLoader(
            className, "IchorClassLoader(PRE_OPTIFINE_PATCH)"
        ));
        assertFalse(HypixelLegitilsLoader.isLunarGameMixinClassLoader(
            "org.spongepowered.asm.launch.MixinClassLoader", "IchorClassLoader(MIXIN)"
        ));
    }
}
