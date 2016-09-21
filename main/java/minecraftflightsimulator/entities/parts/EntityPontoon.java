package minecraftflightsimulator.entities.parts;

import java.util.List;

import minecraftflightsimulator.entities.core.EntityFlyable;
import minecraftflightsimulator.entities.core.EntityLandingGear;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
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
	public List getCollidingBlocks(AxisAlignedBB box){
		//Kill lilypads if present.
		for(int i=MathHelper.floor_double(box.minX); i<MathHelper.floor_double(box.maxX) + 1; ++i){
			for(int j=MathHelper.floor_double(box.minZ); j<MathHelper.floor_double(box.maxZ) + 1; ++j){
				for(int k=MathHelper.floor_double(box.minY); k<MathHelper.floor_double(box.maxY) + 1; ++k){
					if(Blocks.waterlily.equals(getBlockAtLocation(i, k, j))){
						worldObj.setBlockToAir(i, k, j);
					}
				}
			}
		}
		boxList = super.getCollidingBlocks(box);
		if(isLiquidAt(box.minX + (box.maxX - box.minX)/2, box.minY + (box.maxY - box.minY)/2 , box.minZ + (box.maxZ - box.minZ)/2)){
			boxList.add(box);
		}
		return boxList;
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
