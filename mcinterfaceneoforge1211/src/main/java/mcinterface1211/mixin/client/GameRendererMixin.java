package mcinterface1211.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mcinterface1211.InterfaceRender;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    /**
     * Draws the dynamic light frame that {@link LevelRendererMixin} saved at the end of the world render, when an Iris
     * or Oculus pack is active: this runs right after {@code LevelRenderer.renderLevel} returns, so the pack's
     * composite and final passes (hooked at that method's return) are done and the main target holds the finished
     * image and the scene depth.  It is also before the hand clears the depth.  Without a pack nothing is saved and
     * this does nothing.
     */
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V", shift = At.Shift.AFTER))
    private void inject_afterLevelRender(DeltaTracker pDeltaTracker, CallbackInfo ci) {
        InterfaceRender.renderPendingDynamicLightFrame();
    }
}
