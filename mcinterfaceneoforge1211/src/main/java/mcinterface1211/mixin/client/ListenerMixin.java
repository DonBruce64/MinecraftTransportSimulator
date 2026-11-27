package mcinterface1211.mixin.client;

import org.lwjgl.openal.AL10;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.audio.Listener;
import com.mojang.blaze3d.audio.ListenerTransform;

import mcinterface1211.InterfaceEventsEntityRendering;
import minecrafttransportsimulator.baseclasses.Point3D;

@Mixin(Listener.class)
public abstract class ListenerMixin {
    /**
     * Need this to adjust position and orientation for listener properties if we are riding a vehicle since MC doesn't support roll.
     * In 1.21, setListenerPosition and setListenerOrientation were merged into setTransform(ListenerTransform).
     */
    @Inject(method = "setTransform", at = @At(value = "HEAD"), cancellable = true)
    public void inject_setTransform(ListenerTransform pTransform, CallbackInfo ci) {
        float[] transform = InterfaceEventsEntityRendering.listenerTransform;
        if (transform != null) {
            AL10.alListener3f(AL10.AL_POSITION, transform[0], transform[1], transform[2]);
            AL10.alListenerfv(AL10.AL_ORIENTATION, new float[] { transform[3], transform[4], transform[5], transform[6], transform[7], transform[8] });
            ci.cancel();
        }
    }
}
