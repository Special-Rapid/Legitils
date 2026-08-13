package com.snkisk.hypixellegitils.mixin;

import com.snkisk.hypixellegitils.HypixelLegitilsBootstrap;
import com.snkisk.hypixellegitils.detection.NoBreakDelaySignalCheck;
import com.snkisk.hypixellegitils.detection.PlayerObservationEligibility;
import com.snkisk.hypixellegitils.detection.BedLineOfSight;
import com.snkisk.hypixellegitils.mixin.accessor.PlayerIdentityAccess;
import com.snkisk.hypixellegitils.nick.NickChatSignal;
import com.snkisk.hypixellegitils.nick.PregameChatSender;
import com.snkisk.hypixellegitils.party.BedwarsPreGameState;
import com.snkisk.hypixellegitils.stats.BedwarsMode;
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
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Read-only remote break-progress adapter; it never changes packet handling. */
@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient {
    @Inject(method = "handleChat", at = @At("HEAD"), require = 0)
    private void hypixelLegitils$observeServerChat(S02PacketChat packet, CallbackInfo callbackInfo) {
        Minecraft minecraft = Minecraft.getMinecraft();
        IChatComponent component = packet == null ? null : packet.getChatComponent();
        String message = component == null ? null : component.getUnformattedText();
        boolean bedwarsPreGame = BedwarsPreGameState.isActive(minecraft == null ? null : minecraft.theWorld);
        BedwarsMode gameMode = BedwarsPreGameState.mode(minecraft == null ? null : minecraft.theWorld);
        HypixelLegitilsBootstrap.onBedDestructionChat(message, System.currentTimeMillis());
        HypixelLegitilsBootstrap.onWhoRosterResponse(message, System.currentTimeMillis());
        HypixelLegitilsBootstrap.onPregameGameStartChat(message, System.currentTimeMillis());
        if (bedwarsPreGame && NickChatSignal.isGameStartCancelled(message)) {
            HypixelLegitilsBootstrap.onBedwarsGameStartCancelled();
        }
        if (bedwarsPreGame && NickChatSignal.isGameStartCountdown(message)) {
            HypixelLegitilsBootstrap.onBedwarsGameStart(System.currentTimeMillis());
        }
        if (bedwarsPreGame && packet != null && packet.getType() != 2) {
            hypixelLegitils$observePregameNickChat(minecraft, message);
            String senderName = PregameChatSender.visibleName(message);
            HypixelLegitilsBootstrap.traceStats("server chat pregame=" + bedwarsPreGame + " type=" + packet.getType()
                + " mode=" + gameMode + " visibleSender=" + (senderName != null));
            if (senderName != null) {
                HypixelLegitilsBootstrap.onPregameStatsChat(
                    senderName,
                    gameMode
                );
            }
        } else {
            HypixelLegitilsBootstrap.traceStats("server chat skipped pregame=" + bedwarsPreGame
                + " gameStartCountdown=" + NickChatSignal.isGameStartCountdown(message) + " mode=" + gameMode);
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
        if (!PlayerObservationEligibility.shouldObserve(player.isSpectator(), hypixelLegitils$networkSpectator(playerId))) return;
        IBlockState state = world.getBlockState(position);
        if (state != null && state.getBlock() == Blocks.bed && player.getDistanceSq(position) <= 144.0D) {
            HypixelLegitilsBootstrap.onBedBreakAttempt(
                playerId,
                player.getName(),
                hypixelLegitils$isBedRayObstructed(world, player, position)
                    && hypixelLegitils$isPlausiblyAimingAtBed(player, position),
                System.currentTimeMillis()
            );
        }
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

    /** Uses the Bed-targeted server animation as intent; rays only check physical obstruction. */
    private boolean hypixelLegitils$isBedRayObstructed(WorldClient world, EntityPlayer player, BlockPos bedPosition) {
        Vec3 eye = new Vec3(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        return !BedLineOfSight.canSeeAnyBedPoint(
            new BedLineOfSight.Point(eye.xCoord, eye.yCoord, eye.zCoord),
            new BedLineOfSight.BlockPosition(bedPosition.getX(), bedPosition.getY(), bedPosition.getZ()),
            new BedLineOfSight.RayTester() {
                @Override
                public boolean reachesBed(BedLineOfSight.Point eyePoint, BedLineOfSight.Point bedPoint) {
                    MovingObjectPosition hit = world.rayTraceBlocks(
                        new Vec3(eyePoint.x, eyePoint.y, eyePoint.z),
                        new Vec3(bedPoint.x, bedPoint.y, bedPoint.z),
                        false,
                        true,
                        false
                    );
                    if (hit != null && hit.getBlockPos() != null && world.getBlockState(hit.getBlockPos()).getBlock() == Blocks.bed) {
                        return true;
                    }
                    if (hit == null || hit.hitVec == null) return false;
                    Vec3 target = new Vec3(bedPoint.x, bedPoint.y, bedPoint.z);
                    Vec3 rayStart = new Vec3(eyePoint.x, eyePoint.y, eyePoint.z);
                    return BedLineOfSight.isAmbiguousBlocker(rayStart.distanceTo(target), rayStart.distanceTo(hit.hitVec));
                }
            }
        );
    }

    /** Packet-side checks use the same spectator boundary as frame-based anti-cheat checks. */
    private boolean hypixelLegitils$networkSpectator(java.util.UUID playerId) {
        Minecraft minecraft = Minecraft.getMinecraft();
        NetHandlerPlayClient handler = minecraft == null ? null : minecraft.getNetHandler();
        NetworkPlayerInfo info = handler == null || playerId == null ? null : handler.getPlayerInfo(playerId);
        if (info == null && handler != null && playerId != null) {
            for (NetworkPlayerInfo candidate : handler.getPlayerInfoMap()) {
                GameProfile profile = candidate == null ? null : candidate.getGameProfile();
                if (profile != null && playerId.equals(profile.getId())) {
                    info = candidate;
                    break;
                }
            }
        }
        return info != null && info.getGameType() == WorldSettings.GameType.SPECTATOR;
    }

    /** A stale remote rotation is ambiguity, not proof: require a broadly plausible Bed-facing direction. */
    private boolean hypixelLegitils$isPlausiblyAimingAtBed(EntityPlayer player, BlockPos bedPosition) {
        Vec3 eye = new Vec3(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3 toBed = new Vec3(bedPosition.getX() + 0.5D - eye.xCoord, bedPosition.getY() + 0.5D - eye.yCoord, bedPosition.getZ() + 0.5D - eye.zCoord).normalize();
        Vec3 look = player.getLook(1.0F).normalize();
        return look.dotProduct(toBed) >= 0.70D;
    }
}
