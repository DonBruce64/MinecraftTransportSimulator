package mcinterface1192.mixin.client;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import mcinterface1192.TooltipRenderHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

@Mixin(Screen.class)
public abstract class ScreenTooltipMixin {
    @Inject(
        method = "renderTooltipInternal",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(Lcom/mojang/blaze3d/vertex/BufferBuilder$RenderedBuffer;)V",
            shift = At.Shift.AFTER
        )
    )
    private void renderCustomTooltipLayers(PoseStack poseStack, List<ClientTooltipComponent> components, int mouseX, int mouseY, CallbackInfo ci) {
        TooltipRenderHandler.renderPending(poseStack);
    }
}
