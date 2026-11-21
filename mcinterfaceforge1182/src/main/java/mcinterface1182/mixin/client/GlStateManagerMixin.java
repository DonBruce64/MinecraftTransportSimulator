package mcinterface1182.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.platform.GlStateManager;

import mcinterface1182.BrightnessBlending;

@Mixin(GlStateManager.class)
public abstract class GlStateManagerMixin {
    @Unique
    private static Runnable mts$restoreBrightnessBlend;

    @Inject(method = "_drawElements(IIIJ)V", at = @At("HEAD"))
    private static void mts$applyBrightnessBlend(CallbackInfo ci) {
        mts$restoreBrightnessBlend = BrightnessBlending.beginDraw();
    }

    @Inject(method = "_drawElements(IIIJ)V", at = @At("RETURN"))
    private static void mts$restoreBrightnessBlend(CallbackInfo ci) {
        if (mts$restoreBrightnessBlend != null) {
            mts$restoreBrightnessBlend.run();
            mts$restoreBrightnessBlend = null;
        }
    }
}
