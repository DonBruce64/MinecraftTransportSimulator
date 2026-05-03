package minecrafttransportsimulator.systems;

import java.util.Locale;

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

    public static JSONCameraObject activeCamera;
    private static boolean nightVisionEnabled;
    private static float currentFOV;
    private static float currentMouseSensitivity;
    public static String customCameraOverlay;

    private static final Point3D cameraOffset = new Point3D();
    private static final RotationMatrix riderOrientation = new RotationMatrix();

    private static final JSONPotionEffect NIGHT_VISION_CAMERA_POTION = new JSONPotionEffect();

    static {
        NIGHT_VISION_CAMERA_POTION.duration = 300;
        NIGHT_VISION_CAMERA_POTION.name = PotionDefaults.NIGHT_VISION.name().toLowerCase(Locale.ROOT);
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
        resetCameraProperties();
        if (nightVisionEnabled) {
            player.removePotionEffect(NIGHT_VISION_CAMERA_POTION);
            nightVisionEnabled = false;
        }
        customCameraOverlay = null;
        activeCamera = null;

        //Do custom camera operations, if we have one.
        if (cameraProvider != null) {
            activeCamera = cameraProvider.activeCamera;
            if (activeCamera != null) {
                AnimationSwitchbox switchbox = cameraProvider.activeCameraSwitchbox;

                //Set current overlay for future calls.
                customCameraOverlay = activeCamera.overlay != null ? activeCamera.overlay + ".png" : null;

                //If the camera has an FOV override, apply it.
                if (activeCamera.fovOverride != 0) {
                    if (currentFOV == 0) {
                        currentFOV = InterfaceManager.clientInterface.getFOV();
                    }
                    InterfaceManager.clientInterface.setFOV(activeCamera.fovOverride);
                }

                //If the camera has a mouse sensitivity override, apply it.
                if (activeCamera.mouseSensitivityOverride != 0) {
                    if (currentMouseSensitivity == 0) {
                        currentMouseSensitivity = InterfaceManager.clientInterface.getMouseSensitivity();
                    }
                    InterfaceManager.clientInterface.setMouseSensitivity(activeCamera.mouseSensitivityOverride);
                }

                //First set the position of the camera to the defined position.
                cameraAdjustedPosition.set(activeCamera.pos);

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
                if (activeCamera.rot != null) {
                    cameraRotation.multiply(activeCamera.rot);
                }

                //Rotational portion is good.  Finally, add the position of the provider.
                //This needs to be interpolated to ensure smooth movement on partial ticks.
                cameraOffset.set(cameraProvider.activeCameraEntity.prevPosition).interpolate(cameraProvider.activeCameraEntity.position, partialTicks);
                cameraAdjustedPosition.add(cameraOffset);

                //Also check night vision.
                if (activeCamera.nightVision) {
                    player.addPotionEffect(NIGHT_VISION_CAMERA_POTION);
                    nightVisionEnabled = true;
                }
                return true;
            }
        }

        //No custom cameras, check if we are sitting in a seat to adjust orientation.
        if (sittingSeat != null) {
            if (MouseFlightController.isMouseFlightActive) {
                MouseFlightController.getInterpolatedCameraOrientation(cameraRotation, partialTicks);
                CameraMode cameraMode = InterfaceManager.clientInterface.getCameraMode();
                if (cameraMode == CameraMode.FIRST_PERSON) {
                    //First person: use the standard rider eye position without any offset.
                    cameraAdjustedPosition.set(sittingSeat.prevRiderCameraPosition).interpolate(sittingSeat.riderCameraPosition, partialTicks);
                } else {
                    //Third person: use a stable camera position based on the seat's world position
                    //plus a fixed vertical offset.  Avoids X/Z drift when riderEyePosition rotates
                    //with the aircraft orientation.
                    cameraAdjustedPosition.set(sittingSeat.prevPosition).interpolate(sittingSeat.position, partialTicks);
                    cameraAdjustedPosition.y += sittingSeat.rider.getEyeHeight() + sittingSeat.rider.getSeatOffset() + 0.5;
                    int cameraZoomRequired = 4 - InterfaceManager.clientInterface.getCameraDefaultZoom() + sittingSeat.zoomLevel;
                    cameraOffset.set(0, 0, cameraMode == CameraMode.THIRD_PERSON ? -cameraZoomRequired : cameraZoomRequired).rotate(cameraRotation);
                    cameraAdjustedPosition.add(cameraOffset);
                }
            } else {
                cameraAdjustedPosition.set(sittingSeat.prevRiderCameraPosition).interpolate(sittingSeat.riderCameraPosition, partialTicks);

                //In third-person view on a vehicle, orbit the camera around the vehicle's model
                //reference point rather than around the player's eye position inside the vehicle body.
                //The zoom-back vector is preserved: (riderCameraPosition - riderHeadPosition).
                if (sittingSeat.vehicleOn != null && InterfaceManager.clientInterface.getCameraMode() != CameraMode.FIRST_PERSON) {
                    cameraOffset.set(sittingSeat.vehicleOn.prevPosition).interpolate(sittingSeat.vehicleOn.position, partialTicks);
                    cameraAdjustedPosition.set(
                            cameraOffset.x + sittingSeat.riderCameraPosition.x - sittingSeat.riderHeadPosition.x,
                            cameraOffset.y + sittingSeat.riderCameraPosition.y - sittingSeat.riderHeadPosition.y,
                            cameraOffset.z + sittingSeat.riderCameraPosition.z - sittingSeat.riderHeadPosition.z);
                }

                sittingSeat.getInterpolatedOrientation(cameraRotation, partialTicks);
                sittingSeat.getRiderInterpolatedOrientation(riderOrientation, partialTicks);
                cameraRotation.multiply(riderOrientation);
            }
            return true;
        } else {
            //Not doing any camera changes.
            return false;
        }
    }
    
    public static void resetCameraProperties() {
        if (currentFOV != 0) {
            InterfaceManager.clientInterface.setFOV(currentFOV);
            currentFOV = 0;
        }
        if (currentMouseSensitivity != 0) {
            InterfaceManager.clientInterface.setMouseSensitivity(currentMouseSensitivity);
            currentMouseSensitivity = 0;
        }
    }

    public static enum CameraMode{
    	FIRST_PERSON(false),
    	THIRD_PERSON(true),
    	THIRD_PERSON_INVERTED(true);
    	
    	public final boolean thirdPerson;
    	
    	private CameraMode(boolean thirdPerson) {
    		this.thirdPerson = thirdPerson;
    	}
    }
}

