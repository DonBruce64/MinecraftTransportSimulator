package mcinterface1122;

import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemLead;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.Potion;

class WrapperEntity implements IWrapperEntity{
	final Entity entity;
	
	WrapperEntity(Entity entity){
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
	
	@Override
	public boolean isValid(){
		return entity != null && !entity.isDead;
	}
	
	@Override
	public int getID(){
		return entity.getEntityId();
	}
	
	@Override
	public AEntityBase getEntityRiding(){
		return entity.getRidingEntity() instanceof BuilderEntity ? ((BuilderEntity) entity.getRidingEntity()).entity : null;
	}
	
	@Override
	public void setRiding(AEntityBase entityToRide){
		if(entityToRide != null){
			entity.startRiding(BuilderEntity.createdServerBuilders.get(entityToRide), true);
		}else{
			entity.dismountRidingEntity();
		}
	}
	
	@Override
	public double getSeatOffset(){
		return 0D;
	}
	
	@Override
	public double getEyeHeight(){
		return entity.getEyeHeight();
	}
	
	@Override
	public Point3d getPosition(){
		mutablePosition.set(entity.posX, entity.posY, entity.posZ);
		return mutablePosition;
	}
	private final Point3d mutablePosition = new Point3d(0D, 0D, 0D);
	
	@Override
	public void setPosition(Point3d position){
		entity.setPosition(position.x, position.y, position.z);
	}
	
	@Override
	public Point3d getVelocity(){
		mutableVelocity.set(entity.motionX, entity.motionY, entity.motionZ);
		return mutableVelocity;
	}
	private final Point3d mutableVelocity = new Point3d(0D, 0D, 0D);
	
	@Override
	public void setVelocity(Point3d motion){
		entity.motionX = motion.x;
		entity.motionY = motion.y;
		entity.motionZ = motion.z;
	}
	
	@Override
	public float getPitch(){
		return entity.rotationPitch;
	}
	
	@Override
	public float getYaw(){
		return -entity.rotationYaw;
	}
	
	@Override
	public float getHeadYaw(){
		return -entity.getRotationYawHead();
	}
	
	@Override
	public void setYaw(double yaw){
		entity.rotationYaw = (float)-yaw;
	}
	
	@Override
	public void setHeadYaw(double yaw){
		entity.setRotationYawHead((float)-yaw);
	}
	
	@Override
	public void setPitch(double pitch){
		entity.rotationPitch = (float)pitch;
	}
	
	@Override
	public IWrapperNBT getNBT(){
		NBTTagCompound tag = new NBTTagCompound();
		entity.writeToNBT(tag);
		return new WrapperNBT(tag);
	}
	
	@Override
	public void setNBT(IWrapperNBT data){
		entity.readFromNBT(((WrapperNBT) data).tag);
	}
	
	@Override
	public boolean leashTo(IWrapperPlayer playerWrapper){
		EntityPlayer player = ((WrapperPlayer) playerWrapper).player;
		if(entity instanceof EntityLiving){
			ItemStack heldStack = player.getHeldItemMainhand();
			if(((EntityLiving) entity).canBeLeashedTo(player) && heldStack.getItem() instanceof ItemLead){
				((EntityLiving)entity).setLeashHolder(player, true);
				if(!player.isCreative()){
					heldStack.shrink(1);
				}
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void attack(Damage damage){
		attack(entity, damage);
	}
	
	@Override
	public Point3d getRenderedPosition(float partialTicks){
		mutableRenderPosition.x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
		mutableRenderPosition.y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
		mutableRenderPosition.z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
		return mutableRenderPosition;
	}
	private final Point3d mutableRenderPosition = new Point3d(0D, 0D, 0D);
	
	@Override
	public void addEffect(String potionEffectName, int durationIn, int amplifierIn) {
		// Only instances of EntityLivingBase can receive potion effects
		if((entity instanceof EntityLivingBase)) {
			PotionEffect potionEffectIn = new PotionEffect(Potion.getPotionFromResourceLocation(potionEffectName), durationIn, amplifierIn, false, false);
			if(potionEffectIn == null) {
				throw new NullPointerException("Potion " + potionEffectName + " does not exist.");
			}
			else {
				((EntityLivingBase)entity).addPotionEffect(potionEffectIn);
			}
		}
	}
	
	@Override
	public void removeEffect(String potionEffectName) {
		// Only instances of EntityLivingBase can have potion effects
		if((entity instanceof EntityLivingBase)) {
			// Uses a potion here instead of potionEffect because the duration/amplifier is irrelevant
			Potion potionIn = Potion.getPotionFromResourceLocation(potionEffectName);
			if(potionIn == null) {
				throw new NullPointerException("Potion " + potionEffectName + " does not exist.");
			}
			else {
				((EntityLivingBase)entity).removePotionEffect(potionIn);
			}
		}
	}
	
	/**
	 *  Package-private method for attacking.  Allows for direct reference
	 *  to the entity variable through the interfaces without the need to
	 *  create a wrapper instance for attacking entities.
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
								new Object[] {player.getDisplayName(), ((WrapperEntity) damage.attacker).entity.getDisplayName(), recentEntity.getDisplayName()});
					}else{//and then was killed by something.
						return new TextComponentTranslation("death.attack." + this.damageType + ".null.player", 
								new Object[] {player.getDisplayName(), recentEntity.getDisplayName()});
					}
				}else{//Player was minding their own business...
					if(damage.attacker != null){//and was killed by another player.
						return new TextComponentTranslation("death.attack." + this.damageType + ".player.null", 
								new Object[] {player.getDisplayName(), ((WrapperEntity) damage.attacker).entity.getDisplayName()});
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
		if(damage.ignoreCooldown && attackedEntity instanceof EntityLivingBase){
			((EntityLivingBase) attackedEntity).hurtResistantTime = 0;
		}
		if(ConfigSystem.configObject.general.creativeDamage.value){
			newSource.setDamageAllowedInCreativeMode();
		}
		attackedEntity.attackEntityFrom(newSource, (float) damage.amount);
	}
}