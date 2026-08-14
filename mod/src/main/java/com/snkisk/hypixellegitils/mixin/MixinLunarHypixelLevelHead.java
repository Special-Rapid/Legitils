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

/**
 * Lunar's Hypixel module composes a separate level-head line after it has
 * generated the intentionally random level for a UUID-v1 Nick profile. This
 * optional, version-specific hook replaces only that generated text.
 */
@Pseudo
@Mixin(targets = "com.moonsworth.lunar.client.OOIHHOORHCOCHOOHHIHIHICOOOROII.IIHHORIHHIHCOCOIHIOICHRHOHROHC.HOHROIHRORCRHRRRCOCIHHIIHOHOCI.CIICOOHIOOHIHOCRRHRHOIIRIOIIOO", remap = false)
public abstract class MixinLunarHypixelLevelHead {
    @ModifyVariable(
        method = "IROIRHHRRCOCCHOOCOHHORCHHCHOCO(Lcom/moonsworth/lunar/client/ROHIRIOHCIROCRROIRHCIHOCIRORIR/IROIRHHRRCOCCHOOCOHHORCHHCHOCO/ROHIRIOHCIROCRROIRHCIHOCIRORIR/HROORHRRIOHOCROCRRORHORHRIOCCO;)V",
        at = @At("STORE"),
        ordinal = 0,
        remap = false,
        require = 0
    )
    private String hypixelLegitils$replaceNickedLevel(String original, @Coerce Object event) {
        UUID playerId = hypixelLegitils$playerId(event);
        HypixelLegitilsBootstrap.onLunarLevelHeadRendered(playerId);
        return HypixelLegitilsBootstrap.lunarNickedLevelText(original, playerId);
    }

    /** The third String STORE is Lunar's localized `Level`/`BedWars Level`/`SkyWars Level` prefix. */
    @ModifyVariable(
        method = "IROIRHHRRCOCCHOOCOHHORCHHCHOCO(Lcom/moonsworth/lunar/client/ROHIRIOHCIROCRROIRHCIHOCIRORIR/IROIRHHRRCOCCHOOCOHHORCHHCHOCO/ROHIRIOHCIROCRROIRHCIHOCIRORIR/HROORHRRIOHOCROCRRORHORHRIOCCO;)V",
        at = @At("STORE"),
        ordinal = 2,
        remap = false,
        require = 0
    )
    private String hypixelLegitils$hideLevelSourcePrefix(String original) {
        return "";
    }

    @ModifyArg(
        method = "IROIRHHRRCOCCHOOCOHHORCHHCHOCO(Lcom/moonsworth/lunar/client/ROHIRIOHCIROCRROIRHCIHOCIRORIR/IROIRHHRRCOCCHOOCOHHORCHHCHOCO/ROHIRIOHCIROCRROIRHCIHOCIRORIR/HROORHRRIOHOCROCRRORHORHRIOCCO;)V",
        at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"),
        index = 0,
        remap = false,
        require = 0
    )
    private Object hypixelLegitils$appendLevelHeadMarkersAbove(Object original, @Coerce Object event) {
        return hypixelLegitils$appendLevelHeadMarkers(original, event);
    }

    @ModifyArg(
        method = "IROIRHHRRCOCCHOOCOHHORCHHCHOCO(Lcom/moonsworth/lunar/client/ROHIRIOHCIROCRROIRHCIHOCIRORIR/IROIRHHRRCOCCHOOCOHHORCHHCHOCO/ROHIRIOHCIROCRROIRHCIHOCIRORIR/HROORHRRIOHOCROCRRORHORHRIOCCO;)V",
        at = @At(value = "INVOKE", target = "Ljava/util/List;add(ILjava/lang/Object;)V"),
        index = 1,
        remap = false,
        require = 0
    )
    private Object hypixelLegitils$appendLevelHeadMarkersBelow(Object original, @Coerce Object event) {
        return hypixelLegitils$appendLevelHeadMarkers(original, event);
    }

    private Object hypixelLegitils$appendLevelHeadMarkers(Object original, Object event) {
        Object entity = hypixelLegitils$player(event);
        UUID playerId = hypixelLegitils$entityPlayerId(entity);
        HypixelLegitilsBootstrap.onLunarLevelHeadRendered(playerId);
        return HypixelLegitilsBootstrap.appendLunarLevelHeadComponentSuffix(
            original,
            hypixelLegitils$playerName(entity),
            playerId
        );
    }

    /** Uses Lunar's event bridge reflectively so the normal Forge build carries no Lunar classes. */
    private UUID hypixelLegitils$playerId(Object event) {
        return hypixelLegitils$entityPlayerId(hypixelLegitils$player(event));
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
