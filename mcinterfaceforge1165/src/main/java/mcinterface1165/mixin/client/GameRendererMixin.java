package mcinterface1165.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.matrix.MatrixStack;

import mcinterface1165.InterfaceRender;
import net.minecraft.client.renderer.GameRenderer;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    /**
     * Draws the dynamic light frame that {@link WorldRendererMixin} saved at the end of the world render, when an Oculus
     * pack is active: this runs right after {@code WorldRenderer.renderLevel} returns, so the pack's composite and final
     * passes (hooked at that method's return) are done and the main target holds the finished image and the scene depth.
     * It is also before the hand clears the depth.  Without a pack nothing is saved and this does nothing.
     */
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/WorldRenderer;renderLevel(Lcom/mojang/blaze3d/matrix/MatrixStack;FJZLnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/util/math/vector/Matrix4f;)V", shift = At.Shift.AFTER))
    private void inject_afterLevelRender(float pPartialTicks, long pFinishTimeNano, MatrixStack pMatrixStack, CallbackInfo ci) {
        InterfaceRender.renderPendingDynamicLightFrame();
    }
}
