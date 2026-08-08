package com.snkisk.hypixellegitils.mixin;

import com.snkisk.hypixellegitils.HypixelLegitilsBootstrap;
import com.snkisk.hypixellegitils.mixin.accessor.PlayerIdentityAccess;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/** Supplies safe GameProfile identity access to the visible-player observer. */
@Mixin({EntityPlayer.class})
public class MixinEntityPlayer implements PlayerIdentityAccess {
    @Shadow private GameProfile gameProfile;

    @Unique
    @Override
    public java.util.UUID hypixelLegitils$getProfileId() {
        return gameProfile == null ? null : gameProfile.getId();
    }

}
