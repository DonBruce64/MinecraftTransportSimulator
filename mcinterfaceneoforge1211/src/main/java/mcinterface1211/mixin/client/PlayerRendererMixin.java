package mcinterface1211.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mcinterface1211.InterfaceEventsEntityRendering;
import minecrafttransportsimulator.jsondefs.JSONPlayerAnimation.PlayerLimb;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    /** Keeps the first-person camera out of the local player's head. */
    @SuppressWarnings("unchecked")
    @Inject(method = "setModelProperties", at = @At("TAIL"))
    private void inject_setModelProperties(AbstractClientPlayer player, CallbackInfo ci) {
        if (InterfaceEventsEntityRendering.renderingFirstPersonBody) {
            PlayerModel<AbstractClientPlayer> model = ((PlayerRenderer) (Object) this).getModel();
            model.head.visible = false;
            model.hat.visible = false;
            model.body.visible = false;
            model.jacket.visible = false;
            model.leftArm.visible = InterfaceEventsEntityRendering.shouldRenderFirstPersonLimb(PlayerLimb.armLeft);
            model.leftSleeve.visible = model.leftArm.visible;
            model.rightArm.visible = InterfaceEventsEntityRendering.shouldRenderFirstPersonLimb(PlayerLimb.armRight);
            model.rightSleeve.visible = model.rightArm.visible;
            model.leftLeg.visible = InterfaceEventsEntityRendering.shouldRenderFirstPersonLimb(PlayerLimb.legLeft);
            model.leftPants.visible = model.leftLeg.visible;
            model.rightLeg.visible = InterfaceEventsEntityRendering.shouldRenderFirstPersonLimb(PlayerLimb.legRight);
            model.rightPants.visible = model.rightLeg.visible;
        }
    }
}
