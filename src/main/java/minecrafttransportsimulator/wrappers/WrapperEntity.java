package minecrafttransportsimulator.wrappers;

import java.nio.FloatBuffer;

import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.entity.Entity;

/**Wrapper for the base Entity class.  This class mainly allows for interaction with position
 * and motion variables for entities, as well as setting their riding statuses.  Note that 
 * the passed-in {@link Entity} reference MAY be null to allow for more convenient return
 * calls in methods.  Check this via {@link #isValid()} before you do operations with this wrapper!
 *
 * @author don_bruce
 */
public class WrapperEntity{
	private final Entity entity;
	
	public WrapperEntity(Entity entity){
		this.entity = entity;
	}
	
	/**
	 *  Returns true if this entity is valid.  More specifically, this
	 *  returns true if the entity passed-in to create this wrapper was
	 *  not null, and the entity is not "dead".  For all intents, if this
	 *  method returns false the entity in this wrapper can be assumed
	 *  to not exist in the world.
	 */
	public boolean isValid(){
		return entity != null && !entity.isDead;
	}
	
	/**
	 *  Returns the entity's ID.  Useful for packets where you need to tell
	 *  which entity to apply an action to.
	 */
	public int getID(){
		return entity.getEntityId();
	}
	
	/**
	 *  Returns the vehicle this entity is riding, or null if
	 *  the entity is not riding a vehicle.
	 */
	public EntityVehicleE_Powered getVehicleRiding(){
		return entity.getRidingEntity() instanceof EntityVehicleE_Powered ? (EntityVehicleE_Powered) entity.getRidingEntity() : null;
	}
	
	/**
	 *  Tells the entity to start riding the passed-in vehicle.
	 */
	public void setRiding(EntityVehicleE_Powered vehicle){
		entity.startRiding(vehicle);
	}
	
	/**
	 *  Sets the entity's position to the passed-in values.
	 */
	public void setPosition(double x, double y, double z){
		entity.setPosition(x, y, z);
	}
	
	/**
	 *  Adds the passed-in values to the entity's position.
	 */
	public void addToPosition(double x, double y, double z){
		entity.setPosition(entity.posX + x, entity.posY + y, entity.posZ + z);
	}
	
	/**
	 *  Puts the entity's position into the passed-in buffer.
	 *  Used for operations where position needs to be checked frequently.
	 *  Note that this may be called from another thread safely.
	 */
	public void putPosition(FloatBuffer buffer){
		buffer.rewind();
		buffer.put((float) entity.posX);
		buffer.put((float) entity.posY);
		buffer.put((float) entity.posZ);
		buffer.flip();
	}
	
	/**
	 *  Puts the entity's velocity into the passed-in buffer.
	 *  Used for operations where velocity needs to be checked frequently.
	 *  Note that this may be called from another thread safely.
	 */
	public void putVelocity(FloatBuffer buffer){
		buffer.rewind();
		buffer.put((float) entity.motionX);
		buffer.put((float) entity.motionY);
		buffer.put((float) entity.motionZ);
		buffer.flip();
	}
	
	/**
	 *  Puts the entity's orientation as vectors into the passed-in buffer.
	 *  Used for operations where orientation needs to be checked frequently.
	 *  Note that this may be called from another thread safely.
	 */
	public void putOrientation(FloatBuffer buffer){
		buffer.put(0, (float) Math.sin(Math.toRadians(entity.rotationYaw)));
		buffer.put(1, 0.0F);
		buffer.put(2, (float) Math.cos(Math.toRadians(entity.rotationYaw)));
		buffer.put(3, 0.0F);
		buffer.put(4, 1.0F);
		buffer.put(5, 0.0F);
	}
}