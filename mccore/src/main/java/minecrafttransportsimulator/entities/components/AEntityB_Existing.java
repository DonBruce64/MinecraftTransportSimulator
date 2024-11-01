package minecrafttransportsimulator.entities.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.AnimationSwitchbox;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.entities.instances.EntityRadio;
import minecrafttransportsimulator.jsondefs.JSONCameraObject;
import minecrafttransportsimulator.jsondefs.JSONCollisionGroup.CollisionType;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityRiderChange;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.systems.CameraSystem;
import minecrafttransportsimulator.systems.CameraSystem.CameraMode;

/**
 * Base class for entities that exist in the world. In addition to the normal functions
 * of having a lookup ID, this class also has position/velocity information.  This can be
 * modified to move the entity around.  As the entity exists in the world, it can be used
 * to play sounds, though it cannot provide them of its own accord.
 *
 * @author don_bruce
 */
public abstract class AEntityB_Existing extends AEntityA_Base {
    protected static final Point3D ZERO_FOR_CONSTRUCTOR = new Point3D();

    public final Point3D position;
    public final RotationMatrix orientation;
    public final Point3D prevPosition;
    public final RotationMatrix prevOrientation;
    public final Point3D motion;
    public final Point3D prevMotion;
    public double velocity;
    private static final Set<CollisionType> boundingBoxCollisionTypes = new HashSet<>(Arrays.asList(CollisionType.ATTACK, CollisionType.CLICK));
    public final BoundingBox boundingBox;

    /**
     * The entity that is currently riding this entity.  There is only one rider per entity, though one can
     * make a multipart entity where each part has a rider to allow for effectively multiple riders per entity.
     **/
    public IWrapperEntity rider;

    /**
     * True if the running instance is the client, and the rider on this entity is the client player.
     **/
    public boolean riderIsClient;

    /**
     * List of all cameras available on this entity for the rider.  These get populated by other systems as applicable.
     **/
    public final List<JSONCameraObject> cameras = new ArrayList<>();
    public final Map<JSONCameraObject, AEntityD_Definable<?>> cameraEntities = new LinkedHashMap<>();
    public JSONCameraObject activeCamera;
    public AEntityD_Definable<?> activeCameraEntity;
    public AnimationSwitchbox activeCameraSwitchbox;
    private CameraMode lastCameraMode;

    /**
     * The position of the eyes of the rider.  This is slightly different than the return for 
     * {@link IWrapperEntity#getPosition()}, as the former is usually where the rider is sitting, 
     * whereas this is usually that value offset by {@link IWrapperEntity#getEyeHeight()} and
     * {@link IWrapperEntity#getSeatOffset()}, multiplied by {@link IWrapperEntity#getVerticalScale()}.
     * Though this may differ if the default logic is overridden.
     **/
    public final Point3D riderEyePosition = new Point3D();
    /**Like {@link #riderEyePosition}, but for the head.  This won't move even if viewpoints change.**/
    public final Point3D riderHeadPosition = new Point3D();
    /**Like {@link #riderEyePosition}, but for the camera.  This may or may not match the eye position depending on
     * the game implementation and settings, since some settings add a default camera offset we need to account for here.**/
    public final Point3D riderCameraPosition = new Point3D();
    public final Point3D prevRiderCameraPosition = new Point3D();

    /**
     * The orientation of the {@link #rider}.  This will be relative to this entity, and not global to the world.
     * If you desire the world-global orientation, call {@link IWrapperEntity#getOrientation()}.
     **/
    public RotationMatrix riderRelativeOrientation;
    public RotationMatrix prevRiderRelativeOrientation;
    private static final Point3D riderTempPoint = new Point3D();
    private static final RotationMatrix riderTempMatrix = new RotationMatrix();

    //Camera variables.
    public int zoomLevel;
    public int cameraIndex;

    //Internal sound variables.
    public final EntityRadio radio;
    public List<SoundInstance> sounds = new ArrayList<>();//TODO make this a hashmap.

    /**
     * Constructor for synced entities
     **/
    public AEntityB_Existing(AWrapperWorld world, IWrapperNBT data) {
        super(world, data);
        if (data != null) {
            this.position = data.getPoint3d("position");
            this.orientation = new RotationMatrix().setToAngles(data.getPoint3d("angles"));
            this.motion = data.getPoint3d("motion");
            this.zoomLevel = data.getInteger("zoomLevel");
            this.cameraIndex = data.getInteger("cameraIndex");
        } else {
            this.position = new Point3D();
            this.orientation = new RotationMatrix();
            this.motion = new Point3D();
        }

        this.prevPosition = position.copy();
        this.prevOrientation = new RotationMatrix().set(orientation);
        this.prevMotion = motion.copy();
        this.boundingBox = new BoundingBox(shouldLinkBoundsToPosition() ? this.position : this.position.copy(), 0.5, 0.5, 0.5, boundingBoxCollisionTypes);

        if (hasRadio()) {
            if (data != null) {
                this.radio = new EntityRadio(this, data.getData("radio"));
            } else {
                this.radio = new EntityRadio(this, null);
            }
            world.addEntity(radio);
        } else {
            this.radio = null;
        }
    }

