package mcinterface1122;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.systems.CameraSystem;
import minecrafttransportsimulator.systems.CameraSystem.CameraMode;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Interface for handling events pertaining to entity rendering.  This modifies the player's rendered state
 * to handle them being in vehicles, as well as ensuring their model adapts to any objects they may be holding.
 * This also handles the 2D GUI rendering.
 *
 * @author don_bruce
 */
@EventBusSubscriber(Side.CLIENT)
public class InterfaceEventsEntityRendering {
    public static boolean renderCurrentRiderSitting;
    public static boolean renderCurrentRiderStanding;
    private static boolean needPlayerTweaks = false;
    private static boolean needToPopMatrix = false;
    private static ItemStack heldStackHolder = null;
    private static final Set<EntityPlayer> playersOverwritten = new HashSet<>();
    private static final Point3D leftArmAngles = new Point3D();
    private static final Point3D rightArmAngles = new Point3D();
    private static final Point3D entityScale = new Point3D();
    private static final RotationMatrix riderBodyOrientation = new RotationMatrix();
    private static final TransformationMatrix riderTotalTransformation = new TransformationMatrix();
    private static final Point3D playerPosition = new Point3D();
    private static final Point3D playerPrevPosition = new Point3D();
    private static final Point3D playerPositionHelper = new Point3D();
    private static final Point3D cameraAdjustedPosition = new Point3D();
    private static final RotationMatrix cameraAdjustedOrientation = new RotationMatrix();
    private static final TransformationMatrix cameraAdjustments = new TransformationMatrix();
    private static int lastScreenWidth;
    private static int lastScreenHeight;
    private static float lastRiderYawHead;
    private static float lastRiderPrevYawHead;
    private static float lastRiderPitch;
    private static float lastRiderPrevPitch;


    /**
     * Changes camera rotation to match custom rotation, and also gets custom position for custom cameras.
     */
    @SubscribeEvent
    public static void onIVCameraSetup(CameraSetup event) {
        if (event.getEntity() instanceof EntityPlayer) {
            IWrapperPlayer player = WrapperPlayer.getWrapperFor((EntityPlayer) event.getEntity());
            cameraAdjustedPosition.set(0, 0, 0);
            cameraAdjustedOrientation.setToZero();
            if (CameraSystem.adjustCamera(player, cameraAdjustedPosition, cameraAdjustedOrientation, (float) event.getRenderPartialTicks())) {
                //Set helper to the current camera position.
                EntityPlayer mcPlayer = ((WrapperPlayer) player).player;
                playerPosition.set(mcPlayer.posX, mcPlayer.posY, mcPlayer.posZ);
                playerPrevPosition.set(mcPlayer.prevPosX, mcPlayer.prevPosY, mcPlayer.prevPosZ);
                playerPositionHelper.set(playerPrevPosition).interpolate(playerPosition, event.getRenderPartialTicks());
                playerPositionHelper.y += player.getEyeHeight();

                //Need to transpose the rotation so it applies opposite.
                double temp;
                temp = cameraAdjustedOrientation.m01;
                cameraAdjustedOrientation.m01 = cameraAdjustedOrientation.m10;
                cameraAdjustedOrientation.m10 = temp;
                
                temp = cameraAdjustedOrientation.m02;
                cameraAdjustedOrientation.m02 = cameraAdjustedOrientation.m20;
                cameraAdjustedOrientation.m20 = temp;
                
                temp = cameraAdjustedOrientation.m12;
                cameraAdjustedOrientation.m12 = cameraAdjustedOrientation.m21;
                cameraAdjustedOrientation.m21 = temp;

                //Set transform matrix for rotation.
                //Rotate by 180 to get the forwards-facing orientation.
                //Operations are done here with the camera facing the player.;
                cameraAdjustments.resetTransforms();
                cameraAdjustments.rotateY(180);
                cameraAdjustments.multiply(cameraAdjustedOrientation);
                InterfaceRender.applyTransformOpenGL(cameraAdjustments);

                //Set and apply translation.  We need the inverse here as well.
                //This comes after rotation since operations are inverted here.
                cameraAdjustedPosition.subtract(playerPositionHelper).invert();
                GL11.glTranslated(cameraAdjustedPosition.x, cameraAdjustedPosition.y, cameraAdjustedPosition.z);

                //Set event to 0 to block its transforms since they'll be at the wrong origin.
                event.setYaw(0);
                event.setPitch(0);
                event.setRoll(0);
            }
        }
    }

