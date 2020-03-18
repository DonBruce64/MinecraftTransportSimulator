package minecrafttransportsimulator.wrappers;

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
	 *  Returns true if the entity is riding ANY vehicle.
	 */
	public boolean isRidingVehicle(){
		return entity.getRidingEntity() instanceof EntityVehicleE_Powered;
	}
	
	/**
	 *  Returns true if the entity is riding the passed-in vehicle.
	 */
	public boolean isRidingVehicle(EntityVehicleE_Powered vehicle){
		return vehicle.equals(entity.getRidingEntity());
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
}