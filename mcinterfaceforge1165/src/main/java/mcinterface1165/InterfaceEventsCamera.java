package mcinterface1165;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.systems.CameraSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
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
    private static final TransformationMatrix cameraAdjustedOrientation = new TransformationMatrix();
    private static final Point3D cameraAdjustedPosition = new Point3D();
    private static EntityPlayer fakeCameraPlayerEntity;
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
            cameraAdjustedOrientation.resetTransforms();
            if (CameraSystem.adjustCamera(player, cameraAdjustedPosition, cameraAdjustedOrientation, (float) event.getRenderPartialTicks())) {

                //If we are running a custom camera, adjust the actual player position.
                if (CameraSystem.runningCustomCameras) {
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

                    //Global settings.  Rotate by 180 to get the forwards-facing orientation; MC does everything backwards.
                    GL11.glRotated(180, 0, 1, 0);

                    //Now apply our actual offsets.  Need to invert them as this is camera position, not object position.
                    InterfaceManager.renderingInterface.applyTransformOpenGL(cameraAdjustedOrientation, true);
                } else {
                    GL11.glRotated(180, 0, 1, 0);
                    InterfaceManager.renderingInterface.applyTransformOpenGL(cameraAdjustedOrientation, true);
                    
                    cameraAdjustedPosition.invert();
                    GL11.glTranslated(cameraAdjustedPosition.x, cameraAdjustedPosition.y, cameraAdjustedPosition.z);
                }
                event.setPitch(0);
                event.setYaw(0);
                event.setRoll(0);
            } else if (lastRenderEntity != null) {
                Minecraft.getMinecraft().setRenderViewEntity(lastRenderEntity);
                lastRenderEntity = null;
            }
        }
    }
}
