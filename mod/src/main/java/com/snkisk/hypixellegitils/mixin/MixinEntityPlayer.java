package com.snkisk.hypixellegitils.mixin;

import com.snkisk.hypixellegitils.HypixelLegitilsBootstrap;
import com.snkisk.hypixellegitils.mixin.accessor.PlayerIdentityAccess;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Appends local-only markers through the player display-name path used by vanilla name tags. */
@Mixin({EntityPlayer.class})
public class MixinEntityPlayer implements PlayerIdentityAccess {
    @Shadow private GameProfile gameProfile;

    @Unique
    @Override
    public java.util.UUID hypixelLegitils$getProfileId() {
        return gameProfile == null ? null : gameProfile.getId();
    }

    @Inject(method = {"getDisplayName"}, at = {@At("RETURN")}, cancellable = true)
    private void hypixelLegitils$appendMarker(CallbackInfoReturnable<IChatComponent> callbackInfo) {
        HypixelLegitilsBootstrap.onMarkerRenderHookObserved("name-tag");
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.thePlayer == null || minecraft.theWorld == null) return;
        java.util.UUID playerId = hypixelLegitils$getProfileId();
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
        String suffix = "";
        if (HypixelLegitilsBootstrap.shouldShowNickedSessionMarker(playerId)) suffix += " §c[NICK]";
        if (HypixelLegitilsBootstrap.shouldShowAcceptedAlertMarker(playerId)) suffix += " §e⚠";
        return suffix;
    }
}
