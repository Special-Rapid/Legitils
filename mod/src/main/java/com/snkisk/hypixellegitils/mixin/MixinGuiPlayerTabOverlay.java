package com.snkisk.hypixellegitils.mixin;

import com.snkisk.hypixellegitils.HypixelLegitilsBootstrap;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Appends a local-only persistent or session-only marker to the transient Tab render string. */
@Mixin({GuiPlayerTabOverlay.class})
public class MixinGuiPlayerTabOverlay {
    @Inject(method = {"getPlayerName"}, at = {@At("RETURN")}, cancellable = true)
    private void hypixelLegitils$appendMarker(NetworkPlayerInfo info, CallbackInfoReturnable<String> callbackInfo) {
        HypixelLegitilsBootstrap.onMarkerRenderHookObserved("tab");
        if (info == null || info.getGameProfile() == null) return;
        java.util.UUID playerId = info.getGameProfile().getId();
        String suffix = "";
        if (HypixelLegitilsBootstrap.shouldShowNickedProfileMarker(playerId)) suffix += " §c[NICK]";
        if (HypixelLegitilsBootstrap.shouldShowAcceptedAlertMarker(playerId)) suffix += " §e⚠";
        if (suffix.isEmpty()) return;
        callbackInfo.setReturnValue(callbackInfo.getReturnValue() + suffix);
        HypixelLegitilsBootstrap.onMarkerRenderObserved(playerId, suffix);
    }

}
