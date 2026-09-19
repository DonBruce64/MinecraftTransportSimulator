package mcinterface262.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mcinterface262.InterfaceEventsEntityRendering;
import net.minecraft.client.Camera;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Invoker("setPosition")
    public abstract void invoke_setPosition(double pX, double pY, double pZ);

    /**
     * In 26.2, Camera.alignWithEntity() fires the ComputeCameraAngles event and then calls
     * setPosition(), overwriting the custom position MTS set during the event. We re-apply
     * MTS's camera position after the method completes to fix the camera flying away from vehicles.
     */
    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void inject_ivCameraSetupTail(float partialTick, CallbackInfo ci) {
        if (InterfaceEventsEntityRendering.adjustedCamera) {
            invoke_setPosition(
                InterfaceEventsEntityRendering.cameraAdjustedPosition.x,
                InterfaceEventsEntityRendering.cameraAdjustedPosition.y,
                InterfaceEventsEntityRendering.cameraAdjustedPosition.z
            );
        }
    }
}
