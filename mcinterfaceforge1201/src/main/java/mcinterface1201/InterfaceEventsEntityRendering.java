package mcinterface1201;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.Window;

import mcinterface1201.mixin.client.CameraMixin;
import mcinterface1201.mixin.client.LivingEntityRendererAccessor;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.jsondefs.JSONPlayerAnimation.PlayerLimb;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.rendering.PlayerAnimationSystem;
import minecrafttransportsimulator.rendering.dynamiclight.DynamicLightShadowAtlas;
import minecrafttransportsimulator.rendering.dynamiclight.DynamicLightSystem;
import minecrafttransportsimulator.systems.CameraSystem;
import minecrafttransportsimulator.systems.CameraSystem.CameraMode;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.Event;
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
    public static final Point3D cameraAdjustedPosition = new Point3D();
    public static final RotationMatrix cameraAdjustedOrientation = new RotationMatrix();
    private static final RotationMatrix cameraShakeRotation = new RotationMatrix();
    public static final RotationMatrix cameraShakeProjectionRotation = new RotationMatrix();
    public static boolean adjustedCamera;
    public static boolean cameraShakeActive;
    public static boolean renderingFirstPersonBody;
    private static EnumSet<PlayerLimb> firstPersonRenderedLimbs = EnumSet.noneOf(PlayerLimb.class);
    private static Player mcPlayer;

    private static int lastScreenWidth;
    private static int lastScreenHeight;

    public static boolean shouldRenderFirstPersonLimb(PlayerLimb limb) {
        return firstPersonRenderedLimbs.contains(limb);
    }
    /**
     * Changes camera rotation to match custom rotation, and also gets custom position for custom cameras.
     */
    @SubscribeEvent
    public static void onIVCameraSetup(ComputeCameraAngles event) {
        cameraShakeActive = false;
        Camera camera = event.getCamera();
        if (camera.getEntity() instanceof Player) {
            mcPlayer = (Player) camera.getEntity();
            IWrapperPlayer player = WrapperPlayer.getWrapperFor(mcPlayer);
            cameraAdjustedPosition.set(0, 0, 0);
            cameraAdjustedOrientation.setToZero();
            float partialTicks = (float) event.getPartialTick();
            adjustedCamera = false;
            if (CameraSystem.adjustCamera(player, cameraAdjustedPosition, cameraAdjustedOrientation, partialTicks)) {
                //Camera adjustments occur backwards here.  Reverse order in the matrix.
                //Also need to reverse sign of Y, since that's backwards in MC.
                cameraAdjustedOrientation.convertToAngles();
                if (InterfaceManager.clientInterface.getCameraMode() == CameraMode.THIRD_PERSON_INVERTED) {
                    //Inverted third-person needs roll and pitch flipped due to the opposite perspective.
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
                ((CameraMixin) camera).invoke_setPosition(cameraAdjustedPosition.x, cameraAdjustedPosition.y, cameraAdjustedPosition.z);
                adjustedCamera = true;
            }
            cameraShakeRotation.setToZero();
            cameraShakeActive = CameraSystem.applyCameraShake(cameraShakeRotation);
            if (cameraShakeActive) {
                cameraShakeRotation.convertToAngles();
                if (InterfaceManager.clientInterface.getCameraMode() == CameraMode.THIRD_PERSON_INVERTED) {
                    event.setRoll((float) (event.getRoll() - cameraShakeRotation.angles.z));
                    event.setPitch((float) (event.getPitch() - cameraShakeRotation.angles.x));
                } else {
                    event.setRoll((float) (event.getRoll() + cameraShakeRotation.angles.z));
                    event.setPitch((float) (event.getPitch() + cameraShakeRotation.angles.x));
                }
                event.setYaw((float) (event.getYaw() - cameraShakeRotation.angles.y));
                cameraShakeProjectionRotation.angles.set(event.getPitch(), -event.getYaw(), event.getRoll());
                cameraShakeProjectionRotation.updateToAngles();
            }
        }
    }

    /**
     * Blocks all overlays that we don't want to render.
     */
    @SubscribeEvent
    public static void onIVPreLayer(RenderGuiOverlayEvent.Pre event) {
        //If we are rendering the custom camera overlay, block the crosshairs and the hotbar.
        if ((event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type() || event.getOverlay() == VanillaGuiOverlay.HOTBAR.type()) && CameraSystem.customCameraOverlay != null) {
            event.setCanceled(true);
            return;
        }

        //If we are seated in a controller seat, and are rendering GUIs, disable the hotbar.
        if ((event.getOverlay() == VanillaGuiOverlay.HOTBAR.type() || event.getOverlay() == VanillaGuiOverlay.FOOD_LEVEL.type() || event.getOverlay() == VanillaGuiOverlay.PLAYER_HEALTH.type() || event.getOverlay() == VanillaGuiOverlay.ARMOR_LEVEL.type() || event.getOverlay() == VanillaGuiOverlay.EXPERIENCE_BAR.type()) && (InterfaceManager.clientInterface.getCameraMode() == CameraMode.FIRST_PERSON ? ConfigSystem.client.renderingSettings.renderHUD_1P.value : ConfigSystem.client.renderingSettings.renderHUD_3P.value)) {
            IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();
            AEntityB_Existing ridingEntity = player.getEntityRiding();
            if (ridingEntity instanceof PartSeat && ((PartSeat) ridingEntity).placementDefinition.isController) {
                event.setCanceled(true);
                return;
            }
        }
    }

    /**
     * Renders all overlay things.  This is essentially anything that's a 2D render, such as the main overlay,
     * vehicle HUds, GUIs, camera overlays, etc.
     */
    @SubscribeEvent
    public static void onIVRenderOverlayChat(CustomizeGuiOverlayEvent.Chat event) {
        //Do overlay rendering before the chat window is rendered.
        //This renders them over the main hotbar, but doesn't block the chat window.
        Window window = Minecraft.getInstance().getWindow();
        long displaySize = InterfaceManager.clientInterface.getPackedDisplaySize();
        int screenWidth = (int) (displaySize >> Integer.SIZE);
        int screenHeight = (int) displaySize;
        double[] xPos = new double[1];
        double[] yPos = new double[1];
        GLFW.glfwGetCursorPos(window.getWindow(), xPos, yPos);
        int mouseX = (int) (xPos[0] * screenWidth / window.getScreenWidth());
        int mouseY = (int) (yPos[0] * screenHeight / window.getScreenHeight());

        float partialTicks = event.getPartialTick();
        boolean updateGUIs = screenWidth != lastScreenWidth || screenHeight != lastScreenHeight;
        if (updateGUIs) {
            lastScreenWidth = screenWidth;
            lastScreenHeight = screenHeight;
        }

        InterfaceRender.renderGUI(event.getGuiGraphics(), mouseX, mouseY, screenWidth, screenHeight, partialTicks, updateGUIs);
    }

    /**
     * A chunk arrived on the client: the dynamic light block grid reads its sections again.
     */
    @SubscribeEvent
    public static void onIVChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide()) {
            DynamicLightSystem.markColumn(event.getChunk().getPos().x, event.getChunk().getPos().z);
        }
    }

    /**
     * No name tag inside a dynamic light shadow tile.
     */
    @SubscribeEvent
    public static void onIVRenderNameTag(RenderNameTagEvent event) {
        if (DynamicLightShadowAtlas.isRendering()) {
            event.setResult(Event.Result.DENY);
        }
    }

    /**
     * Hand and arm render events.  We use these to disable rendering of the item in the player's hand
     * if they are holding a gun.  Not sure why there's two events, but we cancel them both!
     */
    @SubscribeEvent
    public static void onIVRenderHand(RenderHandEvent event) {
        EntityPlayerGun entity = EntityPlayerGun.playerClientGuns.get(Minecraft.getInstance().player.getUUID());
        WrapperPlayer playerWrapper = WrapperPlayer.getWrapperFor(Minecraft.getInstance().player);
        float partialTicks = Minecraft.getInstance().getFrameTime();
        boolean rightArm = event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND ? playerWrapper.isRightHanded() : !playerWrapper.isRightHanded();
        PlayerLimb handLimb = rightArm ? PlayerLimb.armRight : PlayerLimb.armLeft;
        if ((entity != null && entity.activeGun != null) || CameraSystem.activeCamera != null || PlayerAnimationSystem.getActiveLimbs(playerWrapper, partialTicks).contains(handLimb)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onIVRenderArm(RenderArmEvent event) {
        EntityPlayerGun entity = EntityPlayerGun.playerClientGuns.get(Minecraft.getInstance().player.getUUID());
        if ((entity != null && entity.activeGun != null) || CameraSystem.activeCamera != null) {
            event.setCanceled(true);
            return;
        }
        PlayerLimb limb = event.getArm() == HumanoidArm.LEFT ? PlayerLimb.armLeft : PlayerLimb.armRight;
        if (PlayerAnimationSystem.getActiveLimbs(WrapperPlayer.getWrapperFor(event.getPlayer()), Minecraft.getInstance().getFrameTime()).contains(limb)) {
            event.setCanceled(true);
        }
    }

    /**
     * Renders the local player through the normal world renderer while in first person, showing only the
     * limbs that vehicle animations control.  This keeps them in vehicle/world space rather than attaching
     * an arm to the camera HUD.  Held items, armor, and capes are left out by emptying the renderer's layer list.
     */
    @SubscribeEvent
    public static void onIVRenderFirstPersonBody(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || minecraft.player == null || minecraft.level == null || minecraft.options.getCameraType() != net.minecraft.client.CameraType.FIRST_PERSON || minecraft.getCameraEntity() != minecraft.player) {
            return;
        }

        float partialTicks = event.getPartialTick();
        EnumSet<PlayerLimb> activeLimbs = PlayerAnimationSystem.getActiveLimbs(WrapperPlayer.getWrapperFor(minecraft.player), partialTicks);
        if (activeLimbs.isEmpty()) {
            return;
        }

        firstPersonRenderedLimbs = activeLimbs;
        Vec3 cameraPosition = event.getCamera().getPosition();
        double playerX = Mth.lerp(partialTicks, minecraft.player.xOld, minecraft.player.getX());
        double playerY = Mth.lerp(partialTicks, minecraft.player.yOld, minecraft.player.getY());
        double playerZ = Mth.lerp(partialTicks, minecraft.player.zOld, minecraft.player.getZ());
        float playerYaw = Mth.rotLerp(partialTicks, minecraft.player.yRotO, minecraft.player.getYRot());
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        LivingEntityRendererAccessor layerAccessor = (LivingEntityRendererAccessor) (Object) minecraft.getEntityRenderDispatcher().getRenderer(minecraft.player);
        List<?> renderedLayers = layerAccessor.get_layers();

        renderingFirstPersonBody = true;
        layerAccessor.set_layers(Collections.emptyList());
        minecraft.getEntityRenderDispatcher().setRenderShadow(false);
        try {
            minecraft.getEntityRenderDispatcher().render(minecraft.player, playerX - cameraPosition.x, playerY - cameraPosition.y, playerZ - cameraPosition.z, playerYaw, partialTicks, event.getPoseStack(), buffer, minecraft.getEntityRenderDispatcher().getPackedLightCoords(minecraft.player, partialTicks));
            buffer.endBatch();
        } finally {
            layerAccessor.set_layers(renderedLayers);
            minecraft.getEntityRenderDispatcher().setRenderShadow(true);
            renderingFirstPersonBody = false;
            firstPersonRenderedLimbs = EnumSet.noneOf(PlayerLimb.class);
        }
    }
}
