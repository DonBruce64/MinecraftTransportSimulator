package mcinterface1165.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mojang.blaze3d.matrix.MatrixStack;

import mcinterface1165.InterfaceEventsEntityRendering;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.Entity;

@Mixin(LivingRenderer.class)
public abstract class LivingRendererMixin {
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/LayerRenderer;render(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ILnet/minecraft/entity/Entity;FFFFFF)V"))
    private void redirect_renderLayer(LayerRenderer layer, MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight, Entity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!InterfaceEventsEntityRendering.renderingFirstPersonBody) {
            layer.render(matrixStack, buffer, packedLight, entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
        }
    }
}
