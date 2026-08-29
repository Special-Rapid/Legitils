package com.snkisk.hypixellegitils.loader;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
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
    public void describesOnlyKnownStagesWithoutEmittingTheRawLoaderDescription() {
        String className = "com.moonsworth.lunar.ichor.IchorClassLoader";
        assertEquals(
            className + "[stage=MIXIN]",
            HypixelLegitilsLoader.describeMixinHostLoader(className, "IchorClassLoader(MIXIN) /private/path")
        );
        assertEquals(
            className + "[stage=META_MIXIN]",
            HypixelLegitilsLoader.describeMixinHostLoader(className, "IchorClassLoader(META_MIXIN)")
        );
        assertEquals(
            className + "[stage=PRE_OPTIFINE_PATCH]",
            HypixelLegitilsLoader.describeMixinHostLoader(className, "IchorClassLoader(PRE_OPTIFINE_PATCH)")
        );
        assertEquals(
            className + "[stage=unlabeled]",
            HypixelLegitilsLoader.describeMixinHostLoader(className, "opaque loader 123")
        );
        assertEquals(
            "bootstrap[stage=unlabeled]",
            HypixelLegitilsLoader.describeMixinHostLoader(null, null)
        );
    }

    @Test
    public void boundsAndDeduplicatesMixinHostDiagnostics() {
        List<String> candidates = new ArrayList<String>();
        for (int index = 0; index < HypixelLegitilsLoader.MIXIN_HOST_DIAGNOSTIC_LIMIT + 2; index++) {
            HypixelLegitilsLoader.addMixinHostCandidate(
                candidates,
                "com.moonsworth.lunar.ichor.Loader" + index,
                "opaque loader " + index
            );
        }
        HypixelLegitilsLoader.addMixinHostCandidate(
            candidates,
            "com.moonsworth.lunar.ichor.Loader0",
            "opaque loader 0"
        );
        assertEquals(HypixelLegitilsLoader.MIXIN_HOST_DIAGNOSTIC_LIMIT, candidates.size());
        assertEquals(
            "com.moonsworth.lunar.ichor.Loader0[stage=unlabeled]",
            candidates.get(0)
        );
    }
}
