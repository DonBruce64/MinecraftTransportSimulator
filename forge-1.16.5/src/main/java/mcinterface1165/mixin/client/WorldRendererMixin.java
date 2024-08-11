package mcinterface1165.mixin.client;

import mcinterface1165.InterfaceRender;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Shadow
    @Final
    private MinecraftClient client;
    @Shadow
    @Final
    private BufferBuilderStorage bufferBuilders;

    /**
     * Need this to render translucent things at the right time.  MC doesn't properly support this natively.
     * Instead, it tries to render translucent things with the regular things and fouls the depth buffer.
     */
    @Inject(method = "render", at = @At(value = "TAIL"))
    public void fixDepthBuffer(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager arg4, Matrix4f arg5, CallbackInfo ci) {
        VertexConsumerProvider.Immediate vertexConsumers = bufferBuilders.getEntityVertexConsumers();
        //Set camera offset point for later.
        Vec3d position = client.gameRenderer.getCamera().getPos();
        InterfaceRender.renderCameraOffset.set(position.x, position.y, position.z);
        if (ConfigSystem.settings.general.forceRenderLastSolid.value) {
            InterfaceRender.doRenderCall(matrices, vertexConsumers, false, tickDelta);
        }
        InterfaceRender.doRenderCall(matrices, vertexConsumers, true, tickDelta);
    }
}
