package mcinterface261.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;

@Mixin(VillagerModel.class)
public abstract class VillagerModelMixin {
    @Shadow
    private ModelPart rightLeg;
    @Shadow
    private ModelPart leftLeg;

    /**
     * Need this method to adjust model legs and arms for sitting players.
     * TODO: MC 26.1 render-state system removed entity access from setupAnim; needs rework via extractRenderState hook.
     */
    @Inject(method = "setupAnim", at = @At(value = "TAIL"))
    public void inject_setupAnim(VillagerRenderState state, CallbackInfo ci) {
        //Villager sitting pose disabled pending render-state migration.
    }
}
