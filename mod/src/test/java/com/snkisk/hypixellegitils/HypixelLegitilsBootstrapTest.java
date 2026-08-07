package com.snkisk.hypixellegitils;

import com.snkisk.hypixellegitils.alert.ChatFormat;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;

public final class HypixelLegitilsBootstrapTest {
    @Test
    public void companionConfigurationNoticeIsDeliveredOnceToTheMinecraftChatQueue() {
        HypixelLegitilsBootstrap.drainPendingConfigurationNotices();

        HypixelLegitilsBootstrap.enqueueCompanionSettingsApplied(42L);

        assertArrayEquals(new String[] {
            ChatFormat.line("§aCompanion settings applied. §7Revision §f42")
        }, HypixelLegitilsBootstrap.drainPendingConfigurationNotices());
        assertArrayEquals(new String[0], HypixelLegitilsBootstrap.drainPendingConfigurationNotices());
    }
}
