package mcinterface1165;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

import mcinterface1165.mixin.client.RenderInfoInvokerMixin;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.systems.CameraSystem;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Interface for handling events pertaining to entity rendering.  This modifies the player's rendered state
 * to handle them being in vehicles, as well as ensuring their model adapts to any objects they may be holding.
 * This also handles the 2D GUI rendering.
 *
 * @author don_bruce
 */
@EventBusSubscriber(Dist.CLIENT)
public class InterfaceEventsEntityRendering {
    public static boolean renderCurrentRiderSitting;
    public static boolean renderCurrentRiderStanding;
    private static boolean overwrotePlayerModel = false;
    private static boolean needPlayerTweaks = false;
    private static boolean needToPopMatrix = false;
    private static ItemStack heldStackHolder = null;
    private static final Point3D leftArmAngles = new Point3D();
    private static final Point3D rightArmAngles = new Point3D();
    private static final Point3D entityScale = new Point3D();
    private static final RotationMatrix riderBodyOrientation = new RotationMatrix();
    private static final Point3D riderHeadAngles = new Point3D();
    private static float riderStoredHeadRot;
    private static float riderStoredHeadRotO;
    private static final TransformationMatrix riderTotalTransformation = new TransformationMatrix();
    private static final Point3D cameraAdjustedPosition = new Point3D();
    private static final RotationMatrix cameraAdjustedOrientation = new RotationMatrix();
    private static PlayerEntity mcPlayer;

    private static int lastScreenWidth;
    private static int lastScreenHeight;

    /**
     * Changes camera rotation to match custom rotation, and also gets custom position for custom cameras.
     */
    @SubscribeEvent
    public static void on(CameraSetup event) {
        ActiveRenderInfo info = event.getInfo();
        if (info.getEntity() instanceof PlayerEntity) {
            mcPlayer = (PlayerEntity) info.getEntity();
            IWrapperPlayer player = WrapperPlayer.getWrapperFor(mcPlayer);
            cameraAdjustedPosition.set(0, 0, 0);
            cameraAdjustedOrientation.setToZero();
            float partialTicks = (float) event.getRenderPartialTicks();
            if (CameraSystem.adjustCamera(player, cameraAdjustedPosition, cameraAdjustedOrientation, partialTicks)) {
                //Camera adjustments occur backwards here.  Reverse order in the matrix.
                //Also need to reverse sign of Y, since that's backwards in MC.
                cameraAdjustedOrientation.convertToAngles();
                if (!InterfaceManager.clientInterface.inFirstPerson() && !InterfaceManager.clientInterface.inThirdPerson()) {
                    //Inverted third-person needs roll and pich flipped due to the opposite perspective.
                    //It also needs the camera rotated 180 in the Y to face the other direction.
                    event.setRoll((float) -cameraAdjustedOrientation.angles.z);
                    event.setPitch((float) -cameraAdjustedOrientation.angles.x);
                    event.setYaw((float) (-cameraAdjustedOrientation.angles.y + 180));
                } else {
                    event.setRoll((float) cameraAdjustedOrientation.angles.z);
                    event.setPitch((float) cameraAdjustedOrientation.angles.x);
                    event.setYaw((float) -cameraAdjustedOrientation.angles.y);
                }

                //Move the info's setup to the set position of the camera.
                //This will offset the player's eye position to match the camera.
                //We do this in first-person mode since third-person adds zoom stuff.
                ((RenderInfoInvokerMixin) info).invoke_setPosition(cameraAdjustedPosition.x, cameraAdjustedPosition.y, cameraAdjustedPosition.z);
            }
        }
    }

