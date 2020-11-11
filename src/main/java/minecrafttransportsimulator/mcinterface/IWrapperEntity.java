package minecrafttransportsimulator.mcinterface;

import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.vehicles.main.AEntityBase;

/**Wrapper for the base Entity class.  This class mainly allows for interaction with position
 * and motion variables for entities, as well as setting their riding statuses.
 *
 * @author don_bruce
 */
public interface IWrapperEntity{
	
	/**
	 *  Returns true if this entity is valid.  More specifically, this
	 *  returns true if the entity passed-in to create this wrapper was
	 *  not null, and the entity is not "dead".  For all intents, if this
	 *  method returns false the entity in this wrapper can be assumed
	 *  to not exist in the world.
	 */
	public boolean isValid();
	
	/**
	 *  Returns the entity's ID.  Useful for packets where you need to tell
	 *  which entity to apply an action to.
	 */
	public int getID();
	
	/**
	 *  Returns the entity this entity is riding, or null if
	 *  the entity is not riding any MTS entity (rider may will be riding
	 *  a vanilla entity).
	 */
	public AEntityBase getEntityRiding();
	
	/**
	 *  Tells the entity to start riding the passed-in entity.
	 *  If null is passed-in, then this rider will stop riding whatever entity it
	 *  is riding, if it was riding any entity.
	 */
	public void setRiding(AEntityBase entityToRide);
	
	/**
	 *  If the wrapped entity is an AEntityBase, return that
	 *  base entity. Otherwise return null.
	 */
	public AEntityBase getBaseEntity();
	
	/**
	 *  Returns a Y-offset for where this entity should sit in a seat.
	 *  This is based on how far down the axis drawn on the seat's y-axis
	 *  the position for the entity should be set.  Required as entities
	 *  sit where the bottoms of their bounding boxes are, even if this isn't
	 *  where the bottoms of their models are.  Players, for example, rotate
	 *  their legs upwards when sitting, despite their bounds being lower.
	 */
	public double getSeatOffset();
	
	/**
	 *  Returns how high the eyes of the entity are from its base.
	 *  This does not take into account the base of the model.  If you need
	 *  the distance for that, use {@link #getSeatOffset()}
	 */
	public double getEyeHeight();
	
	/**
	 *  Gets the entity's position as a point.
	 *  The returned position may by modified without affecting the entity's actual position.
	 *  However, the object itself may be re-used on the next call, so do not keep reference to it.
	 */
	public Point3d getPosition();
	
	/**
	 *  Sets the entity's position to the passed-in point.
	 */
	public void setPosition(Point3d position);
	
	/**
	 *  Gets the entity's velocity as a vector.
	 *  The returned velocity may by modified without affecting the entity's actual velocity.
	 *  However, the object itself may be re-used on the next call, so do not keep reference to it.
	 */
	public Point3d getVelocity();
	
	/**
	 *  Sets the entity's velocity to the passed-in vector.
	 */
	public void setVelocity(Point3d motion);
	
	/**
	 *  Returns the entity's pitch (x-axis rotation).
	 */
	public float getPitch();
	
	/**
	 *  Returns the entity's yaw (y-axis rotation).
	 *  NOTE: the return value from this function is inverted
	 *  from the normal MC standard to have it follow the RHR
	 *  for rotations.  This is OpenGL convention, and MC doesn't
	 *  follow it, which is why rendering is such a PITA with yaw.
	 */
	public float getYaw();
	
	/**
	 *  Returns the entity's head yaw (y-axis rotation).
	 *  NOTE: the return value from this function is inverted
	 *  from the normal MC standard to have it follow the RHR
	 *  for rotations.  This is OpenGL convention, and MC doesn't
	 *  follow it, which is why rendering is such a PITA with yaw.
	 */
	public float getHeadYaw();
	
	/**
	 *  Returns a vector in the direction of the entity's line of sight,
	 *  with a magnitude equal to the passed-in distance. 
	 */
	public Point3d getLineOfSight(float distance);
	
	/**
	 *  Sets the entity's yaw to the passed-in yaw.
	 *  NOTE: the yaw value from this function is inverted
	 *  from the normal MC standard to have it follow the RHR
	 *  for rotations.  This is OpenGL convention, and MC doesn't
	 *  follow it, which is why rendering is such a PITA with yaw.
	 */
	public void setYaw(double yaw);
	
	/**
	 *  Sets the entity's head yaw to the passed-in yaw.
	 *  NOTE: the yaw value from this function is inverted
	 *  from the normal MC standard to have it follow the RHR
	 *  for rotations.  This is OpenGL convention, and MC doesn't
	 *  follow it, which is why rendering is such a PITA with yaw.
	 */
	public void setHeadYaw(double yaw);
	
	/**
	 *  Sets the entity's pitch to the passed-in pitch.
	 */
	public void setPitch(double pitch);
	
	/**
	 *  Returns the entity's NBT data.
	 */
	public IWrapperNBT getNBT();
	
	/**
	 *  Loads the entity data from the passed-in NBT.
	 */
	public void setNBT(IWrapperNBT data);
	
	/**
	 *  Tries to leash up the entity to the passed-in player.
	 *  False may be returned if the entity cannot be leashed,
	 *  or if the player isn't holding a leash.
	 */
	public boolean leashTo(IWrapperPlayer player);
	
	/**
	 *  Attacks the entity.
	 */
	public void attack(Damage damage);
	
	/**
	 *  Returns the rendered position based on the passed-in partial ticks.
	 *  The returned position may by modified without affecting the actual rendered position.
	 *  However, the object itself may be re-used on the next call, so do not keep reference to it.
	 */
	public Point3d getRenderedPosition(float partialTicks);
	
	/**
	 * Adds the potion effect with the specified name to the entity.  Only valid for living entities that
	 * are effected by potions.
	 */
	public void addPotionEffect(String name, int duration, int amplifier);
	
	/**
	 * Removes the potion effect with the specified name from the entity.  Only valid for living entities that
	 * are effected by potions.
	 */
	public void removePotionEffect(String name);
}