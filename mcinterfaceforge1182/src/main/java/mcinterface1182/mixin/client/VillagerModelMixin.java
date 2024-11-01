package mcinterface1182.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mcinterface1182.WrapperEntity;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.instances.PartSeat;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;

@Mixin(VillagerModel.class)
public abstract class VillagerModelMixin<T extends Entity> {
    @Shadow
    private ModelPart rightLeg;
    @Shadow
    private ModelPart leftLeg;

    /**
     * Need this method to adjust model legs and arms for sitting players.
     */
    @Inject(method = "setupAnim", at = @At(value = "TAIL"))
    public void inject_setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        WrapperEntity entityWrapper = WrapperEntity.getWrapperFor(entity);
        AEntityB_Existing ridingEntity = entityWrapper.getEntityRiding();

        //This may be null if MC sets this entity as riding before the actual entity has time to load NBT.
        if (ridingEntity != null) {
            //If we render standing, don't do anything since villagers normally stand.
            //If we are sitting however, make them sit.
            if (!(ridingEntity instanceof PartSeat && ((PartSeat) ridingEntity).definition.seat.standing)) {
                rightLeg.xRot = (float) Math.toRadians(-90);
                rightLeg.yRot = 0;
                rightLeg.zRot = 0;
                leftLeg.xRot = (float) Math.toRadians(-90);
                leftLeg.yRot = 0;
                leftLeg.zRot = 0;
            }
        }
    }
}
