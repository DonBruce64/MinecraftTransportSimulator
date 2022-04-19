package minecrafttransportsimulator.core.mixin;

import net.minecraftforge.fml.client.SplashProgress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SplashProgress.class)
public class SplashMixin {

    // Cancel forge stuff so they don't override the fancy splash screen.
    @Inject(at = @At(value = "HEAD"), method = "Lnet/minecraftforge/fml/client/SplashProgress;start()V", cancellable = true, remap = false)
    private static void start(CallbackInfo info) {
        info.cancel();
    }

}
