package mcinterface1165.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.matrix.MatrixStack;

import mcinterface1165.InterfaceRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderTypeBuffers;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Shadow
    private RenderTypeBuffers renderBuffers;

    /**
     * Need this to render translucent things at the right time.  MC doesn't properly support this natively.
     * Instead, it tries to render translucent things with the regular things and fouls the depth buffer.
     */
    @Inject(method = "renderLevel", at = @At(value = "TAIL"))
    public void inject_renderLevelBlended(MatrixStack pMatrixStack, float pPartialTicks, long pFinishTimeNano, boolean pDrawBlockOutline, ActiveRenderInfo pActiveRenderInfo, GameRenderer pGameRenderer, LightTexture pLightmap, Matrix4f pProjection, CallbackInfo ci) {
        IRenderTypeBuffer.Impl irendertypebuffer$impl = renderBuffers.bufferSource();
        //Set camera offset point for later.
        Vector3d position = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        InterfaceRender.renderCameraOffset.set(position.x, position.y, position.z);
        InterfaceRender.doRenderCall(pMatrixStack, irendertypebuffer$impl, true, pPartialTicks);
    }

    /**
     * This changes the heightmap of the rain checker to block rain from vehicles.
     * Better than trying to do block placement which has a host of issues.
     */
    @Redirect(method = "renderSnowAndRain", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/IWorldReader;getHeightmapPos(Lnet/minecraft/world/gen/Heightmap$Type;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/math/BlockPos;"))
    public BlockPos inject_renderSnowAndRain(World world, Heightmap.Type pHeightmapType, BlockPos pPos) {
        BlockPos pos = world.getHeightmapPos(Heightmap.Type.MOTION_BLOCKING, pPos);
        return pos;
    }
}
