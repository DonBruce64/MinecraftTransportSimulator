package mcinterface1192.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;

import mcinterface1192.InterfaceRender;
import mcinterface1192.WrapperWorld;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow
    private RenderBuffers renderBuffers;

    /**
     * Need this to grab variables used in the level rendering routine for the rendering routine.
     */
    @Inject(method = "renderLevel", at = @At(value = "HEAD"))
    public void inject_renderLevelDataGetter(PoseStack pMatrixStack, float pPartialTicks, long pFinishTimeNano, boolean pDrawBlockOutline, Camera pCamera, GameRenderer pGameRenderer, LightTexture pLightmap, Matrix4f pProjection, CallbackInfo ci) {
        InterfaceRender.projectionMatrix = pProjection;
    }

    /**
     * Need this to render translucent things at the right time.  MC doesn't properly support this natively.
     * Instead, it tries to render translucent things with the regular things and fouls the depth buffer.
     */
    @Inject(method = "renderLevel", at = @At(value = "TAIL"))
    public void inject_renderLevelBlended(PoseStack pMatrixStack, float pPartialTicks, long pFinishTimeNano, boolean pDrawBlockOutline, Camera pCamera, GameRenderer pGameRenderer, LightTexture pLightmap, Matrix4f pProjection, CallbackInfo ci) {
        MultiBufferSource.BufferSource irendertypebuffer$impl = renderBuffers.bufferSource();
        //Set camera offset point for later.
        Vec3 position = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        InterfaceRender.renderCameraOffset.set(position.x, position.y, position.z);
        InterfaceRender.matrixStack = pMatrixStack;
        InterfaceRender.renderBuffer = irendertypebuffer$impl;
        if (ConfigSystem.settings.general.forceRenderLastSolid.value) {
            InterfaceRender.doRenderCall(false, pPartialTicks);
        }
        InterfaceRender.doRenderCall(true, pPartialTicks);
        //Need to end batch after drawing translucents, otherwise they'll get other matrices applied.
        irendertypebuffer$impl.endBatch();
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
