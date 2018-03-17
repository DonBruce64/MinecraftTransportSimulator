package minecrafttransportsimulator.entities.parts;

import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSDamageSources.DamageSourceWheel;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.entities.main.EntityCar;
import minecrafttransportsimulator.entities.main.EntityGroundDevice;
import minecrafttransportsimulator.packets.general.FlatWheelPacket;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.SFXSystem;
import minecrafttransportsimulator.systems.SFXSystem.SFXEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class EntityWheel extends EntityGroundDevice implements SFXEntity{
	public boolean isFlat;
	public float angularPosition;
	public float angularVelocity;
	private boolean landedThisTick = false;
	private EntityMultipartMoving mover;
	
	public EntityWheel(World world){
		super(world);
	}
	
	public EntityWheel(World world, EntityMultipartMoving moving, String parentUUID, float offsetX, float offsetY, float offsetZ){
		super(world, moving, parentUUID, offsetX, offsetY, offsetZ, 0.5F, 0.5F);
	}
	
	@Override
	public void setNBTFromStack(ItemStack stack){}
	
	@Override
	public void attackPart(DamageSource source, float damage){
		if(!isFlat){
			if(source.isExplosion() || Math.random() < 0.1){
				setFlat();
			}
		}
	}
	
	@Override
	public void onUpdate(){
		super.onUpdate();
		if(!linked){return;}
		mover = (EntityMultipartMoving) this.parent;
		if(this.isOnGround()){
			if(angularVelocity/(mover.velocity/getHeight()) < 0.25 && mover.velocity > 0.3){
				if(worldObj.getBlockState(this.getPosition().down()).getBlockHardness(worldObj, this.getPosition().down()) >= 1.5){
					landedThisTick = true;
				}
			}
			angularVelocity = (float) (mover.velocity/getHeight());
			
			if(!worldObj.isRemote && mover.velocity > 0.2F){
				List<EntityLivingBase> collidedEntites = worldObj.getEntitiesWithinAABB(EntityLivingBase.class, this.getEntityBoundingBox().expand(0.25F, 0, 0.25F));
				if(!collidedEntites.isEmpty()){
					Entity attacker = null;
					for(EntityMultipartChild child : parent.getChildren()){
						if(child instanceof EntitySeat){
							EntitySeat seat = (EntitySeat) child;
							if(seat.isController){
								if(seat.getPassenger() != null){
									attacker = seat.getPassenger();
									break;
								}
							}
						}
						
					}
					for(int i=0; i < collidedEntites.size(); ++i){
						if(!(collidedEntites.get(i).getRidingEntity() instanceof EntitySeat)){
							collidedEntites.get(i).attackEntityFrom(new DamageSourceWheel(attacker), (float) (ConfigSystem.getDoubleConfig("WheelDamageFactor")*mover.velocity*mover.currentMass/1000F));
						}
					}
				}
			}
		}else if(!(parent instanceof EntityCar)){
			if(mover.brakeOn || mover.parkingBrakeOn){
				angularVelocity = 0;
			}else if(angularVelocity>0){
				angularVelocity = (float) Math.max(angularVelocity - 0.05, 0);
			}
		}
		angularPosition += angularVelocity;
		
		if(worldObj.isRemote){
			MTS.proxy.updateSFXEntity(this, worldObj);
		}
	}
	
	public void setFlat(){
		isFlat = true;
		this.motiveFriction*=10;
		this.lateralFriction*=0.1;
		if(!worldObj.isRemote){
			worldObj.newExplosion(this, posX, posY, posZ, 0.25F, false, false);
			MTS.MTSNet.sendToAll(new FlatWheelPacket(this.getEntityId()));
		}
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public MovingSound getNewSound(){
		return null;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public MovingSound getCurrentSound(){
		return null;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void setCurrentSound(MovingSound sound){}
	
	@Override
	@SideOnly(Side.CLIENT)
	public boolean shouldSoundBePlaying(){
		return false;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public float getVolume(){
		return 0.0F;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public float getPitch(){
		return 0.0F;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void spawnParticles(){
		if(landedThisTick){
			for(byte i=0; i<4; ++i){
				Minecraft.getMinecraft().effectRenderer.addEffect(new SFXSystem.WhiteSmokeFX(worldObj, posX, posY, posZ, Math.random()*0.10 - 0.05, 0.15, Math.random()*0.10 - 0.05));
			}
			MTS.proxy.playSound(this, MTS.MODID + ":" + "wheel_striking", 1, 1);
			landedThisTick = false;
		}
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.isFlat=tagCompound.getBoolean("isFlat");
	}
    
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setBoolean("isFlat", this.isFlat);
		return tagCompound;
	}
	
	public static class EntityWheelSmall extends EntityWheel{
		public EntityWheelSmall(World world){
			super(world);
		}
		
		public EntityWheelSmall(World world, EntityMultipartParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
			super(world, (EntityMultipartMoving) parent, parentUUID, offsetX, offsetY, offsetZ);
		}
		
		@Override
		public float getWidth(){
			return 0.25F;
		}
		
		@Override
		public float getHeight(){
			return this.isFlat ? 0.25F : 0.5F;
		}
		
		@Override
		public ItemStack getItemStack(){
			if(!this.isFlat){
				return new ItemStack(MTSRegistry.wheelSmall);	
			}else{
				return null;
			}
		}
	}
	
	public static class EntityWheelMedium extends EntityWheel{
		public EntityWheelMedium(World world){
			super(world);
		}
		
		public EntityWheelMedium(World world, EntityMultipartParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
			super(world, (EntityMultipartMoving) parent, parentUUID, offsetX, offsetY, offsetZ);
		}
		
		@Override
		public float getWidth(){
			return 0.5F;
		}
		
		@Override
		public float getHeight(){
			return this.isFlat ? 0.375F : 0.75F;
		}

		@Override
		public ItemStack getItemStack(){
			if(!this.isFlat){
				return new ItemStack(MTSRegistry.wheelMedium);	
			}else{
				return null;
			}
		}
	}
	
	public static class EntityWheelLarge extends EntityWheel{
		public EntityWheelLarge(World world){
			super(world);
		}
		
		public EntityWheelLarge(World world, EntityMultipartParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
			super(world, (EntityMultipartMoving) parent, parentUUID, offsetX, offsetY, offsetZ);
		}
		
		@Override
		public float getWidth(){
			return 0.5F;
		}
		
		@Override
		public float getHeight(){
			return this.isFlat ? 0.5F : 1.0F;
		}

		@Override
		public ItemStack getItemStack(){
			if(!this.isFlat){
				return new ItemStack(MTSRegistry.wheelLarge);	
			}else{
				return null;
			}
		}
	}
}
