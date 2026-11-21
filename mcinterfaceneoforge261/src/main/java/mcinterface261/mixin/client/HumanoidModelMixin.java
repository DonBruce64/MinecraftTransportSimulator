package mcinterface261.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mcinterface261.MtsPlayerTweaksRenderState;
import mcinterface261.MtsPlayerTweaksRenderState.PlayerTweaks;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends HumanoidRenderState> {

    /**
     * Need this method to adjust model legs and arms for sitting players.
     * TODO: MC 26.1 render-state system removed entity access from setupAnim; needs rework via extractRenderState hook.
     */
    @SuppressWarnings("unchecked")
    @Inject(method = "setupAnim", at = @At(value = "TAIL"))
    public void inject_setupAnim(T state, CallbackInfo ci) {
        PlayerTweaks tweaks = MtsPlayerTweaksRenderState.get(state);
        if (tweaks != null) {
            HumanoidModel<T> model = (HumanoidModel<T>) ((Object) this);
            if (tweaks.setRightArm) {
                model.rightArm.xRot = tweaks.rightArmXRot;
                model.rightArm.yRot = tweaks.rightArmYRot;
                model.rightArm.zRot = tweaks.rightArmZRot;
            }
            if (tweaks.setLeftArm) {
                model.leftArm.xRot = tweaks.leftArmXRot;
                model.leftArm.yRot = tweaks.leftArmYRot;
                model.leftArm.zRot = tweaks.leftArmZRot;
            }
            if (tweaks.setRightLeg) {
                model.rightLeg.xRot = tweaks.rightLegXRot;
                model.rightLeg.yRot = tweaks.rightLegYRot;
                model.rightLeg.zRot = tweaks.rightLegZRot;
            }
            if (tweaks.setLeftLeg) {
                model.leftLeg.xRot = tweaks.leftLegXRot;
                model.leftLeg.yRot = tweaks.leftLegYRot;
                model.leftLeg.zRot = tweaks.leftLegZRot;
            }
        }
    }
}