    /**
     * Renders all overlay things.  This is essentially anything that's a 2D render, such as the main overlay,
     * vehicle HUds, GUIs, camera overlays, etc.
     */
    @SubscribeEvent
    public static void on(RenderGameOverlayEvent.Pre event) {
        //If we are rendering the custom camera overlay, block the crosshairs and the hotbar..
        if ((event.getType() == RenderGameOverlayEvent.ElementType.CROSSHAIRS || event.getType() == RenderGameOverlayEvent.ElementType.HOTBAR) && CameraSystem.customCameraOverlay != null) {
            event.setCanceled(true);
            return;
        }

        //If we are seated in a controller seat, and are rendering GUIs, disable the hotbar.
        if ((event.getType() == RenderGameOverlayEvent.ElementType.HOTBAR || event.getType() == RenderGameOverlayEvent.ElementType.FOOD || event.getType() == RenderGameOverlayEvent.ElementType.HEALTH || event.getType() == RenderGameOverlayEvent.ElementType.ARMOR || event.getType() == RenderGameOverlayEvent.ElementType.EXPERIENCE) && (InterfaceManager.clientInterface.inFirstPerson() ? ConfigSystem.client.renderingSettings.renderHUD_1P.value : ConfigSystem.client.renderingSettings.renderHUD_3P.value)) {
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
            MainWindow window = Minecraft.getInstance().getWindow();
            long displaySize = InterfaceManager.clientInterface.getPackedDisplaySize();
            int screenWidth = (int) (displaySize >> Integer.SIZE);
            int screenHeight = (int) displaySize;
            double[] xPos = new double[1];
            double[] yPos = new double[1];
            GLFW.glfwGetCursorPos(window.getWindow(), xPos, yPos);
            int mouseX = (int) (xPos[0] * screenWidth / window.getScreenWidth());
            int mouseY = (int) (yPos[0] * screenHeight / window.getScreenHeight());

            float partialTicks = event.getPartialTicks();
            boolean updateGUIs = screenWidth != lastScreenWidth || screenHeight != lastScreenHeight;
            if (updateGUIs) {
                lastScreenWidth = screenWidth;
                lastScreenHeight = screenHeight;
            }

            InterfaceRender.renderGUI(event.getMatrixStack(), mouseX, mouseY, screenWidth, screenHeight, partialTicks, updateGUIs);
        }
    }

    /**
     * Pre-post methods for adjusting entity angles while seated.
     */
    @SubscribeEvent
    public static void on(@SuppressWarnings("rawtypes") RenderLivingEvent.Pre event) {
        needPlayerTweaks = false;
        renderCurrentRiderSitting = false;
        renderCurrentRiderStanding = false;
        leftArmAngles.set(0, 0, 0);
        rightArmAngles.set(0, 0, 0);
        LivingEntity entity = event.getEntity();
        WrapperEntity entityWrapper = WrapperEntity.getWrapperFor(entity);
        AEntityB_Existing ridingEntity = entityWrapper.getEntityRiding();
        //This may be null if MC sets this player as riding before the actual entity has time to load NBT.
        if (ridingEntity != null) {
            event.getMatrixStack().pushPose();
            //Get orientation and scale for entity.
            //Head is relative to the body.
            ridingEntity.getInterpolatedOrientation(riderBodyOrientation, event.getPartialRenderTick());
            riderBodyOrientation.convertToAngles();
            if (ridingEntity instanceof PartSeat) {
                PartSeat seat = (PartSeat) ridingEntity;
                entityScale.set(seat.scale);
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
                    double turningAngle = seat.vehicleOn.rudderInput / 2D;
                    rightArmAngles.set(Math.toRadians(-75 + turningAngle), Math.toRadians(-10), 0);
                    leftArmAngles.set(Math.toRadians(-75 - turningAngle), Math.toRadians(10), 0);
                }
            } else {
                renderCurrentRiderSitting = true;
                entityScale.set(1, 1, 1);
            }

            //Set the entity's head yaw to the delta between their yaw and their angled yaw.
            //This needs to be relative as we're going to render relative to the body here, not the world.
            //Need to store these though, since they get used in other areas not during rendering and this will foul them.
            riderStoredHeadRot = entity.yHeadRot;
            riderStoredHeadRotO = entity.yHeadRotO;
            entity.yHeadRot = (float) -riderHeadAngles.computeVectorAngles(entityWrapper.getOrientation(), riderBodyOrientation).y;
            entity.yHeadRotO = entity.yHeadRot;

            //Set the entity yaw offset to 0.  This forces their body to always face the front of the seat.
            //This isn't the entity's normal yaw, which is the direction they are facing.
            entity.yBodyRot = 0;
            entity.yBodyRotO = 0;

            //Translate the rider to the camera so it rotates on the proper coordinate system.
            PlayerEntity cameraEntity = Minecraft.getInstance().player;
            if (cameraEntity != null) {
                //Get delta between camera and rendered entity.
                Vector3d cameraEntityPos = cameraEntity.position();
                Vector3d entityPos = entity.position();
                new Point3D(entity.xo - cameraEntity.xo + (entityPos.x - entity.xo - (cameraEntityPos.x - cameraEntity.xo)) * event.getPartialRenderTick(), entity.yo - cameraEntity.yo + (entityPos.y - entity.yo - (cameraEntityPos.y - cameraEntity.yo)) * event.getPartialRenderTick(), entity.zo - cameraEntity.zo + (entityPos.z - entity.zo - (cameraEntityPos.z - cameraEntity.zo)) * event.getPartialRenderTick());

                //Apply translations and rotations to move entity to correct position relative to the camera entity.
                riderTotalTransformation.resetTransforms();
                riderTotalTransformation.applyRotation(riderBodyOrientation);
                riderTotalTransformation.applyScaling(entityScale);
                riderTotalTransformation.applyTranslation(0, entityWrapper.getSeatOffset(), 0);
                event.getMatrixStack().last().pose().multiply(InterfaceRender.convertMatrix4f(riderTotalTransformation));
            }

            needToPopMatrix = true;
            if (ConfigSystem.client.renderingSettings.playerTweaks.value) {
                needPlayerTweaks = true;
            }
        }