    /**
     * Renders all overlay things.  This is essentially anything that's a 2D render, such as the main overlay,
     * vehicle HUds, GUIs, camera overlays, etc.
     */
    @SubscribeEvent
    public static void onIVRenderOverlayPre(RenderGameOverlayEvent.Pre event) {
        //If we are rendering the custom camera overlay, block the crosshairs and the hotbar..
        if ((event.getType() == RenderGameOverlayEvent.ElementType.CROSSHAIRS || event.getType() == RenderGameOverlayEvent.ElementType.HOTBAR) && CameraSystem.customCameraOverlay != null) {
            event.setCanceled(true);
            return;
        }

        //If we are seated in a controller seat, and are rendering GUIs, disable the hotbar.
        if ((event.getType() == RenderGameOverlayEvent.ElementType.HOTBAR || event.getType() == RenderGameOverlayEvent.ElementType.FOOD || event.getType() == RenderGameOverlayEvent.ElementType.HEALTH || event.getType() == RenderGameOverlayEvent.ElementType.ARMOR || event.getType() == RenderGameOverlayEvent.ElementType.EXPERIENCE) && (InterfaceManager.clientInterface.getCameraMode() == CameraMode.FIRST_PERSON ? ConfigSystem.client.renderingSettings.renderHUD_1P.value : ConfigSystem.client.renderingSettings.renderHUD_3P.value)) {
            IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();
            AEntityB_Existing ridingEntity = player.getEntityRiding();
            if (ridingEntity instanceof PartSeat && ((PartSeat) ridingEntity).placementDefinition.isController) {
                event.setCanceled(true);
                return;
            }
        }

        //Do overlay rendering before the chat window is rendered.
        //This renders them over the main hotbar, but doesn't block the chat window.
        if (event.getType().equals(RenderGameOverlayEvent.ElementType.CHAT)) {
            //Set up variables.
            long displaySize = InterfaceManager.clientInterface.getPackedDisplaySize();
            int screenWidth = (int) (displaySize >> Integer.SIZE);
            int screenHeight = (int) displaySize;
            int mouseX = Mouse.getX() * screenWidth / Minecraft.getMinecraft().displayWidth;
            int mouseY = screenHeight - Mouse.getY() * screenHeight / Minecraft.getMinecraft().displayHeight - 1;

            float partialTicks = event.getPartialTicks();
            boolean updateGUIs = screenWidth != lastScreenWidth || screenHeight != lastScreenHeight;
            if (updateGUIs) {
                lastScreenWidth = screenWidth;
                lastScreenHeight = screenHeight;
            }
            InterfaceRender.renderGUI(mouseX, mouseY, screenWidth, screenHeight, partialTicks, updateGUIs);
        }
    }

