package com.snkisk.hypixellegitils.mixin;

import com.snkisk.hypixellegitils.HypixelLegitilsBootstrap;
import com.snkisk.hypixellegitils.mixin.accessor.PlayerIdentityAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Read-only development observer for the vanilla post-break blockHitDelay. */
@Mixin(PlayerControllerMP.class)
public abstract class MixinPlayerControllerMP {
    @Shadow private int blockHitDelay;
    private boolean hypixelLegitils$developmentHookLogged;

    @Inject(method = "onPlayerDamageBlock", at = @At("HEAD"))
    private void hypixelLegitils$observePostBreakDelayBeforeDamage(
        BlockPos position,
        EnumFacing facing,
        CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        hypixelLegitils$submit(position, false);
    }

    @Inject(method = "onPlayerDamageBlock", at = @At("RETURN"))
    private void hypixelLegitils$observePostBreakDelayAfterDamage(
        BlockPos position,
        EnumFacing facing,
        CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        hypixelLegitils$submit(position, callbackInfo.getReturnValue());
    }

    private void hypixelLegitils$submit(BlockPos position, boolean breakCompleted) {
        if (!HypixelLegitilsBootstrap.shouldObserveLocalPlayerForDevelopment()) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayerSP player = minecraft == null ? null : minecraft.thePlayer;
        java.util.UUID playerId = player instanceof PlayerIdentityAccess
            ? ((PlayerIdentityAccess) player).hypixelLegitils$getProfileId()
            : null;
        if (player == null || playerId == null || player.worldObj == null || player.capabilities.isCreativeMode || position == null) return;
        if (!hypixelLegitils$developmentHookLogged) {
            hypixelLegitils$developmentHookLogged = true;
            System.out.println("[HypixelLegitils] NoBreakDelay development controller hook observed.");
        }
        HypixelLegitilsBootstrap.onDevelopmentNoBreakDelay(
            playerId,
            player.worldObj.getTotalWorldTime(),
            blockHitDelay,
            breakCompleted && blockHitDelay >= 5
        );
    }
}
