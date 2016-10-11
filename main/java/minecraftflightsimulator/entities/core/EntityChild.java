package minecraftflightsimulator.entities.core;

import minecraftflightsimulator.utilities.MFSVector;
import minecraftflightsimulator.utilities.RotationHelper;
import net.minecraft.block.Block;
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
	
	public boolean collidesWithLiquids(){
		return false;
	}
	
	public boolean isPartCollided(AxisAlignedBB box){
		int minX = MathHelper.floor_double(box.minX);
		int maxX = MathHelper.floor_double(box.maxX + 1.0D);
		int minY = MathHelper.floor_double(box.minY);
		int maxY = MathHelper.floor_double(box.maxY + 1.0D);
		int minZ = MathHelper.floor_double(box.minZ);
		int maxZ = MathHelper.floor_double(box.maxZ + 1.0D);

        for(int i = minX; i < maxX; ++i){
        	for(int j = minY - 1; j < maxY; ++j){
        		for(int k = minZ; k < maxZ; ++k){
        			//DEL180START
                    Block block = worldObj.getBlock(i, j, k);
                    AxisAlignedBB blockBox = block.getCollisionBoundingBoxFromPool(worldObj, i, j, k);
                    if(blockBox != null && box.intersectsWith(blockBox)){
                    	return true;
                    }else if(collidesWithLiquids()){
                    	if(isLiquidAt(i, j, k)){
                    		return true;
                    	}
                    }
                    //DEL180END
                    /*INS180
                    BlockPos pos = new BlockPos(i, j, k);
        			IBlockState state = worldObj.getBlockState(pos);
                    AxisAlignedBB blockBox = state.getBlock().getCollisionBoundingBox(worldObj, pos, state);
                    if(blockBox != null && box.intersectsWith(blockBox)){
                    	return true;
                    }else if(collidesWithLiqids()){
                    	if(isLiquidAt(i, j, k)){
                    		return true;
                    	}
                    } 
                     INS180*/
                }
            }
        }
        return false;
	}
	
	@Override
	public AxisAlignedBB getBoundingBox(){
		return this.boundingBox;
	}
	
	public boolean isLiquidAt(double x, double y, double z){
		return worldObj.getBlock(MathHelper.floor_double(x), MathHelper.floor_double(y), MathHelper.floor_double(z)).getMaterial().isLiquid();
	}
	
	public boolean isOnGround(){
		return isPartCollided(this.getBoundingBox().getOffsetBoundingBox(0, -0.05, 0));
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
