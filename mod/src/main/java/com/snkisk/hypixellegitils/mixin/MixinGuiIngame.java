package com.snkisk.hypixellegitils.mixin;

import com.snkisk.hypixellegitils.HypixelLegitilsBootstrap;
import com.snkisk.hypixellegitils.alert.AlertPresentation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws a local advisory only while an alert is active. */
@Mixin(GuiIngame.class)
public abstract class MixinGuiIngame {
    @Shadow protected Minecraft mc;
    @Shadow protected int recordPlayingUpFor;
    @Shadow public abstract FontRenderer getFontRenderer();

    @Inject(method = "renderGameOverlay", at = @At("RETURN"))
    private void hypixelLegitils$renderAlert(float partialTicks, CallbackInfo callbackInfo) {
        AlertPresentation presentation = HypixelLegitilsBootstrap.currentPresentation();
        if (presentation == null || !presentation.alert || presentation.actionBarText == null) return;
        // Lunar owns an injection at the vanilla Action Bar draw call. Do not
        // redirect or replace that call: server Action Bar text takes priority.
        if (recordPlayingUpFor > 0) return;
        String text = presentation.actionBarText;
        ScaledResolution resolution = new ScaledResolution(mc);
        FontRenderer font = getFontRenderer();
        int x = (resolution.getScaledWidth() - font.getStringWidth(text)) / 2;
        font.drawStringWithShadow(text, (float) x, (float) (resolution.getScaledHeight() - 68), 0xFFFFFF);
    }
}
