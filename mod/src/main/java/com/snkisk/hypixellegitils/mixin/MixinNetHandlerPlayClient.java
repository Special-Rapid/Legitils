package com.snkisk.hypixellegitils.mixin;

import com.snkisk.hypixellegitils.HypixelLegitilsBootstrap;
import com.snkisk.hypixellegitils.detection.NoBreakDelaySignalCheck;
import com.snkisk.hypixellegitils.mixin.accessor.PlayerIdentityAccess;
import com.snkisk.hypixellegitils.nick.NickChatSignal;
import com.snkisk.hypixellegitils.party.BedwarsPreGameState;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S25PacketBlockBreakAnim;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Read-only remote break-progress adapter; it never changes packet handling. */
@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient {
    @Inject(method = "handleChat", at = @At("HEAD"), require = 0)
    private void hypixelLegitils$observePartyJoinChat(S02PacketChat packet, CallbackInfo callbackInfo) {
        Minecraft minecraft = Minecraft.getMinecraft();
        IChatComponent component = packet == null ? null : packet.getChatComponent();
        String message = component == null ? null : component.getUnformattedText();
        boolean bedwarsPreGame = BedwarsPreGameState.isActive(minecraft == null ? null : minecraft.theWorld);
        HypixelLegitilsBootstrap.onPartyDetectorChat(
            message,
            System.currentTimeMillis(),
            bedwarsPreGame
        );
        HypixelLegitilsBootstrap.onPregameGameStartChat(message, System.currentTimeMillis());
        if (bedwarsPreGame && packet != null && packet.getType() != 2) {
            hypixelLegitils$observePregameNickChat(minecraft, message);
        }
    }

    private void hypixelLegitils$observePregameNickChat(Minecraft minecraft, String message) {
        NetHandlerPlayClient handler = minecraft == null ? null : minecraft.getNetHandler();
        if (handler == null) return;
        for (NetworkPlayerInfo info : handler.getPlayerInfoMap()) {
            GameProfile profile = info == null ? null : info.getGameProfile();
            if (profile == null || profile.getId() == null || profile.getId().version() != 1) continue;
            if (NickChatSignal.isMessageFrom(message, profile.getName())) {
                HypixelLegitilsBootstrap.onPregameNickChat(profile.getId(), profile.getName());
                return;
            }
        }
    }

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
