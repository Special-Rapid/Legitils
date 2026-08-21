package com.snkisk.hypixellegitils.mixin;

import com.snkisk.hypixellegitils.HypixelLegitilsBootstrap;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Lunar's current Hypixel Level Head renderer. The target is read from the
 * active Lunar bake, not guessed from a past obfuscated client build.
 *
 * <p>Keep normal players' selected level value, hide only its source label,
 * and append Legitils' local markers to this Level Head component. Operating
 * here preserves that layout when another agent wraps ordinary name tags.</p>
 */
@Pseudo
@Mixin(
    targets = "com.moonsworth.lunar.client.IHORCIOIRCIRROHHIIIHIHCIIHHOIC.HCIHIIHCIRHHCOIICOCHICHOCHHCOC.HCHIOHCRICRHRIIIRCRCHHCOHIIHHI.OIRHROROOOCCHCIIIHCRIOCRCIHICI",
    remap = false
)
public abstract class MixinLunarHypixelLevelHead {
    private static final String RENDER_METHOD =
        "ROOORHOIRCHIOOCRCHIOICIOHRRIRC("
        + "Lcom/moonsworth/lunar/client/HORICHIIIHCRCCCROHOCCOCOOHRCCC/"
        + "ROOORHOIRCHIOOCRCHIOICIOHRRIRC/HORICHIIIHCRCCCROHOCCOCOOHRCCC/"
        + "HCCRCOIOCOIRRCHCCOIIIRIROCCIRI;)V";
    private static final ThreadLocal<LevelHeadPlayer> hypixelLegitils$levelHeadPlayer = new ThreadLocal<LevelHeadPlayer>();

    /** Capture a Nick UUID while Lunar creates its intentionally fictional level. */
    @Redirect(
        method = RENDER_METHOD,
        at = @At(
            value = "INVOKE",
            target = "Lcom/moonsworth/lunar/client/IHORCIOIRCIRROHHIIIHIHCIIHHOIC/"
                + "HCIHIIHCIRHHCOIICOCHICHOCHHCOC/HCHIOHCRICRHRIIIRCRCHHCOHIIHHI/"
                + "HIOOIHRORRRCRRIIOOOOOICRICHIHR;generateRandomLevelForNicked(Ljava/util/UUID;)I"
        ),
        remap = false,
        require = 0
    )
    private int hypixelLegitils$captureNickedLevel(@Coerce Object source, UUID playerId) {
        hypixelLegitils$rememberPlayer(playerId);
        try {
            Method generator = source.getClass().getMethod("generateRandomLevelForNicked", UUID.class);
            Object result = generator.invoke(source, playerId);
            return result instanceof Integer ? ((Integer) result).intValue() : 0;
        } catch (ReflectiveOperationException ignored) {
            // Preserve a visible Lunar value if this private API changes.
            return 0;
        }
    }

    /** Replaces just Lunar's generated Nick level before it enters its cache. */
    @ModifyArg(
        method = RENDER_METHOD,
        at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", ordinal = 0),
        index = 1,
        remap = false,
        require = 0
    )
    private Object hypixelLegitils$replaceNickedLevel(Object original) {
        LevelHeadPlayer player = hypixelLegitils$levelHeadPlayer.get();
        if (!(original instanceof String) || player == null) return original;
        return HypixelLegitilsBootstrap.lunarNickedLevelText((String) original, player.id);
    }

    /** Capture the UUID immediately before Lunar builds the visible component. */
    @Redirect(
        method = RENDER_METHOD,
        at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"),
        remap = false,
        require = 0
    )
    private Object hypixelLegitils$captureRenderedLevelHeadPlayer(Map<Object, Object> values, Object key) {
        if (key instanceof UUID) hypixelLegitils$rememberPlayer((UUID) key);
        return values.get(key);
    }

    /** Hides only Level/BedWars Level/SkyWars Level; Lunar's numeric value remains. */
    @Redirect(
        method = RENDER_METHOD,
        at = @At(
            value = "INVOKE",
            target = "Lcom/moonsworth/lunar/client/IHORCIOIRCIRROHHIIIHIHCIIHHOIC/"
                + "HCIHIIHCIRHHCOIICOCHICHOCHHCOC/HCHIOHCRICRHRIIIRCRCHHCOHIIHHI/"
                + "HIOOIHRORRRCRRIIOOOOOICRICHIHR;getNametagPrefix()Ljava/lang/String;"
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
            // Keep an unrecognised Lunar label visible rather than deleting it.
            return "Level";
        }
    }

    @ModifyArg(
        method = RENDER_METHOD,
        at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"),
        index = 0,
        remap = false,
        require = 0
    )
    private Object hypixelLegitils$appendLevelHeadMarkersAbove(Object original) {
        return hypixelLegitils$appendLevelHeadMarkers(original);
    }

    @ModifyArg(
        method = RENDER_METHOD,
        at = @At(value = "INVOKE", target = "Ljava/util/List;add(ILjava/lang/Object;)V"),
        index = 1,
        remap = false,
        require = 0
    )
    private Object hypixelLegitils$appendLevelHeadMarkersBelow(Object original) {
        return hypixelLegitils$appendLevelHeadMarkers(original);
    }

    private Object hypixelLegitils$appendLevelHeadMarkers(Object original) {
        LevelHeadPlayer player = hypixelLegitils$levelHeadPlayer.get();
        if (player == null) return original;
        try {
            HypixelLegitilsBootstrap.onLunarLevelHeadRendered(player.id);
            return HypixelLegitilsBootstrap.appendLunarLevelHeadComponentSuffix(original, player.name, player.id);
        } finally {
            hypixelLegitils$levelHeadPlayer.remove();
        }
    }

    private static void hypixelLegitils$rememberPlayer(UUID playerId) {
        if (playerId == null) return;
        hypixelLegitils$levelHeadPlayer.set(new LevelHeadPlayer(playerId, hypixelLegitils$playerName(playerId)));
    }

    private static String hypixelLegitils$playerName(UUID playerId) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.getNetHandler() == null) return null;
        NetworkPlayerInfo info = minecraft.getNetHandler().getPlayerInfo(playerId);
        return info == null || info.getGameProfile() == null ? null : info.getGameProfile().getName();
    }

    private static final class LevelHeadPlayer {
        private final UUID id;
        private final String name;

        private LevelHeadPlayer(UUID id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
