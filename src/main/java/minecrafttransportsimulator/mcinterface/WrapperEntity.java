package minecrafttransportsimulator.mcinterface;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Orientation3d;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
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
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**Wrapper for the base Entity class.  This class mainly allows for interaction with position
 * and motion variables for entities, as well as setting their riding statuses.
 *
 * @author don_bruce
 */
@EventBusSubscriber
public class WrapperEntity{
	private static final Map<Entity, WrapperEntity> entityWrappers = new HashMap<Entity, WrapperEntity>();
	
	protected final Entity entity;
	
	/**
	 *  Returns a wrapper instance for the passed-in entity instance.
	 *  Null may be passed-in safely to ease function-forwarding.
	 *  Wrapper is cached to avoid re-creating the wrapper each time it is requested.
	 *  If the entity is a player, then a player wrapper is returned.
	 */
	public static WrapperEntity getWrapperFor(Entity entity){
		if(entity instanceof EntityPlayer){
			return WrapperPlayer.getWrapperFor((EntityPlayer) entity);
		}else if(entity != null){
			WrapperEntity wrapper = entityWrappers.get(entity);
			if(wrapper == null || !wrapper.isValid() || entity != wrapper.entity){
				wrapper = new WrapperEntity(entity);
				entityWrappers.put(entity, wrapper);
			}
			return wrapper;
		}else{
			return null;
		}
	}
	
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
		return entity != null && !entity.isDead && (!(entity instanceof EntityLivingBase) || ((EntityLivingBase) entity).deathTime == 0);
	}
	
	/**
	 *  Returns the entity's global UUID.  This is an ID that's unique to every player on Minecraft.
	 *  Useful for assigning ownership where the entity ID of a player might change between sessions.
	 *  Also should be used during packets to ensure the proper entity is retrieved, as some mods (and
	 *  most Sponge servers) will muck up the entity ID maps and IDs will not be synchronized.
	 *  <br><br>
	 *  NOTE: While this ID isn't supposed to change, some systems WILL, in fact, change it.  Cracked
	 *  servers, and the nastiest of Bukkit systems will deliberately change the UUID of players, which,
	 *  when combined with their changing of entity IDs, makes server-client lookup impossible.
	 */
	public UUID getID(){
		return entity.getUniqueID();
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
	public AEntityE_Interactable<?> getEntityRiding(){
		return entity.getRidingEntity() instanceof BuilderEntityExisting ? (AEntityE_Interactable<?>) ((BuilderEntityExisting) entity.getRidingEntity()).entity : null;
	}
	
	/**
	 *  Tells the entity to start riding the passed-in entity.
	 *  If null is passed-in, then this rider will stop riding whatever entity it
	 *  is riding, if it was riding any entity.
	 */
	public void setRiding(AEntityE_Interactable<?> entityToRide){
		if(entityToRide != null){
			//Get the builder for this entity and set the player to riding it.
			AxisAlignedBB searchBounds = new AxisAlignedBB(new BlockPos(entityToRide.position.x, entityToRide.position.y, entityToRide.position.z)).grow(World.MAX_ENTITY_RADIUS);
			for(BuilderEntityExisting builder : getWorld().world.getEntitiesWithinAABB(BuilderEntityExisting.class, searchBounds)){
				if(entityToRide.equals(builder.entity)){
					entity.startRiding(builder, true);
					return;
				}
			}
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
	 *  Boolean is included to set ground state.  This should
	 *  be set if the entity is on another entity collision box,
	 *  but not if they are riding an entity.
	 */
	public void setPosition(Point3d position, boolean onGround){
		entity.setPosition(position.x, position.y, position.z);
		//Set fallDistance to 0 to prevent damage.
		entity.fallDistance = 0;
		entity.onGround = onGround;
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
	 *  Returns the entity's orientation.
	 *  The returned object is mutable and may be modified without
	 *  affecting the entity's state.
	 */
	public Orientation3d getOrientation(){
		mutableAngles.set(entity.rotationPitch, -entity.rotationYaw, 0);
		mutableOrientation.setAngles(mutableAngles);
		return mutableOrientation;
	}
	Point3d mutableAngles = new Point3d();
	Orientation3d mutableOrientation = new Orientation3d(new Point3d());
	
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
	 *  Returns the entity's body yaw (y-axis rotation).
	 *  NOTE: the return value from this function is inverted
	 *  from the normal MC standard to have it follow the RHR
	 *  for rotations.  This is OpenGL convention, and MC doesn't
	 *  follow it, which is why rendering is such a PITA with yaw.
	 */
	public float getBodyYaw(){
		return entity instanceof EntityLivingBase ? -((EntityLivingBase) entity).renderYawOffset : 0;
	}
	
	/**
	 *  Returns a vector in the direction of the entity's line of sight,
	 *  with a magnitude equal to the passed-in distance. 
	 *  The returned vector  may by modified without affecting the entity's actual line of sight.
	 *  However, the object itself may be re-used on the next call, so do not keep references to it.
	 */
	public Point3d getLineOfSight(double distance){
		mutableSightRotation.set(entity.rotationPitch, -entity.rotationYaw, 0);
		return mutableSight.set(0,  0,  distance).rotateFine(mutableSightRotation);
	}
	private final Point3d mutableSight = new Point3d();
	private final Point3d mutableSightRotation = new Point3d();
	
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
	 *  Sets the entity's bod yyaw to the passed-in yaw.
	 *  NOTE: the yaw value from this function is inverted
	 *  from the normal MC standard to have it follow the RHR
	 *  for rotations.  This is OpenGL convention, and MC doesn't
	 *  follow it, which is why rendering is such a PITA with yaw.
	 */
	public void setBodyYaw(double yaw){
		if(entity instanceof EntityLivingBase){
			((EntityLivingBase) entity).setRenderYawOffset((float) -yaw);
		}
	}
	
	/**
	 *  Sets the entity's pitch to the passed-in pitch.
	 */
	public void setPitch(double pitch){
		entity.rotationPitch = (float)pitch;
	}
	
	/**
	 *  Gets the entity's bounding box.
	 *  The returned velocity may by modified without affecting the entity's actual bounds.
	 *  However, the object itself may be re-used on the next call, so do not keep reference to it.
	 */
	public BoundingBox getBounds(){
		mutableBounds.widthRadius = entity.width/2F;
		mutableBounds.heightRadius = entity.height/2F;
		mutableBounds.depthRadius = entity.width/2F;
		mutableBounds.globalCenter.set(entity.posX, entity.posY + mutableBounds.heightRadius, entity.posZ);
		return mutableBounds;
	}
	private final BoundingBox mutableBounds = new BoundingBox(new Point3d(), 0, 0, 0);
	
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
		DamageSource newSource = new DamageSource(damage.name){
			@Override
			public ITextComponent getDeathMessage(EntityLivingBase player){
				EntityLivingBase recentEntity = player.getAttackingEntity();
				if(recentEntity != null){//Player engaged with another player...
					if(damage.entityResponsible != null){//and then was killed by another player.
						return new TextComponentTranslation("death.attack." + this.damageType + ".player.player", 
								new Object[] {player.getDisplayName(), damage.entityResponsible.entity.getDisplayName(), recentEntity.getDisplayName()});
					}else{//and then was killed by something.
						return new TextComponentTranslation("death.attack." + this.damageType + ".null.player", 
								new Object[] {player.getDisplayName(), recentEntity.getDisplayName()});
					}
				}else{//Player was minding their own business...
					if(damage.entityResponsible != null){//and was killed by another player.
						return new TextComponentTranslation("death.attack." + this.damageType + ".player.null", 
								new Object[] {player.getDisplayName(), damage.entityResponsible.entity.getDisplayName()});
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
		
		if(damage.effects != null && entity instanceof EntityLivingBase){
			for(JSONPotionEffect effect : damage.effects){
            	Potion potion = Potion.getPotionFromResourceLocation(effect.name);
    			if(potion != null){
    				((EntityLivingBase) entity).addPotionEffect(new PotionEffect(potion, effect.duration, effect.amplifier, false, true));
    			}else{
    				throw new NullPointerException("Potion " + effect.name + " does not exist.");
    			}
        	}
		}
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
	
	/**
     * Remove all entities from our maps if we unload the world.  This will cause duplicates if we don't.
     */
    @SubscribeEvent
    public static void on(WorldEvent.Unload event){
    	Iterator<Entity> iterator = entityWrappers.keySet().iterator();
    	while(iterator.hasNext()){
    		if(iterator.next().world.equals(event.getWorld())){
    			iterator.remove();
    		}
    	}
    }
}