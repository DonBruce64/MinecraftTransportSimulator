package minecrafttransportsimulator.systems;

import minecrafttransportsimulator.baseclasses.AnimationSwitchbox;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.jsondefs.JSONCameraObject;
import minecrafttransportsimulator.jsondefs.JSONPotionEffect;
import minecrafttransportsimulator.jsondefs.JSONPotionEffect.PotionDefaults;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;

/**
 * System for handling camera zoom, position, and overlays.  Note that actual overlay
 * rendering is left up to the interface: this class only maintains which overlay
 * needs to be rendered, if any.
 *
 * @author don_bruce
 */
public class CameraSystem {

    public static boolean runningCustomCameras;
    private static boolean nightVisionEnabled;
    private static float currentFOV;
    private static float currentMouseSensitivity;
    public static String customCameraOverlay;

    private static final Point3D cameraOffset = new Point3D();
    private static final RotationMatrix riderOrientation = new RotationMatrix();

    private static final JSONPotionEffect NIGHT_VISION_CAMERA_POTION = new JSONPotionEffect();

    static {
        NIGHT_VISION_CAMERA_POTION.duration = 300;
        NIGHT_VISION_CAMERA_POTION.name = PotionDefaults.NIGHT_VISION.name().toLowerCase();
    }

    /**
     * Call to have the camera position and rotation set to whatever the camera system required.
     * Passed-in position and rotation should be zero, and the camera will be set to the desired position
     * and rotation, or left at zero if no transforms are required.  The important thing is
     * that after calling this method, no other camera modification operations are performed
     * and the camera is set to the position and rotation that were passed in.  We may or may not
     * specify an overlay for {@link #customCameraOverlay}.  Return true if we adjusted the camera.
     */
    public static boolean adjustCamera(IWrapperPlayer player, Point3D cameraAdjustedPosition, RotationMatrix cameraRotation, float partialTicks) {
        //Get camera.
        AEntityB_Existing ridingEntity = player.getEntityRiding();
        PartSeat sittingSeat = ridingEntity instanceof PartSeat ? (PartSeat) ridingEntity : null;
        EntityPlayerGun playerGunEntity = EntityPlayerGun.playerClientGuns.get(player.getID());
        AEntityB_Existing cameraProvider = sittingSeat != null ? sittingSeat : playerGunEntity;

        //Reset FOV, sensitivity, overlay, and effect.
        if (currentFOV != 0) {
            InterfaceManager.clientInterface.setFOV(currentFOV);
            currentFOV = 0;
        }
        if (currentMouseSensitivity != 0) {
            InterfaceManager.clientInterface.setMouseSensitivity(currentMouseSensitivity);
            currentMouseSensitivity = 0;
        }
        if (nightVisionEnabled) {
            player.removePotionEffect(NIGHT_VISION_CAMERA_POTION);
            nightVisionEnabled = false;
        }
        customCameraOverlay = null;
        runningCustomCameras = false;

        //Do custom camera operations, if we have one.
        if (InterfaceManager.clientInterface.inFirstPerson() && cameraProvider != null) {
            JSONCameraObject camera = cameraProvider.activeCamera;
            if (camera != null) {
                AnimationSwitchbox switchbox = cameraProvider.activeCameraSwitchbox;

                //Set current overlay for future calls.
                customCameraOverlay = camera.overlay != null ? camera.overlay + ".png" : null;

                //If the camera has an FOV override, apply it.
                if (camera.fovOverride != 0) {
                    if (currentFOV == 0) {
                        currentFOV = InterfaceManager.clientInterface.getFOV();
                    }
                    InterfaceManager.clientInterface.setFOV(camera.fovOverride);
                }

                //If the camera has a mouse sensitivity override, apply it.
                if (camera.mouseSensitivityOverride != 0) {
                    if (currentMouseSensitivity == 0) {
                        currentMouseSensitivity = InterfaceManager.clientInterface.getMouseSensitivity();
                    }
                    InterfaceManager.clientInterface.setMouseSensitivity(camera.mouseSensitivityOverride);
                }

                //First set the position of the camera to the defined position.
                cameraAdjustedPosition.set(camera.pos);

                //Now run transforms on this position to get it's proper position.
                if (switchbox != null) {
                    switchbox.runSwitchbox(partialTicks, false);
                    cameraAdjustedPosition.transform(switchbox.netMatrix);
                }

                //Get the rotational component of the operation.
                //First, get the orientation of the entity we are on.
                cameraProvider.activeCameraEntity.getInterpolatedOrientation(cameraRotation, partialTicks);

                //We need to rotate the camera position by our orientation here.
                //This puts the position into global orientation rather than animation-local.
                cameraAdjustedPosition.rotate(cameraRotation);

                //Now add the rotation from the animation, plus the definition rotation, if we have it.
                if (switchbox != null) {
                    cameraRotation.multiply(switchbox.rotation);
                }
                if (camera.rot != null) {
                    cameraRotation.multiply(camera.rot);
                }

                //Rotational portion is good.  Finally, add the position of the provider.
                //This needs to be interpolated to ensure smooth movement on partial ticks.
                cameraOffset.set(cameraProvider.activeCameraEntity.prevPosition).interpolate(cameraProvider.activeCameraEntity.position, partialTicks);
                cameraAdjustedPosition.add(cameraOffset);

                //Also check night vision.
                if (camera.nightVision) {
                    player.addPotionEffect(NIGHT_VISION_CAMERA_POTION);
                    nightVisionEnabled = true;
                }

                runningCustomCameras = true;
                return true;
            }
        }

        //No custom cameras, check if we are sitting in a seat to adjust orientation.
        if (sittingSeat != null) {
            cameraAdjustedPosition.set(sittingSeat.prevRiderCameraPosition).interpolate(sittingSeat.riderCameraPosition, partialTicks);
            sittingSeat.getInterpolatedOrientation(cameraRotation, partialTicks);
            sittingSeat.getRiderInterpolatedOrientation(riderOrientation, partialTicks);
            cameraRotation.multiply(riderOrientation);
            return true;
        } else {
            //Not doing any camera changes.
            return false;
        }
    }
}
