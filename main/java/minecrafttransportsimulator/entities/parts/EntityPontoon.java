package minecrafttransportsimulator.entities.parts;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.entities.core.EntityMultipartVehicle;
import minecrafttransportsimulator.entities.main.EntityGroundDevice;
import minecrafttransportsimulator.helpers.EntityHelper;
import minecrafttransportsimulator.helpers.WorldHelper;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import java.util.List;

public class EntityPontoon extends EntityGroundDevice{
	protected String otherHalfUUID;
	protected EntityPontoon otherHalf;
	private List boxList;
	
	public EntityPontoon(World world){
		super(world);
	}
	
	public EntityPontoon(World world, EntityMultipartParent vehicle, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		this(world, vehicle, parentUUID, offsetX, offsetY, offsetZ);
		this.otherHalf = new EntityPontoonDummy(world, vehicle, parentUUID, offsetX, offsetY, offsetZ - 2);
		this.setOtherHalf(otherHalf);
		otherHalf.setOtherHalf(this);
		vehicle.addChild(this.otherHalfUUID, otherHalf, true);
	}
	
	protected EntityPontoon(World world, EntityMultipartParent vehicle, String parentUUID, float offsetX, float offsetY, float offsetZ){
		super(world, (EntityMultipartVehicle) vehicle, parentUUID, offsetX, offsetY, offsetZ, 0.75F, 0.75F, 0.1F, 2.5F);
	}
	
	@Override
	public void setNBTFromStack(ItemStack stack){}

	@Override
	public ItemStack getItemStack(){
		return new ItemStack(MTSRegistry.pontoon);
	}
	
	@Override
	public void onUpdate(){
		super.onUpdate();
		if(!linked){return;}
		if(otherHalf == null){
			if(ticksExisted==1 || ticksExisted%10==0){
				this.linkToOtherHalf();
			}else if(this.ticksExisted>100){
				System.err.println("KILLING ORPHANED PONTOON HALF!");
				this.setDead();
			}
			return;
		}
		if(WorldHelper.isPositionInLiquid(worldObj, posX, posY + 1, posZ)){
			//Plane dive-bombed into the water.
			parent.removeChild(UUID, true);
		}
	}
	
	@Override
	protected boolean attackChild(DamageSource source, float damage){
		return true;
	}
	
	@Override
	public void setDead(){
		super.setDead();
		if(otherHalf != null){
			if(!otherHalf.isDead){
				if(parent != null){
					this.parent.removeChild(otherHalfUUID, false);
				}
			}
		}
	}
	
	@Override
	public boolean collidesWithLiquids(){
		return true;
	}
	
	private void linkToOtherHalf(){
		Entity entity = EntityHelper.getEntityByUUID(worldObj, otherHalfUUID);
		if(entity != null){
			this.otherHalf=(EntityPontoon) entity;
		}
	}
	
	public void setOtherHalf(EntityPontoon otherHalf){
		this.otherHalf = otherHalf;
		this.otherHalfUUID = otherHalf.UUID;
		
		otherHalf.otherHalf = this;
		otherHalf.otherHalfUUID = this.UUID;
	}

	@Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.otherHalfUUID=tagCompound.getString("otherHalfUUID");
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setString("otherHalfUUID", this.otherHalfUUID);
		return tagCompound;
	}
	
	public static class EntityPontoonDummy extends EntityPontoon{
		public EntityPontoonDummy(World world){
			super(world);
		}
		
		public EntityPontoonDummy(World world, EntityMultipartParent vehicle, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
			this(world, vehicle, parentUUID, offsetX, offsetY, offsetZ);
			this.otherHalf = new EntityPontoon(world, vehicle, parentUUID, offsetX, offsetY, offsetZ + 2);
			this.setOtherHalf(otherHalf);
			otherHalf.setOtherHalf(this);
			vehicle.addChild(this.otherHalfUUID, otherHalf, true);
		}
		
		public EntityPontoonDummy(World world, EntityMultipartParent vehicle, String parentUUID, float offsetX, float offsetY, float offsetZ){
			super(world, vehicle, parentUUID, offsetX, offsetY, offsetZ);
		}
		
		@Override
		public ItemStack getItemStack(){
			return null;
		}
	}
}
