package mcinterface1165.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraftforge/client/ForgeHooksClient;onCameraSetup(Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/ActiveRenderInfo;F)Lnet/minecraftforge/client/event/EntityViewRenderEvent$CameraSetup;"), method = "renderLevel")
    private void renderLevelInject(float partialTicks, long finishTime, MatrixStack matrixStack, CallbackInfo ci) {
        System.out.println("I HAS CODE");
    }
}