    /**
     * Constructor for un-synced entities.  Allows for specification of position/motion/angles.
     **/
    public AEntityB_Existing(AWrapperWorld world, Point3D position, Point3D motion, Point3D angles) {
        super(world, null);
        this.position = position.copy();
        this.orientation = new RotationMatrix().setToAngles(angles);
        this.prevPosition = position.copy();
        this.prevOrientation = new RotationMatrix().set(orientation);
        this.motion = motion.copy();
        this.prevMotion = motion.copy();
        this.boundingBox = new BoundingBox(shouldLinkBoundsToPosition() ? this.position : this.position.copy(), 0.5, 0.5, 0.5, boundingBoxCollisionTypes);
        this.radio = null;
    }

    @Override
    public void update() {
        super.update();
        world.beginProfiling("EntityB_Level", true);
        if (world.isClient()) {
            updateSounds(0);
        }
        if (requiresDeltaUpdates()) {
            prevPosition.set(position);
            prevMotion.set(motion);
            prevOrientation.set(orientation);
            velocity = motion.length();
        }

        //Only do camera checks if we have an active camera.
        //Checking at other times wastes CPU cycles.
        //We also wait 5 ticks after spawn before checking, since it might take time to init the cameras.
        if (cameraIndex != 0 && ticksExisted >= 5) {
            //Check for valid camera, and perform operations if so.
            activeCamera = null;
            while (cameraIndex != 0 && activeCamera == null) {
                if ((cameraIndex - 1) < cameras.size()) {
                    activeCamera = cameras.get(cameraIndex - 1);
                    activeCameraEntity = cameraEntities.get(activeCamera);
                    activeCameraSwitchbox = activeCameraEntity.cameraSwitchboxes.get(activeCamera);
                    if (activeCameraSwitchbox != null && !activeCameraSwitchbox.runSwitchbox(0, false)) {
                        //Camera is inactive, go to next.
                        ++cameraIndex;
                        activeCamera = null;
                    }
                } else {
                    //No active cameras found, set index to 0 to disable and go back to normal rendering.
                    cameraIndex = 0;
                    activeCamera = null;
                    if(lastCameraMode != null && world.isClient() && InterfaceManager.clientInterface.getClientPlayer().equals(rider)) {
                    	InterfaceManager.clientInterface.setCameraMode(lastCameraMode);
                    	lastCameraMode = null;
                    }
                }
            }
            
            //If we just got to an active camera, store last camera mode and change to first-person if required.
            //If we have an active camera, force first-person if we don't have it.
            if(activeCamera != null && world.isClient() && InterfaceManager.clientInterface.getClientPlayer().equals(rider)) {
            	if(lastCameraMode == null) {
            		lastCameraMode = InterfaceManager.clientInterface.getCameraMode();
            	}
            	if(InterfaceManager.clientInterface.getCameraMode() != CameraMode.FIRST_PERSON) {
            		InterfaceManager.clientInterface.setCameraMode(CameraMode.FIRST_PERSON);
            	}
            }
        }

        world.endProfiling();
    }

    @Override
    public void remove() {
        super.remove();
        if (world.isClient()) {
            if (radio != null) {
                radio.stop();
            }
            for (SoundInstance sound : sounds) {
                //Only stop looping sounds, other sounds should play till they run out.
                if (sound.soundDef != null && sound.soundDef.looping) {
                    sound.stopSound = true;
                }
            }
        }
        if (rider != null) {
            removeRider();
        }
        if (radio != null) {
            radio.remove();
        }
    }

    @Override
    public double getMass() {
        return rider != null ? 100 : 0;
    }

