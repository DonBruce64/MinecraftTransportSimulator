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
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.jsondefs.JSONCameraObject;
import minecrafttransportsimulator.jsondefs.JSONCollisionGroup.CollisionType;
import minecrafttransportsimulator.jsondefs.JSONPotionEffect;
import minecrafttransportsimulator.jsondefs.JSONPotionEffect.PotionDefaults;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketCameraShake;

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

    private static final double CAMERA_COLLISION_PADDING = 0.25D;
    private static final double CAMERA_SHAKE_MAX_DISTANCE = 128.0D;
    private static final double CAMERA_SHAKE_ANGLE_FACTOR = 0.1D;
    private static final double CAMERA_SHAKE_MIN_ANGLE = 0.01D;
    private static final double CAMERA_SHAKE_MAX_ANGLE = 10.0D;
    private static final int CAMERA_SHAKE_DURATION_TICKS = 10;
    private static final Point3D cameraOffset = new Point3D();
    private static final Point3D cameraEntityPosition = new Point3D();
    private static final Point3D cameraCollisionStart = new Point3D();
    private static final Point3D cameraCollisionVector = new Point3D();
    private static final RotationMatrix riderOrientation = new RotationMatrix();
    private static final Point3D riderCameraRelativeAngles = new Point3D();
    private static final RotationMatrix cameraOffsetOrientation = new RotationMatrix();
    private static double cameraShakeAmplitude;
    private static double cameraShakePhase;
    private static double cameraShakeTime;
    private static double cameraShakeIntensity = 1.0D;
    private static long cameraShakeStartTime;
    private static long cameraShakeEndTime;

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

                getCameraTransform(cameraProvider, cameraAdjustedPosition, cameraRotation, partialTicks);

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
            boolean freecamThirdPerson = ConfigSystem.client.renderingSettings.freecam_3P.value && cameraMode.thirdPerson;
            if (freecamThirdPerson) {
                sittingSeat.getRiderInterpolatedOrientation(cameraRotation, partialTicks);
            } else if (MouseFlightController.isMouseFlightActive && cameraMode.thirdPerson) {
                MouseFlightController.getInterpolatedCameraOrientation(cameraRotation, partialTicks);
            } else {
                sittingSeat.getInterpolatedOrientation(cameraRotation, partialTicks);
                sittingSeat.getRiderInterpolatedOrientation(riderOrientation, partialTicks);
                cameraRotation.multiply(riderOrientation);
            }

            if (cameraMode == CameraMode.FIRST_PERSON) {
                //First person: use the standard rider eye position without any offset.
                cameraAdjustedPosition.set(sittingSeat.prevRiderCameraPosition).interpolate(sittingSeat.riderCameraPosition, partialTicks);
            } else {
                if (sittingSeat.vehicleOn != null && sittingSeat.vehicleOn.definition.motorized != null && sittingSeat.vehicleOn.definition.motorized.cameraOffset != null) {
                    //Use the configured vehicle-relative camera anchor.
                    cameraAdjustedPosition.set(sittingSeat.vehicleOn.prevPosition).interpolate(sittingSeat.vehicleOn.position, partialTicks);
                    sittingSeat.vehicleOn.getInterpolatedOrientation(cameraOffsetOrientation, partialTicks);
                    cameraAdjustedPosition.add(cameraOffset.set(sittingSeat.vehicleOn.definition.motorized.cameraOffset).rotate(cameraOffsetOrientation));
                } else {
                    //Without a configured offset, retain the rider-relative camera anchor.
                    cameraAdjustedPosition.set(sittingSeat.prevPosition).interpolate(sittingSeat.position, partialTicks);
                    sittingSeat.getInterpolatedOrientation(cameraOffsetOrientation, partialTicks);
                    cameraAdjustedPosition.add(cameraOffset.set(0, (sittingSeat.rider.getEyeHeight() + sittingSeat.rider.getSeatOffset()) * sittingSeat.rider.getVerticalScale(), 0).rotate(cameraOffsetOrientation));
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

    /**
     * Sets the passed-in position and rotation to the world position and rotation of the provider's active camera.
     * This is where the player sees from when looking through it, so things aimed with the camera can use it too.
     * The provider must have an active camera.  A partialTicks of 1 gives where the camera is this tick.
     */
    public static void getCameraTransform(AEntityB_Existing cameraProvider, Point3D cameraPosition, RotationMatrix cameraRotation, float partialTicks) {
        AnimationSwitchbox switchbox = cameraProvider.activeCameraSwitchbox;
        JSONCameraObject camera = cameraProvider.activeCamera;

        //First set the position of the camera to the defined position.
        cameraPosition.set(camera.pos);

        //Now run transforms on this position to get it's proper position.
        if (switchbox != null) {
            switchbox.runSwitchbox(partialTicks, false);
            cameraPosition.transform(switchbox.netMatrix);
        }

        //Get the rotational component of the operation.
        //First, get the orientation of the entity we are on.
        cameraProvider.activeCameraEntity.getInterpolatedOrientation(cameraRotation, partialTicks);

        //We need to rotate the camera position by our orientation here.
        //This puts the position into global orientation rather than animation-local.
        cameraPosition.rotate(cameraRotation);

        //Now add the rotation from the animation, plus the definition rotation, if we have it.
        if (switchbox != null) {
            cameraRotation.multiply(switchbox.rotation);
        }
        if (camera.rot != null) {
            cameraRotation.multiply(camera.rot);
        }

        //Rotational portion is good.  Finally, add the position of the provider.
        //This needs to be interpolated to ensure smooth movement on partial ticks.
        //Custom cameras keep their defined position; collision clipping only applies to third-person views.
        cameraEntityPosition.set(cameraProvider.activeCameraEntity.prevPosition).interpolate(cameraProvider.activeCameraEntity.position, partialTicks);
        cameraPosition.add(cameraEntityPosition);
    }

    /**Applies the active camera's final rotation after rider input and entity movement have been updated.**/
    public static void adjustRiderOrientation(AEntityB_Existing cameraProvider, RotationMatrix riderRotation) {
        if (cameraProvider.activeCameraSwitchbox.updateRider(cameraProvider)) {
            riderRotation.set(cameraProvider.activeCameraEntity.orientation).multiply(cameraProvider.activeCameraSwitchbox.rotation);
            if (cameraProvider.activeCamera.rot != null) {
                riderRotation.multiply(cameraProvider.activeCamera.rot);
            }
            riderRotation.convertToAngles();
            riderCameraRelativeAngles.computeVectorAngles(riderRotation, cameraProvider.orientation);
            cameraProvider.riderRelativeOrientation.angles.set(riderCameraRelativeAngles);
            cameraProvider.riderRelativeOrientation.updateToAngles();
        }
        double yawDelta = cameraProvider.riderRelativeOrientation.angles.y - cameraProvider.prevRiderRelativeOrientation.angles.y;
        if (yawDelta > 180) {
            cameraProvider.prevRiderRelativeOrientation.angles.y += 360;
        } else if (yawDelta < -180) {
            cameraProvider.prevRiderRelativeOrientation.angles.y -= 360;
        }
        cameraProvider.riderCameraInputOrientation.updateToAngles();
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

    public static void sendExplosionCameraShake(AWrapperWorld world, Point3D source, double strength, double radius, int duration, double intensity) {
        sendCameraShake(world, source, strength, ConfigSystem.settings.damage.cameraShakeFactor.value, radius, duration, intensity);
    }

    public static void sendGunshotCameraShake(AWrapperWorld world, Point3D source, double strength, double radius, int duration, double intensity) {
        sendCameraShake(world, source, strength, ConfigSystem.settings.damage.gunCameraShakeFactor.value, radius, duration, intensity);
    }

    /**
     * Sends a camera shake impulse to players close enough to the source.  Strength is attenuated
     * by distance on the server so the server config remains authoritative without synchronizing it
     * to every client.  Radius is in blocks and duration is in ticks, with non-positive values
     * retaining the default radius and duration.  Intensity multiplies the oscillation speed,
     * with non-positive or non-finite values retaining the default speed.
     */
    private static void sendCameraShake(AWrapperWorld world, Point3D source, double strength, double factor, double radius, int duration, double intensity) {
        if (world.isClient() || strength <= 0 || Double.isNaN(strength) || Double.isInfinite(strength)) {
            return;
        }
        if (factor <= 0 || Double.isNaN(factor) || Double.isInfinite(factor)) {
            return;
        }

        if (radius <= 0 || Double.isNaN(radius) || Double.isInfinite(radius)) {
            radius = CAMERA_SHAKE_MAX_DISTANCE;
        }
        if (duration <= 0) {
            duration = CAMERA_SHAKE_DURATION_TICKS;
        }
        if (intensity <= 0 || Double.isNaN(intensity) || Double.isInfinite(intensity)) {
            intensity = 1.0D;
        }

        for (IWrapperPlayer player : world.getPlayersWithin(new BoundingBox(source, radius))) {
            double distance = source.distanceTo(player.getEyePosition());
            if (distance <= radius) {
                double adjustedStrength = strength * factor / Math.max(1.0D, distance);
                if (!Double.isNaN(adjustedStrength) && !Double.isInfinite(adjustedStrength) && adjustedStrength * CAMERA_SHAKE_ANGLE_FACTOR >= CAMERA_SHAKE_MIN_ANGLE) {
                    player.sendPacket(new PacketCameraShake(adjustedStrength, duration, intensity));
                }
            }
        }
    }

    /**
     * Starts a client-side shake impulse.  Overlapping impulses add to the currently visible
     * amplitude without causing the previous impulse to jump back to its original strength.
     */
    public static void startCameraShake(double strength, int duration, double intensity) {
        if (strength <= 0 || Double.isNaN(strength) || Double.isInfinite(strength) || duration <= 0) {
            return;
        }
        if (intensity <= 0 || Double.isNaN(intensity) || Double.isInfinite(intensity)) {
            intensity = 1.0D;
        }

        long currentTime = System.nanoTime();
        double remainingAmplitude = 0;
        if (currentTime < cameraShakeEndTime) {
            double elapsed = (double) (currentTime - cameraShakeStartTime) / (cameraShakeEndTime - cameraShakeStartTime);
            double decay = Math.max(0, 1.0D - elapsed);
            remainingAmplitude = cameraShakeAmplitude * decay * decay;
            //Anchor the current oscillation before changing its speed to preserve phase.
            cameraShakeTime += (currentTime - cameraShakeStartTime) * 0.000000001D * cameraShakeIntensity;
        } else {
            cameraShakePhase = currentTime * 0.000000001D;
            cameraShakeTime = cameraShakePhase;
        }
        cameraShakeIntensity = intensity;
        double impulseAmplitude = Math.min(strength * CAMERA_SHAKE_ANGLE_FACTOR, CAMERA_SHAKE_MAX_ANGLE);
        cameraShakeAmplitude = Math.min(remainingAmplitude + impulseAmplitude, CAMERA_SHAKE_MAX_ANGLE);
        cameraShakeStartTime = currentTime;
        cameraShakeEndTime = currentTime + duration * 50000000L;
    }

    /**
     * Applies the current shake as an additive camera rotation.  Returns true while an impulse is active.
     */
    public static boolean applyCameraShake(RotationMatrix cameraRotation) {
        long currentTime = System.nanoTime();
        if (currentTime >= cameraShakeEndTime) {
            cameraShakeAmplitude = 0;
            return false;
        }

        double elapsed = (double) (currentTime - cameraShakeStartTime) / (cameraShakeEndTime - cameraShakeStartTime);
        double decay = 1.0D - elapsed;
        decay *= decay;
        double amplitude = cameraShakeAmplitude * decay;
        double time = cameraShakeTime + (currentTime - cameraShakeStartTime) * 0.000000001D * cameraShakeIntensity;
        cameraRotation.rotateY(Math.sin(time * 38.0D + cameraShakePhase * 1.3D) * amplitude * 0.75D);
        cameraRotation.rotateX(Math.sin(time * 45.0D + cameraShakePhase) * amplitude);
        cameraRotation.rotateZ(Math.sin(time * 51.0D + cameraShakePhase * 0.7D) * amplitude * 0.5D);
        return true;
    }

    public static void clearCameraShake() {
        cameraShakeAmplitude = 0;
        cameraShakePhase = 0;
        cameraShakeTime = 0;
        cameraShakeIntensity = 1.0D;
        cameraShakeStartTime = 0;
        cameraShakeEndTime = 0;
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

