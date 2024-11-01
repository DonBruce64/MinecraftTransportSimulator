package mcinterface1182.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Minecraft;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    /**
     * Need this to force dev multiplayer.
     * MC otherwise voids this due to access token crap.
     */
    @Inject(method = "allowsMultiplayer()Z", at = @At(value = "HEAD"), cancellable = true)
    private void inject_ivAllowsMultiplayer(CallbackInfoReturnable<Boolean> ci) {
        ci.setReturnValue(true);
    }
}
