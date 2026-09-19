package mcinterface261.mixin.client;

import mcinterface261.InterfaceEventsEntityRendering;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Invoker("setPosition")
    public abstract void invoke_setPosition(double x, double y, double z);

    /**
     * Override camera position at the TAIL of alignWithEntity — after vanilla setPosition+zoom.
     * Applies for both 1P and 3P, completely replacing vanilla position:
     *   1P: cameraAdjustedPosition = exact MTS rider eye (riderCameraPosition, no zoom).
     *   3P: cameraAdjustedPosition = riderEyePos + mts_zoom_vector (incl. PageUp/Down zoomLevel),
     *       computed from riderCameraPosition in the event handler.
     * Vanilla zoom is discarded — MTS zoom is the sole authority.
     */
    @Inject(method = "alignWithEntity(F)V", at = @At("TAIL"))
    private void mts_overridePosition(float partialTicks, CallbackInfo ci) {
        if (!InterfaceEventsEntityRendering.adjustedCamera) return;
        invoke_setPosition(
            InterfaceEventsEntityRendering.cameraAdjustedPosition.x,
            InterfaceEventsEntityRendering.cameraAdjustedPosition.y,
            InterfaceEventsEntityRendering.cameraAdjustedPosition.z
        );
    }
}
