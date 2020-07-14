package mcinterface;

import java.nio.FloatBuffer;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import net.minecraft.entity.Entity;

/**Wrapper for the base Entity class.  This class mainly allows for interaction with position
 * and motion variables for entities, as well as setting their riding statuses.  Note that 
 * the passed-in {@link Entity} reference MAY be null to allow for more convenient return
 * calls in methods.  Check this via {@link #isValid()} before you do operations with this wrapper!
 *
 * @author don_bruce
 */
public class WrapperEntity{
	final Entity entity;
	
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
	 *  Returns the entity this entity is riding, or null if
	 *  the entity is not riding any MTS entity (rider may will be riding
	 *  a vanilla entity).
	 */
	public AEntityBase getVehicleRiding(){
		return entity.getRidingEntity() instanceof BuilderEntity ? ((BuilderEntity) entity.getRidingEntity()).entity : null;
	}
	
	/**
	 *  Returns the entity's y-offset.
	 *  This is how far above the mount position this entity sits when riding things.
	 */
	public double getYOffset(){
		return entity.getYOffset();
	}
	
	/**
	 *  Returns the entity's height.
	 */
	public double getHeight(){
		return entity.height;
	}
	
	/**
	 *  Tells the entity to start riding the passed-in entity.
	 */
	public void setRiding(AEntityBase entityToRide){
		entity.startRiding(entityToRide.builder);
	}
	
	/**
	 *  Sets the entity's position to the passed-in point.
	 */
	public void setPosition(Point3d position){
		entity.setPosition(position.x, position.y, position.z);
	}
	
	/**
	 *  Sets the entity's yaw to the passed-in yaw.
	 */
	public void setYaw(double yaw){
		entity.rotationYaw = (float)yaw;
	}
	
	/**
	 *  Sets the entity's yaw to the passed-in yaw.
	 */
	public void setPitch(double pitch){
		entity.rotationPitch = (float)pitch;
	}
	
	/**
	 *  Adds the passed-in values to the entity's position.
	 */
	public void addToPosition(double x, double y, double z){
		entity.setPosition(entity.posX + x, entity.posY + y, entity.posZ + z);
	}
	
	/**
	 *  Returns the rendered position based on the passed-in partial ticks.
	 *  Used for interpolation in movement.  Returned vector is re-used for
	 *  all operations to prevent new objects from being created each tick.
	 */
	public Point3d getRenderedPosition(float partialTicks){
		mutableRenderPosition.x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
		mutableRenderPosition.y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
		mutableRenderPosition.z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
		return mutableRenderPosition;
	}
	private final Point3d mutableRenderPosition = new Point3d(0D, 0D, 0D);
	
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