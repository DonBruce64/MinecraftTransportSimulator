package minecraftflightsimulator.entities;

import minecraftflightsimulator.other.RotationHelper;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public abstract class EntityChild extends EntityBase{
	public int propertyCode;
	public float offsetX;
	public float offsetY;
	public float offsetZ;
	protected String parentUUID;
	public EntityParent parent;
	
	public EntityChild(World world) {
		super(world);
	}
	
	public EntityChild(World world, EntityParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		this(world);
		this.motionX=0;
		this.motionY=0;
		this.motionZ=0;
		this.offsetX=offsetX;
		this.offsetY=offsetY;
		this.offsetZ=offsetZ;
		this.propertyCode=propertyCode;
		this.UUID=String.valueOf(this.getUniqueID());
		this.parentUUID=parentUUID;
		Vec3 offset = RotationHelper.getRotatedPoint(offsetX, offsetY, offsetZ, parent.rotationPitch, parent.rotationYaw, parent.rotationRoll);
		this.setPositionAndRotation(parent.posX+offset.xCoord, parent.posY+offset.yCoord, parent.posZ+offset.zCoord, parent.rotationYaw, parent.rotationPitch);
	}
	
	@Override
	protected void entityInit(){}
	
	@Override
	public void onEntityUpdate(){
		super.onEntityUpdate();
		if(!hasUUID() || !hasParent()){
			linked=false;
		}else{
			linked=true;
		}
	}
	
	public boolean hasParent(){
		if(this.parent==null){
			if(ticksExisted==1 || ticksExisted%10==0){
				this.linkToParent();
			}else if(this.ticksExisted>100){
				System.err.println("KILLING ORPHANED CHILD!");
				this.setDead();
			}
			return false;
		}
		return true;
	}
	
	private void linkToParent(){
		for(int i=0; i<this.worldObj.loadedEntityList.size(); ++i){
			Entity entity = (Entity) this.worldObj.loadedEntityList.get(i);
			if(entity instanceof EntityParent){
				EntityParent parent =  (EntityParent) entity;
				if(parent.UUID != null){
					if(parent.UUID.equals(this.parentUUID)){
						parent.addChild(this.UUID, this, false);
						this.parent=parent;
					}
				}
			}
		}
	}
	
	@Override
	public boolean attackEntityFrom(DamageSource source, float damage){
		super.attackEntityFrom(source, damage);
		if(this.parent!=null){
			parent.attackEntityFrom(source, damage);
		}
		return false;
    }
	
	@Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.propertyCode=tagCompound.getInteger("propertyCode");
		this.offsetX=tagCompound.getFloat("offsetX");
		this.offsetY=tagCompound.getFloat("offsetY");
		this.offsetZ=tagCompound.getFloat("offsetZ");
		this.parentUUID=tagCompound.getString("parentUUID");
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setInteger("propertyCode", this.propertyCode);
		tagCompound.setFloat("offsetX", this.offsetX);
		tagCompound.setFloat("offsetY", this.offsetY);
		tagCompound.setFloat("offsetZ", this.offsetZ);
		tagCompound.setString("parentUUID", this.parentUUID);
	}
}
