package mcinterface1165.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.matrix.MatrixStack;

import mcinterface1165.InterfaceRender;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderTypeBuffers;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.util.math.vector.Matrix4f;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Shadow
    private RenderTypeBuffers renderBuffers;

    /**
     * Need this to force our own rendering in the world.  We could create a fake entity, but that causes issue with
     * various systems and spawning and other mods.  Just easier to grab the stack and forward at the right time.
     */
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderTypeBuffers;bufferSource()Lnet/minecraft/client/renderer/IRenderTypeBuffer$Impl;"))
    public void inject_renderLevelSolid(MatrixStack pMatrixStack, float pPartialTicks, long pFinishTimeNano, boolean pDrawBlockOutline, ActiveRenderInfo pActiveRenderInfo, GameRenderer pGameRenderer, LightTexture pLightmap, Matrix4f pProjection, CallbackInfo ci) {
        IRenderTypeBuffer.Impl irendertypebuffer$impl = renderBuffers.bufferSource();
        InterfaceRender.doRenderCall(pMatrixStack, irendertypebuffer$impl, false, pPartialTicks);
    }

    @Inject(method = "renderLevel", at = @At(value = "TAIL"))
    public void inject_renderLevelBlended(MatrixStack pMatrixStack, float pPartialTicks, long pFinishTimeNano, boolean pDrawBlockOutline, ActiveRenderInfo pActiveRenderInfo, GameRenderer pGameRenderer, LightTexture pLightmap, Matrix4f pProjection, CallbackInfo ci) {
        IRenderTypeBuffer.Impl irendertypebuffer$impl = renderBuffers.bufferSource();
        InterfaceRender.doRenderCall(pMatrixStack, irendertypebuffer$impl, true, pPartialTicks);
    }
}
