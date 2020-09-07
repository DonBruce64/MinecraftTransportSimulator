package mcinterface;

import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemLead;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

/**Wrapper for the base Entity class.  This class mainly allows for interaction with position
 * and motion variables for entities, as well as setting their riding statuses.  Note that 
 * the passed-in {@link Entity} reference MAY be null to allow for more convenient return
 * calls in methods.  Check this via {@link #isValid()} before you do operations with this wrapper!
 * Also note that wrappers are cached to allow map operations for their hashcodes, and to prevent
 * excess calls to creating instances.
 *
 * @author don_bruce
 */
public class WrapperEntity{
	final Entity entity;
	
	protected WrapperEntity(Entity entity){
		this.entity = entity;
	}
	
	@Override
	public boolean equals(Object obj){
		return entity.equals(obj instanceof WrapperEntity ? ((WrapperEntity) obj).entity : obj);
	}
	
	@Override
	public int hashCode(){
        return entity.hashCode();
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
	public AEntityBase getEntityRiding(){
		return entity.getRidingEntity() instanceof BuilderEntity ? ((BuilderEntity) entity.getRidingEntity()).entity : null;
	}
	
	/**
	 *  Tells the entity to start riding the passed-in entity.
	 *  If null is passed-in, then this rider will stop riding whatever entity it
	 *  is riding, if it was riding any entity.
	 */
	public void setRiding(AEntityBase entityToRide){
		if(entityToRide != null){
			entity.startRiding(BuilderEntity.entitiesToBuilders.get(entityToRide), true);
		}else{
			entity.dismountRidingEntity();
		}
	}
	
	/**
	 *  Returns a Y-offset for where this entity should sit in a seat.
	 *  This is based on how far down the axis drawn on the seat's y-axis
	 *  the position for the entity should be set.  Required as entities
	 *  sit where the bottoms of their bounding boxes are, even if this isn't
	 *  where the bottoms of their models are.  Players, for example, rotate
	 *  their legs upwards when sitting, despite their bounds being lower.
	 */
	public double getSeatOffset(){
		return 0D;
	}
	
	/**
	 *  Returns how high the eyes of the entity are from its base.
	 *  This does not take into account the base of the model.  If you need
	 *  the distance for that, use {@link #getSeatOffset()}
	 */
	public double getEyeHeight(){
		return entity.getEyeHeight();
	}
	
	/**
	 *  Gets the entity's position as a point.
	 */
	public Point3d getPosition(){
		mutablePosition.set(entity.posX, entity.posY, entity.posZ);
		return mutablePosition;
	}
	private final Point3d mutablePosition = new Point3d(0D, 0D, 0D);
	
	/**
	 *  Sets the entity's position to the passed-in point.
	 */
	public void setPosition(Point3d position){
		entity.setPosition(position.x, position.y, position.z);
	}
	
	/**
	 *  Gets the entity's velocity as a vector.
	 */
	public Point3d getVelocity(){
		mutableVelocity.set(entity.motionX, entity.motionY, entity.motionZ);
		return mutableVelocity;
	}
	private final Point3d mutableVelocity = new Point3d(0D, 0D, 0D);
	
	/**
	 *  Sets the entity's velocity to the passed-in vector.
	 */
	public void setVelocity(Point3d motion){
		entity.motionX = motion.x;
		entity.motionY = motion.y;
		entity.motionZ = motion.z;
	}
	
	/**
	 *  Returns the entity's pitch (x-axis rotation).
	 */
	public float getPitch(){
		return entity.rotationPitch;
	}
	
	/**
	 *  Returns the entity's yaw (y-axis rotation).
	 *  NOTE: the return value from this function is inverted
	 *  from the normal MC standard to have it follow the RHR
	 *  for rotations.  This is OpenGL convention, and MC doesn't
	 *  follow it, which is why rendering is such a PITA with yaw.
	 */
	public float getYaw(){
		return -entity.rotationYaw;
	}
	
	/**
	 *  Sets the entity's yaw to the passed-in yaw.
	 *  NOTE: the yaw value from this function is inverted
	 *  from the normal MC standard to have it follow the RHR
	 *  for rotations.  This is OpenGL convention, and MC doesn't
	 *  follow it, which is why rendering is such a PITA with yaw.
	 */
	public void setYaw(double yaw){
		entity.rotationYaw = (float)-yaw;
	}
	
	/**
	 *  Sets the entity's pitch to the passed-in pitch.
	 */
	public void setPitch(double pitch){
		entity.rotationPitch = (float)pitch;
	}
	
	
	/**
	 *  Returns the entity's NBT data.
	 */
	public WrapperNBT getNBT(){
		return new WrapperNBT(entity);
	}
	
	/**
	 *  Loads the entity data from the passed-in NBT.
	 */
	public void setNBT(WrapperNBT data){
		entity.readFromNBT(data.tag);
	}
	
	/**
	 *  Tries to leash up the entity to the passed-in player.
	 *  False may be returned if the entity cannot be leashed,
	 *  or if the player isn't holding a leash.
	 */
	public boolean leashTo(WrapperPlayer player){
		if(entity instanceof EntityLiving){
			ItemStack heldStack = player.player.getHeldItemMainhand();
			if(((EntityLiving) entity).canBeLeashedTo(player.player) && heldStack.getItem() instanceof ItemLead){
				((EntityLiving)entity).setLeashHolder(player.player, true);
				if(!player.isCreative()){
					heldStack.shrink(1);
				}
				return true;
			}
		}
		return false;
	}
	
	/**
	 *  Attacks the entity.  The string passed-in should be the damage type.
	 */
	public void attack(Damage damage){
		attack(entity, damage);
	}
	
	/**
	 *  Package-private method for attacking.  Allows for direct reference
	 *  to the entity variable through the interfaces without the need to
	 *  create a wrapper instance.
	 */
	static void attack(Entity attackedEntity, Damage damage){
		//If this entity is one of ours, just forward the damage and exit.
		if(attackedEntity instanceof BuilderEntity){
			((BuilderEntity) attackedEntity).entity.attack(damage);
			return;
		}
		DamageSource newSource = new DamageSource(damage.name){
			@Override
			public ITextComponent getDeathMessage(EntityLivingBase player){
				EntityLivingBase recentEntity = player.getAttackingEntity();
				if(recentEntity != null){//Player engaged with another player...
					if(damage.attacker != null){//and then was killed by another player.
						return new TextComponentTranslation("death.attack." + this.damageType + ".player.player", 
								new Object[] {player.getDisplayName(), damage.attacker.player.getDisplayName(), recentEntity.getDisplayName()});
					}else{//and then was killed by something.
						return new TextComponentTranslation("death.attack." + this.damageType + ".null.player", 
								new Object[] {player.getDisplayName(), recentEntity.getDisplayName()});
					}
				}else{//Player was minding their own business...
					if(damage.attacker != null){//and was killed by another player.
						return new TextComponentTranslation("death.attack." + this.damageType + ".player.null", 
								new Object[] {player.getDisplayName(), damage.attacker.player.getDisplayName()});
					}else{//and then was killed by something.
						return new TextComponentTranslation("death.attack." + this.damageType + ".null.null", 
								new Object[] {player.getDisplayName()});
					}
				}
			}
		};
		if(damage.isFire){
			newSource.setFireDamage();
			attackedEntity.setFire(5);
		}
		if(damage.isWater){
			attackedEntity.extinguish();
			//Don't attack this entity with water.
			return;
		}
		if(damage.isExplosion){
			newSource.setExplosion();
		}
		if(damage.ignoreArmor){
			newSource.setDamageBypassesArmor();
		}
		if(ConfigSystem.configObject.general.creativeDamage.value){
			newSource.setDamageAllowedInCreativeMode();
		}
		attackedEntity.attackEntityFrom(newSource, (float) damage.amount);
		if(attackedEntity instanceof EntityLivingBase){
			((EntityLivingBase) attackedEntity).hurtResistantTime = 0;
		}
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
}