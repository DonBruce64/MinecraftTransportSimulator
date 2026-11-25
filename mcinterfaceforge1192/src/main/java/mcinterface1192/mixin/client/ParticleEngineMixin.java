package mcinterface1192.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mcinterface1192.WrapperParticle;
import net.minecraft.client.particle.ParticleEngine;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleRenderType;end(Lcom/mojang/blaze3d/vertex/Tesselator;)V", shift = At.Shift.AFTER))
    private void inject_renderParticles(CallbackInfo ci) {
        //Keep custom textures and models in the shader mod's native translucent particle pass.
        WrapperParticle.finishRender();
    }
}
