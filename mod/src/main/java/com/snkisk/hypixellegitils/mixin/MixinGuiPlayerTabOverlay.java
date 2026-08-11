package com.snkisk.hypixellegitils.mixin;

import com.snkisk.hypixellegitils.HypixelLegitilsBootstrap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Appends a local-only persistent or session-only marker to the transient Tab render string. */
@Mixin({GuiPlayerTabOverlay.class})
public class MixinGuiPlayerTabOverlay {
    /**
     * Uses the post-sort local snapshot, so Lunar remains free to wrap its own sort invocation.
     * This deliberately avoids redirecting {@code Ordering.sortedCopy}, which Lunar redirects itself.
     */
    @ModifyVariable(
        method = {"renderPlayerlist"},
        at = @At("STORE"),
        ordinal = 0,
        require = 0
    )
    private java.util.List<NetworkPlayerInfo> hypixelLegitils$sortTabPlayers(java.util.List<NetworkPlayerInfo> vanillaOrder) {
        return HypixelLegitilsBootstrap.sortedTabPlayers(vanillaOrder);
    }

    @Inject(method = {"renderPlayerlist"}, at = {@At("HEAD")}, require = 0)
    private void hypixelLegitils$beginStatsNameColumn(int width, Scoreboard scoreboard, ScoreObjective objective, CallbackInfo callbackInfo) {
        HypixelLegitilsBootstrap.beginTabStatsRender();
    }

    @Inject(method = {"renderPlayerlist"}, at = {@At("RETURN")}, require = 0)
    private void hypixelLegitils$finishStatsNameColumn(int width, Scoreboard scoreboard, ScoreObjective objective, CallbackInfo callbackInfo) {
        HypixelLegitilsBootstrap.finishTabStatsRender();
    }

    @Inject(method = {"getPlayerName"}, at = {@At("RETURN")}, cancellable = true)
    private void hypixelLegitils$appendMarker(NetworkPlayerInfo info, CallbackInfoReturnable<String> callbackInfo) {
        HypixelLegitilsBootstrap.onMarkerRenderHookObserved("tab");
        if (info == null || info.getGameProfile() == null) return;
        java.util.UUID playerId = info.getGameProfile().getId();
        String renderedName = callbackInfo.getReturnValue();
        String markers = TabStatsMarkers.forPlayer(
            HypixelLegitilsBootstrap.shouldShowNickedProfileMarker(playerId),
            HypixelLegitilsBootstrap.shouldShowAcceptedAlertMarker(playerId)
        );
        // Markers can appear after the prior roster snapshot (for example when
        // a player is blacklisted). Include them before measuring this frame so
        // the next complete snapshot re-aligns every Stats column immediately.
        String statsColumnName = renderedName + markers;
        FontRenderer font = Minecraft.getMinecraft().fontRendererObj;
        int plainRenderedPixelWidth = font == null ? 0 : font.getStringWidth(statsColumnName);
        int renderedPixelWidth = LunarTabIconWidth.measuredWidth(this, playerId, font, statsColumnName, plainRenderedPixelWidth);
        int spacePixelWidth = font == null ? 4 : font.getStringWidth(" ");
        int boldSpacePixelWidth = font == null ? 5 : font.getStringWidth("§l ");
        String starText = HypixelLegitilsBootstrap.statsTabStar(info.getGameProfile().getName(), playerId);
        int starPixelWidth = font == null ? 0 : font.getStringWidth(starText);
        String padding = HypixelLegitilsBootstrap.observeTabStatsName(
            info.getGameProfile().getName(), statsColumnName, renderedPixelWidth, spacePixelWidth, boldSpacePixelWidth,
            starText, starPixelWidth
        );
        String suffix = HypixelLegitilsBootstrap.statsTabSuffix(
            info.getGameProfile().getName(), playerId,
            HypixelLegitilsBootstrap.statsTabStarPadding(info.getGameProfile().getName(), starText, starPixelWidth)
        );
        if (markers.isEmpty() && suffix.isEmpty()) return;
        callbackInfo.setReturnValue(statsColumnName + padding + suffix);
        HypixelLegitilsBootstrap.onMarkerRenderObserved(playerId, markers + suffix);
    }

}
