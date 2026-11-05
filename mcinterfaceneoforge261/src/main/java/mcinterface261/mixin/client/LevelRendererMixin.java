package mcinterface261.mixin.client;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import mcinterface261.InterfaceRender;
import mcinterface261.WrapperWorld;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.phys.Vec3;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow
    private RenderBuffers renderBuffers;

    /**
     * Need this to grab variables used in the level rendering routine for the rendering routine.
     * In 26.1, renderLevel signature changed: Camera->CameraRenderState, GameRenderer/LightTexture
     * removed, Matrix4f->Matrix4fc for modelView, projection matrix removed from params (now on camera),
     * and ChunkSectionsToRender added.
     */
    @Inject(method = "renderLevel", at = @At(value = "HEAD"))
    public void inject_renderLevelDataGetter(GraphicsResourceAllocator pAllocator, DeltaTracker pDeltaTracker, boolean pDrawBlockOutline, CameraRenderState pCamera, Matrix4fc pModelViewMatrix, GpuBufferSlice pGpuBuffer, Vector4f pFogParams, boolean pSomeBool, ChunkSectionsToRender pChunkSections, CallbackInfo ci) {
        //Projection matrix is no longer passed into renderLevel; obtain from the main camera.
        //Camera#getViewRotationProjectionMatrix replaces GameRenderer#getProjectionMatrix in 26.1.
        InterfaceRender.projectionMatrix = Minecraft.getInstance().gameRenderer.getMainCamera().getViewRotationProjectionMatrix(new Matrix4f());
        InterfaceRender.viewMatrix = new Matrix4f(pModelViewMatrix);
    }

    /**
     * Need this to render translucent things at the right time.  MC doesn't properly support this natively.
     * Instead, it tries to render translucent things with the regular things and fouls the depth buffer.
     */
    @Inject(method = "renderLevel", at = @At(value = "TAIL"))
    public void inject_renderLevelBlended(GraphicsResourceAllocator pAllocator, DeltaTracker pDeltaTracker, boolean pDrawBlockOutline, CameraRenderState pCamera, Matrix4fc pModelViewMatrix, GpuBufferSlice pGpuBuffer, Vector4f pFogParams, boolean pSomeBool, ChunkSectionsToRender pChunkSections, CallbackInfo ci) {
        float pPartialTicks = pDeltaTracker.getGameTimeDeltaPartialTick(false);
        MultiBufferSource.BufferSource irendertypebuffer$impl = renderBuffers.bufferSource();
        //Camera position is now read from the main Camera object.
        Vec3 position = Minecraft.getInstance().gameRenderer.getMainCamera().position();
        InterfaceRender.renderCameraOffset.set(position.x, position.y, position.z);
        InterfaceRender.matrixStack = new PoseStack();
        InterfaceRender.renderBuffer = irendertypebuffer$impl;

        //In 26.1, the model-view stack is managed differently; mul/push/pop still work but applyModelViewMatrix is gone.
        org.joml.Matrix4fStack matrix4fstack = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
        matrix4fstack.pushMatrix();
        matrix4fstack.mul(pModelViewMatrix);

        if (ConfigSystem.settings.general.forceRenderLastSolid.value) {
            InterfaceRender.doRenderCall(false, pPartialTicks);
        }
        FogRenderer fogRenderer = ((GameRendererAccessor) Minecraft.getInstance().gameRenderer).mts$getFogRenderer();
        RenderSystem.setShaderFog(fogRenderer.getBuffer(FogRenderer.FogMode.NONE));
        InterfaceRender.doRenderCall(true, pPartialTicks);
        //Need to end batch after drawing translucents, otherwise they'll get other matrices applied.
        irendertypebuffer$impl.endBatch();
        RenderSystem.setShaderFog(pGpuBuffer);

        //Pop the view matrix we pushed.
        matrix4fstack.popMatrix();
    }
}