        //Check for player model tweaks and changes.
        if (entity instanceof PlayerEntity) {
            //Check if we are holding a gun.  This is the only other time
            //we apply player tweaks besides riding in a vehicle.
            if (ConfigSystem.client.renderingSettings.playerTweaks.value) {
                EntityPlayerGun gunEntity = EntityPlayerGun.playerClientGuns.get(entity.getUUID());
                if (gunEntity != null && gunEntity.activeGun != null) {
                    PlayerEntity player = (PlayerEntity) entity;

                    //Get arm rotations.
                    Point3D heldVector;
                    if (gunEntity.activeGun.isHandHeldGunAimed) {
                        heldVector = gunEntity.activeGun.definition.gun.handHeldAimedOffset;
                    } else {
                        heldVector = gunEntity.activeGun.definition.gun.handHeldNormalOffset;
                    }
                    double heldVectorLength = heldVector.length();
                    double armPitchOffset = Math.toRadians(-90 + entity.xRot) - Math.asin(heldVector.y / heldVectorLength);
                    double armYawOffset = -Math.atan2(heldVector.x / heldVectorLength, heldVector.z / heldVectorLength);

                    //Set rotation points on the model.
                    if (WrapperPlayer.getWrapperFor(player).isRightHanded()) {
                        rightArmAngles.set(armPitchOffset, armYawOffset + Math.toRadians(entity.yHeadRot - entity.yBodyRot), 0);
                    } else {
                        leftArmAngles.set(armPitchOffset, -armYawOffset + Math.toRadians(entity.yHeadRot - entity.yBodyRot), 0);
                    }
                    if (gunEntity.activeGun.isHandHeldGunAimed || gunEntity.activeGun.definition.gun.isTwoHanded) {
                        heldVector = heldVector.copy();
                        heldVector.x = 0.3125 * 2 - heldVector.x;
                        heldVectorLength = heldVector.length();
                        armPitchOffset = Math.toRadians(-90 + entity.xRot) - Math.asin(heldVector.y / heldVectorLength);
                        armYawOffset = -Math.atan2(heldVector.x / heldVectorLength, heldVector.z / heldVectorLength);
                        if (WrapperPlayer.getWrapperFor(player).isRightHanded()) {
                            leftArmAngles.set(armPitchOffset, -armYawOffset + Math.toRadians(entity.yHeadRot - entity.yBodyRot), 0);
                        } else {
                            rightArmAngles.set(armPitchOffset, armYawOffset + Math.toRadians(entity.yHeadRot - entity.yBodyRot), 0);
                        }
                    }

                    //Remove the held item from the enitty's hand
                    heldStackHolder = player.getMainHandItem();
                    player.inventory.setItem(player.inventory.selected, ItemStack.EMPTY);

                    //Flag us for player tweaks in the render.
                    needPlayerTweaks = true;
                }
            }

            //Check if we modified the player model to allow us to change angles.
            boolean setModelToCustom = ConfigSystem.client.renderingSettings.playerTweaks.value && needPlayerTweaks && !overwrotePlayerModel;
            boolean setModelToDefault = (!ConfigSystem.client.renderingSettings.playerTweaks.value || !needPlayerTweaks) && overwrotePlayerModel;
            if (setModelToCustom || setModelToDefault) {
                for (Field renderManagerField : EntityRendererManager.class.getDeclaredFields()) {
                    if (renderManagerField.getName().equals("playerRenderers") || renderManagerField.getName().equals("field_178636_l")) {
                        try {
                            if (!renderManagerField.isAccessible()) {
                                renderManagerField.setAccessible(true);
                            }

                            @SuppressWarnings("unchecked")
                            Map<String, PlayerRenderer> skinMap = (Map<String, PlayerRenderer>) renderManagerField.get(Minecraft.getInstance().getEntityRenderDispatcher());
                            List<String> skinTypes = new ArrayList<>(skinMap.keySet());

                            for (String skinType : skinTypes) {
                                PlayerRenderer render = skinMap.get(skinType);
                                for (Field field : LivingRenderer.class.getDeclaredFields()) {
                                    if (field.getName().equals("model") || field.getName().equals("field_77045_g")) {
                                        try {
                                            if (!field.isAccessible()) {
                                                field.setAccessible(true);
                                            }

                                            if (setModelToCustom) {
                                                overwrotePlayerModel = true;
                                                field.set(render, new ModelPlayerCustom<>((PlayerModel<?>) field.get(render), skinType.equals("slim")));
                                            } else {
                                                overwrotePlayerModel = false;
                                                field.set(render, ((ModelPlayerCustom<?>) field.get(render)).base);
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

    /**
     * Pre-post methods for adjusting entity angles while seated.
     */
    @SubscribeEvent
    public static void on(@SuppressWarnings("rawtypes") RenderLivingEvent.Post event) {
        if (needToPopMatrix) {
            event.getMatrixStack().popPose();
            LivingEntity entity = event.getEntity();
            entity.yHeadRot = riderStoredHeadRot;
            entity.yHeadRotO = riderStoredHeadRotO;
            needToPopMatrix = false;
        }
        if (heldStackHolder != null) {
            PlayerEntity player = (PlayerEntity) event.getEntity();
            player.inventory.setItem(player.inventory.selected, heldStackHolder);
            heldStackHolder = null;
        }
    }

    private static class ModelPlayerCustom<T extends LivingEntity> extends PlayerModel<T> {
        private final PlayerModel<T> base;

        private ModelPlayerCustom(PlayerModel<T> base, boolean smallArms) {
            super(0.0F, smallArms);
            this.base = base;
        }

        @Override
        public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            if (needPlayerTweaks) {
                if (renderCurrentRiderSitting) {
                    this.leftLeg.xRot = (float) Math.toRadians(-90);
                    this.leftLeg.yRot = 0;
                    this.leftLeg.zRot = 0;
                    this.leftPants.copyFrom(this.leftLeg);
                    this.rightLeg.xRot = (float) Math.toRadians(-90);
                    this.rightLeg.yRot = 0;
                    this.rightLeg.zRot = 0;
                    this.rightPants.copyFrom(this.rightLeg);
                } else if (renderCurrentRiderStanding) {
                    this.leftLeg.xRot = 0;
                    this.leftLeg.yRot = 0;
                    this.leftLeg.zRot = 0;
                    this.leftPants.copyFrom(this.leftLeg);
                    this.rightLeg.xRot = 0;
                    this.rightLeg.yRot = 0;
                    this.rightLeg.zRot = 0;
                    this.rightPants.copyFrom(this.rightLeg);
                }
                this.leftArm.xRot = (float) leftArmAngles.x;
                this.leftArm.yRot = (float) leftArmAngles.y;
                this.leftArm.zRot = (float) leftArmAngles.z;
                this.leftSleeve.copyFrom(this.leftArm);
                this.rightArm.xRot = (float) rightArmAngles.x;
                this.rightArm.yRot = (float) rightArmAngles.y;
                this.rightArm.zRot = (float) rightArmAngles.z;
                this.rightSleeve.copyFrom(this.rightArm);
            }
        }
    }

    /**
     * Hand and arm render events.  We use these to disable rendering of the item in the player's hand
     * if they are holding a gun.  Not sure why there's two events, but we cancel them both!
     */
    @SubscribeEvent
    public static void on(RenderHandEvent event) {
        EntityPlayerGun entity = EntityPlayerGun.playerClientGuns.get(Minecraft.getInstance().player.getUUID());
        if ((entity != null && entity.activeGun != null) || CameraSystem.runningCustomCameras) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void on(RenderArmEvent event) {
        EntityPlayerGun entity = EntityPlayerGun.playerClientGuns.get(Minecraft.getInstance().player.getUUID());
        if ((entity != null && entity.activeGun != null) || CameraSystem.runningCustomCameras) {
            event.setCanceled(true);
        }
    }
}
