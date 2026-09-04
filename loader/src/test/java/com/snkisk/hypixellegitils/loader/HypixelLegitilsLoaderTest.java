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
    public void doesNotRequireAnImplementationClassNameForAnExplicitGameStage() {
        assertTrue(HypixelLegitilsLoader.isLabeledLunarGameMixinHost(
            "IchorClassLoader(MIXIN)"
        ));
        assertFalse(HypixelLegitilsLoader.isLabeledLunarGameMixinHost(
            "IchorClassLoader(META_MIXIN)"
        ));
    }

    @Test
    public void rejectsMetaAndPreStagesForTheLoadedMinecraftHost() {
        assertTrue(HypixelLegitilsLoader.isNonMetaOrPreMixinHost("unlabeled Genesis loader"));
        assertFalse(HypixelLegitilsLoader.isNonMetaOrPreMixinHost(
            "IchorClassLoader(META_MIXIN)"
        ));
        assertFalse(HypixelLegitilsLoader.isNonMetaOrPreMixinHost(
            "IchorClassLoader(PRE_OPTIFINE_PATCH)"
        ));
    }

    @Test
    public void emitsOnlyBoundedRecognizedStageDiagnostics() {
        String className = "com.moonsworth.lunar.ichor.IchorClassLoader";
        assertEquals(
            className + "[stage=MIXIN]",
            HypixelLegitilsLoader.describeMixinHostLoader(
                className, "IchorClassLoader(MIXIN) /private/path"
            )
        );
        assertEquals(
            className + "[stage=META_MIXIN]",
            HypixelLegitilsLoader.describeMixinHostLoader(className, "IchorClassLoader(META_MIXIN)")
        );
        List<String> candidates = new ArrayList<String>();
        for (int index = 0; index < HypixelLegitilsLoader.MIXIN_HOST_DIAGNOSTIC_LIMIT + 2; index++) {
            HypixelLegitilsLoader.addMixinHostCandidate(
                candidates, "loader." + index, "opaque loader " + index
            );
        }
        assertEquals(HypixelLegitilsLoader.MIXIN_HOST_DIAGNOSTIC_LIMIT, candidates.size());
        assertEquals("loader.0[stage=unlabeled]", candidates.get(0));
    }

}
