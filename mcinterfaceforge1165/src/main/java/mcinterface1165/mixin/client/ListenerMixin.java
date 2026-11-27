package mcinterface1165.mixin.client;

import org.lwjgl.openal.AL10;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mcinterface1165.InterfaceEventsEntityRendering;
import minecrafttransportsimulator.baseclasses.Point3D;
import net.minecraft.client.audio.Listener;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;

@Mixin(Listener.class)
public abstract class ListenerMixin {
    /**
     * Need this to adjust rotation in roll for listener properties if we are riding a vehicle since MC doesn't support this.
     */
    @Inject(method = "setListenerPosition", at = @At(value = "HEAD"), cancellable = true)
    public void inject_setListenerPosition(Vector3d pPosition, CallbackInfo ci) {
        float [] transform = InterfaceEventsEntityRendering.listenerTransform;
        if (transform != null) {
            AL10.alListener3f(AL10.AL_POSITION, transform[0], transform[1], transform[2]);
            ci.cancel();
        }
    }

    /**
     * Need this to adjust rotation in roll for listener properties if we are riding a vehicle since MC doesn't support this.
     */
    @Inject(method = "setListenerOrientation", at = @At(value = "HEAD"), cancellable = true)
    public void inject_setListenerOrientation(Vector3f pClientViewVector, Vector3f pViewVectorRaised, CallbackInfo ci) {
        float [] transform = InterfaceEventsEntityRendering.listenerTransform;
        if (transform != null) {
            AL10.alListenerfv(AL10.AL_ORIENTATION, new float[] { transform[3], transform[4], transform[5], transform[6], transform[7], transform[8] });
            ci.cancel();
        }
    }
}
