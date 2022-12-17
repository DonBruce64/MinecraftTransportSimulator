package mcinterface1122;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.systems.CameraSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Interface for handling events pertaining to camera orientation.  This class is responsible for handling
 * camera position and orientation operations.  The only camera functions this class does not handle is overlays,
 * as those are 2D rendering of textures, not 3D orientation of the camera itself.
 *
 * @author don_bruce
 */
@EventBusSubscriber(Side.CLIENT)
public class InterfaceEventsCamera {
    private static final RotationMatrix cameraAdjustedOrientation = new RotationMatrix();
    private static final Point3D cameraAdjustedPosition = new Point3D();
    protected static EntityPlayer fakeCameraPlayerEntity;
    private static Entity lastRenderEntity;

    @SubscribeEvent
    public static void on(CameraSetup event) {
        if (event.getEntity() instanceof EntityPlayer) {
            EntityPlayer mcPlayer = (EntityPlayer) event.getEntity();
            if (mcPlayer == fakeCameraPlayerEntity) {
                mcPlayer = (EntityPlayer) lastRenderEntity;
            }
            WrapperPlayer player = WrapperPlayer.getWrapperFor(mcPlayer);
            cameraAdjustedPosition.set(0, 0, 0);
            cameraAdjustedOrientation.setToZero();
            if (CameraSystem.adjustCamera(player, cameraAdjustedPosition, cameraAdjustedOrientation, (float) event.getRenderPartialTicks())) {

                //If we are running a custom camera, adjust the rendering player position.
                //We create a fake entity for this and align it to our player->custom camera position.
                if (lastRenderEntity == null || fakeCameraPlayerEntity.world != event.getEntity().world) {
                    fakeCameraPlayerEntity = new EntityPlayer(event.getEntity().world, mcPlayer.getGameProfile()) {

                        @Override
                        public boolean isSpectator() {
                            return true;
                        }

                        @Override
                        public boolean isCreative() {
                            return false;
                        }

                        @Override
                        public boolean isUser() {
                            return false;
                        }
                    };
                    fakeCameraPlayerEntity.noClip = true;
                    lastRenderEntity = Minecraft.getMinecraft().getRenderViewEntity();
                    Minecraft.getMinecraft().setRenderViewEntity(fakeCameraPlayerEntity);
                }

                //Set fake player position to real player.
                fakeCameraPlayerEntity.posX = mcPlayer.posX + cameraAdjustedPosition.x;
                fakeCameraPlayerEntity.posY = mcPlayer.posY + cameraAdjustedPosition.y;
                fakeCameraPlayerEntity.posZ = mcPlayer.posZ + cameraAdjustedPosition.z;
                fakeCameraPlayerEntity.prevPosX = mcPlayer.prevPosX + cameraAdjustedPosition.x;
                fakeCameraPlayerEntity.prevPosY = mcPlayer.prevPosY + cameraAdjustedPosition.y;
                fakeCameraPlayerEntity.prevPosZ = mcPlayer.prevPosZ + cameraAdjustedPosition.z;
                fakeCameraPlayerEntity.lastTickPosX = mcPlayer.lastTickPosX + cameraAdjustedPosition.x;
                fakeCameraPlayerEntity.lastTickPosY = mcPlayer.lastTickPosY + cameraAdjustedPosition.y;
                fakeCameraPlayerEntity.lastTickPosZ = mcPlayer.lastTickPosZ + cameraAdjustedPosition.z;

                fakeCameraPlayerEntity.cameraYaw = mcPlayer.cameraYaw;
                fakeCameraPlayerEntity.prevCameraYaw = mcPlayer.prevCameraYaw;
                fakeCameraPlayerEntity.renderYawOffset = mcPlayer.renderYawOffset;
                fakeCameraPlayerEntity.prevRenderYawOffset = mcPlayer.prevRenderYawOffset;
                fakeCameraPlayerEntity.rotationYawHead = mcPlayer.rotationYawHead;
                fakeCameraPlayerEntity.prevRotationYawHead = mcPlayer.prevRotationYawHead;
                fakeCameraPlayerEntity.rotationYaw = mcPlayer.rotationYaw;
                fakeCameraPlayerEntity.prevRotationYaw = mcPlayer.prevRotationYaw;
                fakeCameraPlayerEntity.rotationPitch = mcPlayer.rotationPitch;
                fakeCameraPlayerEntity.prevRotationPitch = mcPlayer.prevRotationPitch;

                //Now apply orientation changes.  This ensures that only the camera's view angle changes.
                //Since the "player" moves, however, it will allow chunks to render in that area.
                cameraAdjustedOrientation.convertToAngles();

                //Rotate by 180 to get the forwards-facing orientation; MC does everything backwards.
                event.setYaw((float) -cameraAdjustedOrientation.angles.y + 180);
                event.setPitch((float) cameraAdjustedOrientation.angles.x);
                event.setRoll((float) cameraAdjustedOrientation.angles.z);
            } else if (lastRenderEntity != null) {
                Minecraft.getMinecraft().setRenderViewEntity(lastRenderEntity);
                lastRenderEntity = null;
                fakeCameraPlayerEntity = null;
            }
        }
    }

    /**
     * Pre-post methods for tricking MC into rendering the player, even if we are running a custom render entity.
     * We do this by setting back the view entity for the render call.  Do this for all cases, except standard first-person.
     */
    @SubscribeEvent
    public static void on(RenderPlayerEvent.Pre event) {
        if (fakeCameraPlayerEntity != null && (!InterfaceManager.clientInterface.inFirstPerson() || CameraSystem.runningCustomCameras)) {
            Minecraft.getMinecraft().getRenderManager().renderViewEntity = lastRenderEntity;
        }
    }

    @SubscribeEvent
    public static void on(RenderPlayerEvent.Post event) {
        if (fakeCameraPlayerEntity != null && (!InterfaceManager.clientInterface.inFirstPerson() || CameraSystem.runningCustomCameras)) {
            Minecraft.getMinecraft().getRenderManager().renderViewEntity = fakeCameraPlayerEntity;
        }
    }
}
