package mcinterface1211.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mcinterface1211.InterfaceEventsEntityRendering;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Invoker("setPosition")
    public abstract void invoke_setPosition(double pX, double pY, double pZ);

    /**
     * In MC 1.21, Camera.setup() calls setPosition() AFTER the ComputeCameraAngles event,
     * overwriting the custom position MTS set during the event. We re-apply MTS's camera
     * position after setup() completes to fix the camera flying away from vehicles.
     */
    @Inject(method = "setup", at = @At("TAIL"))
    private void inject_ivCameraSetupTail(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        if (InterfaceEventsEntityRendering.adjustedCamera) {
            invoke_setPosition(
                InterfaceEventsEntityRendering.cameraAdjustedPosition.x,
                InterfaceEventsEntityRendering.cameraAdjustedPosition.y,
                InterfaceEventsEntityRendering.cameraAdjustedPosition.z
            );
        }
    }
}
