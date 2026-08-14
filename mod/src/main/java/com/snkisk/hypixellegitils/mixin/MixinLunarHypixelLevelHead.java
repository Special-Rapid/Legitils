package com.snkisk.hypixellegitils.mixin;

import com.snkisk.hypixellegitils.HypixelLegitilsBootstrap;
import java.lang.reflect.Method;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
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
        HypixelLegitilsBootstrap.onLunarNickedLevelHeadRendered(playerId);
        return HypixelLegitilsBootstrap.lunarNickedLevelText(original, playerId);
    }

    /** Uses Lunar's event bridge reflectively so the normal Forge build carries no Lunar classes. */
    private UUID hypixelLegitils$playerId(Object event) {
        if (event == null) return null;
        try {
            Method entityMethod = event.getClass().getMethod("ORCROOOIRHOIOOOOCOHORHIRHOCRIO");
            Object entity = entityMethod.invoke(event);
            if (entity == null) return null;
            Object playerId = entity.getClass().getMethod("bridge$getUniqueID").invoke(entity);
            return playerId instanceof UUID ? (UUID) playerId : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
