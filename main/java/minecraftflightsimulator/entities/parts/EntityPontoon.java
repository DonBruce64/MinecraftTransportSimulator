package minecraftflightsimulator.entities.parts;

import java.util.List;

import minecraftflightsimulator.entities.core.EntityFlyable;
import minecraftflightsimulator.entities.core.EntityLandingGear;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class EntityPontoon extends EntityLandingGear{
	protected String otherHalfUUID;
	protected EntityPontoon otherHalf;
	private List boxList;
	
	public EntityPontoon(World world){
		super(world);
	}
	
	public EntityPontoon(World world, EntityFlyable flyer, String parentUUID, float offsetX, float offsetY, float offsetZ){
		super(world, flyer, parentUUID, offsetX, offsetY, offsetZ, 0.75F, 0.75F);
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
		if(isLiquidAt(posX, posY + 1, posZ)){
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
	public boolean collidesWithLiqids(){
		return true;
	}
	
	private void linkToOtherHalf(){
		for(int i=0; i<this.worldObj.loadedEntityList.size(); ++i){
			Entity entity = (Entity) this.worldObj.loadedEntityList.get(i);
			if(entity instanceof EntityPontoon){
				EntityPontoon otherHalf =  (EntityPontoon) entity;
				if(otherHalf.UUID != null){
					if(otherHalf.UUID.equals(this.otherHalfUUID)){
						this.otherHalf=otherHalf;
					}
				}
			}
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
