package mcinterface1201.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mcinterface1201.TooltipRenderHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;

@Mixin(TooltipRenderUtil.class)
public abstract class TooltipRenderUtilMixin {
    @Inject(method = "renderTooltipBackground(Lnet/minecraft/client/gui/GuiGraphics;IIIIIIIII)V", at = @At("HEAD"), remap = false)
    private static void renderCustomBackground(GuiGraphics graphics, int x, int y, int width, int height, int z, int backgroundStart, int backgroundEnd, int borderStart, int borderEnd, CallbackInfo ci) {
        TooltipRenderHandler.renderBackground(graphics, x, y, width, height, z);
    }

    @Inject(method = "renderTooltipBackground(Lnet/minecraft/client/gui/GuiGraphics;IIIIIIIII)V", at = @At("TAIL"), remap = false)
    private static void renderCustomBorder(GuiGraphics graphics, int x, int y, int width, int height, int z, int backgroundStart, int backgroundEnd, int borderStart, int borderEnd, CallbackInfo ci) {
        TooltipRenderHandler.renderBorder(graphics, x, y, width, height, z);
    }
}
