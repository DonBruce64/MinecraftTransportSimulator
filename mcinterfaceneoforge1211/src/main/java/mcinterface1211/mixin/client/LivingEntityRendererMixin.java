package mcinterface1211.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import mcinterface1211.InterfaceRender;
import mcinterface1211.WrapperEntity;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.entities.instances.PartSeat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity> {
    private static boolean needToRestoreState = false;
    private static ItemStack heldStackHolder = null;
    private static final Point3D entityScale = new Point3D();
    private static final RotationMatrix riderBodyOrientation = new RotationMatrix();
    private static float riderStoredHeadRot;
    private static float riderStoredHeadRotO;
    private static final TransformationMatrix riderTotalTransformation = new TransformationMatrix();
    private static float lastRiderPitch;
    private static float lastRiderPrevPitch;

    /**
     * Need this method to modify the player's body position while riding a seat.
     */
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V"))
    public void inject_renderPre(T pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight, CallbackInfo ci) {
        WrapperEntity entityWrapper = WrapperEntity.getWrapperFor(pEntity);
        AEntityB_Existing ridingEntity = entityWrapper.getEntityRiding();
        //This may be null if MC sets this player as riding before the actual entity has time to load NBT.
        if (ridingEntity != null) {
            //Get orientation and scale for entity.
            //Head is relative to the body.
            ridingEntity.getInterpolatedOrientation(riderBodyOrientation, pPartialTicks);
            riderBodyOrientation.convertToAngles();
            if (ridingEntity instanceof PartSeat) {
                PartSeat seat = (PartSeat) ridingEntity;
                entityScale.set(seat.riderScale);
                if (seat.definition.seat.playerScale != null) {
                    entityScale.multiply(seat.definition.seat.playerScale);
                }
                if (seat.placementDefinition.playerScale != null) {
                    entityScale.multiply(seat.placementDefinition.playerScale);
                }
            } else {
                entityScale.set(1, 1, 1);
            }

            //Set the entity's head yaw to the delta between their yaw and their angled yaw.
            //This needs to be relative as we're going to render relative to the body here, not the world.
            //Need to store these though, since they get used in other areas not during rendering and this will foul them.
            riderStoredHeadRot = pEntity.yHeadRot;
            riderStoredHeadRotO = pEntity.yHeadRotO;
            lastRiderPitch = pEntity.getXRot();
            lastRiderPrevPitch = pEntity.xRotO;
            pEntity.yHeadRot = (float) -ridingEntity.riderRelativeOrientation.convertToAngles().y;
            pEntity.yHeadRotO = pEntity.yHeadRot;
            pEntity.setXRot((float) ridingEntity.riderRelativeOrientation.angles.x);
            pEntity.xRotO = pEntity.getXRot();

            //Set the entity yaw offset to 0.  This forces their body to always face the front of the seat.
            //This isn't the entity's normal yaw, which is the direction they are facing.
            pEntity.yBodyRot = 0;
            pEntity.yBodyRotO = 0;

            //Translate the rider to the camera so it rotates on the proper coordinate system.
            Player cameraEntity = Minecraft.getInstance().player;
            if (cameraEntity != null) {
                //Get delta between camera and rendered entity.
                Vec3 cameraEntityPos = cameraEntity.position();
                Vec3 entityPos = pEntity.position();
                new Point3D(pEntity.xo - cameraEntity.xo + (entityPos.x - pEntity.xo - (cameraEntityPos.x - cameraEntity.xo)) * pPartialTicks, pEntity.yo - cameraEntity.yo + (entityPos.y - pEntity.yo - (cameraEntityPos.y - cameraEntity.yo)) * pPartialTicks, pEntity.zo - cameraEntity.zo + (entityPos.z - pEntity.zo - (cameraEntityPos.z - cameraEntity.zo)) * pPartialTicks);

                //Apply translations and rotations to move entity to correct position relative to the camera entity.
                riderTotalTransformation.resetTransforms();
                riderTotalTransformation.applyTranslation(0, -entityWrapper.getSeatOffset(), 0);
                riderTotalTransformation.applyRotation(riderBodyOrientation);
                riderTotalTransformation.applyScaling(entityScale);
                riderTotalTransformation.applyTranslation(0, entityWrapper.getSeatOffset(), 0);
                pMatrixStack.last().pose().mul(InterfaceRender.convertMatrix4f(riderTotalTransformation));
            }

            needToRestoreState = true;
        }

        //Check for player model tweaks and changes.
        if (pEntity instanceof Player) {
            //Check if we are holding a gun.  This is the only other time
            //we apply player tweaks besides riding in a vehicle.
            EntityPlayerGun gunEntity = EntityPlayerGun.playerClientGuns.get(pEntity.getUUID());
            if (gunEntity != null && gunEntity.activeGun != null) {
                Player player = (Player) pEntity;

                //Remove the held item from the enitty's hand
                heldStackHolder = player.getMainHandItem();
                player.getInventory().setItem(player.getInventory().selected, ItemStack.EMPTY);
            }
        }
    }

    @Inject(method = "render", at = @At(value = "TAIL"))
    public void inject_renderPost(T pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight, CallbackInfo ci) {
        if (needToRestoreState) {
            pEntity.yHeadRot = riderStoredHeadRot;
            pEntity.yHeadRotO = riderStoredHeadRotO;
            pEntity.setXRot(lastRiderPitch);
            pEntity.xRotO = lastRiderPrevPitch;
            needToRestoreState = false;
        }
        if (heldStackHolder != null) {
            Player player = (Player) pEntity;
            player.getInventory().setItem(player.getInventory().selected, heldStackHolder);
            heldStackHolder = null;
        }
    }
}
