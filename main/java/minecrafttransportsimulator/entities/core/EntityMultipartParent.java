package minecrafttransportsimulator.entities.core;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MTSVector;
import minecrafttransportsimulator.helpers.EntityHelper;
import minecrafttransportsimulator.packets.general.ServerSyncPacket;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.RotationSystem;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;

/**Main parent class.  All entities that have parts should extend this class.
 * It is responsible for part management, checks, rendering and other functions.
 * It is NOT responsible for custom data sets, spawnable operations, and the like.
 * That should be done in sub-classes as different implementations may be desired.
 * 
 * @author don_bruce
 */
public abstract class EntityMultipartParent extends EntityMultipartBase{
	public byte numberChildren;
	public float rotationRoll;
	public float prevRotationRoll;
	public float pitchCorrection;
	public float yawCorrection;
	public float rollCorrection;	
	
	/**
	 * Map that contains child mappings.  Keyed by child's UUID.
	 * Note that this is for moving and linking children, and will be empty until
	 * children get linked.
	 */
	private Map<String, EntityMultipartChild> children = new HashMap<String, EntityMultipartChild>();
	public boolean rendered;

	public EntityMultipartParent(World world){
		super(world);
		this.setSize(0.75F, 0.75F);
		this.preventEntitySpawning = false;
	}
	
	public EntityMultipartParent(World world, float posX, float posY, float posZ, float playerRotation){
		this(world);
		this.setPositionAndRotation(posX, posY, posZ, playerRotation-90, 0);
		this.UUID=String.valueOf(this.getUniqueID());
	}
	
	@Override
	public void onEntityUpdate(){
		super.onEntityUpdate();
		if(!this.hasUUID()){return;}
		if(!linked){
			linked = children.size() == numberChildren;
			//Sometimes parts don't load right.  Need to reset the number of children then.
			if(!linked && ticksExisted == 100){
				if(children.size() == numberChildren - 1){
					System.err.println("A PART HAS FAILED TO LOAD!  SKIPPNG!");
				}else if(children.size() == numberChildren + 1){
					System.err.println("AN EXTRA PART HAS BEEN LOADED!  ADDING!");
				}else{
					return;
				}
				numberChildren = (byte) children.size();
				linked = true;
			}	
		}else if(!worldObj.isRemote && this.ticksExisted%ConfigSystem.getIntegerConfig("SyncDelay")==0){
			MTS.MFSNet.sendToAll(new ServerSyncPacket(getEntityId(), posX, posY, posZ, motionX, motionY, motionZ, rotationPitch, rotationRoll, rotationYaw));
		}
		prevRotationRoll = rotationRoll + rollCorrection;
		prevRotationPitch = rotationPitch + pitchCorrection;
		prevRotationYaw = rotationYaw + yawCorrection;
		rollCorrection = pitchCorrection = yawCorrection = 0;
	}
	
	@Override
	public void setDead(){
		super.setDead();
		for(EntityMultipartChild child : getChildren()){
			removeChild(child.UUID, false);
		}
	}
	
	/**
	 * Spawns a child and adds a child to all appropriate mappings.
	 * Set newChild to true if parent needs to keep track of an additional child.
	 * @param childUUID
	 * @param child
	 * @param newChild
	 */
	public void addChild(String childUUID, EntityMultipartChild child, boolean newChild){
		if(!children.containsKey(childUUID)){
			children.put(childUUID, child);
			if(newChild){
				++numberChildren;
				if(EntityHelper.isBoxCollidingWithBlocks(worldObj, child.getEntityBoundingBox(), child.collidesWithLiquids())){
					float boost = Math.max(0, -child.offsetY);
					this.rotationRoll = 0;
					this.setPositionAndRotation(posX, posY + boost, posZ, rotationYaw, 0);
					child.setPosition(posX + child.offsetX, posY + child.offsetY + boost, posZ + child.offsetZ);
					
					//Sometimes children can break off if the vehicle rotates and shoves something under the ground.
					for(EntityMultipartChild testChild : this.children.values()){
						if(EntityHelper.isBoxCollidingWithBlocks(worldObj, testChild.getEntityBoundingBox().offset(0, boost, 0), child.collidesWithLiquids())){
							this.setPositionAndRotation(posX, posY + 1, posZ, rotationYaw, 0);
							break;
						}
					}
				}
				worldObj.spawnEntityInWorld(child);
			}
		}
	}
	
	/**
	 * Removes a child from mappings, setting it dead in the process. 
	 * @param childUUID
	 */
	public void removeChild(String childUUID, boolean playBreakSound){
		if(children.containsKey(childUUID)){
			children.remove(childUUID).setDead();
			--numberChildren;
		}
		if(playBreakSound){
			MTS.proxy.playSound(this, "random.break", 2, 1);
		}
	}

	public void moveChildren(){
		for(EntityMultipartChild child : getChildren()){
			if(child.isDead){
				removeChild(child.UUID, false);
			}else{
				MTSVector offset = RotationSystem.getRotatedPoint(child.offsetX, child.offsetY, child.offsetZ, rotationPitch, rotationYaw, rotationRoll);
				child.setPosition(posX + offset.xCoord, posY + offset.yCoord, posZ + offset.zCoord);
				Entity rider = child.getRidingEntity();
				if(rider != null){
					if(Loader.MC_VERSION.equals("1.7.10")){
						MTSVector posVec = RotationSystem.getRotatedPoint(child.offsetX, (float) (child.offsetY + rider.getYOffset()), (float) child.offsetZ, this.rotationPitch, this.rotationYaw, this.rotationRoll);
						rider.setPosition(this.posX + posVec.xCoord, this.posY + posVec.yCoord, this.posZ + posVec.zCoord);
					}else{
						MTSVector posVec = RotationSystem.getRotatedPoint(child.offsetX, (float) (child.offsetY + rider.getYOffset() + rider.height), (float) child.offsetZ, this.rotationPitch, this.rotationYaw, this.rotationRoll);
						rider.setPosition(this.posX + posVec.xCoord, this.posY + posVec.yCoord - rider.height, this.posZ + posVec.zCoord);
					}
					
				}
			}
		}
	}
	
	public EntityMultipartChild[] getChildren(){return ImmutableList.copyOf(children.values()).toArray(new EntityMultipartChild[children.size()]);}
		
    @Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.numberChildren=tagCompound.getByte("numberChildren");
		this.rotationRoll=tagCompound.getFloat("rotationRoll");
	}
    
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setByte("numberChildren", this.numberChildren);
		tagCompound.setFloat("rotationRoll", this.rotationRoll);
		return tagCompound;
	}
}
