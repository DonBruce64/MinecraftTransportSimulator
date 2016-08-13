package minecraftflightsimulator.entities.core;

import java.util.List;

import minecraftflightsimulator.utilities.MFSVector;
import minecraftflightsimulator.utilities.RotationHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/**Main child class.  This class is the base for all child entities and should be
 * extended to use the parent-child linking system.  Many helper functions are present
 * to allow re-directing methods that Mojang/Forge changes frequently (like block detection).
 * Use {@link EntityParent#addChild(String, EntityChild, boolean)} to add children 
 * and {@link EntityParent#removeChild(String, boolean)} to kill and remove them.
 * You may extend {@link EntityParent} to get more functionality with those system.
 * Beware of children with offsetZ of 0, as they can cause problems with pitch calculations.
 * 
 * @author don_bruce
 */
public abstract class EntityChild extends EntityBase{	
	public int propertyCode;
	public float offsetX;
	public float offsetY;
	public float offsetZ;
	public EntityParent parent;
	protected String parentUUID;
	
	public EntityChild(World world) {
		super(world);
	}
	
	public EntityChild(World world, EntityParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, float width, float height, int propertyCode){
		this(world);
		this.motionX=0;
		this.motionY=0;
		this.motionZ=0;
		this.offsetX=offsetX;
		this.offsetY=offsetY;
		this.offsetZ=offsetZ;
		this.setSize(width, height);
		this.propertyCode=propertyCode;
		this.UUID=String.valueOf(this.getUniqueID());
		this.parentUUID=parentUUID;
		MFSVector offset = RotationHelper.getRotatedPoint(offsetX, offsetY, offsetZ, parent.rotationPitch, parent.rotationYaw, parent.rotationRoll);
		this.setPositionAndRotation(parent.posX+offset.xCoord, parent.posY+offset.yCoord, parent.posZ+offset.zCoord, parent.rotationYaw, parent.rotationPitch);
	}
	
	@Override
	protected void entityInit(){}
	
	@Override
	public void onEntityUpdate(){
		super.onEntityUpdate();
		linked = hasUUID() && hasParent();
	}
	
	@Override
    public boolean performRightClickAction(EntityPlayer player){
		return parent != null ? parent.performRightClickAction(player) : false;
	}
	
	@Override
	public boolean performAttackAction(DamageSource source, float damage){
		return parent != null ? parent.performAttackAction(source, damage) : false;
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
	public boolean canBeCollidedWith(){
		return true;
	}
	
	@Override
	public AxisAlignedBB getBoundingBox(){
		return this.getEntityBoundingBox();
	}
	
	public List getCollidingBlocks(AxisAlignedBB box){
		return worldObj.func_147461_a(box);
	}
	
	public boolean isOnGround(){
		return isBlockAtLocation(posX, posY - 0.05, posZ);
	}
	
	public boolean isLiquidAt(double x, double y, double z){
		return worldObj.getBlock(MathHelper.floor_double(x), MathHelper.floor_double(y), MathHelper.floor_double(z)).getMaterial().isLiquid();
	}
	
	public boolean isCollidedHorizontally(){
		return isBlockAtLocation(posX, posY, posZ)
				|| isBlockAtLocation(posX + this.width, posY, posZ)
				|| isBlockAtLocation(posX, posY, posZ + this.width)
				|| isBlockAtLocation(posX + this.width, posY, posZ + this.width);
	}
	
	public boolean willCollideVerticallyWithOffset(double offsetX, double offsetY, double offsetZ){
		return isBlockAtLocation(posX + offsetX, posY + offsetY, posZ + offsetZ)
			|| isBlockAtLocation(posX + offsetX, posY + offsetY + this.height, posZ + offsetY);
	}
	
	protected boolean isBlockAtLocation(double x, double y, double z){
		return worldObj.getBlock(MathHelper.floor_double(x), MathHelper.floor_double(y), MathHelper.floor_double(z)).getCollisionBoundingBoxFromPool(worldObj, 0, 0, 0) != null;
	}
	
	public void setRider(Entity rider){
		rider.mountEntity(this);
	}
	
	public Entity getRider(){
		return this.riddenByEntity;
	}
	
	@Override
	public void updateRiderPosition(){
		//Rider updates are handled by the parent.
		return;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.propertyCode=tagCompound.getInteger("propertyCode");
		this.offsetX=tagCompound.getFloat("offsetX");
		this.offsetY=tagCompound.getFloat("offsetY");
		this.offsetZ=tagCompound.getFloat("offsetZ");
		this.parentUUID=tagCompound.getString("parentUUID");
		this.width=tagCompound.getFloat("width");
		this.height=tagCompound.getFloat("height");
    	this.setSize(width, height);
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setInteger("propertyCode", this.propertyCode);
		tagCompound.setFloat("offsetX", this.offsetX);
		tagCompound.setFloat("offsetY", this.offsetY);
		tagCompound.setFloat("offsetZ", this.offsetZ);
		tagCompound.setFloat("width", this.width);
		tagCompound.setFloat("height", this.height);
		if(!this.parentUUID.isEmpty()){
			tagCompound.setString("parentUUID", this.parentUUID);
		}
	}
}
