package mcinterface1211;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.Window;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.systems.CameraSystem;
import minecrafttransportsimulator.systems.CameraSystem.CameraMode;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Interface for handling events pertaining to entity rendering.  This modifies the player's rendered state
 * to handle them being in vehicles, as well as ensuring their model adapts to any objects they may be holding.
 * This also handles the 2D GUI rendering.
 *
 * @author don_bruce
 */
@EventBusSubscriber(modid = InterfaceLoader.MODID, value = Dist.CLIENT)
public class InterfaceEventsEntityRendering {
    public static final Point3D cameraAdjustedPosition = new Point3D();
    public static final RotationMatrix cameraAdjustedOrientation = new RotationMatrix();
    public static boolean adjustedCamera;
    private static Player mcPlayer;

    private static int lastScreenWidth;
    private static int lastScreenHeight;
    /**
     * Changes camera rotation to match custom rotation, and also gets custom position for custom cameras.
     */
    @SubscribeEvent
    public static void onIVCameraSetup(ComputeCameraAngles event) {
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
                //In MC 1.21, Camera.setup() applies its own thirdPersonReverse logic
                //(yaw+180, -pitch, -roll) AFTER this event returns, so we must not
                //pre-invert for THIRD_PERSON_INVERTED — MC handles that itself.
                event.setRoll((float) cameraAdjustedOrientation.angles.z);
                event.setPitch((float) cameraAdjustedOrientation.angles.x);
                event.setYaw((float) -cameraAdjustedOrientation.angles.y);
                //Flag that we adjusted the camera.  The CameraMixin will re-apply
                //our position at the tail of Camera.setup(), since in MC 1.21
                //setup() overwrites the position after this event returns.
                adjustedCamera = true;
            }
        }
    }

    /**
     * Blocks all overlays that we don't want to render.
     */
    @SubscribeEvent
    public static void onIVPreLayer(RenderGuiLayerEvent.Pre event) {
        //If we are rendering the custom camera overlay, block the crosshairs and the hotbar.
        if ((event.getName().equals(VanillaGuiLayers.CROSSHAIR) || event.getName().equals(VanillaGuiLayers.HOTBAR)) && CameraSystem.customCameraOverlay != null) {
            event.setCanceled(true);
            return;
        }

        //If we are seated in a controller seat, and are rendering GUIs, disable the hotbar.
        if ((event.getName().equals(VanillaGuiLayers.HOTBAR) || event.getName().equals(VanillaGuiLayers.FOOD_LEVEL) || event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH) || event.getName().equals(VanillaGuiLayers.ARMOR_LEVEL) || event.getName().equals(VanillaGuiLayers.EXPERIENCE_BAR)) && (InterfaceManager.clientInterface.getCameraMode() == CameraMode.FIRST_PERSON ? ConfigSystem.client.renderingSettings.renderHUD_1P.value : ConfigSystem.client.renderingSettings.renderHUD_3P.value)) {
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
    public static void onIVRenderOverlayPost(RenderGuiLayerEvent.Post event) {
        //Do overlay rendering after the CHAT layer is rendered.
        //This renders the HUD over the main hotbar and doesn't block the chat window.
        //In NeoForge 1.21.1, CustomizeGuiOverlayEvent.Chat no longer fires,
        //so we use RenderGuiLayerEvent.Post on CHAT instead.
        //Using Post ensures the event fires even if Pre was not cancelled.
        if (!event.getName().equals(VanillaGuiLayers.CHAT)) {
            return;
        }
        Window window = Minecraft.getInstance().getWindow();
        long displaySize = InterfaceManager.clientInterface.getPackedDisplaySize();
        int screenWidth = (int) (displaySize >> Integer.SIZE);
        int screenHeight = (int) displaySize;
        double[] xPos = new double[1];
        double[] yPos = new double[1];
        GLFW.glfwGetCursorPos(window.getWindow(), xPos, yPos);
        int mouseX = (int) (xPos[0] * screenWidth / window.getScreenWidth());
        int mouseY = (int) (yPos[0] * screenHeight / window.getScreenHeight());

        float partialTicks = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        boolean updateGUIs = screenWidth != lastScreenWidth || screenHeight != lastScreenHeight;
        if (updateGUIs) {
            lastScreenWidth = screenWidth;
            lastScreenHeight = screenHeight;
        }

        InterfaceRender.renderGUI(event.getGuiGraphics(), mouseX, mouseY, screenWidth, screenHeight, partialTicks, updateGUIs);
    }

    /**
     * Hand and arm render events.  We use these to disable rendering of the item in the player's hand
     * if they are holding a gun.  Not sure why there's two events, but we cancel them both!
     */
    @SubscribeEvent
    public static void onIVRenderHand(RenderHandEvent event) {
        EntityPlayerGun entity = EntityPlayerGun.playerClientGuns.get(Minecraft.getInstance().player.getUUID());
        if ((entity != null && entity.activeGun != null) || CameraSystem.activeCamera != null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onIVRenderArm(RenderArmEvent event) {
        EntityPlayerGun entity = EntityPlayerGun.playerClientGuns.get(Minecraft.getInstance().player.getUUID());
        if ((entity != null && entity.activeGun != null) || CameraSystem.activeCamera != null) {
            event.setCanceled(true);
        }
    }
}
