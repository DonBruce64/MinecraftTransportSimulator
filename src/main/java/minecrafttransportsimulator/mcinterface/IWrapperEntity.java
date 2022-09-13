package minecrafttransportsimulator.mcinterface;

import java.util.UUID;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.jsondefs.JSONPotionEffect;

/**
 * IWrapper for the base Entity class.  This class mainly allows for interaction with position
 * and motion variables for entities, as well as setting their riding statuses.
 *
 * @author don_bruce
 */
public interface IWrapperEntity {

    /**
     * Returns true if this entity is valid.  More specifically, this
     * returns true if the entity passed-in to create this IWrapper was
     * not null, and the entity is not "dead".  For all intents, if this
     * method returns false the entity in this IWrapper can be assumed
     * to not exist in the world.
     */
    boolean isValid();

    /**
     * Returns the entity's global UUID.  This is an ID that's unique to every player on Minecraft.
     * Useful for assigning ownership where the entity ID of a player might change between sessions.
     * Also should be used during packets to ensure the proper entity is retrieved, as some mods (and
     * most Sponge servers) will muck up the entity ID maps and IDs will not be synchronized.
     * <br><br>
     * NOTE: While this ID isn't supposed to change, some systems WILL, in fact, change it.  Cracked
     * servers, and the nastiest of Bukkit systems will deliberately change the UUID of players, which,
     * when combined with their changing of entity IDs, makes server-client lookup impossible.
     */
    UUID getID();

    /**
     * Returns the name for this entity.  This is the general name for
     * the entity, and may be whatever the coder who made this entity
     * set it to.
     */
    String getName();

    /**
     * Returns the world this entity is in.
     */
    AWrapperWorld getWorld();

    /**
     * Returns the entity this entity is riding, or null if
     * the entity is not riding any MTS entity (rider may will be riding
     * a vanilla entity).
     */
    AEntityE_Interactable<?> getEntityRiding();

    /**
     * Tells the entity to start riding the passed-in entity.
     * If null is passed-in, then this rider will stop riding whatever entity it
     * is riding, if it was riding any entity.
     */
    void setRiding(AEntityE_Interactable<?> entityToRide);

    /**
     * Returns the vertical scaling factor for this entity.  Normally is 1,
     * but can differ (usually smaller) if the entity is riding a vehicle
     * and that vehicle has a scaled seat.  This should be taken into account
     * for all calls that care about eye height, as seat scaling in the Y direction
     * will affect eye and camera heights.
     */
    double getVerticalScale();

    /**
     * Returns a Y-offset for where this entity should sit in a seat.
     * This is used if the sitting point of the entity isn't at the base
     * of the entity. For example, players, when sitting, rotate their
     * legs forwards, but they don't translate down.  This parameter is the
     * amount that they should be translated, and takes into account whether
     * or not the entity is actually sitting.  Useful in rendering and camera
     * operations as this will also affect eye height.
     */
    double getSeatOffset();

    /**
     * Returns how high the eyes of the entity are from its base.
     * This does not take into account the base of the model.  If you need
     * the distance for that, use {@link #getSeatOffset()}
     */
    double getEyeHeight();

    /**
     * Gets the entity's position as a point.
     * The returned position may by modified without affecting the entity's actual position.
     * However, the object itself may be re-used on the next call, so do not keep reference to it.
     */
    Point3D getPosition();

    /**
     * Sets the entity's position to the passed-in point.
     * Boolean is included to set ground state.  This should
     * be set if the entity is on another entity collision box,
     * but not if they are riding an entity.
     */
    void setPosition(Point3D position, boolean onGround);

    /**
     * Gets the entity's velocity as a vector.
     * The returned velocity may by modified without affecting the entity's actual velocity.
     * However, the object itself may be re-used on the next call, so do not keep reference to it.
     */
    Point3D getVelocity();

    /**
     * Sets the entity's velocity to the passed-in vector.
     */
    void setVelocity(Point3D motion);

