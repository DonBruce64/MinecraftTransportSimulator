package mcinterface1122;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.systems.CameraSystem;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Interface for handling events pertaining to entity rendering.  This modifies the player's rendered state
 * to handle them being in vehicles, as well as ensuring their model adapts to any objects they may be holding.
 * This also handles the final world rendering pass, which may render entities, and the 2D GUI rendering.
 *
 * @author don_bruce
 */
@EventBusSubscriber(Side.CLIENT)
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
    private static final RotationMatrix riderHeadOrientation = new RotationMatrix();
    private static final TransformationMatrix riderTotalTransformation = new TransformationMatrix();

    /**
     * World last event.  This occurs at the end of rendering in a special pass of -1.
     * We normally don't do anything here.  The exception is if the {@link BuilderEntityRenderForwarder}
     * didn't get rendered.  In this case, we manually render it.  The rendering pipelines
     * of those methods are set up to handle this and will tread a -1 pass as a combined 0/1 pass.
     */
    @SubscribeEvent
    public static void on(RenderWorldLastEvent event) {
        float partialTicks = event.getPartialTicks();

        //Enable lighting as pass -1 has that disabled.
        RenderHelper.enableStandardItemLighting();
        //TODO check if we need this.  If so, this goes into the render interface as a block.
        //Minecraft.getMinecraft().entityRenderer.enableLightmap();
        InterfaceManager.renderingInterface.setLightingState(true);

        //Render pass 0 and 1 here manually.
        for (int pass = 0; pass < 2; ++pass) {
            if (pass == 1) {
                InterfaceManager.renderingInterface.setBlend(true);
                GlStateManager.depthMask(false);
            }

            for (Entity entity : Minecraft.getMinecraft().world.loadedEntityList) {
                if (entity instanceof BuilderEntityRenderForwarder) {
                    BuilderEntityRenderForwarder forwarder = (BuilderEntityRenderForwarder) entity;
                    Minecraft.getMinecraft().getRenderManager().getEntityRenderObject(forwarder).doRender(forwarder, 0, 0, 0, 0, partialTicks);
                }
            }

            if (pass == 1) {
                InterfaceManager.renderingInterface.setBlend(false);
                GlStateManager.depthMask(true);
            }
        }

        //Turn lighting back off.
        RenderHelper.disableStandardItemLighting();
        InterfaceManager.renderingInterface.setLightingState(false);
    }

    private static int lastScreenWidth;
    private static int lastScreenHeight;

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
            AEntityE_Interactable<?> ridingEntity = player.getEntityRiding();
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

            //Render GUIs, re-creating their components if needed.
            //Set Y-axis to inverted to have correct orientation.
            GL11.glScalef(1.0F, -1.0F, 1.0F);

            //Enable alpha testing.  This can be disabled by mods doing bad state management during their event calls.
            //We don't want to enable blending though, as that's on-demand.
            //Just in case it is enabled, however, disable it.
            //This ensures the blending state is as it will be for the main rendering pass of -1.
            InterfaceManager.renderingInterface.setBlend(false);
            GL11.glEnable(GL11.GL_ALPHA_TEST);

            //Enable lighting.
            RenderHelper.enableStandardItemLighting();
            Minecraft.getMinecraft().entityRenderer.enableLightmap();
            InterfaceManager.renderingInterface.setLightingState(true);

            //Render main pass, then blended pass.
            int displayGUIIndex = 0;
            for (AGUIBase gui : AGUIBase.activeGUIs) {
                if (updateGUIs || gui.components.isEmpty()) {
                    gui.setupComponentsInit(screenWidth, screenHeight);
                }
                GL11.glPushMatrix();
                if (gui.capturesPlayer()) {
                    //Translate in front of the main GUI components.
                    GL11.glTranslated(0, 0, 250);
                } else {
                    //Translate far enough to render behind the chat window.
                    GL11.glTranslated(0, 0, -500 + 250 * displayGUIIndex++);
                }
                gui.render(mouseX, mouseY, false, partialTicks);
                ((InterfaceRender) InterfaceManager.renderingInterface).renderAllStacks();
                GL11.glPopMatrix();
            }
            displayGUIIndex = 0;
            InterfaceManager.renderingInterface.setBlend(true);
            for (AGUIBase gui : AGUIBase.activeGUIs) {
                GL11.glPushMatrix();
                if (gui.capturesPlayer()) {
                    //Translate in front of the main GUI components.
                    GL11.glTranslated(0, 0, 250);
                } else {
                    //Translate far enough to render behind the chat window.
                    GL11.glTranslated(0, 0, -500 + 250 * displayGUIIndex++);
                }
                gui.render(mouseX, mouseY, true, partialTicks);
                GL11.glPopMatrix();
            }

            //Set state back to normal.
            InterfaceManager.renderingInterface.setLightingState(false);
            Minecraft.getMinecraft().entityRenderer.disableLightmap();
            RenderHelper.disableStandardItemLighting();
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glScalef(1.0F, -1.0F, 1.0F);
        }
    }

    /**
     * Pre-post methods for adjusting entity angles while seated.
     */
    @SubscribeEvent
    public static void on(@SuppressWarnings("rawtypes") RenderLivingEvent.Pre event) {
        needPlayerTweaks = false;
        needToPopMatrix = false;
        renderCurrentRiderSitting = false;
        renderCurrentRiderStanding = false;
        leftArmAngles.set(0, 0, 0);
        rightArmAngles.set(0, 0, 0);
        EntityLivingBase entity = event.getEntity();
        WrapperEntity entityWrapper = WrapperEntity.getWrapperFor(entity);
        AEntityE_Interactable<?> ridingEntity = entityWrapper.getEntityRiding();
        //This may be null if MC sets this player as riding before the actual entity has time to load NBT.
        if (ridingEntity != null) {
            GL11.glPushMatrix();
            //Get orientation and scale for entity.
            //Head is relative to the body.
            ridingEntity.getInterpolatedOrientation(riderBodyOrientation, event.getPartialRenderTick());
            riderBodyOrientation.convertToAngles();
            riderHeadOrientation.set(ridingEntity.riderRelativeOrientation).convertToAngles();
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

            //Before we do further matrix transforms, get the head yaw for the entity.
            riderHeadOrientation.setToVector(new Point3D(0, 0, 1).rotate(riderHeadOrientation).reOrigin(riderBodyOrientation), false);

            //Set the entity's head yaw to the delta between their yaw and their angled yaw.
            //This needs to be relative as we're going to render relative to the body here, not the world.
            entity.rotationYawHead = (float) -riderHeadOrientation.angles.y;
            entity.prevRotationYawHead = entity.rotationYawHead;

            //Set the entity yaw offset to 0.  This forces their body to always face the front of the seat.
            //This isn't the entity's normal yaw, which is the direction they are facing.
            entity.renderYawOffset = 0;
            entity.prevRenderYawOffset = 0;

            //Get total translation.
            riderTotalTransformation.resetTransforms();
            riderTotalTransformation.setRotation(riderBodyOrientation);

            //Apply scale.
            riderTotalTransformation.applyScaling(entityScale);

            //Adjust for seated offset.
            riderTotalTransformation.applyTranslation(0, entityWrapper.getSeatOffset(), 0);

            //Push matrix and apply transform.
            //If we aren't the rider, translate the rider to us so it rotates on the proper coordinate system.
            EntityPlayerSP masterPlayer = Minecraft.getMinecraft().player;
            if (!entity.equals(masterPlayer)) {
                double playerDistanceX = entity.lastTickPosX - masterPlayer.lastTickPosX + (entity.posX - entity.lastTickPosX - (masterPlayer.posX - masterPlayer.lastTickPosX)) * event.getPartialRenderTick();
                double playerDistanceY = entity.lastTickPosY - masterPlayer.lastTickPosY + (entity.posY - entity.lastTickPosY - (masterPlayer.posY - masterPlayer.lastTickPosY)) * event.getPartialRenderTick();
                double playerDistanceZ = entity.lastTickPosZ - masterPlayer.lastTickPosZ + (entity.posZ - entity.lastTickPosZ - (masterPlayer.posZ - masterPlayer.lastTickPosZ)) * event.getPartialRenderTick();
                GL11.glTranslated(playerDistanceX, playerDistanceY, playerDistanceZ);
                InterfaceManager.renderingInterface.applyTransformOpenGL(riderTotalTransformation, false);
                GL11.glTranslated(-playerDistanceX, -playerDistanceY, -playerDistanceZ);
            } else {
                InterfaceManager.renderingInterface.applyTransformOpenGL(riderTotalTransformation, false);
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
                    rightArmAngles.set(armPitchOffset, armYawOffset + Math.toRadians(entity.rotationYawHead - entity.renderYawOffset), 0);
                    if (gunEntity.activeGun.isHandHeldGunAimed) {
                        leftArmAngles.set(armPitchOffset, -armYawOffset + Math.toRadians(entity.rotationYawHead - entity.renderYawOffset), 0);
                    }

                    //Remove the held item from the enitty's hand
                    EntityPlayer player = (EntityPlayer) entity;
                    heldStackHolder = player.getHeldItemMainhand();
                    player.inventory.setInventorySlotContents(player.inventory.currentItem, ItemStack.EMPTY);

                    //Flag us for player tweaks in the render.
                    needPlayerTweaks = true;
                }
            }

            //Check if we modified the player model to allow us to change angles.
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
                                                overwrotePlayerModel = true;
                                                field.set(render, new ModelPlayerCustom((ModelPlayer) field.get(render), skinType.equals("slim")));
                                            } else {
                                                overwrotePlayerModel = false;
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

    /**
     * Pre-post methods for adjusting entity angles while seated.
     */
    @SubscribeEvent
    public static void on(@SuppressWarnings("rawtypes") RenderLivingEvent.Post event) {
        if (needToPopMatrix) {
            GL11.glPopMatrix();
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
            }
        }
    }

    /**
     * Hand render events.  We use these to disable rendering of the item in the player's hand
     * if they are holding a gun.  Not sure why there's two events, but we cancel them both!
     */
    @SubscribeEvent
    public static void on(RenderHandEvent event) {
        EntityPlayerGun entity = EntityPlayerGun.playerClientGuns.get(Minecraft.getMinecraft().player.getUniqueID());
        if ((entity != null && entity.activeGun != null) || CameraSystem.runningCustomCameras) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void on(RenderSpecificHandEvent event) {
        EntityPlayerGun entity = EntityPlayerGun.playerClientGuns.get(Minecraft.getMinecraft().player.getUniqueID());
        if ((entity != null && entity.activeGun != null) || CameraSystem.runningCustomCameras) {
            event.setCanceled(true);
        }
    }
}
