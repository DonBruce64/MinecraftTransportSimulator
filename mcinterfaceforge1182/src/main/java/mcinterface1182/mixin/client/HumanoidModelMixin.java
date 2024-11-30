package mcinterface1182.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mcinterface1182.WrapperEntity;
import mcinterface1182.WrapperPlayer;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends LivingEntity> {
    private static final float yArmAngleRad = (float) Math.toRadians(10);

    /**
     * Need this method to adjust model legs and arms for sitting players.
     */
    @SuppressWarnings("unchecked")
    @Inject(method = "setupAnim", at = @At(value = "TAIL"))
    public void inject_setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (ConfigSystem.client.renderingSettings.playerTweaks.value) {
            HumanoidModel<T> model = (HumanoidModel<T>) ((Object) this);
            WrapperEntity entityWrapper = WrapperEntity.getWrapperFor(entity);
            AEntityB_Existing ridingEntity = entityWrapper.getEntityRiding();

            //This may be null if MC sets this player as riding before the actual entity has time to load NBT.
            if (ridingEntity != null) {
                boolean renderCurrentRiderStanding = false;
                if (ridingEntity instanceof PartSeat) {
                    PartSeat seat = (PartSeat) ridingEntity;
                    renderCurrentRiderStanding = seat.definition.seat.standing;

                    if (seat.vehicleOn != null && seat.placementDefinition.isController) {
                        double turningAngle = seat.vehicleOn.rudderInputVar.currentValue / 2D;
                        model.rightArm.xRot = (float) Math.toRadians(-75 + turningAngle);
                        model.rightArm.yRot = -yArmAngleRad;
                        model.rightArm.zRot = 0;

                        model.leftArm.xRot = (float) Math.toRadians(-75 - turningAngle);
                        model.leftArm.yRot = yArmAngleRad;
                        model.leftArm.zRot = 0;
                    }
                }
                if (renderCurrentRiderStanding) {
                    model.leftLeg.xRot = 0;
                    model.leftLeg.yRot = 0;
                    model.leftLeg.zRot = 0;
                    model.rightLeg.xRot = 0;
                    model.rightLeg.yRot = 0;
                    model.rightLeg.zRot = 0;
                } else {
                    model.leftLeg.xRot = (float) Math.toRadians(-90);
                    model.leftLeg.yRot = 0;
                    model.leftLeg.zRot = 0;
                    model.rightLeg.xRot = (float) Math.toRadians(-90);
                    model.rightLeg.yRot = 0;
                    model.rightLeg.zRot = 0;
                }
            }

            if (entity instanceof Player) {
                EntityPlayerGun gunEntity = EntityPlayerGun.playerClientGuns.get(entity.getUUID());
                if (gunEntity != null && gunEntity.activeGun != null) {
                    //Get arm rotations.
                    Point3D heldVector;
                    if (gunEntity.activeGun.isHandHeldGunAimed) {
                        heldVector = gunEntity.activeGun.definition.gun.handHeldAimedOffset;
                    } else {
                        heldVector = gunEntity.activeGun.definition.gun.handHeldNormalOffset;
                    }
                    double heldVectorLength = heldVector.length();
                    double armPitchOffset = Math.toRadians(-90 + entity.getXRot()) - Math.asin(heldVector.y / heldVectorLength);
                    double armYawOffset = -Math.atan2(heldVector.x / heldVectorLength, heldVector.z / heldVectorLength);

                    //Set rotation points on the primary hand.
                    WrapperPlayer playerWrapper = WrapperPlayer.getWrapperFor((Player) entity);
                    if (playerWrapper.isRightHanded()) {
                        model.rightArm.xRot = (float) armPitchOffset;
                        model.rightArm.yRot = (float) (armYawOffset + Math.toRadians(entity.yHeadRot - entity.yBodyRot));
                        model.rightArm.zRot = 0;
                    } else {
                        model.leftArm.xRot = (float) armPitchOffset;
                        model.leftArm.yRot = (float) (-armYawOffset + Math.toRadians(entity.yHeadRot - entity.yBodyRot));
                        model.leftArm.zRot = 0;
                    }

                    //If needed, set rotation on off-hand.
                    if (gunEntity.activeGun.isHandHeldGunAimed || gunEntity.activeGun.twoHandedVar.isActive) {
                        heldVector = heldVector.copy();
                        heldVector.x = 0.3125 * 2 - heldVector.x;
                        heldVectorLength = heldVector.length();
                        armPitchOffset = Math.toRadians(-90 + entity.getXRot()) - Math.asin(heldVector.y / heldVectorLength);
                        armYawOffset = -Math.atan2(heldVector.x / heldVectorLength, heldVector.z / heldVectorLength);
                        if (playerWrapper.isRightHanded()) {
                            model.leftArm.xRot = (float) armPitchOffset;
                            model.leftArm.yRot = (float) (-armYawOffset + Math.toRadians(entity.yHeadRot - entity.yBodyRot));
                            model.leftArm.zRot = 0;
                        } else {
                            model.rightArm.xRot = (float) armPitchOffset;
                            model.rightArm.yRot = (float) (armYawOffset + Math.toRadians(entity.yHeadRot - entity.yBodyRot));
                            model.rightArm.zRot = 0;
                        }
                    }
                }
            }
        }
    }
}
