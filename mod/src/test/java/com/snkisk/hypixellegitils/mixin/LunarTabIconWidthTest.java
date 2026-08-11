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

    @Test
    public void recognizesAProfileEntryAsWellAsTheLegacyUuidContext() {
        UUID playerId = UUID.randomUUID();
        assertTrue(LunarTabIconWidth.matchesPlayerEntry(playerId, playerId));
        assertTrue(LunarTabIconWidth.matchesPlayerEntry(playerId, new FakeEntry(playerId)));
        assertFalse(LunarTabIconWidth.matchesPlayerEntry(playerId, new FakeEntry(UUID.randomUUID())));
    }

    @Test
    public void placesNewNickAndAlertMarkersInsideTheMeasuredStatsColumn() {
        assertEquals("", MixinGuiPlayerTabOverlay.hypixelLegitils$statsColumnMarkers(false, false));
        assertEquals(" §c[NICK]", MixinGuiPlayerTabOverlay.hypixelLegitils$statsColumnMarkers(true, false));
        assertEquals(" §e⚠", MixinGuiPlayerTabOverlay.hypixelLegitils$statsColumnMarkers(false, true));
        assertEquals(" §c[NICK] §e⚠", MixinGuiPlayerTabOverlay.hypixelLegitils$statsColumnMarkers(true, true));
    }

    public static final class FakeEntry {
        private final UUID playerId;

        public FakeEntry(UUID playerId) {
            this.playerId = playerId;
        }

        public FakeProfile getGameProfile() {
            return new FakeProfile(playerId);
        }
    }

    public static final class FakeProfile {
        private final UUID playerId;

        public FakeProfile(UUID playerId) {
            this.playerId = playerId;
        }

        public UUID getId() {
            return playerId;
        }
    }
}
