package minecraftflightsimulator.entities.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.packets.general.ServerSyncPacket;
import minecraftflightsimulator.utilities.MFSVector;
import minecraftflightsimulator.utilities.RotationHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public abstract class EntityParent extends EntityBase{
	public byte numberChildren;
	public float rotationRoll;
	public float prevRotationRoll;
	
	/**
	 * Map that contains child mappings.  Keyed by child's UUID.
	 * Note that this is for moving and linking children, and will be empty until
	 * children get linked.
	 */
	private Map<String, EntityChild> children = new HashMap<String, EntityChild>();
	
	public EntityParent(World world){
		super(world);
		this.setSize(0.75F, 0.75F);
		this.ignoreFrustumCheck=true;
		this.preventEntitySpawning = false;
	}
	
	public EntityParent(World world, float posX, float posY, float posZ, float playerRotation){
		this(world);
		this.setPositionAndRotation(posX, posY, posZ, playerRotation-90, 0);
		this.UUID=String.valueOf(this.getUniqueID());
		this.numberChildren=(byte) this.getCoreLocations().length;
	}
	
	@Override
	public void onEntityUpdate(){
		super.onEntityUpdate();
		if(!this.hasUUID()){return;}
		if(!linked){
			if(this.ticksExisted>100){
				System.err.println("KILLING PARENT WITH WRONG NUMBER OF CHILDREN.  WANTED:" + numberChildren + " FOUND:" + children.size() +".");
				this.setDead();
			}else{
				linked = children.size() == numberChildren ? true : false;
			}
		}else if(!worldObj.isRemote && this.ticksExisted%5==0){
			MFS.MFSNet.sendToAll(new ServerSyncPacket(getEntityId(), posX, posY, posZ, motionX, motionY, motionZ, rotationPitch, rotationRoll, rotationYaw));
		}
		prevRotationRoll = rotationRoll;
		prevRotationPitch = rotationPitch;
		prevRotationYaw = rotationYaw;
	}
	
	@Override
    public boolean interactFirst(EntityPlayer player){
		return this.performRightClickAction(this, player);
	}
	
	@Override
    public boolean attackEntityFrom(DamageSource source, float damage){
		return this.performAttackAction(this, source, damage);
	}
	
	public MFSVector getHeadingVec(){
        float f1 = MathHelper.cos(-this.rotationYaw * 0.017453292F - (float)Math.PI);
        float f2 = MathHelper.sin(-this.rotationYaw * 0.017453292F - (float)Math.PI);
        float f3 = -MathHelper.cos(-this.rotationPitch * 0.017453292F);
        float f4 = MathHelper.sin(-this.rotationPitch * 0.017453292F);
        return new MFSVector((double)(f2 * f3), (double)f4, (double)(f1 * f3));
   	}
	
	@Override
	public void setDead(){
		super.setDead();
		for(EntityChild child : getChildren()){
			removeChild(child.UUID);
		}
	}
	
	//Start of custom methods
	/**
	 * Handler for all right-clicking actions performed.
	 * the appropriate methods in subclassed child entities.
	 * @param entityClicked the entity that was clicked
	 * @param player the player that clicked this entity
	 * 
	 * @return whether or not an action occurred.
	 */
	public abstract boolean performRightClickAction(EntityBase entityClicked, EntityPlayer player);
	
	public abstract boolean performAttackAction(EntityBase attackedEntity, DamageSource source, float damage);
	
	/**
	 * Spawns a child and adds a child to all appropriate mappings.
	 * Set newChild to true if parent needs to keep track of an additional child.
	 * @param childUUID
	 * @param child
	 * @param newChild
	 */
	public void addChild(String childUUID, EntityChild child, boolean newChild){
		if(!children.containsKey(childUUID)){
			children.put(childUUID, child);
			if(newChild){
				++numberChildren;
				if(child.isCollidedHorizontally()){
					float boost = Math.max(0, -child.offsetY);
					this.rotationRoll = 0;
					this.setPositionAndRotation(posX, posY + boost, posZ, rotationYaw, 0);
					child.setPosition(posX + child.offsetX, posY + child.offsetY + boost, posZ + child.offsetZ);
				}
				worldObj.spawnEntityInWorld(child);
			}
		}
	}
	
	/**
	 * Removes a child from mappings, setting it dead in the process.
	 * @param childUUID
	 */
	public void removeChild(String childUUID){
		if(children.containsKey(childUUID)){
			children.remove(childUUID).setDead();
			--numberChildren;
		}
	}

	public void moveChildren(){
		for(EntityChild child : getChildren()){
			if(child.isDead){
				removeChild(child.UUID);
			}else{
				MFSVector offset = RotationHelper.getRotatedPoint(child.offsetX, child.offsetY, child.offsetZ, rotationPitch, rotationYaw, rotationRoll);
				child.setPosition(posX + offset.xCoord, posY + offset.yCoord, posZ + offset.zCoord);
				child.updateRiderPosition();
			}
		}
	}
	
	/**
	 * Given an offset coordinate set, returns the child at that offset.
	 * @param offsetCoords
	 * @return the child with the specified offset.
	 */
	protected EntityChild getChildAtLocation(float[] offsetCoords){
		for(EntityChild child : getChildren()){
			if(child.getClass().equals(EntityCore.class)){continue;}
			float[] childOffset = new float[]{child.offsetX, child.offsetY, child.offsetZ};
			if(Arrays.equals(childOffset, offsetCoords)){
				return child;
			}
		}
		return null;
	}
	
	protected EntityChild[] getChildren(){return children.values().toArray(new EntityChild[children.size()]);}	
	public abstract float[][] getCoreLocations();	
		
    @Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.numberChildren=tagCompound.getByte("numberChildren");
		this.rotationRoll=tagCompound.getFloat("rotationRoll");
	}
    
	@Override
	public void writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setByte("numberChildren", this.numberChildren);
		tagCompound.setFloat("rotationRoll", this.rotationRoll);
	}
}
