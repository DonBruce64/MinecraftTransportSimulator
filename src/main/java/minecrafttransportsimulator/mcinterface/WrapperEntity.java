package minecrafttransportsimulator.mcinterface;

import minecrafttransportsimulator.baseclasses.AEntityA_Base;
import minecrafttransportsimulator.baseclasses.AEntityD_Interactable;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.jsondefs.JSONPotionEffect;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemLead;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

/**Wrapper for the base Entity class.  This class mainly allows for interaction with position
 * and motion variables for entities, as well as setting their riding statuses.
 *
 * @author don_bruce
 */
public class WrapperEntity{
	public final Entity entity;
	
	public WrapperEntity(Entity entity){
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
	 *  Returns the world this entity is in.
	 */
	public WrapperWorld getWorld(){
		return WrapperWorld.getWrapperFor(entity.world);
	}
	
	/**
	 *  Returns the entity this entity is riding, or null if
	 *  the entity is not riding any MTS entity (rider may will be riding
	 *  a vanilla entity).
	 */
	public AEntityD_Interactable<?> getEntityRiding(){
		return entity.getRidingEntity() instanceof BuilderEntity ? (AEntityD_Interactable<?>) ((BuilderEntity) entity.getRidingEntity()).entity : null;
	}
	
	/**
	 *  Tells the entity to start riding the passed-in entity.
	 *  If null is passed-in, then this rider will stop riding whatever entity it
	 *  is riding, if it was riding any entity.
	 */
	public void setRiding(AEntityD_Interactable<?> entityToRide){
		if(entityToRide != null){
			entity.startRiding(entityToRide.wrapper.entity, true);
		}else{
			entity.dismountRidingEntity();
		}
	}
	
	/**
	 *  If the wrapped entity is an AEntityBase, return that
	 *  base entity. Otherwise return null.
	 */
	public AEntityA_Base getBaseEntity(){
		return entity instanceof BuilderEntity ? ((BuilderEntity) entity).entity : null;
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
	 *  The returned position may by modified without affecting the entity's actual position.
	 *  However, the object itself may be re-used on the next call, so do not keep reference to it.
	 */
	public Point3d getPosition(){
		mutablePosition.set(entity.posX, entity.posY, entity.posZ);
		return mutablePosition;
	}
	private final Point3d mutablePosition = new Point3d();
	
	/**
	 *  Sets the entity's position to the passed-in point.
	 */
	public void setPosition(Point3d position){
		entity.setPosition(position.x, position.y, position.z);
	}
	
	/**
	 *  Gets the entity's velocity as a vector.
	 *  The returned velocity may by modified without affecting the entity's actual velocity.
	 *  However, the object itself may be re-used on the next call, so do not keep reference to it.
	 */
	public Point3d getVelocity(){
		mutableVelocity.set(entity.motionX, entity.motionY, entity.motionZ);
		return mutableVelocity;
	}
	private final Point3d mutableVelocity = new Point3d();
	
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
	 *  Returns the entity's head yaw (y-axis rotation).
	 *  NOTE: the return value from this function is inverted
	 *  from the normal MC standard to have it follow the RHR
	 *  for rotations.  This is OpenGL convention, and MC doesn't
	 *  follow it, which is why rendering is such a PITA with yaw.
	 */
	public float getHeadYaw(){
		return -entity.getRotationYawHead();
	}
	
	/**
	 *  Returns a vector in the direction of the entity's line of sight,
	 *  with a magnitude equal to the passed-in distance. 
	 */
	public Point3d getLineOfSight(double distance){
		return (new Point3d(0D, 0D, distance)).rotateFine(new Point3d(entity.rotationPitch, 0D, 0D)).rotateFine(new Point3d(0D, -entity.rotationYaw, 0));
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
	 *  Sets the entity's head yaw to the passed-in yaw.
	 *  NOTE: the yaw value from this function is inverted
	 *  from the normal MC standard to have it follow the RHR
	 *  for rotations.  This is OpenGL convention, and MC doesn't
	 *  follow it, which is why rendering is such a PITA with yaw.
	 */
	public void setHeadYaw(double yaw){
		entity.setRotationYawHead((float)-yaw);
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
	public WrapperNBT getData(){
		NBTTagCompound tag = new NBTTagCompound();
		entity.writeToNBT(tag);
		return new WrapperNBT(tag);
	}
	
	/**
	 *  Loads the entity data from the passed-in NBT.
	 */
	public void setData(WrapperNBT data){
		entity.readFromNBT(data.tag);
	}
	
	/**
	 *  Tries to leash up the entity to the passed-in player.
	 *  False may be returned if the entity cannot be leashed,
	 *  or if the player isn't holding a leash.
	 */
	public boolean leashTo(WrapperPlayer player){
		EntityPlayer mcPlayer = player.player;
		if(entity instanceof EntityLiving){
			ItemStack heldStack = mcPlayer.getHeldItemMainhand();
			if(((EntityLiving) entity).canBeLeashedTo(mcPlayer) && heldStack.getItem() instanceof ItemLead){
				((EntityLiving)entity).setLeashHolder(mcPlayer, true);
				if(!mcPlayer.isCreative()){
					heldStack.shrink(1);
				}
				return true;
			}
		}
		return false;
	}
	
	/**
	 *  Attacks the entity.
	 */
	public void attack(Damage damage){
		//If this entity is one of ours, just forward the damage and exit.
		if(entity instanceof BuilderEntity){
			if(((BuilderEntity) entity).entity instanceof AEntityD_Interactable){
				((AEntityD_Interactable<?>) ((BuilderEntity) entity).entity).attack(damage);
				return;
			}
		}
		DamageSource newSource = new DamageSource(damage.name){
			@Override
			public ITextComponent getDeathMessage(EntityLivingBase player){
				EntityLivingBase recentEntity = player.getAttackingEntity();
				if(recentEntity != null){//Player engaged with another player...
					if(damage.attacker != null){//and then was killed by another player.
						return new TextComponentTranslation("death.attack." + this.damageType + ".player.player", 
								new Object[] {player.getDisplayName(), damage.attacker.entity.getDisplayName(), recentEntity.getDisplayName()});
					}else{//and then was killed by something.
						return new TextComponentTranslation("death.attack." + this.damageType + ".null.player", 
								new Object[] {player.getDisplayName(), recentEntity.getDisplayName()});
					}
				}else{//Player was minding their own business...
					if(damage.attacker != null){//and was killed by another player.
						return new TextComponentTranslation("death.attack." + this.damageType + ".player.null", 
								new Object[] {player.getDisplayName(), damage.attacker.entity.getDisplayName()});
					}else{//and then was killed by something.
						return new TextComponentTranslation("death.attack." + this.damageType + ".null.null", 
								new Object[] {player.getDisplayName()});
					}
				}
			}
		};
		if(damage.isFire){
			newSource.setFireDamage();
			entity.setFire(5);
		}
		if(damage.isWater){
			entity.extinguish();
			//Don't attack this entity with water.
			return;
		}
		if(damage.isExplosion){
			newSource.setExplosion();
		}
		if(damage.ignoreArmor){
			newSource.setDamageBypassesArmor();
		}
		if(damage.ignoreCooldown && entity instanceof EntityLivingBase){
			((EntityLivingBase) entity).hurtResistantTime = 0;
		}
		if(ConfigSystem.configObject.general.creativeDamage.value){
			newSource.setDamageAllowedInCreativeMode();
		}
		entity.attackEntityFrom(newSource, (float) damage.amount);
	}
	
	/**
	 *  Returns the rendered position based on the passed-in partial ticks.
	 *  The returned position may by modified without affecting the actual rendered position.
	 *  However, the object itself may be re-used on the next call, so do not keep reference to it.
	 */
	public Point3d getRenderedPosition(float partialTicks){
		mutableRenderPosition.x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
		mutableRenderPosition.y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
		mutableRenderPosition.z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
		return mutableRenderPosition;
	}
	private final Point3d mutableRenderPosition = new Point3d();
	
	/**
	 * Adds the potion effect with the specified name to the entity.  Only valid for living entities that
	 * are effected by potions.
	 */
	public void addPotionEffect(JSONPotionEffect effect){
		// Only instances of EntityLivingBase can receive potion effects
		if((entity instanceof EntityLivingBase)){
			Potion potion = Potion.getPotionFromResourceLocation(effect.name);
			if(potion != null){
				((EntityLivingBase)entity).addPotionEffect(new PotionEffect(potion, effect.duration, effect.amplifier, false, false));
			}else{
				throw new NullPointerException("Potion " + effect.name + " does not exist.");
			}
		}
	}
	
	/**
	 * Removes the potion effect with the specified name from the entity.  Only valid for living entities that
	 * are effected by potions.
	 */
	public void removePotionEffect(JSONPotionEffect effect){
		// Only instances of EntityLivingBase can have potion effects
		if((entity instanceof EntityLivingBase)){
			//Uses a potion here instead of potionEffect because the duration/amplifier is irrelevant
			Potion potion = Potion.getPotionFromResourceLocation(effect.name);
			if(potion != null){
				((EntityLivingBase)entity).removePotionEffect(potion);
			}else{
				throw new NullPointerException("Potion " + effect.name + " does not exist.");
			}
		}
	}
}