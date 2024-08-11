package mcinterface1165.mixin.client;

import mcinterface1165.WrapperEntity;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.instances.PartSeat;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.CompositeEntityModel;
import net.minecraft.client.render.entity.model.ModelWithHat;
import net.minecraft.client.render.entity.model.ModelWithHead;
import net.minecraft.client.render.entity.model.VillagerResemblingModel;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerResemblingModel.class)
public abstract class VillagerResemblingModelMixin<T extends Entity> extends CompositeEntityModel<T> implements ModelWithHead, ModelWithHat {
    @Final
    @Shadow
    protected ModelPart rightLeg;
    @Final
    @Shadow
    protected ModelPart leftLeg;

    /**
     * Need this method to adjust model legs and arms for sitting players.
     */
    @Inject(method = "setAngles", at = @At(value = "TAIL"))
    public void adjustModelForSitting(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        WrapperEntity entityWrapper = WrapperEntity.getWrapperFor(entity);
        AEntityB_Existing ridingEntity = entityWrapper.getEntityRiding();

        //This may be null if MC sets this entity as riding before the actual entity has time to load NBT.
        if (ridingEntity != null) {
            //If we render standing, don't do anything since villagers normally stand.
            //If we are sitting however, make them sit.
            if (!(ridingEntity instanceof PartSeat && ((PartSeat) ridingEntity).definition.seat.standing)) {
                rightLeg.pitch = (float) Math.toRadians(-90);
                rightLeg.yaw = 0;
                rightLeg.roll = 0;
                leftLeg.pitch = (float) Math.toRadians(-90);
                leftLeg.yaw = 0;
                leftLeg.roll = 0;
            }
        }
    }
}
