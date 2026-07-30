package com.snkisk.hypixellegitils.mixin;

import com.snkisk.hypixellegitils.HypixelLegitilsBootstrap;
import com.snkisk.hypixellegitils.detection.NoBreakDelaySignalCheck;
import com.snkisk.hypixellegitils.mixin.accessor.PlayerIdentityAccess;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S25PacketBlockBreakAnim;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Read-only remote break-progress adapter; it never changes packet handling. */
@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient {
    @Inject(
        method = "handleBlockBreakAnim",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/WorldClient;sendBlockBreakProgress(ILnet/minecraft/util/BlockPos;I)V"
        ),
        require = 0
    )
    private void hypixelLegitils$observeRemoteBreakProgress(S25PacketBlockBreakAnim packet, CallbackInfo callbackInfo) {
        Minecraft minecraft = Minecraft.getMinecraft();
        WorldClient world = minecraft == null ? null : minecraft.theWorld;
        if (world == null || packet == null) return;
        BlockPos position = packet.getPosition();
        Entity entity = world.getEntityByID(packet.getBreakerId());
        if (!(entity instanceof EntityPlayer) || entity == minecraft.thePlayer || position == null || !world.isBlockLoaded(position)) return;
        EntityPlayer player = (EntityPlayer) entity;
        java.util.UUID playerId = player instanceof PlayerIdentityAccess
            ? ((PlayerIdentityAccess) player).hypixelLegitils$getProfileId()
            : null;
        if (playerId == null) return;
        IBlockState state = world.getBlockState(position);
        ItemStack held = player.getHeldItem();
        boolean ordinaryContext = state != null && state.getBlock().getMaterial() != net.minecraft.block.material.Material.air
            && state.getBlock().isFullCube() && held != null && !held.isItemEnchanted()
            && !player.capabilities.isCreativeMode && !player.isPotionActive(Potion.digSpeed)
            && player.getDistanceSq(position) <= 1024.0D;
        HypixelLegitilsBootstrap.onNoBreakDelayProgress(new NoBreakDelaySignalCheck.Progress(
            playerId,
            new NoBreakDelaySignalCheck.BlockPosition(position.getX(), position.getY(), position.getZ()),
            world.getTotalWorldTime(),
            packet.getProgress(),
            true,
            ordinaryContext
        ));
    }
}
