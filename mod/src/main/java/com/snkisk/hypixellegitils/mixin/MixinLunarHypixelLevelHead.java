package com.snkisk.hypixellegitils.mixin;

import com.snkisk.hypixellegitils.HypixelLegitilsBootstrap;
import java.lang.reflect.Method;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Lunar's Hypixel module composes a separate level-head line after it has
 * generated the intentionally random level for a UUID-v1 Nick profile. This
 * optional, version-specific hook replaces only that generated text.
 */
@Pseudo
@Mixin(targets = "com.moonsworth.lunar.client.OOIHHOORHCOCHOOHHIHIHICOOOROII.IIHHORIHHIHCOCOIHIOICHRHOHROHC.HOHROIHRORCRHRRRCOCIHHIIHOHOCI.CIICOOHIOOHIHOCRRHRHOIIRIOIIOO", remap = false)
public abstract class MixinLunarHypixelLevelHead {
    private static final ThreadLocal<LevelHeadPlayer> hypixelLegitils$levelHeadPlayer = new ThreadLocal<LevelHeadPlayer>();

    @ModifyVariable(
        method = "IROIRHHRRCOCCHOOCOHHORCHHCHOCO(Lcom/moonsworth/lunar/client/ROHIRIOHCIROCRROIRHCIHOCIRORIR/IROIRHHRRCOCCHOOCOHHORCHHCHOCO/ROHIRIOHCIROCRROIRHCIHOCIRORIR/HROORHRRIOHOCROCRRORHORHRIOCCO;)V",
        at = @At("STORE"),
        ordinal = 0,
        remap = false,
        require = 0
    )
    private String hypixelLegitils$replaceNickedLevel(String original, @Coerce Object event) {
        Object player = hypixelLegitils$player(event);
        UUID playerId = hypixelLegitils$entityPlayerId(player);
        hypixelLegitils$levelHeadPlayer.set(new LevelHeadPlayer(playerId, hypixelLegitils$playerName(player)));
        HypixelLegitilsBootstrap.onLunarLevelHeadRendered(playerId);
        return HypixelLegitilsBootstrap.lunarNickedLevelText(original, playerId);
    }

    /**
     * The selected Level/BedWars Level/SkyWars Level source is composed by
     * this exact getter in current Lunar builds. Redirecting it avoids relying
     * on unstable String-local ordinals while retaining the numeric component.
     */
    @Redirect(
        method = "IROIRHHRRCOCCHOOCOHHORCHHCHOCO(Lcom/moonsworth/lunar/client/ROHIRIOHCIROCRROIRHCIHOCIRORIR/IROIRHHRRCOCCHOOCOHHORCHHCHOCO/ROHIRIOHCIROCRROIRHCIHOCIRORIR/HROORHRRIOHOCROCRRORHORHRIOCCO;)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/moonsworth/lunar/client/OOIHHOORHCOCHOOHHIHIHICOOOROII/IIHHORIHHIHCOCOIHIOICHRHOHROHC/HOHROIHRORCRHRRRCOCIHHIIHOHOCI/OCICHIOHRIHHCCOIOROHCRICCHHCHI;getNametagPrefix()Ljava/lang/String;"
        ),
        remap = false,
        require = 0
    )
    private String hypixelLegitils$hideLevelSourcePrefix(@Coerce Object source) {
        if (source == null) return "Level";
        try {
            Object prefix = source.getClass().getMethod("getNametagPrefix").invoke(source);
            return HypixelLegitilsBootstrap.lunarLevelHeadPrefixText(prefix instanceof String ? (String) prefix : null);
        } catch (ReflectiveOperationException ignored) {
            // Preserve a visible source label if Lunar changes the private object shape.
            // Never turn an unrecognized label into an empty value.
            return "Level";
        }
    }

    @ModifyArg(
        method = "IROIRHHRRCOCCHOOCOHHORCHHCHOCO(Lcom/moonsworth/lunar/client/ROHIRIOHCIROCRROIRHCIHOCIRORIR/IROIRHHRRCOCCHOOCOHHORCHHCHOCO/ROHIRIOHCIROCRROIRHCIHOCIRORIR/HROORHRRIOHOCROCRRORHORHRIOCCO;)V",
        at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"),
        index = 0,
        remap = false,
        require = 0
    )
    private Object hypixelLegitils$appendLevelHeadMarkersAbove(Object original) {
        return hypixelLegitils$appendLevelHeadMarkers(original, false);
    }

    @ModifyArg(
        method = "IROIRHHRRCOCCHOOCOHHORCHHCHOCO(Lcom/moonsworth/lunar/client/ROHIRIOHCIROCRROIRHCIHOCIRORIR/IROIRHHRRCOCCHOOCOHHORCHHCHOCO/ROHIRIOHCIROCRROIRHCIHOCIRORIR/HROORHRRIOHOCROCRRORHORHRIOCCO;)V",
        at = @At(value = "INVOKE", target = "Ljava/util/List;add(ILjava/lang/Object;)V"),
        index = 1,
        remap = false,
        require = 0
    )
    private Object hypixelLegitils$appendLevelHeadMarkersBelow(Object original) {
        return hypixelLegitils$appendLevelHeadMarkers(original, true);
    }

    /**
     * ModifyArg may receive only the invoked method's arguments. Lunar's current
     * List.add calls have no event argument, so use the context captured by the
     * preceding String STORE rather than declaring an invalid second parameter.
     */
    private Object hypixelLegitils$appendLevelHeadMarkers(Object original, boolean clearAfterUse) {
        LevelHeadPlayer player = hypixelLegitils$levelHeadPlayer.get();
        if (player == null) return original;
        try {
            HypixelLegitilsBootstrap.onLunarLevelHeadRendered(player.id);
            return HypixelLegitilsBootstrap.appendLunarLevelHeadComponentSuffix(original, player.name, player.id);
        } finally {
            if (clearAfterUse) hypixelLegitils$levelHeadPlayer.remove();
        }
    }

    private static final class LevelHeadPlayer {
        private final UUID id;
        private final String name;

        private LevelHeadPlayer(UUID id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private UUID hypixelLegitils$entityPlayerId(Object entity) {
        if (entity == null) return null;
        try {
            Object playerId = entity.getClass().getMethod("bridge$getUniqueID").invoke(entity);
            return playerId instanceof UUID ? (UUID) playerId : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private Object hypixelLegitils$player(Object event) {
        if (event == null) return null;
        try {
            return event.getClass().getMethod("ORCROOOIRHOIOOOOCOHORHIRHOCRIO").invoke(event);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private String hypixelLegitils$playerName(Object entity) {
        if (entity == null) return null;
        try {
            Object name = entity.getClass().getMethod("bridge$getName").invoke(entity);
            return name instanceof String ? (String) name : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
