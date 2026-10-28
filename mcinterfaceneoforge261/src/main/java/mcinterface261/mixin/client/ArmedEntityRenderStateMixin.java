package mcinterface261.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mcinterface261.MtsPlayerTweaksRenderState;
import mcinterface261.MtsPlayerTweaksRenderState.PlayerTweaks;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@Mixin(ArmedEntityRenderState.class)
public abstract class ArmedEntityRenderStateMixin {
    @Inject(method = "extractArmedEntityRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;Lnet/minecraft/client/renderer/item/ItemModelResolver;F)V", at = @At("TAIL"))
    private static void inject_extractArmedEntityRenderState(LivingEntity entity, ArmedEntityRenderState state, ItemModelResolver itemModelResolver, float partialTicks, CallbackInfo ci) {
        if (state instanceof HumanoidRenderState) {
            MtsPlayerTweaksRenderState.clear((HumanoidRenderState) state);
        }
        if (entity instanceof Player) {
            EntityPlayerGun gunEntity = EntityPlayerGun.playerClientGuns.get(entity.getUUID());
            if (gunEntity != null && gunEntity.activeGun != null) {
                if (state.mainArm == HumanoidArm.RIGHT) {
                    state.rightHandItemState.clear();
                    state.rightHandItemStack = ItemStack.EMPTY;
                } else {
                    state.leftHandItemState.clear();
                    state.leftHandItemStack = ItemStack.EMPTY;
                }
                if (ConfigSystem.client.renderingSettings.playerTweaks.value && state instanceof HumanoidRenderState) {
                    PlayerTweaks tweaks = MtsPlayerTweaksRenderState.put((HumanoidRenderState) state);
                    Point3D heldVector = gunEntity.activeGun.isHandHeldGunAimed ? gunEntity.activeGun.definition.gun.handHeldAimedOffset : gunEntity.activeGun.definition.gun.handHeldNormalOffset;
                    double heldVectorLength = heldVector.length();
                    double armPitchOffset = Math.toRadians(-90 + entity.getXRot(partialTicks)) - Math.asin(heldVector.y / heldVectorLength);
                    double armYawOffset = -Math.atan2(heldVector.x / heldVectorLength, heldVector.z / heldVectorLength);
                    if (state.mainArm == HumanoidArm.RIGHT) {
                        tweaks.setRightArm = true;
                        tweaks.rightArmXRot = (float) armPitchOffset;
                        tweaks.rightArmYRot = (float) (armYawOffset + Math.toRadians(entity.yHeadRot - entity.yBodyRot));
                        tweaks.rightArmZRot = 0;
                    } else {
                        tweaks.setLeftArm = true;
                        tweaks.leftArmXRot = (float) armPitchOffset;
                        tweaks.leftArmYRot = (float) (-armYawOffset + Math.toRadians(entity.yHeadRot - entity.yBodyRot));
                        tweaks.leftArmZRot = 0;
                    }
                    if (gunEntity.activeGun.isHandHeldGunAimed || gunEntity.activeGun.twoHandedVar.isActive) {
                        heldVector = heldVector.copy();
                        heldVector.x = 0.3125 * 2 - heldVector.x;
                        heldVectorLength = heldVector.length();
                        armPitchOffset = Math.toRadians(-90 + entity.getXRot(partialTicks)) - Math.asin(heldVector.y / heldVectorLength);
                        armYawOffset = -Math.atan2(heldVector.x / heldVectorLength, heldVector.z / heldVectorLength);
                        if (state.mainArm == HumanoidArm.RIGHT) {
                            tweaks.setLeftArm = true;
                            tweaks.leftArmXRot = (float) armPitchOffset;
                            tweaks.leftArmYRot = (float) (-armYawOffset + Math.toRadians(entity.yHeadRot - entity.yBodyRot));
                            tweaks.leftArmZRot = 0;
                        } else {
                            tweaks.setRightArm = true;
                            tweaks.rightArmXRot = (float) armPitchOffset;
                            tweaks.rightArmYRot = (float) (armYawOffset + Math.toRadians(entity.yHeadRot - entity.yBodyRot));
                            tweaks.rightArmZRot = 0;
                        }
                    }
                }
            }
        }
    }
}
