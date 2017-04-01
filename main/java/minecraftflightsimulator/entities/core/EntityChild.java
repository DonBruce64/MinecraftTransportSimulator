package minecraftflightsimulator.entities.core;

import minecraftflightsimulator.baseclasses.MTSVector;
import minecraftflightsimulator.minecrafthelpers.AABBHelper;
import minecraftflightsimulator.minecrafthelpers.EntityHelper;
import minecraftflightsimulator.minecrafthelpers.PlayerHelper;
import minecraftflightsimulator.systems.RotationSystem;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

/**Main child class.  This class is the base for all child entities and should be
 * extended to use the parent-child linking system.
 * Use {@link EntityParent#addChild(String, EntityChild, boolean)} to add children 
 * and {@link EntityParent#removeChild(String, boolean)} to kill and remove them.
 * You may extend {@link EntityParent} to get more functionality with those systems.
 * Beware of children with offsetZ of 0, as they can cause problems with pitch calculations.
 * Also note that all childeren must have a constructor of the form: 
 * public EntityChild(World world, EntityParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, float width, float height, int propertyCode)
 * 
 * @author don_bruce
 */
public abstract class EntityChild extends EntityBase{	
	/** Integer for storing data about color, type, and other things.*/
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
		MTSVector offset = RotationSystem.getRotatedPoint(offsetX, offsetY, offsetZ, parent.rotationPitch, parent.rotationYaw, parent.rotationRoll);
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
    public boolean performRightClickAction(EntityBase clicked, EntityPlayer player){
		return parent != null ? parent.performRightClickAction(clicked, player) : false;
	}
	
	@Override
	public boolean performAttackAction(DamageSource source, float damage){
		if(isDamageWrench(source)){
			return true;
		}else{
			return parent != null ? parent.performAttackAction(source, damage) : false;
		}
    }
	
	/**Checks to see if damage came from a player holding a wrench.
	 * Removes the entity if so, dropping the entity as an item.
	 * Called each attack before deciding whether or not to forward damage to the parent.
	 * If overriding {@link performAttackAction} make sure to call this or wrenches won't work!
	 */
	protected boolean isDamageWrench(DamageSource source){
		if(!worldObj.isRemote){
			if(source.getEntity() instanceof EntityPlayer){
				if(PlayerHelper.isPlayerHoldingWrench((EntityPlayer) source.getEntity())){
					ItemStack droppedItem = this.getItemStack();
					worldObj.spawnEntityInWorld(new EntityItem(worldObj, posX, posY, posZ, droppedItem));
					parent.removeChild(UUID, false);
					return true;
				}
			}
		}
		return false;
	}
	
	/**Sets the NBT of the entity to that of the stack.
	 */
	public abstract void setNBTFromStack(ItemStack stack);
	
	/**Gets an ItemStack that represents the entity.
	 * This should be called when removing the entity from the world to return an item.
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
		EntityBase entity = EntityHelper.getEntityByMFSUUID(worldObj, (this.parentUUID));
		if(entity != null){
			EntityParent parent =  (EntityParent) entity;
			parent.addChild(this.UUID, this, false);
			this.parent=parent;
		}
	}
	
	@Override
	public boolean canBeCollidedWith(){
		//This gets overridden to do collisions with players.
		return true;
	}
	
	public boolean collidesWithLiquids(){
		return false;
	}
	
	@Override
	public AxisAlignedBB getBoundingBox(){
		//This gets overridden to do collisions with players.
		return this.boundingBox;
	}
	
	public boolean isOnGround(){
		return !AABBHelper.getCollidingBlockBoxes(worldObj, AABBHelper.getOffsetEntityBoundingBox(this, 0, -0.05F, 0), this.collidesWithLiquids()).isEmpty();
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