    /**
     * Pre-post methods for adjusting entity angles while seated.
     */
    @SubscribeEvent
    public static void onIVRenderLivingPre(@SuppressWarnings("rawtypes") RenderLivingEvent.Pre event) {
        needPlayerTweaks = false;
        needToPopMatrix = false;
        renderCurrentRiderSitting = false;
        renderCurrentRiderStanding = false;
        leftArmAngles.set(0, 0, 0);
        rightArmAngles.set(0, 0, 0);
        EntityLivingBase entity = event.getEntity();
        WrapperEntity entityWrapper = WrapperEntity.getWrapperFor(entity);
        AEntityB_Existing ridingEntity = entityWrapper.getEntityRiding();
        //This may be null if MC sets this player as riding before the actual entity has time to load NBT.
        if (ridingEntity != null) {
            GL11.glPushMatrix();
            //Get orientation and scale for entity.
            //Head is relative to the body.
            ridingEntity.getInterpolatedOrientation(riderBodyOrientation, event.getPartialRenderTick());
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
                if (seat.definition.seat.standing) {
                    renderCurrentRiderStanding = true;
                } else {
                    renderCurrentRiderSitting = true;
                }

                if (seat.vehicleOn != null && seat.placementDefinition.isController) {
                    double turningAngle = seat.vehicleOn.rudderInputVar.currentValue/ 2D;
                    rightArmAngles.set(Math.toRadians(-75 + turningAngle), Math.toRadians(-10), 0);
                    leftArmAngles.set(Math.toRadians(-75 - turningAngle), Math.toRadians(10), 0);
                }
            } else {
                renderCurrentRiderSitting = true;
                entityScale.set(1, 1, 1);
            }

            //Set the entity's head yaw to the delta between their yaw and their angled yaw.
            //This needs to be relative as we're going to render relative to the body here, not the world.
            lastRiderYawHead = entity.rotationYawHead;
            lastRiderPrevYawHead = entity.prevRotationYawHead;
            lastRiderPitch = entity.rotationPitch;
            lastRiderPrevPitch = entity.prevRotationPitch;
            entity.rotationYawHead = (float) -ridingEntity.riderRelativeOrientation.convertToAngles().y;
            entity.prevRotationYawHead = entity.rotationYawHead;
            entity.rotationPitch = (float) ridingEntity.riderRelativeOrientation.angles.x;
            entity.prevRotationPitch = entity.rotationPitch;

            //Set the entity yaw offset to 0.  This forces their body to always face the front of the seat.
            //This isn't the entity's normal yaw, which is the direction they are facing.
            entity.renderYawOffset = 0;
            entity.prevRenderYawOffset = 0;

            //Translate the rider to the camera so it rotates on the proper coordinate system.
            Entity cameraEntity = Minecraft.getMinecraft().getRenderViewEntity();
            if (cameraEntity != null) {
                //Get delta between camera and rendered entity.
                Point3D deltaDistance = new Point3D(entity.lastTickPosX - cameraEntity.lastTickPosX + (entity.posX - entity.lastTickPosX - (cameraEntity.posX - cameraEntity.lastTickPosX)) * event.getPartialRenderTick(), entity.lastTickPosY - cameraEntity.lastTickPosY + (entity.posY - entity.lastTickPosY - (cameraEntity.posY - cameraEntity.lastTickPosY)) * event.getPartialRenderTick(), entity.lastTickPosZ - cameraEntity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ - (cameraEntity.posZ - cameraEntity.lastTickPosZ)) * event.getPartialRenderTick());

                //Apply translations and rotations to move entity to correct position relative to the camera entity.
                riderTotalTransformation.resetTransforms();
                riderTotalTransformation.setTranslation(deltaDistance);
                riderTotalTransformation.applyTranslation(0, -entityWrapper.getSeatOffset(), 0);
                riderTotalTransformation.applyRotation(riderBodyOrientation);
                riderTotalTransformation.applyScaling(entityScale);
                riderTotalTransformation.applyTranslation(0, entityWrapper.getSeatOffset(), 0);
                riderTotalTransformation.applyInvertedTranslation(deltaDistance);
                InterfaceRender.applyTransformOpenGL(riderTotalTransformation);
            }

