package com.snkisk.hypixellegitils.mixin;

import com.mojang.authlib.GameProfile;
import com.snkisk.hypixellegitils.HypixelLegitilsBootstrap;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Changes the player-name local that Lunar's head-name renderer reads. */
@Mixin(RendererLivingEntity.class)
public abstract class MixinRendererLivingEntity {
    @ModifyVariable(
        // Lunar retains this MCP method name but replaces the inherited
        // renderOffsetLivingLabel invocation with its own renderer.
        method = "renderName(Lnet/minecraft/entity/EntityLivingBase;DDD)V",
        at = @At("STORE"),
        ordinal = 0
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
