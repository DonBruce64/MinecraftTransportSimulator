package minecrafttransportsimulator.systems;

import java.util.Locale;

import minecrafttransportsimulator.baseclasses.AnimationSwitchbox;
import minecrafttransportsimulator.baseclasses.BlockHitResult;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.BoundingBoxHitResult;
import minecrafttransportsimulator.baseclasses.EntityInteractResult;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.jsondefs.JSONCameraObject;
import minecrafttransportsimulator.jsondefs.JSONCollisionGroup.CollisionType;
import minecrafttransportsimulator.jsondefs.JSONPotionEffect;
import minecrafttransportsimulator.jsondefs.JSONPotionEffect.PotionDefaults;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
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

    private static final double GROUND_VEHICLE_CAMERA_Y_OFFSET = 4.5D;
    private static final double CAMERA_COLLISION_PADDING = 0.25D;
    private static final Point3D cameraOffset = new Point3D();
    private static final Point3D cameraCollisionStart = new Point3D();
    private static final Point3D cameraCollisionVector = new Point3D();
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
                applyCameraCollision(player, cameraOffset, cameraAdjustedPosition, getMultipartToIgnore(cameraProvider, sittingSeat));

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
            CameraMode cameraMode = InterfaceManager.clientInterface.getCameraMode();
            if (MouseFlightController.isMouseFlightActive) {
                MouseFlightController.getInterpolatedCameraOrientation(cameraRotation, partialTicks);
            } else if (ConfigSystem.client.renderingSettings.freecam_3P.value && cameraMode.thirdPerson) {
                sittingSeat.getRiderInterpolatedOrientation(cameraRotation, partialTicks);
            } else {
                sittingSeat.getInterpolatedOrientation(cameraRotation, partialTicks);
                sittingSeat.getRiderInterpolatedOrientation(riderOrientation, partialTicks);
                cameraRotation.multiply(riderOrientation);
            }

            if (cameraMode == CameraMode.FIRST_PERSON) {
                //First person: use the standard rider eye position without any offset.
                cameraAdjustedPosition.set(sittingSeat.prevRiderCameraPosition).interpolate(sittingSeat.riderCameraPosition, partialTicks);
            } else {
                if (MouseFlightController.isMouseFlightActive || sittingSeat.vehicleOn == null) {
                    //Mouse flight uses a seat-relative stable point so the camera follows the aim controller.
                    cameraAdjustedPosition.set(sittingSeat.prevPosition).interpolate(sittingSeat.position, partialTicks);
                    cameraAdjustedPosition.y += sittingSeat.rider.getEyeHeight() + sittingSeat.rider.getSeatOffset() + 2.5;
                } else {
                    //Standard 3P view orbits around the vehicle reference point instead of the
                    //rider eye point, which moves around the aircraft as vehicle orientation changes.
                    cameraAdjustedPosition.set(sittingSeat.vehicleOn.prevPosition).interpolate(sittingSeat.vehicleOn.position, partialTicks);
                    if (sittingSeat.vehicleOn.definition.motorized != null && !sittingSeat.vehicleOn.definition.motorized.isAircraft) {
                        cameraAdjustedPosition.y += GROUND_VEHICLE_CAMERA_Y_OFFSET;
                    }
                }

                cameraCollisionStart.set(cameraAdjustedPosition);
                int cameraZoomRequired = 4 - InterfaceManager.clientInterface.getCameraDefaultZoom() + sittingSeat.zoomLevel;
                cameraOffset.set(0, 0, cameraMode == CameraMode.THIRD_PERSON ? -cameraZoomRequired : cameraZoomRequired).rotate(cameraRotation);
                cameraAdjustedPosition.add(cameraOffset);
                applyCameraCollision(player, cameraCollisionStart, cameraAdjustedPosition, sittingSeat.vehicleOn);
            }
            return true;
        } else {
            //Not doing any camera changes.
            return false;
        }
    }

    private static AEntityF_Multipart<?> getMultipartToIgnore(AEntityB_Existing cameraProvider, PartSeat sittingSeat) {
        if (sittingSeat != null && sittingSeat.vehicleOn != null) {
            return sittingSeat.vehicleOn;
        } else if (cameraProvider instanceof APart) {
            return ((APart) cameraProvider).masterEntity;
        } else if (cameraProvider instanceof AEntityF_Multipart) {
            return (AEntityF_Multipart<?>) cameraProvider;
        } else {
            return null;
        }
    }

    private static void applyCameraCollision(IWrapperPlayer player, Point3D startPoint, Point3D cameraAdjustedPosition, AEntityF_Multipart<?> multipartToIgnore) {
        cameraCollisionVector.set(cameraAdjustedPosition).subtract(startPoint);
        double desiredDistance = cameraCollisionVector.length();
        if (desiredDistance < 0.001D) {
            return;
        }

        AWrapperWorld world = player.getWorld();
        double closestDistance = desiredDistance;

        BlockHitResult blockHit = world.getBlockHit(startPoint, cameraCollisionVector);
        if (blockHit != null) {
            closestDistance = Math.min(closestDistance, startPoint.distanceTo(blockHit.hitPosition));
        }

        EntityInteractResult multipartHit = world.getMultipartEntityIntersect(startPoint, cameraAdjustedPosition, multipartToIgnore, CollisionType.ENTITY, CollisionType.VEHICLE);
        if (multipartHit != null) {
            closestDistance = Math.min(closestDistance, startPoint.distanceTo(multipartHit.position));
        }

        BoundingBox cameraVectorBounds = new BoundingBox(startPoint, cameraAdjustedPosition);
        for (IWrapperEntity entity : world.getEntitiesWithin(cameraVectorBounds)) {
            if (entity.getID().equals(player.getID())) {
                continue;
            }
            BoundingBoxHitResult entityHit = entity.getBounds().getIntersection(startPoint, cameraAdjustedPosition);
            if (entityHit != null) {
                closestDistance = Math.min(closestDistance, startPoint.distanceTo(entityHit.position));
            }
        }

        if (closestDistance < desiredDistance) {
            cameraAdjustedPosition.set(startPoint).add(cameraCollisionVector.normalize().scale(Math.max(0, closestDistance - CAMERA_COLLISION_PADDING)));
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

