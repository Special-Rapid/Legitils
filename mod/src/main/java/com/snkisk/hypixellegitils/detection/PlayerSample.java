package com.snkisk.hypixellegitils.detection;

import java.util.UUID;

/** Immutable, client-visible player state copied by the Mixin adapter. */
public final class PlayerSample {
    public final UUID playerId;
    public final long observedAtMillis;
    /** Client-visible world tick captured after the world tick completes. */
    public final long worldTick;
    public final double x;
    public final double y;
    public final double z;
    public final boolean blocking;
    public final boolean swinging;
    /** True only when the visible swing progress begins on this world tick. */
    public final boolean swingStartedThisTick;
    public final boolean sprinting;
    public final boolean usingItem;
    public final boolean sneaking;
    public final boolean holdingBlock;
    public final boolean onGround;
    public final boolean riding;
    /** -1 means no visible speed effect. */
    public final int speedPotionAmplifier;
    public final float pitch;
    /** Nearby hurt animation is only ambiguous local combat context. */
    public final boolean combatContext;
    /** The held item is a food, normal potion, or milk bucket. */
    public final boolean holdingConsumable;
    /** Exact reference-equivalent observation: {@code swingProgressInt > 0}. */
    public final boolean attackAnimationActive;
    /** False means the adapter could not establish a complete visible sample. */
    public final boolean reliable;
    /** Median one-tick movement of nearby visible comparison players, or zero when unavailable. */
    public final double nearbyMovementMedian;
    /** Number of nearby visible players contributing to {@link #nearbyMovementMedian}. */
    public final int nearbyMovementCount;
    /** Median one-tick movement of every other loaded visible player in this world, or zero when unavailable. */
    public final double worldMovementMedian;
    /** Number of loaded visible players contributing to {@link #worldMovementMedian}. */
    public final int worldMovementCount;
    /** True only when the adapter fully loaded the conservative support probe. */
    public final boolean supportStateComplete;
    /** A loaded non-air block could support the visible player. */
    public final boolean supportPresent;
    /** The visible player is in water or lava. */
    public final boolean inLiquid;
    /** The visible player is on a climbable block. */
    public final boolean onClimbable;

    public PlayerSample(
        UUID playerId,
        long observedAtMillis,
        long worldTick,
        double x,
        double y,
        double z,
        boolean blocking,
        boolean swinging,
        boolean swingStartedThisTick,
        boolean sprinting,
        boolean usingItem,
        boolean sneaking,
        boolean holdingBlock,
        boolean onGround,
        boolean riding,
        int speedPotionAmplifier,
        float pitch,
        boolean combatContext,
        boolean holdingConsumable,
        boolean attackAnimationActive,
        boolean reliable
    ) {
        this(
            playerId,
            observedAtMillis,
            worldTick,
            x,
            y,
            z,
            blocking,
            swinging,
            swingStartedThisTick,
            sprinting,
            usingItem,
            sneaking,
            holdingBlock,
            onGround,
            riding,
            speedPotionAmplifier,
            pitch,
            combatContext,
            holdingConsumable,
            attackAnimationActive,
            reliable,
            0.0D,
            0,
            0.0D,
            0,
            false,
            false,
            false,
            false
        );
    }

    /**
     * Full Phase 4 contract.  The adapter must use explicit unavailable
     * values rather than infer nearby movement or world support state.
     */
    public PlayerSample(
        UUID playerId,
        long observedAtMillis,
        long worldTick,
        double x,
        double y,
        double z,
        boolean blocking,
        boolean swinging,
        boolean swingStartedThisTick,
        boolean sprinting,
        boolean usingItem,
        boolean sneaking,
        boolean holdingBlock,
        boolean onGround,
        boolean riding,
        int speedPotionAmplifier,
        float pitch,
        boolean combatContext,
        boolean holdingConsumable,
        boolean attackAnimationActive,
        boolean reliable,
        double nearbyMovementMedian,
        int nearbyMovementCount,
        double worldMovementMedian,
        int worldMovementCount,
        boolean supportStateComplete,
        boolean supportPresent,
        boolean inLiquid,
        boolean onClimbable
    ) {
        if (playerId == null || observedAtMillis < 0L || worldTick < 0L || !finite(x) || !finite(y) || !finite(z)
            || !finite(nearbyMovementMedian) || nearbyMovementMedian < 0.0D || nearbyMovementCount < 0
            || !finite(worldMovementMedian) || worldMovementMedian < 0.0D || worldMovementCount < 0
            || Float.isNaN(pitch) || Float.isInfinite(pitch)) {
            throw new IllegalArgumentException("Player sample requires finite visible state");
        }
        this.playerId = playerId;
        this.observedAtMillis = observedAtMillis;
        this.worldTick = worldTick;
        this.x = x;
        this.y = y;
        this.z = z;
        this.blocking = blocking;
        this.swinging = swinging;
        this.swingStartedThisTick = swingStartedThisTick;
        this.sprinting = sprinting;
        this.usingItem = usingItem;
        this.sneaking = sneaking;
        this.holdingBlock = holdingBlock;
        this.onGround = onGround;
        this.riding = riding;
        this.speedPotionAmplifier = speedPotionAmplifier;
        this.pitch = pitch;
        this.combatContext = combatContext;
        this.holdingConsumable = holdingConsumable;
        this.attackAnimationActive = attackAnimationActive;
        this.reliable = reliable;
        this.nearbyMovementMedian = nearbyMovementMedian;
        this.nearbyMovementCount = nearbyMovementCount;
        this.worldMovementMedian = worldMovementMedian;
        this.worldMovementCount = worldMovementCount;
        this.supportStateComplete = supportStateComplete;
        this.supportPresent = supportPresent;
        this.inLiquid = inLiquid;
        this.onClimbable = onClimbable;
    }

    /**
     * Compatibility overload for existing pure-Java tests that do not need
     * exact tick/swing-start semantics. Runtime adapters must use the full
     * constructor above.
     */
    public PlayerSample(
        UUID playerId,
        long observedAtMillis,
        double x,
        double y,
        double z,
        boolean blocking,
        boolean swinging,
        boolean sprinting,
        boolean usingItem,
        boolean sneaking,
        boolean holdingBlock,
        boolean onGround,
        boolean riding,
        int speedPotionAmplifier,
        float pitch,
        boolean combatContext,
        boolean reliable
    ) {
        this(
            playerId,
            observedAtMillis,
            observedAtMillis / 50L,
            x,
            y,
            z,
            blocking,
            swinging,
            swinging,
            sprinting,
            usingItem,
            sneaking,
            holdingBlock,
            onGround,
            riding,
            speedPotionAmplifier,
            pitch,
            combatContext,
            false,
            swinging,
            reliable
        );
    }

    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}
