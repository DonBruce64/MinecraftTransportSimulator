package minecrafttransportsimulator.entities.core;

import javax.annotation.Nullable;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MTSEntity;
import minecrafttransportsimulator.baseclasses.MTSVector;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.packets.general.ChatPacket;
import minecrafttransportsimulator.systems.RotationSystem;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**Main child class.  This class is the base for all child entities and should be
 * extended to use the parent-child linking system.
 * Use {@link EntityMultipartParent#addChild(String, EntityMultipartChild, boolean)} to add children 
 * and {@link EntityMultipartParent#removeChild(String, boolean)} to kill and remove them.
 * You may extend {@link EntityMultipartParent} to get more functionality with those systems.
 * Beware of children with offsetZ of 0, as they can cause problems with pitch calculations.
 * Also note that all children must have a constructor of the form: 
 * public EntityChild(World world, EntityParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, float width, float height, int propertyCode)
 * 
 * @author don_bruce
 */
public abstract class EntityMultipartChild extends EntityMultipartBase{	
	/** Can a rider of this child send inputs to the parent.*/
	public boolean isController;
	/** Does this child rotate in-sync with the yaw changes of the parent.*/
	public boolean turnsWithSteer;
	/** Integer for storing data about color, type, and other things.*/
	public int propertyCode;
	public float offsetX;
	public float offsetY;
	public float offsetZ;
	public EntityMultipartParent parent;
	protected String parentUUID = "";
	
	public EntityMultipartChild(World world) {
		super(world);
	}
	
	public EntityMultipartChild(World world, EntityMultipartParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, float width, float height, int propertyCode){
		this(world);
		this.offsetX=offsetX;
		this.offsetY=offsetY;
		this.offsetZ=offsetZ;
		this.width=width;
		this.height=height;
		this.propertyCode=propertyCode;
		this.UUID=String.valueOf(this.getUniqueID());
		this.parentUUID=parentUUID;
		MTSVector offset = RotationSystem.getRotatedPoint(offsetX, offsetY, offsetZ, parent.rotationPitch, parent.rotationYaw, parent.rotationRoll);
		this.setPositionAndRotation(parent.posX+offset.xCoord, parent.posY+offset.yCoord, parent.posZ+offset.zCoord, parent.rotationYaw, parent.rotationPitch);
	}
	
	@Override
	public void onEntityUpdate(){
		super.onEntityUpdate();
		linked = hasUUID() && hasParent();
	}
	
	@Override
	public boolean processInitialInteract(EntityPlayer player, @Nullable ItemStack stack, EnumHand hand){
		return parent != null ? parent.processInitialInteractFromChild(player, this, stack) : true;
	}
	
	@Override
	public boolean attackEntityFrom(DamageSource source, float damage){
		if(!worldObj.isRemote){
			if(source.getEntity() instanceof EntityPlayer){
				EntityPlayer attackingPlayer = (EntityPlayer) source.getEntity();
				if(attackingPlayer.getHeldItemMainhand() != null && attackingPlayer.getHeldItemMainhand().getItem().equals(MTSRegistry.wrench)){
					boolean isPlayerOP = attackingPlayer.getServer().getPlayerList().getOppedPlayers().getEntry(attackingPlayer.getGameProfile()) != null || attackingPlayer.getServer().isSinglePlayer();
					if(parent == null || ((EntityMultipartMoving) parent).ownerName.isEmpty() || ((EntityMultipartMoving) parent).ownerName.equals(attackingPlayer.getUUID(attackingPlayer.getGameProfile()).toString()) || isPlayerOP){
						ItemStack droppedItem = this.getItemStack();
						if(droppedItem != null){
							worldObj.spawnEntityInWorld(new EntityItem(worldObj, posX, posY, posZ, droppedItem));
						}
						if(parent != null){
							parent.removeChild(UUID, false);
						}else{
							this.setDead();
						}
						return false;
					}else{
						MTS.MTSNet.sendTo(new ChatPacket("interact.failure.vehicleowned"), (EntityPlayerMP) attackingPlayer);
					}
				}
			}
			if(!attackChild(source, damage)){
				return parent != null ? parent.attackEntityFrom(source, damage) : false;
			}
		}
		return false;
    }
	
	/**Called when child is attacked.  Return true to end attack, false to forward attack to parent. 
	 */
	protected abstract boolean attackChild(DamageSource source, float damage);
	
	/**Sets the NBT of the entity to that of the stack.
	 */
	public abstract void setNBTFromStack(ItemStack stack);
	
	/**Gets an ItemStack that represents the entity.
	 * This is called when removing the entity from the world to return an item.
	 */
	public abstract ItemStack getItemStack();
	
	public boolean hasParent(){
		if(this.parent==null){
			if(ticksExisted==1 || ticksExisted%10==0){
				this.linkToParent();
			}
			return false;
		}
		return true;
	}
	
	private void linkToParent(){
		MTSEntity entity = getEntityByUUID(worldObj, this.parentUUID);
		if(entity != null){
			EntityMultipartParent parent =  (EntityMultipartParent) entity;
			parent.addChild(this.UUID, this, false);
			this.parent=parent;
		}
	}
	
	protected static EntityMultipartBase getEntityByUUID(World world, String UUID){
    	if(!UUID.equals("")){
	        for(Object obj : world.loadedEntityList){
	            if(obj instanceof EntityMultipartBase){
	                if(UUID.equals(((EntityMultipartBase) obj).UUID)){
	                    return (EntityMultipartBase) obj;
	                }
	            }
	        }
    	}
        return null;
    }
	
    //Child rendering is done by the parent.
    @Override
    public boolean shouldRenderInPass(int pass){
    	return false;
    }
	
	@Override
	public boolean canBeCollidedWith(){
		//This gets overridden to do collisions with players.
		return true;
	}
	
	@Override
    @Nullable
    public AxisAlignedBB getCollisionBoundingBox(){
		//Need this to do collision with other Entities.
        return this.getEntityBoundingBox();
    }
	
	protected boolean collidesWithLiquids(){
		return false;
	}

	public boolean isChildOffsetBoxCollidingWithBlocks(AxisAlignedBB box){ 
    	if(!worldObj.getCollisionBoxes(box).isEmpty()){
    		return true;
    	}else{
    		if(!collidesWithLiquids()){
    			return false;
    		}else{
    			int minX = (int) Math.floor(box.minX);
    	    	int maxX = (int) Math.floor(box.maxX + 1.0D);
    	    	int minY = (int) Math.floor(box.minY);
    	    	int maxY = (int) Math.floor(box.maxY + 1.0D);
    	    	int minZ = (int) Math.floor(box.minZ);
    	    	int maxZ = (int) Math.floor(box.maxZ + 1.0D);
    	    	
    	    	for(int i = minX; i < maxX; ++i){
    	    		for(int j = minY; j < maxY; ++j){
    	    			for(int k = minZ; k < maxZ; ++k){
    	    				if(worldObj.getBlockState(new BlockPos(i, j, k)).getMaterial().isLiquid()){
    	    					return true;
    	    				}
    	    			}
    	    		}
    	    	}
    	    	return false;
    		}
    	}
    }
	public boolean isOnGround(){
		return worldObj.getCollisionBoxes(this.getEntityBoundingBox().offset(0, -0.05F, 0)).isEmpty() ? isChildOffsetBoxCollidingWithBlocks(this.getEntityBoundingBox().offset(0, -0.05F, 0)) : true;
	}

	
	public void setController(boolean isController){
		this.isController = isController;
	}
	
	public void setTurnsWithSteer(boolean turnsWithSteer){
		this.turnsWithSteer = turnsWithSteer;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.isController=tagCompound.getBoolean("isController");
		this.turnsWithSteer=tagCompound.getBoolean("turnsWithSteer");
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
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setBoolean("isController", this.isController);
		tagCompound.setBoolean("turnsWithSteer", this.turnsWithSteer);
		tagCompound.setInteger("propertyCode", this.propertyCode);
		tagCompound.setFloat("offsetX", this.offsetX);
		tagCompound.setFloat("offsetY", this.offsetY);
		tagCompound.setFloat("offsetZ", this.offsetZ);
		tagCompound.setFloat("width", this.width);
		tagCompound.setFloat("height", this.height);
		if(!this.parentUUID.isEmpty()){
			tagCompound.setString("parentUUID", this.parentUUID);
		}
		return tagCompound;
	}
}