    /**
     * Returns the entity's orientation.
     * Do NOT modify the returned object.  This object is cached on
     * calculation to avoid the need to re-calculate it every tick/frame.
     * If you modify the object and the entity does not change, you will get
     * invalid results.
     */
    RotationMatrix getOrientation();

    /**
     * Sets the entity's orientation.
     * Note that this method runs off the angles inside the
     * matrix object, not the actual transform, so keep this in mind
     * when calling this method.
     */
    void setOrientation(RotationMatrix rotation);

    /**
     * Returns the entity's pitch (x-axis rotation).
     */
    float getPitch();

    /**
     * Returns the pitch delta for this entity, relative to the last time this method was called.
     */
    float getPitchDelta();

    /**
     * Returns the entity's yaw (y-axis rotation).
     * NOTE: the return value from this function is inverted
     * from the normal MC standard to have it follow the RHR
     * for rotations.  This is OpenGL convention, and MC doesn't
     * follow it, which is why rendering is such a PITA with yaw.
     */
    float getYaw();

    /**
     * Returns the yaw delta for this entity, relative to the last time this method was called.
     */
    float getYawDelta();

    /**
     * Returns the entity's body yaw (y-axis rotation).
     * NOTE: the return value from this function is inverted
     * from the normal MC standard to have it follow the RHR
     * for rotations.  This is OpenGL convention, and MC doesn't
     * follow it, which is why rendering is such a PITA with yaw.
     */
    float getBodyYaw();

    /**
     * Returns a vector in the direction of the entity's line of sight,
     * with a magnitude equal to the passed-in distance.
     * The returned vector  may by modified without affecting the entity's actual line of sight.
     * However, the object itself may be re-used on the next call, so do not keep references to it.
     */
    Point3D getLineOfSight(double distance);

    /**
     * Sets the entity's yaw to the passed-in yaw.
     * NOTE: the yaw value from this function is inverted
     * from the normal MC standard to have it follow the RHR
     * for rotations.  This is OpenGL convention, and MC doesn't
     * follow it, which is why rendering is such a PITA with yaw.
     */
    void setYaw(double yaw);

    /**
     * Sets the entity's bod yyaw to the passed-in yaw.
     * NOTE: the yaw value from this function is inverted
     * from the normal MC standard to have it follow the RHR
     * for rotations.  This is OpenGL convention, and MC doesn't
     * follow it, which is why rendering is such a PITA with yaw.
     */
    void setBodyYaw(double yaw);

    /**
     * Sets the entity's pitch to the passed-in pitch.
     */
    void setPitch(double pitch);

    /**
     * Gets the entity's bounding box.
     * The returned velocity may by modified without affecting the entity's actual bounds.
     * However, the object itself may be re-used on the next call, so do not keep reference to it.
     */
    BoundingBox getBounds();

    /**
     * Returns the entity's NBT data.
     */
    IWrapperNBT getData();

    /**
     * Loads the entity data from the passed-in NBT.
     */
    void setData(IWrapperNBT data);

    /**
     * Tries to leash up the entity to the passed-in player.
     * False may be returned if the entity cannot be leashed,
     * or if the player isn't holding a leash.
     */
    boolean leashTo(IWrapperPlayer player);

    /**
     * Attacks the entity.
     */
    void attack(Damage damage);

    /**
     * Returns the rendered position based on the passed-in partial ticks.
     * The returned position may by modified without affecting the actual rendered position.
     * However, the object itself may be re-used on the next call, so do not keep reference to it.
     */
    Point3D getRenderedPosition(float partialTicks);

    /**
     * Adds the potion effect with the specified name to the entity.  Only valid for living entities that
     * are effected by potions.
     */
    void addPotionEffect(JSONPotionEffect effect);

    /**
     * Removes the potion effect with the specified name from the entity.  Only valid for living entities that
     * are effected by potions.
     */
    void removePotionEffect(JSONPotionEffect effect);
}