    /**
     * Called to update the rider on this entity.  This gets called after the update loop,
     * as the entity needs to move to its new position before we can know where the
     * riders of said entity will be.  The calling function will assure that the rider
     * is non-null at this point, so null checks are not required in this function.
     * However, if the rider is removed, false is returned, and further processing should halt.
     */
    public boolean updateRider() {
        //Update entity position, motion, and orientation.
        if (rider.isValid()) {
            rider.setPosition(position, false);
            rider.setVelocity(motion);
            prevRiderRelativeOrientation.set(riderRelativeOrientation);
            riderRelativeOrientation.angles.y += rider.getYawDelta();
            //Need to clamp between +/- 180 to ensure that we don't confuse things and other variables and animations.
            if (riderRelativeOrientation.angles.y > 180) {
                riderRelativeOrientation.angles.y -= 360;
                prevRiderRelativeOrientation.angles.y -= 360;
            } else if (riderRelativeOrientation.angles.y < -180) {
                riderRelativeOrientation.angles.y += 360;
                prevRiderRelativeOrientation.angles.y += 360;
            }

            //Rider yaw can go full 360, but clamp pitch to +/- 85 so the player's head can't go upside-down.
            float pitchDelta = rider.getPitchDelta();
            if (Math.abs(riderRelativeOrientation.angles.x + pitchDelta) < 85) {
                riderRelativeOrientation.angles.x += pitchDelta;
            }
            riderRelativeOrientation.updateToAngles();
            riderTempMatrix.set(orientation).multiply(riderRelativeOrientation).convertToAngles();
            rider.setOrientation(riderTempMatrix);
            riderEyePosition.set(0, (rider.getEyeHeight() + rider.getSeatOffset()) * rider.getVerticalScale(), 0).rotate(orientation).add(position);
            riderHeadPosition.set(riderEyePosition);

            //If we are a client, and aren't running a custom camera, and are in third-person, adjust zoom.
            if (world.isClient()) {
                prevRiderCameraPosition.set(riderCameraPosition);
                CameraMode cameraMode = InterfaceManager.clientInterface.getCameraMode();
                if (CameraSystem.activeCamera == null && cameraMode != CameraMode.FIRST_PERSON) {
                    riderCameraPosition.set(riderEyePosition);

                    //Adjust eye position to account for zoom settings.
                    int zoomRequired = 4 + zoomLevel;
                    riderTempPoint.set(0, 0, cameraMode == CameraMode.THIRD_PERSON ? -zoomRequired : zoomRequired).rotate(rider.getOrientation());
                    riderEyePosition.add(riderTempPoint);

                    //Check if camera should be where eyes are, or somewhere different.
                    int cameraZoomRequired = 4 - InterfaceManager.clientInterface.getCameraDefaultZoom() + zoomLevel;
                    if (zoomRequired != cameraZoomRequired) {
                        riderTempPoint.set(0, 0, cameraMode == CameraMode.THIRD_PERSON ? -cameraZoomRequired : cameraZoomRequired).rotate(rider.getOrientation());
                        riderCameraPosition.add(riderTempPoint);
                    } else {
                        riderCameraPosition.add(riderTempPoint);
                    }
                } else {
                    riderCameraPosition.set(riderEyePosition);
                }
            }
            return true;
        } else {
            //Remove invalid rider.
            //Don't call this on the client; they will get a removal packet from this method.
            if (!world.isClient()) {
                removeRider();
            }
            return false;
        }
    }

    /**
     * Called to set the rider for this entity.  If this isn't possible because
     * there is already a rider, or we shouldn't accept riders, return false.
     * Otherwise, return true.  
     * If the rider needs to face forward when they are added, set the boolean to true.
     * Note: this will only set them to face forwards on the tick they mount.
     * It won't block them from turning to a different orientation later.
     */
    public boolean setRider(IWrapperEntity newRider, boolean facesForwards) {
        if (rider != null) {
            return false;
        } else {
            rider = newRider;
            riderIsClient = world.isClient() && rider.equals(InterfaceManager.clientInterface.getClientPlayer());

            //Create variables for use in other code areas.
            if (riderRelativeOrientation == null) {
                riderRelativeOrientation = new RotationMatrix();
                prevRiderRelativeOrientation = new RotationMatrix();
            }

            if (facesForwards) {
                riderRelativeOrientation.setToZero();
            } else {
                riderTempPoint.set(0, 0, 1).rotate(rider.getOrientation()).reOrigin(orientation);
                riderRelativeOrientation.setToVector(riderTempPoint, false);
            }
            riderRelativeOrientation.convertToAngles();
            prevRiderRelativeOrientation.set(riderRelativeOrientation);
            riderTempMatrix.set(orientation).multiply(riderRelativeOrientation).convertToAngles();
            rider.setOrientation(riderTempMatrix);
            rider.setPosition(position, false);
            //Call getters so it resets to current value, if we don't do this, they'll get flagged for a change in the update call.
            rider.getYawDelta();
            rider.getPitchDelta();
            rider.setRiding(this);
            if (!world.isClient()) {
                InterfaceManager.packetInterface.sendToAllClients(new PacketEntityRiderChange(this, rider, facesForwards));
            }
            return true;
        }
    }

    /**
     * Called to remove the rider that is currently riding this entity.
     */
    public void removeRider() {
        rider.setRiding(null);
        if (!world.isClient()) {
            InterfaceManager.packetInterface.sendToAllClients(new PacketEntityRiderChange(this, rider));
        }
        rider = null;
        riderIsClient = false;
    }

