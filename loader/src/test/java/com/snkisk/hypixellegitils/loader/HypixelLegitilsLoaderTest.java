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

    @Test
    public void acceptsTheExactLabeledGameStageWithoutTrustingOtherStages() {
        assertTrue(HypixelLegitilsLoader.isLabeledLunarGameMixinHost(
            "IchorClassLoader(MIXIN)"
        ));
        assertFalse(HypixelLegitilsLoader.isLabeledLunarGameMixinHost(
            "IchorClassLoader(META_MIXIN)"
        ));
        assertFalse(HypixelLegitilsLoader.isLabeledLunarGameMixinHost(
            "IchorClassLoader(PRE_OPTIFINE_PATCH)"
        ));
        assertFalse(HypixelLegitilsLoader.isLabeledLunarGameMixinHost(
            "IchorClassLoader(MIXIN) META_MIXIN"
        ));
        assertFalse(HypixelLegitilsLoader.isLabeledLunarGameMixinHost(
            "IchorClassLoader(MIXIN) PRE_OPTIFINE_PATCH"
        ));
        assertFalse(HypixelLegitilsLoader.isLabeledLunarGameMixinHost(null));
    }

    @Test
    public void acceptsOnlyAnUnlabeledIchorHostAfterItsMixinClassIsObserved() {
        String className = "com.moonsworth.lunar.ichor.IchorClassLoader";
        assertTrue(HypixelLegitilsLoader.isUnlabeledLunarIchorMixinClassLoader(
            className, "com.moonsworth.lunar.ichor.IchorClassLoader@1234"
        ));
        assertFalse(HypixelLegitilsLoader.isUnlabeledLunarIchorMixinClassLoader(
            className, "IchorClassLoader(META_MIXIN)"
        ));
        assertFalse(HypixelLegitilsLoader.isUnlabeledLunarIchorMixinClassLoader(
            className, "IchorClassLoader(PRE_OPTIFINE_PATCH)"
        ));
        assertFalse(HypixelLegitilsLoader.isUnlabeledLunarIchorMixinClassLoader(
            "org.spongepowered.asm.launch.MixinClassLoader", "unlabeled"
        ));
    }
}
