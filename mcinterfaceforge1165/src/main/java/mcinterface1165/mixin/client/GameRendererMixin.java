package mcinterface1165.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.renderer.GameRenderer;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraftforge/client/ForgeHooksClient;onCameraSetup(Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/ActiveRenderInfo;F)Lnet/minecraftforge/client/event/EntityViewRenderEvent$CameraSetup;"), method = "renderLevel")
    private void renderLevelInject(float partialTicks, long finishTime, MatrixStack matrixStack, CallbackInfo ci) {
        //FIXME delete later, keep as reference for now.
        //InterfaceEventsEntityRendering.adjustCamera(matrixStack, partialTicks);
    }
}
