package com.snkisk.hypixellegitils.mixin;

import com.snkisk.hypixellegitils.HypixelLegitilsBootstrap;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Appends local-only markers through the player display-name path used by vanilla name tags. */
@Mixin({EntityPlayer.class})
public class MixinEntityPlayer {
    @Inject(method = {"getDisplayName"}, at = {@At("RETURN")}, cancellable = true)
    private void hypixelLegitils$appendMarker(CallbackInfoReturnable<IChatComponent> callbackInfo) {
        HypixelLegitilsBootstrap.onMarkerRenderHookObserved("name-tag");
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.thePlayer == null || minecraft.theWorld == null) return;
        EntityPlayer player = (EntityPlayer) (Object) this;
        java.util.UUID playerId = player.getUniqueID();
        String suffix = hypixelLegitils$markerSuffix(playerId);
        if (suffix.isEmpty()) return;
        IChatComponent original = callbackInfo.getReturnValue();
        ChatComponentText marked = new ChatComponentText("");
        if (original != null) marked.appendSibling(original);
        marked.appendSibling(new ChatComponentText(suffix));
        callbackInfo.setReturnValue(marked);
        HypixelLegitilsBootstrap.onMarkerRenderObserved(playerId, suffix);
    }

    private String hypixelLegitils$markerSuffix(java.util.UUID playerId) {
        if (HypixelLegitilsBootstrap.shouldShowNickedSessionMarker(playerId)) return " §c[NICK]";
        if (HypixelLegitilsBootstrap.shouldShowAcceptedAlertMarker(playerId)) return " §e⚠";
        return "";
    }
}
