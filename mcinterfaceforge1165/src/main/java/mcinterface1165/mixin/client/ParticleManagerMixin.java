package mcinterface1165.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mcinterface1165.BuilderParticle;
import net.minecraft.client.particle.ParticleManager;

/** Keeps pack particles inside the vanilla translucent sheet and its shader pass. */
@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
    @Inject(method = "renderParticles", at = @At("HEAD"), remap = false)
    private void inject_beginParticlePass(CallbackInfo ci) {
        BuilderParticle.beginRendering();
    }

    @Inject(method = "renderParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/IParticleRenderType;end(Lnet/minecraft/client/renderer/Tessellator;)V", shift = At.Shift.AFTER, remap = true), remap = false)
    private void inject_finishParticleSheet(CallbackInfo ci) {
        BuilderParticle.finishRendering();
    }
}
