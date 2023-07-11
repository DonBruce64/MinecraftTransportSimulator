package mcinterface1182.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import mcinterface1182.InterfaceRender;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class WorldRendererMixin {
    @Shadow
    private RenderBuffers renderBuffers;

    /**
     * Need this to force our own rendering in the world.  We could create a fake entity, but that causes issue with
     * various systems and spawning and other mods.  Just easier to grab the stack and forward at the right time.
     */
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/.client/renderer/RenderBuffers;bufferSource()Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;"))
    public void inject_renderLevelSolid(PoseStack pMatrixStack, float pPartialTicks, long pFinishTimeNano, boolean pDrawBlockOutline, Camera pActiveRenderInfo, GameRenderer pGameRenderer, LightTexture pLightmap, Matrix4f pProjection, CallbackInfo ci) {
        MultiBufferSource.BufferSource MultiBufferSource$impl = renderBuffers.bufferSource();
        InterfaceRender.doRenderCall(pMatrixStack, MultiBufferSource$impl, false, pPartialTicks);
    }

    @Inject(method = "renderLevel", at = @At(value = "TAIL"))
    public void inject_renderLevelBlended(PoseStack pMatrixStack, float pPartialTicks, long pFinishTimeNano, boolean pDrawBlockOutline, Camera pActiveRenderInfo, GameRenderer pGameRenderer, LightTexture pLightmap, Matrix4f pProjection, CallbackInfo ci) {
        MultiBufferSource.BufferSource MultiBufferSource$impl = renderBuffers.bufferSource();
        InterfaceRender.doRenderCall(pMatrixStack, MultiBufferSource$impl, true, pPartialTicks);
    }
}
