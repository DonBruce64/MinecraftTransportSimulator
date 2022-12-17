package mcinterface1165;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.systems.CameraSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Interface for handling events pertaining to camera orientation.  This class is responsible for handling
 * camera position and orientation operations.  The only camera functions this class does not handle is overlays,
 * as those are 2D rendering of textures, not 3D orientation of the camera itself.
 *
 * @author don_bruce
 */
@EventBusSubscriber(Dist.CLIENT)
public class InterfaceEventsCamera {
    private static final RotationMatrix cameraAdjustedOrientation = new RotationMatrix();
    private static final Point3D cameraAdjustedPosition = new Point3D();
    protected static PlayerEntity fakeCameraPlayerEntity;
    private static Entity lastRenderEntity;

    @SubscribeEvent
    public static void on(CameraSetup event) {
        if (event.getInfo().getEntity() instanceof PlayerEntity) {
            PlayerEntity mcPlayer = (PlayerEntity) event.getInfo().getEntity();
            if (mcPlayer == fakeCameraPlayerEntity) {
                mcPlayer = (PlayerEntity) lastRenderEntity;
            }
            WrapperPlayer player = WrapperPlayer.getWrapperFor(mcPlayer);
            cameraAdjustedPosition.set(0, 0, 0);
            cameraAdjustedOrientation.setToZero();
            if (CameraSystem.adjustCamera(player, cameraAdjustedPosition, cameraAdjustedOrientation, (float) event.getRenderPartialTicks())) {

                //If we are running a custom camera, adjust the rendering player position.
                //We create a fake entity for this and align it to our player->custom camera position.
                if (lastRenderEntity == null || fakeCameraPlayerEntity.level != event.getInfo().getEntity().level) {
                    fakeCameraPlayerEntity = new PlayerEntity(event.getInfo().getEntity().level, mcPlayer.blockPosition(), mcPlayer.yRot, mcPlayer.getGameProfile()) {

                        @Override
                        public boolean isSpectator() {
                            return true;
                        }

                        @Override
                        public boolean isCreative() {
                            return false;
                        }

                        @Override
                        public boolean isLocalPlayer() {
                            return false;
                        }
                    };
                    fakeCameraPlayerEntity.noPhysics = true;
                    lastRenderEntity = Minecraft.getInstance().getCameraEntity();
                    Minecraft.getInstance().setCameraEntity(fakeCameraPlayerEntity);
                }

                //Set fake player position to real player.
                fakeCameraPlayerEntity.setPosAndOldPos(cameraAdjustedPosition.x, cameraAdjustedPosition.y, cameraAdjustedPosition.z);

                //FIXME see if we need any of these, can't find them in the code, so maybe we don't?
                /*
                fakeCameraPlayerEntity.lastTickPosX = mcPlayer.lastTickPosX + cameraAdjustedPosition.x;
                fakeCameraPlayerEntity.lastTickPosY = mcPlayer.lastTickPosY + cameraAdjustedPosition.y;
                fakeCameraPlayerEntity.lastTickPosZ = mcPlayer.lastTickPosZ + cameraAdjustedPosition.z;
                fakeCameraPlayerEntity.cameraYaw = mcPlayer.cameraYaw;
                fakeCameraPlayerEntity.prevCameraYaw = mcPlayer.prevCameraYaw;
                */

                fakeCameraPlayerEntity.yBodyRot = mcPlayer.yBodyRot;
                fakeCameraPlayerEntity.yBodyRotO = mcPlayer.yBodyRotO;
                fakeCameraPlayerEntity.yHeadRot = mcPlayer.yHeadRot;
                fakeCameraPlayerEntity.yHeadRotO = mcPlayer.yHeadRotO;
                fakeCameraPlayerEntity.yRot = mcPlayer.yRot;
                fakeCameraPlayerEntity.yRotO = mcPlayer.yRotO;
                fakeCameraPlayerEntity.xRot = mcPlayer.xRot;
                fakeCameraPlayerEntity.xRotO = mcPlayer.xRotO;

                //Now apply orientation changes.  This ensures that only the camera's view angle changes.
                //Since the "player" moves, however, it will allow chunks to render in that area.
                cameraAdjustedOrientation.convertToAngles();

                //Rotate by 180 to get the forwards-facing orientation; MC does everything backwards.
                event.setYaw((float) -cameraAdjustedOrientation.angles.y + 180);
                event.setPitch((float) cameraAdjustedOrientation.angles.x);
                event.setRoll((float) cameraAdjustedOrientation.angles.z);
            } else if (lastRenderEntity != null) {
                Minecraft.getInstance().setCameraEntity(lastRenderEntity);
                lastRenderEntity = null;
                fakeCameraPlayerEntity = null;
            }
        }
    }

    /**
     * Pre-post methods for tricking MC into rendering the player, even if we are running a custom render entity.
     * We do this by setting back the view entity for the render call.  Do this for all cases, except standard first-person.
     */
    //FIXME suspect this doesn't work here, as the same render logic ain't around on the other side.
    @SubscribeEvent
    public static void on(RenderPlayerEvent.Pre event) {
        if (fakeCameraPlayerEntity != null && (!InterfaceManager.clientInterface.inFirstPerson() || CameraSystem.runningCustomCameras)) {
            Minecraft.getInstance().cameraEntity = lastRenderEntity;
        }
    }

    @SubscribeEvent
    public static void on(RenderPlayerEvent.Post event) {
        if (fakeCameraPlayerEntity != null && (!InterfaceManager.clientInterface.inFirstPerson() || CameraSystem.runningCustomCameras)) {
            Minecraft.getInstance().cameraEntity = fakeCameraPlayerEntity;
        }
    }
}
