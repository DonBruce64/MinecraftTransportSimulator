package mcinterface261.mixin.client;

import mcinterface261.InterfaceEventsEntityRendering;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Authoritatively writes MTS camera position and rotation into {@link CameraRenderState}
 * at the TAIL of {@code Camera.extractRenderState}.
 *
 * <p>In MC 26.1 the renderer does <em>not</em> read from the live {@code Camera} object;
 * it reads from the {@code CameraRenderState} snapshot captured by
 * {@code GameRenderer.extract()} → {@code mainCamera.extractRenderState(cameraState, ...)}.
 * Injecting here — after the snapshot is populated — is the only place where an override
 * is guaranteed to survive into actual rendering, regardless of how many times
 * {@code alignWithEntity} re-applies vanilla setRotation/setPosition/move.</p>
 *
 */
@Mixin(Camera.class)
public abstract class CameraExtractStateMixin {

    @Inject(
        method = "extractRenderState(Lnet/minecraft/client/renderer/state/level/CameraRenderState;F)V",
        at = @At("TAIL")
    )
    private void mts_overrideCameraRenderState(CameraRenderState cameraState, float cameraEntityPartialTicks, CallbackInfo ci) {
        if (!InterfaceEventsEntityRendering.adjustedCamera) {
            return;
        }
        InterfaceEventsEntityRendering.adjustedCamera = false;
        //For 3P: mts_overrideBeforeZoom already set the pivot, vanilla zoom applied on top.
        //The live Camera.position() already has the correct final position; extractRenderState
        //copied it into cameraState.pos above — do NOT override it here.
        //For 1P: mts_overrideTail set the rider eye position into live camera; also already copied.
        //In both cases cameraState.pos is already correct — nothing to do.
    }
}
