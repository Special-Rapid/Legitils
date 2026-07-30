package com.snkisk.hypixellegitils.mixin;

import com.snkisk.hypixellegitils.HypixelLegitilsBootstrap;
import com.snkisk.hypixellegitils.detection.BedNukeSignalCheck;
import com.snkisk.hypixellegitils.detection.NoBreakDelaySignalCheck;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Read-only adapter for server-applied local world state. */
@Mixin(WorldClient.class)
public abstract class MixinWorldClient {
    private static final int HORIZONTAL_RADIUS = 3;
    private static final int VERTICAL_RADIUS = 2;
    private BlockPos hypixelLegitils$pendingOtherBedHalf;

    @Inject(method = "invalidateRegionAndSetBlock", at = @At("HEAD"))
    private void hypixelLegitils$beforeServerBlockState(
        BlockPos position,
        IBlockState nextState,
        CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        hypixelLegitils$pendingOtherBedHalf = null;
        IBlockState currentState = hypixelLegitils$world().getBlockState(position);
        if (currentState.getBlock() != Blocks.bed || nextState.getBlock() == Blocks.bed) return;
        BlockPos otherHalf = hypixelLegitils$otherBedHalf(position, currentState);
        if (otherHalf == null) return;
        BedNukeSignalCheck.BedStructure structure = hypixelLegitils$snapshot(position, otherHalf);
        if (structure == null) return;
        HypixelLegitilsBootstrap.onBedStructure(structure, System.currentTimeMillis());
        hypixelLegitils$pendingOtherBedHalf = otherHalf;
    }

    @Inject(method = "invalidateRegionAndSetBlock", at = @At("RETURN"))
    private void hypixelLegitils$afterServerBlockState(
        BlockPos position,
        IBlockState nextState,
        CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        long nowMillis = System.currentTimeMillis();
        HypixelLegitilsBootstrap.onBedBlockState(hypixelLegitils$position(position), hypixelLegitils$kind(hypixelLegitils$world().getBlockState(position)), nowMillis);
        if (hypixelLegitils$pendingOtherBedHalf != null) {
            HypixelLegitilsBootstrap.onBedBlockState(
                hypixelLegitils$position(hypixelLegitils$pendingOtherBedHalf),
                hypixelLegitils$kind(hypixelLegitils$world().getBlockState(hypixelLegitils$pendingOtherBedHalf)),
                nowMillis
            );
            hypixelLegitils$pendingOtherBedHalf = null;
        }
        IBlockState appliedState = hypixelLegitils$world().getBlockState(position);
        if (appliedState != null && appliedState.getBlock().getMaterial() == net.minecraft.block.material.Material.air) {
            HypixelLegitilsBootstrap.onNoBreakDelayBlockRemoval(
                new NoBreakDelaySignalCheck.BlockPosition(position.getX(), position.getY(), position.getZ()),
                hypixelLegitils$world().getTotalWorldTime(),
                hypixelLegitils$world().isBlockLoaded(position)
            );
        }
    }

    /** Any chunk replacement invalidates a previously complete local cuboid. */
    @Inject(method = "doPreChunk", at = @At("HEAD"))
    private void hypixelLegitils$beforeChunkTransition(int chunkX, int chunkZ, boolean load, CallbackInfo callbackInfo) {
        HypixelLegitilsBootstrap.onChunkTransition();
    }

    /**
     * S21 chunk data also reaches this method when it updates an existing
     * chunk, in which case doPreChunk is not called. A one-block region is the
     * ordinary S22/S23 path used by invalidateRegionAndSetBlock and remains
     * tracked; any larger region makes the cuboid history incomplete.
     */
    @Inject(method = "invalidateBlockReceiveRegion", at = @At("HEAD"))
    private void hypixelLegitils$beforeReceivedRegion(
        int minimumX,
        int minimumY,
        int minimumZ,
        int maximumX,
        int maximumY,
        int maximumZ,
        CallbackInfo callbackInfo
    ) {
        if (minimumX != maximumX || minimumY != maximumY || minimumZ != maximumZ) {
            HypixelLegitilsBootstrap.onChunkTransition();
        }
    }

    private BlockPos hypixelLegitils$otherBedHalf(BlockPos position, IBlockState state) {
        if (!(state.getBlock() instanceof BlockBed)) return null;
        EnumFacing facing = (EnumFacing) state.getValue(BlockBed.FACING);
        BlockBed.EnumPartType part = (BlockBed.EnumPartType) state.getValue(BlockBed.PART);
        BlockPos other = part == BlockBed.EnumPartType.HEAD ? position.offset(facing.getOpposite()) : position.offset(facing);
        return hypixelLegitils$world().getBlockState(other).getBlock() == Blocks.bed ? other : null;
    }

    private BedNukeSignalCheck.BedStructure hypixelLegitils$snapshot(BlockPos first, BlockPos second) {
        int minimumX = Math.min(first.getX(), second.getX()) - HORIZONTAL_RADIUS;
        int maximumX = Math.max(first.getX(), second.getX()) + HORIZONTAL_RADIUS;
        int minimumY = Math.max(0, Math.min(first.getY(), second.getY()) - VERTICAL_RADIUS);
        int maximumY = Math.min(255, Math.max(first.getY(), second.getY()) + VERTICAL_RADIUS);
        int minimumZ = Math.min(first.getZ(), second.getZ()) - HORIZONTAL_RADIUS;
        int maximumZ = Math.max(first.getZ(), second.getZ()) + HORIZONTAL_RADIUS;
        Map<BedNukeSignalCheck.BlockPosition, BedNukeSignalCheck.BlockKind> states = new HashMap<BedNukeSignalCheck.BlockPosition, BedNukeSignalCheck.BlockKind>();
        for (int x = minimumX; x <= maximumX; x++) {
            for (int y = minimumY; y <= maximumY; y++) {
                for (int z = minimumZ; z <= maximumZ; z++) {
                    BlockPos position = new BlockPos(x, y, z);
                    if (!hypixelLegitils$world().isBlockLoaded(position)) return null;
                    states.put(hypixelLegitils$position(position), hypixelLegitils$kind(hypixelLegitils$world().getBlockState(position)));
                }
            }
        }
        try {
            return new BedNukeSignalCheck.BedStructure(
                hypixelLegitils$position(first),
                hypixelLegitils$position(second),
                new BedNukeSignalCheck.BlockPosition(minimumX, minimumY, minimumZ),
                new BedNukeSignalCheck.BlockPosition(maximumX, maximumY, maximumZ),
                states
            );
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static BedNukeSignalCheck.BlockPosition hypixelLegitils$position(BlockPos position) {
        return new BedNukeSignalCheck.BlockPosition(position.getX(), position.getY(), position.getZ());
    }

    private World hypixelLegitils$world() {
        return (World) (Object) this;
    }

    private static BedNukeSignalCheck.BlockKind hypixelLegitils$kind(IBlockState state) {
        if (state.getBlock() == Blocks.bed) return BedNukeSignalCheck.BlockKind.BED;
        // A partial collision shape (slab, fence, stairs, door, etc.) is not
        // enough to establish a sealed voxel. Treat it as open so ambiguity
        // suppresses this advisory detector instead of creating an alert.
        return state.getBlock().getMaterial().blocksMovement() && state.getBlock().isFullCube()
            ? BedNukeSignalCheck.BlockKind.SOLID
            : BedNukeSignalCheck.BlockKind.OPEN;
    }
}
