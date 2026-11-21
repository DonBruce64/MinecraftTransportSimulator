package mcinterface1211.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mcinterface1211.WrapperParticle;
import net.minecraft.client.particle.ParticleEngine;

/** Flush pack particles before shader mods end or move the native particle pass. */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Inject(method = "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V", at = @At("HEAD"), remap = false)
    private void inject_beginParticlePass(CallbackInfo ci) {
        WrapperParticle.beginRendering();
    }

    @Inject(method = "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;depthMask(Z)V"), remap = false)
    private void inject_finishParticlePass(CallbackInfo ci) {
        WrapperParticle.finishRendering();
    }
}
