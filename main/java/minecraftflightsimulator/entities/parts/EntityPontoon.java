package minecraftflightsimulator.entities.parts;

import java.util.List;

import minecraftflightsimulator.dataclasses.MTSRegistry;
import minecraftflightsimulator.entities.core.EntityGroundDevice;
import minecraftflightsimulator.entities.core.EntityParent;
import minecraftflightsimulator.entities.core.EntityVehicle;
import minecraftflightsimulator.minecrafthelpers.BlockHelper;
import minecraftflightsimulator.minecrafthelpers.EntityHelper;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class EntityPontoon extends EntityGroundDevice{
	protected String otherHalfUUID;
	protected EntityPontoon otherHalf;
	private List boxList;
	
	public EntityPontoon(World world){
		super(world);
	}
	
	public EntityPontoon(World world, EntityParent vehicle, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		super(world, (EntityVehicle) vehicle, parentUUID, offsetX, offsetY, offsetZ, 0.75F, 0.75F, 0.1F, 2.5F);
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
		if(BlockHelper.isPositionInLiquid(worldObj, posX, posY + 1, posZ)){
			//Plane dive-bombed into the water.
			parent.removeChild(UUID, true);
		}
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
	public void writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setString("otherHalfUUID", this.otherHalfUUID);
	}
}
