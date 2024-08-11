package mcinterface1165.mixin.client;

import mcinterface1165.WrapperEntity;
import mcinterface1165.WrapperPlayer;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedEntityModel.class)
public abstract class BipedEntityModelMixin<T extends LivingEntity> {
    @Unique
    private static final float immersiveVehicles$armYawRad = (float) Math.toRadians(10);

    /**
     * Need this method to adjust model legs and arms for sitting players.
     */
    @SuppressWarnings("unchecked")
    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At(value = "TAIL"))
    public void adjustModelForSitting(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (ConfigSystem.client.renderingSettings.playerTweaks.value) {
            BipedEntityModel<T> model = (BipedEntityModel<T>) ((Object) this);
            WrapperEntity entityWrapper = WrapperEntity.getWrapperFor(entity);
            AEntityB_Existing ridingEntity = entityWrapper.getEntityRiding();

            //This may be null if MC sets this player as riding before the actual entity has time to load NBT.
            if (ridingEntity != null) {
                boolean renderCurrentRiderStanding = false;
                if (ridingEntity instanceof PartSeat) {
                    PartSeat seat = (PartSeat) ridingEntity;
                    renderCurrentRiderStanding = seat.definition.seat.standing;

                    if (seat.vehicleOn != null && seat.placementDefinition.isController) {
                        double turningAngle = seat.vehicleOn.rudderInput / 2D;
                        model.rightArm.pitch = (float) Math.toRadians(-75 + turningAngle);
                        model.rightArm.yaw = -immersiveVehicles$armYawRad;
                        model.rightArm.roll = 0;

                        model.leftArm.pitch = (float) Math.toRadians(-75 - turningAngle);
                        model.leftArm.yaw = immersiveVehicles$armYawRad;
                        model.leftArm.roll = 0;
                    }
                }
                if (renderCurrentRiderStanding) {
                    model.leftLeg.pitch = 0;
                    model.leftLeg.yaw = 0;
                    model.leftLeg.roll = 0;
                    model.rightLeg.pitch = 0;
                } else {
                    model.leftLeg.pitch = (float) Math.toRadians(-90);
                    model.leftLeg.yaw = 0;
                    model.leftLeg.roll = 0;
                    model.rightLeg.pitch = (float) Math.toRadians(-90);
                }
                model.rightLeg.yaw = 0;
                model.rightLeg.roll = 0;
            }

            if (entity instanceof PlayerEntity) {
                EntityPlayerGun gunEntity = EntityPlayerGun.playerClientGuns.get(entity.getUuid());
                if (gunEntity != null && gunEntity.activeGun != null) {
                    //Get arm rotations.
                    Point3D heldVector;
                    if (gunEntity.activeGun.isHandHeldGunAimed) {
                        heldVector = gunEntity.activeGun.definition.gun.handHeldAimedOffset;
                    } else {
                        heldVector = gunEntity.activeGun.definition.gun.handHeldNormalOffset;
                    }
                    double heldVectorLength = heldVector.length();
                    double armPitchOffset = Math.toRadians(-90 + entity.pitch) - Math.asin(heldVector.y / heldVectorLength);
                    double armYawOffset = -Math.atan2(heldVector.x / heldVectorLength, heldVector.z / heldVectorLength);

                    //Set rotation points on the primary hand.
                    WrapperPlayer playerWrapper = WrapperPlayer.getWrapperFor((PlayerEntity) entity);
                    if (playerWrapper.isRightHanded()) {
                        model.rightArm.pitch = (float) armPitchOffset;
                        model.rightArm.yaw = (float) (armYawOffset + Math.toRadians(entity.headYaw - entity.bodyYaw));
                        model.rightArm.roll = 0;
                    } else {
                        model.leftArm.pitch = (float) armPitchOffset;
                        model.leftArm.yaw = (float) (-armYawOffset + Math.toRadians(entity.headYaw - entity.bodyYaw));
                        model.leftArm.roll = 0;
                    }

                    //If needed, set rotation on off-hand.
                    if (gunEntity.activeGun.isHandHeldGunAimed || gunEntity.activeGun.currentIsTwoHandedness != 0) {
                        heldVector = heldVector.copy();
                        heldVector.x = 0.3125 * 2 - heldVector.x;
                        heldVectorLength = heldVector.length();
                        armPitchOffset = Math.toRadians(-90 + entity.pitch) - Math.asin(heldVector.y / heldVectorLength);
                        armYawOffset = -Math.atan2(heldVector.x / heldVectorLength, heldVector.z / heldVectorLength);
                        if (playerWrapper.isRightHanded()) {
                            model.leftArm.pitch = (float) armPitchOffset;
                            model.leftArm.yaw = (float) (-armYawOffset + Math.toRadians(entity.headYaw - entity.bodyYaw));
                            model.leftArm.roll = 0;
                        } else {
                            model.rightArm.pitch = (float) armPitchOffset;
                            model.rightArm.yaw = (float) (armYawOffset + Math.toRadians(entity.headYaw - entity.bodyYaw));
                            model.rightArm.roll = 0;
                        }
                    }
                }
            }
        }
    }
}
