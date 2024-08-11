package mcinterface1165.mixin.client;

import mcinterface1165.InterfaceEventsEntityRendering;
import minecrafttransportsimulator.baseclasses.Point3D;
import net.minecraft.client.sound.SoundListener;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import org.lwjgl.openal.AL10;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundListener.class)
public abstract class SoundListenerMixin {
    @Unique
    private final Point3D immersiveVehicles$forwards = new Point3D();
    @Unique
    private final Point3D immersiveVehicles$up = new Point3D();

    /**
     * Need this to adjust rotation in roll for listener properties if we are riding a vehicle since MC doesn't support this.
     */
    @Inject(method = "setPosition", at = @At(value = "HEAD"), cancellable = true)
    public void inject_setListenerPosition(Vec3d position, CallbackInfo ci) {
        if (InterfaceEventsEntityRendering.adjustedCamera) {
            AL10.alListener3f(AL10.AL_POSITION, (float) InterfaceEventsEntityRendering.cameraAdjustedPosition.x, (float) InterfaceEventsEntityRendering.cameraAdjustedPosition.y, (float) InterfaceEventsEntityRendering.cameraAdjustedPosition.z);
            ci.cancel();
        }
    }

    /**
     * Need this to adjust rotation in roll for listener properties if we are riding a vehicle since MC doesn't support this.
     */
    @Inject(method = "setOrientation", at = @At(value = "HEAD"), cancellable = true)
    public void inject_setListenerOrientation(Vec3f at, Vec3f up, CallbackInfo ci) {
        if (InterfaceEventsEntityRendering.adjustedCamera) {
            immersiveVehicles$forwards.set(0, 0, 1).rotate(InterfaceEventsEntityRendering.cameraAdjustedOrientation);
            immersiveVehicles$up.set(0, 1, 0).rotate(InterfaceEventsEntityRendering.cameraAdjustedOrientation);
            AL10.alListenerfv(AL10.AL_ORIENTATION, new float[]{(float) immersiveVehicles$forwards.x, (float) immersiveVehicles$forwards.y, (float) immersiveVehicles$forwards.z, (float) immersiveVehicles$up.x, (float) immersiveVehicles$up.y, (float) immersiveVehicles$up.z});
            ci.cancel();
        }
    }
}
