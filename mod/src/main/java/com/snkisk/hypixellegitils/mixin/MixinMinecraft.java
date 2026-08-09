package com.snkisk.hypixellegitils.mixin;

import com.snkisk.hypixellegitils.HypixelLegitilsBootstrap;
import com.snkisk.hypixellegitils.alert.AlertPresentation;
import com.snkisk.hypixellegitils.alert.FlagMessage;
import com.snkisk.hypixellegitils.alert.LocalNotice;
import com.snkisk.hypixellegitils.detection.PlayerSample;
import com.snkisk.hypixellegitils.mixin.accessor.PlayerIdentityAccess;
import com.snkisk.hypixellegitils.party.BedwarsPreGameState;
import com.snkisk.hypixellegitils.stats.StatsBridgeRosterMember;
import com.snkisk.hypixellegitils.stats.BedwarsMode;
import com.snkisk.hypixellegitils.stats.WhoStatsRefresh;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBucketMilk;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.BlockPos;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.scoreboard.ScorePlayerTeam;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @Shadow private EntityPlayerSP thePlayer;
    @Shadow private WorldClient theWorld;
    private long hypixelLegitils$lastChatSequence;
    private long hypixelLegitils$lastSoundSequence;
    private long hypixelLegitils$lastObservationAtMillis;
    private long hypixelLegitils$frameNowMillis;
    private boolean hypixelLegitils$frameGlobalLag;
    private WorldClient hypixelLegitils$observedWorld;
    private boolean hypixelLegitils$injectedNoticeShown;
    private long hypixelLegitils$lastObservedWorldTick = -1L;
    private UUID hypixelLegitils$selfPlayerId;
    private final Map<UUID, hypixelLegitils$VisiblePosition> hypixelLegitils$previousVisiblePositions
        = new HashMap<UUID, hypixelLegitils$VisiblePosition>();
    private final Map<UUID, hypixelLegitils$LunarNametagCache> hypixelLegitils$lunarNametagCaches
        = new HashMap<UUID, hypixelLegitils$LunarNametagCache>();
    private boolean hypixelLegitils$lunarNametagCacheUnavailable;

    @Inject(method = "startGame", at = @At("RETURN"))
    private void hypixelLegitils$afterStartGame(CallbackInfo callbackInfo) {
        HypixelLegitilsBootstrap.onMinecraftStarted();
    }

    @Inject(method = "runTick", at = @At("HEAD"))
    private void hypixelLegitils$beforeClientTick(CallbackInfo callbackInfo) {
        long nowMillis = System.currentTimeMillis();
        boolean globalLag = hypixelLegitils$lastObservationAtMillis > 0L
            && nowMillis >= hypixelLegitils$lastObservationAtMillis
            && nowMillis - hypixelLegitils$lastObservationAtMillis > 250L;
        hypixelLegitils$lastObservationAtMillis = nowMillis;
        hypixelLegitils$frameNowMillis = nowMillis;
        hypixelLegitils$frameGlobalLag = globalLag;
        // Use the EntityPlayer mixin's runtime-safe profile access. Direct Session#getProfile
        // calls are reobfuscated to Forge SRG names, which Lunar's Ichor runtime does not expose.
        hypixelLegitils$selfPlayerId = hypixelLegitils$profileId(thePlayer);
        if (thePlayer != null && hypixelLegitils$selfPlayerId != null) {
            HypixelLegitilsBootstrap.onDeveloperSelfPlayerObserved(hypixelLegitils$selfPlayerId);
        }
        HypixelLegitilsBootstrap.onDevelopmentFrame(globalLag);
    }

    @Inject(method = "runTick", at = @At("RETURN"))
    private void hypixelLegitils$afterClientTick(CallbackInfo callbackInfo) {
        HypixelLegitilsBootstrap.onVisibleBedwarsMode(BedwarsPreGameState.mode(theWorld));
        boolean bedwarsPreGame = BedwarsPreGameState.isActive(theWorld);
        HypixelLegitilsBootstrap.onPartyDetectorTick(BedwarsPreGameState.playerCount(theWorld));
        HypixelLegitilsBootstrap.onBedwarsPregameState(bedwarsPreGame, hypixelLegitils$frameNowMillis);
        if (theWorld != null && thePlayer != null) {
            boolean worldChanged = theWorld != hypixelLegitils$observedWorld;
            if (LocalNotice.shouldShowFor(hypixelLegitils$injectedNoticeShown, theWorld)) {
                thePlayer.addChatMessage(new ChatComponentText(LocalNotice.injectedText()));
                hypixelLegitils$injectedNoticeShown = true;
            }
            long worldTick = theWorld.getTotalWorldTime();
            boolean discontinuity = worldChanged
                || hypixelLegitils$lastObservedWorldTick >= 0L
                && worldTick != hypixelLegitils$lastObservedWorldTick + 1L;
            if (discontinuity) {
                hypixelLegitils$previousVisiblePositions.clear();
                HypixelLegitilsBootstrap.onObservationDiscontinuity();
            }
            hypixelLegitils$observedWorld = theWorld;
            hypixelLegitils$lastObservedWorldTick = worldTick;
            if (!discontinuity || worldChanged) {
                HypixelLegitilsBootstrap.onObservedPlayers(
                    hypixelLegitils$visiblePlayerSamples(hypixelLegitils$frameNowMillis, worldTick),
                    hypixelLegitils$frameGlobalLag
                );
            }
            hypixelLegitils$submitDueStatsRoster();
            hypixelLegitils$refreshLunarNametagCaches();
        } else {
            hypixelLegitils$observedWorld = null;
            hypixelLegitils$lastObservedWorldTick = -1L;
            hypixelLegitils$previousVisiblePositions.clear();
            hypixelLegitils$lunarNametagCaches.clear();
        }
        if (thePlayer != null) {
            for (String response : HypixelLegitilsBootstrap.drainPendingBlacklistResponses()) {
                if (response != null && !response.isEmpty()) thePlayer.addChatMessage(new ChatComponentText(response));
            }
            for (String notice : HypixelLegitilsBootstrap.drainPendingNickNotices()) {
                if (notice != null && !notice.isEmpty()) thePlayer.addChatMessage(new ChatComponentText(notice));
            }
            for (HypixelLegitilsBootstrap.PendingTeamNickNotice notice : HypixelLegitilsBootstrap.drainPendingTeamNickNotices(hypixelLegitils$frameNowMillis)) {
                if (notice == null) continue;
                thePlayer.addChatMessage(new ChatComponentText(HypixelLegitilsBootstrap.pregameNickNotice(
                    notice.serverPresentedName,
                    hypixelLegitils$teamFormattedName(notice.playerId, notice.serverPresentedName)
                )));
            }
            for (String notice : HypixelLegitilsBootstrap.drainPendingPartyDetectorNotices()) {
                if (notice != null && !notice.isEmpty()) thePlayer.addChatMessage(new ChatComponentText(notice));
            }
            for (HypixelLegitilsBootstrap.PendingStatsNotice notice : HypixelLegitilsBootstrap.drainPendingStatsNotices()) {
                if (notice != null && notice.text != null && !notice.text.isEmpty()) {
                    thePlayer.addChatMessage(hypixelLegitils$statsChatComponent(notice));
                }
            }
            for (String notice : HypixelLegitilsBootstrap.drainPendingConfigurationNotices()) {
                if (notice != null && !notice.isEmpty()) thePlayer.addChatMessage(new ChatComponentText(notice));
            }
            for (HypixelLegitilsBootstrap.PendingStatsNotice response : HypixelLegitilsBootstrap.drainPendingManualStatsNotices()) {
                if (response != null && response.text != null && !response.text.isEmpty()) {
                    thePlayer.addChatMessage(hypixelLegitils$statsChatComponent(response));
                }
            }
        }
        AlertPresentation presentation = HypixelLegitilsBootstrap.onClientTick(hypixelLegitils$frameNowMillis);
        if (presentation == null) return;
        if (presentation.shouldEmitChatAfter(hypixelLegitils$lastChatSequence) && thePlayer != null) {
            thePlayer.addChatMessage(hypixelLegitils$chatComponent(presentation));
        }
        hypixelLegitils$lastChatSequence = presentation.sequence;
        if (presentation.sequence > hypixelLegitils$lastSoundSequence) {
            if (presentation.shouldEmitSoundAfter(hypixelLegitils$lastSoundSequence)) {
                ((Minecraft) (Object) this).getSoundHandler().playSound(
                    PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F)
                );
            }
            hypixelLegitils$lastSoundSequence = presentation.sequence;
        }
    }

    private ChatComponentText hypixelLegitils$statsChatComponent(HypixelLegitilsBootstrap.PendingStatsNotice notice) {
        ChatComponentText component = new ChatComponentText(notice.text);
        String styledCode = notice.tagCode;
        int codeStart = styledCode == null ? -1 : notice.text.indexOf(styledCode);
        if (notice.tooltip != null && !notice.tooltip.isEmpty() && codeStart >= 0) {
            component = new ChatComponentText(notice.text.substring(0, codeStart));
            ChatComponentText code = new ChatComponentText(styledCode);
            code.setChatStyle(new ChatStyle().setChatHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT, new ChatComponentText(notice.tooltip)
            )));
            component.appendSibling(code);
            component.appendText(notice.text.substring(codeStart + styledCode.length()));
        }
        return component;
    }

    private ChatComponentText hypixelLegitils$chatComponent(AlertPresentation presentation) {
        FlagMessage message = FlagMessage.anonymous(presentation.detector);
        EntityPlayer player = hypixelLegitils$visiblePlayer(presentation.playerId);
        if (player != null) {
            message = FlagMessage.attributed(
                presentation.detector,
                hypixelLegitils$teamFormattedName(presentation.playerId, player.getName()),
                player.getName(),
                hypixelLegitils$selfPlayerId == null || !presentation.playerId.equals(hypixelLegitils$selfPlayerId)
            );
        }
        ChatComponentText root = new ChatComponentText(message.chatPrefixText);
        if (message.wdrTarget != null) {
            ChatComponentText wdr = new ChatComponentText("\u00a74[WDR]");
            wdr.getChatStyle().setChatClickEvent(
                new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/wdr " + message.wdrTarget)
            );
            root.appendSibling(wdr);
        }
        return root;
    }

    private EntityPlayer hypixelLegitils$visiblePlayer(UUID playerId) {
        if (theWorld == null || playerId == null) return null;
        List<EntityPlayer> players = theWorld.playerEntities;
        for (EntityPlayer player : players) {
            if (player != null && playerId.equals(hypixelLegitils$profileId(player))) return player;
        }
        return null;
    }

    private void hypixelLegitils$submitDueStatsRoster() {
        NetHandlerPlayClient handler = ((Minecraft) (Object) this).getNetHandler();
        if (handler == null) {
            HypixelLegitilsBootstrap.traceStats("roster due skipped no net handler");
            return;
        }
        BedwarsMode gameMode = HypixelLegitilsBootstrap.statsModeFor(BedwarsPreGameState.mode(theWorld));
        String requestedWhoRefresh = HypixelLegitilsBootstrap.consumePendingWhoStatsRefresh();
        if (requestedWhoRefresh != null) {
            hypixelLegitils$collectAndRequestStatsRoster(handler, gameMode, requestedWhoRefresh, "who refresh");
        }
        if (!HypixelLegitilsBootstrap.isStatsRosterDue(hypixelLegitils$frameNowMillis)) return;
        String postStartMatchId = HypixelLegitilsBootstrap.consumeDueStatsMatchId(hypixelLegitils$frameNowMillis);
        String automaticWhoRefresh = HypixelLegitilsBootstrap.automaticWhoStatsRefreshMatchId(postStartMatchId);
        WhoStatsRefresh.PostStartAction action = WhoStatsRefresh.postStartAction(postStartMatchId, automaticWhoRefresh);
        if (action == null || thePlayer == null) return;
        thePlayer.sendChatMessage(action.outboundCommand);
        hypixelLegitils$collectAndRequestStatsRoster(handler, gameMode, action.refreshMatchId, "post-start automatic who");
    }

    /** Client-thread Tab snapshot used by both manual and automatic `/who` refreshes. */
    private void hypixelLegitils$collectAndRequestStatsRoster(
        NetHandlerPlayClient handler,
        BedwarsMode gameMode,
        String matchId,
        String source
    ) {
        if (handler == null || matchId == null) return;
        Map<String, StatsBridgeRosterMember> members = new LinkedHashMap<String, StatsBridgeRosterMember>();
        Map<String, String> teamFormattedNames = new LinkedHashMap<String, String>();
        for (NetworkPlayerInfo info : handler.getPlayerInfoMap()) {
            GameProfile profile = info == null ? null : info.getGameProfile();
            if (profile == null || profile.getName() == null) continue;
            UUID profileId = profile.getId();
            // Lunar can hide the pre-game roster but leaves the local profile in Tab.
            // The local player is never a useful automatic Stats target.
            if (profileId != null && profileId.equals(hypixelLegitils$selfPlayerId)) continue;
            String uuid = profileId == null || profileId.version() == 1 ? null : profileId.toString();
            StatsBridgeRosterMember member = new StatsBridgeRosterMember(profile.getName(), uuid);
            if (!member.isValid()) continue;
            String key = profile.getName().toLowerCase(java.util.Locale.ROOT);
            members.put(key, member);
            teamFormattedNames.put(key, hypixelLegitils$teamFormattedName(profileId, profile.getName()));
        }
        if (!members.isEmpty()) {
            HypixelLegitilsBootstrap.traceStats(source + " collected players=" + members.size());
            HypixelLegitilsBootstrap.requestStatsRoster(
                matchId,
                gameMode,
                new ArrayList<StatsBridgeRosterMember>(members.values()),
                teamFormattedNames
            );
        } else HypixelLegitilsBootstrap.traceStats(source + " had no valid visible players");
    }

    private String hypixelLegitils$teamFormattedName(UUID playerId, String fallbackName) {
        NetHandlerPlayClient handler = ((Minecraft) (Object) this).getNetHandler();
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
        if (info != null) {
            String scoreboardTeamName = ScorePlayerTeam.formatPlayerName(info.getPlayerTeam(), fallbackName);
            if (!fallbackName.equals(scoreboardTeamName)) {
                return FlagMessage.teamFormattedName(scoreboardTeamName, fallbackName);
            }
            if (info.getDisplayName() != null) {
                return FlagMessage.teamFormattedName(info.getDisplayName().getFormattedText(), fallbackName);
            }
        }
        EntityPlayer player = hypixelLegitils$visiblePlayer(playerId);
        if (player != null && player.getDisplayName() != null) {
            return FlagMessage.teamFormattedName(player.getDisplayName().getFormattedText(), fallbackName);
        }
        return fallbackName;
    }

    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At("HEAD"))
    private void hypixelLegitils$beforeWorldLoad(CallbackInfo callbackInfo) {
        hypixelLegitils$lastObservationAtMillis = 0L;
        hypixelLegitils$observedWorld = null;
        hypixelLegitils$lastObservedWorldTick = -1L;
        hypixelLegitils$previousVisiblePositions.clear();
        HypixelLegitilsBootstrap.onWorldLoading();
    }

    @SuppressWarnings("unchecked")
    private List<PlayerSample> hypixelLegitils$visiblePlayerSamples(long nowMillis, long worldTick) {
        List<EntityPlayer> players = theWorld.playerEntities;
        List<hypixelLegitils$PlayerFrame> frames = new ArrayList<hypixelLegitils$PlayerFrame>(players.size());
        Map<UUID, hypixelLegitils$VisiblePosition> currentPositions = new HashMap<UUID, hypixelLegitils$VisiblePosition>();
        for (EntityPlayer player : players) {
            UUID playerId = hypixelLegitils$profileId(player);
            if (player == null || playerId == null
                || player == thePlayer && !HypixelLegitilsBootstrap.shouldObserveLocalPlayerForDevelopment()) continue;
            if (player == thePlayer) HypixelLegitilsBootstrap.onDeveloperSelfPlayerObserved(playerId);
            HypixelLegitilsBootstrap.onObservedPlayerIdentity(
                playerId,
                player.getName(),
                hypixelLegitils$teamFormattedName(playerId, player.getName()),
                BedwarsPreGameState.isActive(theWorld)
            );
            hypixelLegitils$VisiblePosition previous = hypixelLegitils$previousVisiblePositions.get(playerId);
            boolean movementKnown = previous != null && previous.worldTick == worldTick - 1L;
            double movement = movementKnown
                ? hypixelLegitils$distance(player.posX, player.posY, player.posZ, previous.x, previous.y, previous.z)
                : 0.0D;
            frames.add(new hypixelLegitils$PlayerFrame(player, playerId, movementKnown, movement));
            currentPositions.put(playerId, new hypixelLegitils$VisiblePosition(worldTick, player.posX, player.posY, player.posZ));
        }

        List<PlayerSample> samples = new ArrayList<PlayerSample>(frames.size());
        for (hypixelLegitils$PlayerFrame frame : frames) {
            EntityPlayer player = frame.player;
            ItemStack heldItem = player.getHeldItem();
            PotionEffect speed = player.getActivePotionEffect(Potion.moveSpeed);
            hypixelLegitils$SupportState support = hypixelLegitils$supportState(player);
            hypixelLegitils$NearbyMovement nearby = hypixelLegitils$nearbyMovement(frame, frames);
            samples.add(new PlayerSample(
                frame.playerId,
                nowMillis,
                worldTick,
                player.posX,
                player.posY,
                player.posZ,
                player.isBlocking(),
                player.isSwingInProgress,
                player.swingProgressInt == 1,
                player.isSprinting(),
                player.isUsingItem(),
                player.isSneaking(),
                heldItem != null && heldItem.getItem() instanceof ItemBlock,
                player.onGround,
                player.isRiding(),
                speed == null ? -1 : speed.getAmplifier(),
                player.rotationPitch,
                hypixelLegitils$hasNearbyHurtAnimation(player, players),
                hypixelLegitils$isConsumable(heldItem),
                player.swingProgressInt > 0,
                true,
                nearby.median,
                nearby.count,
                support.complete,
                support.present,
                player.isInWater() || player.isInLava(),
                player.isOnLadder()
            ));
        }
        hypixelLegitils$previousVisiblePositions.clear();
        hypixelLegitils$previousVisiblePositions.putAll(currentPositions);
        return samples;
    }

    private UUID hypixelLegitils$profileId(EntityPlayer player) {
        return player instanceof PlayerIdentityAccess
            ? ((PlayerIdentityAccess) player).hypixelLegitils$getProfileId()
            : null;
    }

    /**
     * Lunar's renderer reads this cache directly instead of the vanilla String
     * label. Updating it after every relevant invalidation keeps the local
     * suffix in the same Adventure component and avoids renderer-Mixin races.
     */
    private void hypixelLegitils$refreshLunarNametagCaches() {
        if (hypixelLegitils$lunarNametagCacheUnavailable || theWorld == null) return;
        try {
            Field cacheField = EntityLivingBase.class.getDeclaredField("lunar$displayNameCache");
            cacheField.setAccessible(true);
            Method displayNameComponent = EntityLivingBase.class.getMethod("bridge$getDisplayNameComponent");
            Set<UUID> observed = new HashSet<UUID>();
            for (EntityPlayer player : theWorld.playerEntities) {
                UUID playerId = hypixelLegitils$profileId(player);
                if (player == null || playerId == null) continue;
                observed.add(playerId);
                String suffix = HypixelLegitilsBootstrap.playerNametagSuffix(player.getName(), playerId);
                hypixelLegitils$LunarNametagCache previous = hypixelLegitils$lunarNametagCaches.get(playerId);
                Object cached = cacheField.get(player);
                if (suffix.isEmpty()) {
                    if (previous != null && cached == previous.component) cacheField.set(player, null);
                    hypixelLegitils$lunarNametagCaches.remove(playerId);
                    continue;
                }
                if (previous != null && suffix.equals(previous.suffix) && cached == previous.component) continue;
                cacheField.set(player, null);
                Object base = displayNameComponent.invoke(player);
                Object updated = HypixelLegitilsBootstrap.appendLunarNametagComponentSuffix(base, player.getName(), playerId);
                if (updated == base) {
                    cacheField.set(player, null);
                    continue;
                }
                cacheField.set(player, updated);
                hypixelLegitils$lunarNametagCaches.put(playerId, new hypixelLegitils$LunarNametagCache(suffix, updated));
                HypixelLegitilsBootstrap.onMarkerRenderHookObserved("name-tag-cache");
            }
            hypixelLegitils$lunarNametagCaches.keySet().retainAll(observed);
        } catch (ReflectiveOperationException e) {
            hypixelLegitils$lunarNametagCacheUnavailable = true;
            HypixelLegitilsBootstrap.traceStats("name-tag Lunar cache unavailable");
        }
    }

    private hypixelLegitils$NearbyMovement hypixelLegitils$nearbyMovement(
        hypixelLegitils$PlayerFrame candidate,
        List<hypixelLegitils$PlayerFrame> frames
    ) {
        List<Double> movement = new ArrayList<Double>();
        for (hypixelLegitils$PlayerFrame other : frames) {
            if (other == candidate || !other.movementKnown) continue;
            if (hypixelLegitils$distance(candidate.player.posX, candidate.player.posY, candidate.player.posZ,
                other.player.posX, other.player.posY, other.player.posZ) <= 12.0D) {
                movement.add(Double.valueOf(other.movement));
            }
        }
        if (movement.isEmpty()) return new hypixelLegitils$NearbyMovement(0.0D, 0);
        Collections.sort(movement);
        int middle = movement.size() / 2;
        double median = movement.size() % 2 == 0
            ? (movement.get(middle - 1).doubleValue() + movement.get(middle).doubleValue()) / 2.0D
            : movement.get(middle).doubleValue();
        return new hypixelLegitils$NearbyMovement(median, movement.size());
    }

    /** Unknown block loading or any non-air block below the feet suppresses AirStall. */
    private hypixelLegitils$SupportState hypixelLegitils$supportState(EntityPlayer player) {
        if (theWorld == null || player.getEntityBoundingBox() == null) return hypixelLegitils$SupportState.unknown();
        AxisAlignedBB bounds = player.getEntityBoundingBox();
        int minX = MathHelper.floor_double(bounds.minX + 0.001D);
        int maxX = MathHelper.floor_double(bounds.maxX - 0.001D);
        int minZ = MathHelper.floor_double(bounds.minZ + 0.001D);
        int maxZ = MathHelper.floor_double(bounds.maxZ - 0.001D);
        if (maxX < minX) maxX = minX;
        if (maxZ < minZ) maxZ = minZ;
        int blockY = MathHelper.floor_double(bounds.minY - 0.05D);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos position = new BlockPos(x, blockY, z);
                if (!theWorld.isBlockLoaded(position)) return hypixelLegitils$SupportState.unknown();
                IBlockState state = theWorld.getBlockState(position);
                Block block = state == null ? null : state.getBlock();
                if (block == null) return hypixelLegitils$SupportState.unknown();
                if (block.getMaterial() != Material.air) return hypixelLegitils$SupportState.present();
            }
        }
        return hypixelLegitils$SupportState.absent();
    }

    private static double hypixelLegitils$distance(double x, double y, double z, double otherX, double otherY, double otherZ) {
        double dx = x - otherX;
        double dy = y - otherY;
        double dz = z - otherZ;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private boolean hypixelLegitils$hasNearbyHurtAnimation(EntityPlayer candidate, List<EntityPlayer> players) {
        for (EntityPlayer other : players) {
            if (other == null || other == candidate || other.hurtTime <= 0) continue;
            double x = candidate.posX - other.posX;
            double y = candidate.posY - other.posY;
            double z = candidate.posZ - other.posZ;
            if (x * x + y * y + z * z <= 20.25D) return true;
        }
        return false;
    }

    private boolean hypixelLegitils$isConsumable(ItemStack heldItem) {
        return heldItem != null && (
            heldItem.getItem() instanceof ItemFood
                || heldItem.getItem() instanceof ItemPotion
                || heldItem.getItem() instanceof ItemBucketMilk
        );
    }

    private static final class hypixelLegitils$VisiblePosition {
        private final long worldTick;
        private final double x;
        private final double y;
        private final double z;

        private hypixelLegitils$VisiblePosition(long worldTick, double x, double y, double z) {
            this.worldTick = worldTick;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static final class hypixelLegitils$LunarNametagCache {
        private final String suffix;
        private final Object component;

        private hypixelLegitils$LunarNametagCache(String suffix, Object component) {
            this.suffix = suffix;
            this.component = component;
        }
    }

    private static final class hypixelLegitils$PlayerFrame {
        private final EntityPlayer player;
        private final UUID playerId;
        private final boolean movementKnown;
        private final double movement;

        private hypixelLegitils$PlayerFrame(EntityPlayer player, UUID playerId, boolean movementKnown, double movement) {
            this.player = player;
            this.playerId = playerId;
            this.movementKnown = movementKnown;
            this.movement = movement;
        }
    }

    private static final class hypixelLegitils$NearbyMovement {
        private final double median;
        private final int count;

        private hypixelLegitils$NearbyMovement(double median, int count) {
            this.median = median;
            this.count = count;
        }
    }

    private static final class hypixelLegitils$SupportState {
        private final boolean complete;
        private final boolean present;

        private hypixelLegitils$SupportState(boolean complete, boolean present) {
            this.complete = complete;
            this.present = present;
        }

        private static hypixelLegitils$SupportState unknown() {
            return new hypixelLegitils$SupportState(false, false);
        }

        private static hypixelLegitils$SupportState present() {
            return new hypixelLegitils$SupportState(true, true);
        }

        private static hypixelLegitils$SupportState absent() {
            return new hypixelLegitils$SupportState(true, false);
        }
    }
}
