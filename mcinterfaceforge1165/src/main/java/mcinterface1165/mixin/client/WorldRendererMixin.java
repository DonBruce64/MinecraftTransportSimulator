package mcinterface1165.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.matrix.MatrixStack;

import mcinterface1165.InterfaceRender;
import mcinterface1165.WrapperWorld;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.rendering.dynamiclight.DynamicLightSystem;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderTypeBuffers;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Shadow
    private RenderTypeBuffers renderBuffers;

    /**
     * Need this to grab variables used in the level rendering routine for the rendering routine.
     */
    @Inject(method = "renderLevel", at = @At(value = "HEAD"))
    public void inject_renderLevelDataGetter(MatrixStack pMatrixStack, float pPartialTicks, long pFinishTimeNano, boolean pDrawBlockOutline, ActiveRenderInfo pActiveRenderInfo, GameRenderer pGameRenderer, LightTexture pLightmap, Matrix4f pProjection, CallbackInfo ci) {
        InterfaceRender.projectionMatrix = pProjection;
        InterfaceRender.startDynamicLightFrame();
    }

    /**
     * Need this to render translucent things at the right time.  MC doesn't properly support this natively.
     * Instead, it tries to render translucent things with the regular things and fouls the depth buffer.
     */
    @Inject(method = "renderLevel", at = @At(value = "TAIL"))
    public void inject_renderLevelBlended(MatrixStack pMatrixStack, float pPartialTicks, long pFinishTimeNano, boolean pDrawBlockOutline, ActiveRenderInfo pActiveRenderInfo, GameRenderer pGameRenderer, LightTexture pLightmap, Matrix4f pProjection, CallbackInfo ci) {
        InterfaceRender.projectionMatrix = pProjection;
        IRenderTypeBuffer.Impl irendertypebuffer$impl = renderBuffers.bufferSource();
        //Set camera offset point for later.
        Vector3d position = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        InterfaceRender.renderCameraOffset.set(position.x, position.y, position.z);
        if (ConfigSystem.settings.general.forceRenderLastSolid.value) {
            InterfaceRender.doRenderCall(pMatrixStack, irendertypebuffer$impl, false, pPartialTicks);
        }
        //Dynamic lights go over the finished world and IV's solid pass, under IV's blended pass.
        InterfaceRender.renderDynamicLightFrame(pMatrixStack.last().pose(), pProjection, pActiveRenderInfo, pPartialTicks);
        InterfaceRender.doRenderCall(pMatrixStack, irendertypebuffer$impl, true, pPartialTicks);
    }

    /**
     * Raises the block light chunk meshes bake by the dynamic lights while a shader pack is active
     * ({@link DynamicLightSystem#blockLightAt} is 0 otherwise).
     */
    @Inject(method = "getLightColor(Lnet/minecraft/world/IBlockDisplayReader;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;)I", at = @At("RETURN"), cancellable = true)
    private static void inject_getLightColor(IBlockDisplayReader world, BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
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
    @Redirect(method = "renderSnowAndRain", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getHeightmapPos(Lnet/minecraft/world/gen/Heightmap$Type;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/math/BlockPos;"))
    public BlockPos inject_renderSnowAndRain(World world, Heightmap.Type pHeightmapType, BlockPos pPos) {
        Point3D position = new Point3D(pPos.getX() + 0.5, world.getHeightmapPos(Heightmap.Type.MOTION_BLOCKING, pPos).getY(), pPos.getZ() + 0.5);
        WrapperWorld.getWrapperFor(world).adjustHeightForRain(position);
        return new BlockPos(pPos.getX(), Math.ceil(position.y), pPos.getZ());
    }
}
