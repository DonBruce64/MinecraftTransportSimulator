package mcinterface1192.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mcinterface1192.InterfaceEventsEntityRendering;
import minecrafttransportsimulator.jsondefs.JSONPlayerAnimation.PlayerLimb;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @Inject(method = "setModelProperties", at = @At("TAIL"))
    private void inject_setModelProperties(AbstractClientPlayer player, CallbackInfo ci) {
        if (InterfaceEventsEntityRendering.renderingFirstPersonBody) {
            PlayerRenderer renderer = (PlayerRenderer) (Object) this;
            renderer.getModel().head.visible = false;
            renderer.getModel().hat.visible = false;
            renderer.getModel().body.visible = false;
            renderer.getModel().jacket.visible = false;
            renderer.getModel().leftArm.visible = InterfaceEventsEntityRendering.shouldRenderFirstPersonLimb(PlayerLimb.armLeft);
            renderer.getModel().leftSleeve.visible = renderer.getModel().leftArm.visible;
            renderer.getModel().rightArm.visible = InterfaceEventsEntityRendering.shouldRenderFirstPersonLimb(PlayerLimb.armRight);
            renderer.getModel().rightSleeve.visible = renderer.getModel().rightArm.visible;
            renderer.getModel().leftLeg.visible = InterfaceEventsEntityRendering.shouldRenderFirstPersonLimb(PlayerLimb.legLeft);
            renderer.getModel().leftPants.visible = renderer.getModel().leftLeg.visible;
            renderer.getModel().rightLeg.visible = InterfaceEventsEntityRendering.shouldRenderFirstPersonLimb(PlayerLimb.legRight);
            renderer.getModel().rightPants.visible = renderer.getModel().rightLeg.visible;
        }
    }
}
