package com.snkisk.hypixellegitils.mixin;

import com.snkisk.hypixellegitils.HypixelLegitilsBootstrap;
import com.snkisk.hypixellegitils.mixin.accessor.PlayerIdentityAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Handles only a manually submitted chat line; clickable RUN_COMMAND input never reaches this hook. */
@Mixin(GuiChat.class)
public abstract class MixinGuiChat {
    @Redirect(
        method = "keyTyped",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiChat;sendChatMessage(Ljava/lang/String;)V"
        )
    )
    private void hypixelLegitils$handleManualSubmit(GuiChat chat, String message) {
        String[] responses = HypixelLegitilsBootstrap.localCommandResponses(message, true, hypixelLegitils$visiblePlayers());
        if (responses == null) {
            chat.sendChatMessage(message);
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null && minecraft.thePlayer != null) {
            for (String response : responses) {
                if (response != null && !response.isEmpty()) {
                    minecraft.thePlayer.addChatMessage(new ChatComponentText(response));
                }
            }
        }
    }

    /** Supplies a local UUID when available; Bootstrap otherwise performs the user-approved Mojang lookup. */
    private Map<String, UUID> hypixelLegitils$visiblePlayers() {
        Map<String, UUID> players = new HashMap<String, UUID>();
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.theWorld == null) return players;
        for (Object entity : minecraft.theWorld.playerEntities) {
            if (!(entity instanceof EntityPlayer)) continue;
            EntityPlayer player = (EntityPlayer) entity;
            UUID playerId = player instanceof PlayerIdentityAccess
                ? ((PlayerIdentityAccess) player).hypixelLegitils$getProfileId()
                : null;
            if (player.getName() == null || playerId == null) continue;
            players.put(player.getName().toLowerCase(Locale.ROOT), playerId);
        }
        return players;
    }
}