    /**
     * Like {@link #getInterpolatedOrientation(RotationMatrix, double)}, just for
     * the rider's {@link #riderRelativeOrientation}.
     */
    public void getRiderInterpolatedOrientation(RotationMatrix store, double partialTicks) {
        store.interploate(prevRiderRelativeOrientation, riderRelativeOrientation, partialTicks);
    }

    /**
     * Called to destroy this entity.  While removal will still allow the entity to be re-created
     * into the world on the next loading of the world or the chunk it is is, destruction is the
     * permanent removal of this entity from the world.  Think breaking blocks or crashing vehicles.
     * The passed-in bounding box may be considered the location of destruction.  Used in cases where
     * an entity has multiple bounding boxes and the destruction is location-specific.
     */
    public void destroy(BoundingBox box) {
        //Do normal removal operations.
        remove();
    }

    /**
     * Returning false here will prevent this entity's positional data from being saved during saving
     * operations.  Normally you want this, but if your entity dynamically calculates its position based
     * on other data, such as another entity, then you may not care for this data and can return false.
     * This will save on disk space and networking if you have a lot of entities.
     */
    public boolean shouldSavePosition() {
        return true;
    }

    /**
     * This method returns true if this entity should link its bounding box to its position.  This will
     * result in the box always being centered on the entity.  This is normally true, but may be made
     * false for entities that need their collision offset from their position.  This will require manual bounding
     * box syncing, though the initial location of the box will be the position of the entity.
     */
    public boolean shouldLinkBoundsToPosition() {
        return true;
    }

    /**
     * This method returns true if this entity needs to handle positional delta data.
     * This is normally false, since basic "entities" like radios, fluid tanks, bullets, and decor, either don't every move,
     * or do move, but their movement is based on another entity that does handle deltas.  Returning false here will make
     * this entity not care about {@link #prevPosition}, {@link #motion}, {@link #prevMotion}, and {@link #prevOrientation}.
     * It should also be used to bypass bounding box and collision updates, since those won't change if this entity delta doesn't,
     * as well as not doing rider positional updates, since the rider won't change position if the entity doesn't.
     */
    public boolean requiresDeltaUpdates() {
        return false;
    }

    /**
     * Sets the interpolated orientation into the passed-in {@link RotationMatrix}.
     * The position is not interpolated with this as {@link #orientation}
     * only contains the rotational elements of this entity.
     */
    public void getInterpolatedOrientation(RotationMatrix store, double partialTicks) {
        if (requiresDeltaUpdates()) {
            store.interploate(prevOrientation, orientation, partialTicks);
        } else {
            store.set(orientation);
        }
    }

    /**
     * Returns true if this entity can collide with the passed-in entity.  Normally this is false, but there
     * are times where entities should affect collision.
     */
    public boolean canCollideWith(AEntityB_Existing entityToCollide) {
        return false;
    }

    /**
     * Called when checking if this entity can be interacted with.
     * If it does interactions it should do them and then return true.
     * This is only called on the server: client modifications will be done via packets.
     */
    public boolean interact(IWrapperPlayer player) {
        return false;
    }

    /**
     * This method returns how much light this entity is providing.  Used to send lighting status to various
     * systems for rendering in the world to provide actual light rather than rendered light.
     */
    public float getLightProvided() {
        return 0.0F;
    }

    /**
     * Returns true if this entity should render light beams.  This is entity-specific in the config,
     * so the method is abstract here.
     */
    public boolean shouldRenderBeams() {
        return false;
    }

    /**
     * Returns true if this entity has a radio.  Radios are updated to sync with the entity and
     * will save on them as applicable.
     */
    public boolean hasRadio() {
        return false;
    }

    /**
     * This method should start/stop any sounds, and change any existing sound properties when called.
     * Called at the start of every update tick to update sounds, and on partial tick frames.  You can
     * tell if the method is being called on a partial tick if the partial ticks parameter is non-zero.
     * Use this to ensure you don't query slow-activating sounds every frame.
     */
    public void updateSounds(float partialTicks) {
        //Update radio of we have one and we're on the main update.
        if (radio != null && partialTicks == 0) {
            radio.update();
        }
    }

    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        if (shouldSavePosition()) {
            data.setPoint3d("position", position);
            data.setPoint3d("motion", motion);
            data.setPoint3d("angles", orientation.convertToAngles());
        }
        if (radio != null) {
            data.setData("radio", radio.save(InterfaceManager.coreInterface.getNewNBTWrapper()));
        }
        if (!cameras.isEmpty()) {
            data.setInteger("zoomLevel", zoomLevel);
            data.setInteger("cameraIndex", cameraIndex);
        }
        return data;
    }
}
