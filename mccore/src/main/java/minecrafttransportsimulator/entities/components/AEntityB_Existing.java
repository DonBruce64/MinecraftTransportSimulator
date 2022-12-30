package minecrafttransportsimulator.entities.components;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.entities.instances.EntityRadio;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityRiderChange;
import minecrafttransportsimulator.sound.SoundInstance;

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
    public final BoundingBox boundingBox;

    /**
     * The entity that is currently riding this entity.  There is only one rider per entity, though one can
     * make a multipart entity where each part has a rider to allow for effectively multiple riders per entity.
     **/
    public IWrapperEntity rider;

    /**
     * The orientation of the {@link #rider}.  This will be relative to this entity, and not global to the world.
     * If you desire the world-global orientation, call {@link IWrapperEntity#getOrientation()}.
     **/
    public RotationMatrix riderRelativeOrientation;
    public RotationMatrix prevRiderRelativeOrientation;
    private static final Point3D riderTempPoint = new Point3D();
    private static final RotationMatrix riderTempMatrix = new RotationMatrix();

    //Internal sound variables.
    public final EntityRadio radio;
    public List<SoundInstance> sounds = new ArrayList<>();

    /**
     * Constructor for synced entities
     **/
    public AEntityB_Existing(AWrapperWorld world, IWrapperPlayer placingPlayer, IWrapperNBT data) {
        super(world, data);
        this.position = data.getPoint3d("position");
        this.orientation = new RotationMatrix().setToAngles(data.getPoint3d("angles"));
        this.prevPosition = position.copy();
        this.prevOrientation = new RotationMatrix().set(orientation);
        this.motion = data.getPoint3d("motion");
        this.prevMotion = motion.copy();

        this.boundingBox = new BoundingBox(shouldLinkBoundsToPosition() ? this.position : this.position.copy(), 0.5, 0.5, 0.5);
        if (hasRadio()) {
            this.radio = new EntityRadio(this, data.getDataOrNew("radio"));
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
        this.boundingBox = new BoundingBox(shouldLinkBoundsToPosition() ? this.position : this.position.copy(), 0.5, 0.5, 0.5);
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
                sound.stopSound = true;
            }
        }
        if (rider != null) {
            removeRider();
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
            if (requiresDeltaUpdates()) {
                rider.setPosition(position, false);
                rider.setVelocity(motion);
                prevRiderRelativeOrientation.set(riderRelativeOrientation);
                riderRelativeOrientation.angles.y += rider.getYawDelta();
                //Rider yaw can go full 360, but clamp pitch to +/- 85 so the player's head can't go upside-down.
                float pitchDelta = rider.getPitchDelta();
                if (Math.abs(riderRelativeOrientation.angles.x + pitchDelta) < 85) {
                    riderRelativeOrientation.angles.x += pitchDelta;
                }
                riderRelativeOrientation.updateToAngles();
                riderTempMatrix.set(orientation).multiply(riderRelativeOrientation).convertToAngles();
                rider.setOrientation(riderTempMatrix);
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
     * Otherwise, return true.  Call this ONLY on the server!  Packets are sent to clients
     * for syncing so calling this on clients will result in Bad Stuff.
     * If the rider needs to face forward when they are added, set the boolean to true.
     * Note: this will only set them to face forwards on the tick they mount.
     * It won't block them from turning to a different orientation later.
     */
    public boolean setRider(IWrapperEntity newRider, boolean facesForwards) {
        if (rider != null) {
            return false;
        } else {
            rider = newRider;

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
     * Sets the interpolated orientation into the passed-in Matrix4d.
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
        return data;
    }
}
