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
    private final Point3D forwards = new Point3D();
    private final Point3D up = new Point3D();

    /**
     * Need this to adjust position and orientation for listener properties if we are riding a vehicle since MC doesn't support roll.
     * In 1.21, setListenerPosition and setListenerOrientation were merged into setTransform(ListenerTransform).
     */
    @Inject(method = "setTransform", at = @At(value = "HEAD"), cancellable = true)
    public void inject_setTransform(ListenerTransform pTransform, CallbackInfo ci) {
        if (InterfaceEventsEntityRendering.adjustedCamera) {
            AL10.alListener3f(AL10.AL_POSITION, (float) InterfaceEventsEntityRendering.cameraAdjustedPosition.x, (float) InterfaceEventsEntityRendering.cameraAdjustedPosition.y, (float) InterfaceEventsEntityRendering.cameraAdjustedPosition.z);
            forwards.set(0, 0, 1).rotate(InterfaceEventsEntityRendering.cameraAdjustedOrientation);
            up.set(0, 1, 0).rotate(InterfaceEventsEntityRendering.cameraAdjustedOrientation);
            AL10.alListenerfv(AL10.AL_ORIENTATION, new float[] { (float) forwards.x, (float) forwards.y, (float) forwards.z, (float) up.x, (float) up.y, (float) up.z });
            ci.cancel();
        }
    }
}
