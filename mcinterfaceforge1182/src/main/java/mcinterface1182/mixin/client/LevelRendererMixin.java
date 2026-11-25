package mcinterface1182.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;

import mcinterface1182.InterfaceRender;
import mcinterface1182.WrapperWorld;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.rendering.dynamiclight.DynamicLightSystem;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow
    private RenderBuffers renderBuffers;
    @Shadow
    private Frustum cullingFrustum;

    /**
     * Need this to grab variables used in the level rendering routine for the rendering routine.
     */
    @Inject(method = "renderLevel", at = @At(value = "HEAD"))
    public void inject_renderLevelDataGetter(PoseStack pMatrixStack, float pPartialTicks, long pFinishTimeNano, boolean pDrawBlockOutline, Camera pCamera, GameRenderer pGameRenderer, LightTexture pLightmap, Matrix4f pProjection, CallbackInfo ci) {
        InterfaceRender.projectionMatrix = pProjection;
        InterfaceRender.startDynamicLightFrame();
    }

    /**
     * Render translucent vehicle effects before native translucent terrain and particles.
     * Rendering them at the end of the frame makes lights overlay foreground particles.
     */
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;translucent()Lnet/minecraft/client/renderer/RenderType;"))
    public void inject_renderLevelBlended(PoseStack pMatrixStack, float pPartialTicks, long pFinishTimeNano, boolean pDrawBlockOutline, Camera pCamera, GameRenderer pGameRenderer, LightTexture pLightmap, Matrix4f pProjection, CallbackInfo ci) {
        //Fabulous copies depth without leaving the main framebuffer bound.
        //Other pipelines, including shader packs, keep their own framebuffer active here.
        if (Minecraft.useShaderTransparency()) {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        }
        MultiBufferSource.BufferSource irendertypebuffer$impl = renderBuffers.bufferSource();
        //Set camera offset point for later.
        Vec3 position = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        InterfaceRender.renderCameraOffset.set(position.x, position.y, position.z);
        InterfaceRender.matrixStack = pMatrixStack;
        InterfaceRender.renderBuffer = irendertypebuffer$impl;
        if (ConfigSystem.settings.general.forceRenderLastSolid.value) {
            InterfaceRender.doRenderCall(false, pPartialTicks);
        }
        //Dynamic lights go over the finished world and IV's solid pass, under IV's blended pass.
        InterfaceRender.renderDynamicLightFrame(pMatrixStack.last().pose(), pProjection, cullingFrustum, pCamera, pPartialTicks);
        InterfaceRender.doRenderCall(true, pPartialTicks);
        //Need to end batch after drawing translucents, otherwise they'll get other matrices applied.
        irendertypebuffer$impl.endBatch();
    }

    /**
     * Raises the block light chunk meshes bake by the dynamic lights while a shader pack is active
     * ({@link DynamicLightSystem#blockLightAt} is 0 otherwise).
     */
    @Inject(method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I", at = @At("RETURN"), cancellable = true)
    private static void inject_getLightColor(BlockAndTintGetter world, BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        int raised = DynamicLightSystem.blockLightAt(pos.getX(), pos.getY(), pos.getZ());
        if (raised > 0) {
            int packed = cir.getReturnValueI();
            if (raised > LightTexture.block(packed)) {
                cir.setReturnValue(LightTexture.pack(raised, LightTexture.sky(packed)));
            }
        }
    }

    /**
     * This changes the heightmap of the rain checker to block rain from vehicles.
     * Better than trying to do block placement which has a host of issues.
     */
    @Redirect(method = "renderSnowAndRain", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I"))
    public int redirect_renderSnowAndRain(Level world, Heightmap.Types pHeightmapType, int pX, int pZ) {
        Point3D position = new Point3D(pX + 0.5, world.getHeight(Heightmap.Types.MOTION_BLOCKING, pX, pZ), pZ + 0.5);
        WrapperWorld.getWrapperFor(world).adjustHeightForRain(position);
        return (int) Math.ceil(position.y);
    }
}
