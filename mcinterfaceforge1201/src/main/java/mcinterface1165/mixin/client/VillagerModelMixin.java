package mcinterface1165.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mcinterface1165.WrapperEntity;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.instances.PartSeat;
import net.minecraft.client.renderer.entity.model.VillagerModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.Entity;

@Mixin(VillagerModel.class)
public abstract class VillagerModelMixin<T extends Entity> {
    @Shadow
    private ModelRenderer leg0;
    @Shadow
    private ModelRenderer leg1;

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
                leg0.xRot = (float) Math.toRadians(-90);
                leg0.yRot = 0;
                leg0.zRot = 0;
                leg1.xRot = (float) Math.toRadians(-90);
                leg1.yRot = 0;
                leg1.zRot = 0;
            }
        }
    }
}