            needToPopMatrix = true;
            if (ConfigSystem.client.renderingSettings.playerTweaks.value) {
                needPlayerTweaks = true;
            }
        }

        //Check for player model tweaks and changes.
        if (entity instanceof EntityPlayer) {
            //Check if we are holding a gun.  This is the only other time
            //we apply player tweaks besides riding in a vehicle.
            if (ConfigSystem.client.renderingSettings.playerTweaks.value) {
                EntityPlayer player = (EntityPlayer) entity;
                EntityPlayerGun gunEntity = EntityPlayerGun.playerClientGuns.get(entity.getUniqueID());
                if (gunEntity != null && gunEntity.activeGun != null) {

                    //Get arm rotations.
                    Point3D heldVector;
                    if (gunEntity.activeGun.isHandHeldGunAimed) {
                        heldVector = gunEntity.activeGun.definition.gun.handHeldAimedOffset;
                    } else {
                        heldVector = gunEntity.activeGun.definition.gun.handHeldNormalOffset;
                    }
                    double heldVectorLength = heldVector.length();
                    double armPitchOffset = Math.toRadians(-90 + entity.rotationPitch) - Math.asin(heldVector.y / heldVectorLength);
                    double armYawOffset = -Math.atan2(heldVector.x / heldVectorLength, heldVector.z / heldVectorLength);

                    //Set rotation points on the model.
                    if (WrapperPlayer.getWrapperFor(player).isRightHanded()) {
                        rightArmAngles.set(armPitchOffset, armYawOffset + Math.toRadians(entity.rotationYawHead - entity.renderYawOffset), 0);
                    } else {
                        leftArmAngles.set(armPitchOffset, -armYawOffset + Math.toRadians(entity.rotationYawHead - entity.renderYawOffset), 0);
                    }
                    if (gunEntity.activeGun.isHandHeldGunAimed || gunEntity.activeGun.twoHandedVar.isActive) {
                        heldVector = heldVector.copy();
                        heldVector.x = 0.3125 * 2 - heldVector.x;
                        heldVectorLength = heldVector.length();
                        armPitchOffset = Math.toRadians(-90 + entity.rotationPitch) - Math.asin(heldVector.y / heldVectorLength);
                        armYawOffset = -Math.atan2(heldVector.x / heldVectorLength, heldVector.z / heldVectorLength);
                        if (WrapperPlayer.getWrapperFor(player).isRightHanded()) {
                            leftArmAngles.set(armPitchOffset, -armYawOffset + Math.toRadians(entity.rotationYawHead - entity.renderYawOffset), 0);
                        } else {
                            rightArmAngles.set(armPitchOffset, armYawOffset + Math.toRadians(entity.rotationYawHead - entity.renderYawOffset), 0);
                        }
                    }

                    //Remove the held item from the enitty's hand
                    heldStackHolder = player.getHeldItemMainhand();
                    player.inventory.setInventorySlotContents(player.inventory.currentItem, ItemStack.EMPTY);

                    //Flag us for player tweaks in the render.
                    needPlayerTweaks = true;
                }

                //Check if we modified the player model to allow us to change angles.
                boolean overwrotePlayerModel = playersOverwritten.contains(player);
                boolean setModelToCustom = ConfigSystem.client.renderingSettings.playerTweaks.value && needPlayerTweaks && !overwrotePlayerModel;
                boolean setModelToDefault = (!ConfigSystem.client.renderingSettings.playerTweaks.value || !needPlayerTweaks) && overwrotePlayerModel;
                if (setModelToCustom || setModelToDefault) {
                    for (Field renderManagerField : RenderManager.class.getDeclaredFields()) {
                        if (renderManagerField.getName().equals("skinMap") || renderManagerField.getName().equals("field_178636_l")) {
                            try {
                                if (!renderManagerField.isAccessible()) {
                                    renderManagerField.setAccessible(true);
                                }

                                @SuppressWarnings("unchecked")
                                Map<String, RenderPlayer> skinMap = (Map<String, RenderPlayer>) renderManagerField.get(Minecraft.getMinecraft().getRenderManager());
                                List<String> skinTypes = new ArrayList<>(skinMap.keySet());

                                for (String skinType : skinTypes) {
                                    RenderPlayer render = skinMap.get(skinType);
                                    for (Field field : RenderLivingBase.class.getDeclaredFields()) {
                                        if (field.getName().equals("mainModel") || field.getName().equals("field_77045_g")) {
                                            try {
                                                if (!field.isAccessible()) {
                                                    field.setAccessible(true);
                                                }

                                                if (setModelToCustom) {
                                                    playersOverwritten.add(player);
                                                    field.set(render, new ModelPlayerCustom((ModelPlayer) field.get(render), skinType.equals("slim")));
                                                } else {
                                                    playersOverwritten.remove(player);
                                                    field.set(render, ((ModelPlayerCustom) field.get(render)).base);
                                                }
                                            } catch (Exception e) {
                                                ConfigSystem.client.renderingSettings.playerTweaks.value = false;
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                ConfigSystem.client.renderingSettings.playerTweaks.value = false;
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Pre-post methods for adjusting entity angles while seated.
     */
    @SubscribeEvent
    public static void onIVRenderLivingPost(@SuppressWarnings("rawtypes") RenderLivingEvent.Post event) {
        if (needToPopMatrix) {
            GL11.glPopMatrix();
            EntityLivingBase entity = event.getEntity();
            entity.rotationYawHead = lastRiderYawHead;
            entity.prevRotationYawHead = lastRiderPrevYawHead;
            entity.rotationPitch = lastRiderPitch;
            entity.prevRotationPitch = lastRiderPrevPitch;
        }
        if (heldStackHolder != null) {
            EntityPlayer player = (EntityPlayer) event.getEntity();
            player.inventory.setInventorySlotContents(player.inventory.currentItem, heldStackHolder);
            heldStackHolder = null;
        }
    }

    private static class ModelPlayerCustom extends ModelPlayer {
        private final ModelPlayer base;

        private ModelPlayerCustom(ModelPlayer base, boolean smallArms) {
            super(0.0F, smallArms);
            this.base = base;
        }

        @Override
        public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
            super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
            if (needPlayerTweaks) {
                if (renderCurrentRiderSitting) {
                    this.bipedLeftLeg.rotateAngleX = (float) Math.toRadians(-90);
                    this.bipedLeftLeg.rotateAngleY = 0;
                    this.bipedLeftLeg.rotateAngleZ = 0;
                    copyModelAngles(this.bipedLeftLeg, this.bipedLeftLegwear);
                    this.bipedRightLeg.rotateAngleX = (float) Math.toRadians(-90);
                    this.bipedRightLeg.rotateAngleY = 0;
                    this.bipedRightLeg.rotateAngleZ = 0;
                    copyModelAngles(this.bipedRightLeg, this.bipedRightLegwear);
                } else if (renderCurrentRiderStanding) {
                    this.bipedLeftLeg.rotateAngleX = 0;
                    this.bipedLeftLeg.rotateAngleY = 0;
                    this.bipedLeftLeg.rotateAngleZ = 0;
                    copyModelAngles(this.bipedLeftLeg, this.bipedLeftLegwear);
                    this.bipedRightLeg.rotateAngleX = 0;
                    this.bipedRightLeg.rotateAngleY = 0;
                    this.bipedRightLeg.rotateAngleZ = 0;
                    copyModelAngles(this.bipedRightLeg, this.bipedRightLegwear);

                }
                this.bipedLeftArm.rotateAngleX = (float) leftArmAngles.x;
                this.bipedLeftArm.rotateAngleY = (float) leftArmAngles.y;
                this.bipedLeftArm.rotateAngleZ = (float) leftArmAngles.z;
                copyModelAngles(this.bipedLeftArm, this.bipedLeftArmwear);
                this.bipedRightArm.rotateAngleX = (float) rightArmAngles.x;
                this.bipedRightArm.rotateAngleY = (float) rightArmAngles.y;
                this.bipedRightArm.rotateAngleZ = (float) rightArmAngles.z;
                copyModelAngles(this.bipedRightArm, this.bipedRightArmwear);

                //Copy model information to the core model in case other mods are referencing it.
                copyModelAngles(this.bipedLeftLeg, base.bipedLeftLeg);
                copyModelAngles(this.bipedLeftLeg, base.bipedLeftLegwear);
                copyModelAngles(this.bipedRightLeg, base.bipedRightLeg);
                copyModelAngles(this.bipedRightLeg, base.bipedRightLegwear);
                copyModelAngles(this.bipedLeftArm, base.bipedLeftArm);
                copyModelAngles(this.bipedLeftArm, base.bipedLeftArmwear);
                copyModelAngles(this.bipedRightArm, base.bipedRightArm);
                copyModelAngles(this.bipedRightArm, base.bipedRightArmwear);
                copyModelAngles(this.bipedHead, base.bipedHead);
                copyModelAngles(this.bipedHeadwear, base.bipedHeadwear);
                copyModelAngles(this.bipedBody, base.bipedBody);
                copyModelAngles(this.bipedBodyWear, base.bipedBodyWear);
            }
        }
    }

    /**
     * Hand render events.  We use these to disable rendering of the item in the player's hand
     * if they are holding a gun.  Not sure why there's two events, but we cancel them both!
     */
    @SubscribeEvent
    public static void onIVRenderHand(RenderHandEvent event) {
        EntityPlayerGun entity = EntityPlayerGun.playerClientGuns.get(Minecraft.getMinecraft().player.getUniqueID());
        if ((entity != null && entity.activeGun != null) || CameraSystem.activeCamera != null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onIVrenderSpecificHand(RenderSpecificHandEvent event) {
        EntityPlayerGun entity = EntityPlayerGun.playerClientGuns.get(Minecraft.getMinecraft().player.getUniqueID());
        if ((entity != null && entity.activeGun != null) || CameraSystem.activeCamera != null) {
            event.setCanceled(true);
        }
    }
}
