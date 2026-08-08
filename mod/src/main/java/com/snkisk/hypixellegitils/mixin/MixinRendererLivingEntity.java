package com.snkisk.hypixellegitils.mixin;

import com.mojang.authlib.GameProfile;
import com.snkisk.hypixellegitils.HypixelLegitilsBootstrap;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Changes only the final string supplied to vanilla's head-name renderer. */
@Mixin(RendererLivingEntity.class)
public abstract class MixinRendererLivingEntity {
    @ModifyArg(
        method = "renderName(Lnet/minecraft/entity/EntityLivingBase;DDD)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/Render;renderOffsetLivingLabel(Lnet/minecraft/entity/Entity;DDDLjava/lang/String;FD)V"
        ),
        index = 4
    )
    private String hypixelLegitils$appendLocalNametagSuffix(String original, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer)) return original;
        GameProfile profile = ((EntityPlayer) entity).getGameProfile();
        if (profile == null) return original;
        String suffix = HypixelLegitilsBootstrap.playerNametagSuffix(profile.getName(), profile.getId());
        if (suffix.isEmpty()) return original;
        HypixelLegitilsBootstrap.onMarkerRenderHookObserved("name-tag-renderer");
        HypixelLegitilsBootstrap.onMarkerRenderObserved(profile.getId(), suffix);
        return original + suffix;
    }
}
