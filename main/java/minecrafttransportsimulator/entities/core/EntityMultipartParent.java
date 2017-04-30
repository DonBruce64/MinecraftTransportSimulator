package minecrafttransportsimulator.entities.core;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import cpw.mods.fml.common.Loader;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MTSEntity;
import minecrafttransportsimulator.baseclasses.MTSVector;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import minecrafttransportsimulator.minecrafthelpers.AABBHelper;
import minecrafttransportsimulator.minecrafthelpers.EntityHelper;
import minecrafttransportsimulator.minecrafthelpers.ItemStackHelper;
import minecrafttransportsimulator.minecrafthelpers.PlayerHelper;
import minecrafttransportsimulator.packets.general.ServerSyncPacket;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.RotationSystem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
/**Main parent class.  All entities that have parts should extend this class.
 * It is responsible for part management, checks, rendering and other functions.
 * 
 * @author don_bruce
 */
public abstract class EntityMultipartParent extends EntityMultipartBase{
	public boolean rendered;
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
	
	/**
	 * Array containing locations of all parts.
	 * All parts should be initialized in entity's {@link initPartData} method.
	 * Note that core entities should NOT be put here, as they're
	 * directly linked to the parent and can't be added manually.
	 */
	protected List<PartData> partData;
	
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
	protected void entityInit(){
		partData = new ArrayList<PartData>();
		this.initPartData();
	}
	
	/**
	 * Called from {@link EntityInit}, this is where all part data sets should be added.
	 * Method should be called by the final subclass of this class.
	 * Note that the order in which engines are added determines their number in the 
	 * HUD and other areas.
	 */
	protected abstract void initPartData();
	
	@Override
	public boolean performRightClickAction(MTSEntity clicked, EntityPlayer player){
		//No in-use changes for sneaky sneaks!
		if(player.ridingEntity instanceof EntitySeat){
			if(this.equals(((EntitySeat) player.ridingEntity).parent)){
				return false;
			}
		}
		if(!worldObj.isRemote){
			ItemStack heldStack = PlayerHelper.getHeldStack(player);
			if(heldStack != null){
				if(ItemStackHelper.getItemFromStack(heldStack).equals(MTSRegistry.wrench)){
					return false;
				}
				
				Item heldItem = ItemStackHelper.getItemFromStack(heldStack);
				EntityMultipartChild childClicked = (EntityMultipartChild) clicked;
				Class<? extends EntityMultipartChild> childClassToSpawn = null;
				PartData dataToSpawn = null;
				float closestPosition = 9999;
				//Look though the part data to find the class that goes with the held item.
				for(PartData data : partData){
					for(Class partClass : data.acceptableClasses){
						if(heldItem.equals(MTSRegistry.entityItems.get(partClass))){
							//The held item can spawn a part.t.
							//Now find the closest spot to put it.
							float distance = (float) Math.hypot(childClicked.offsetX - data.offsetX, childClicked.offsetZ - data.offsetZ);
							if(distance < closestPosition){
								//Make sure a part doesn't exist already.
								boolean childPresent = false;
								for(EntityMultipartChild child : children.values()){
									if(child.offsetX == data.offsetX && child.offsetY == data.offsetY && child.offsetZ == data.offsetZ){
										childPresent = true;
										break;
									}
								}
								if(!childPresent){
									closestPosition = distance;
									dataToSpawn = data;
									childClassToSpawn = partClass;
								}
							}
						}
					}
				}
				if(dataToSpawn != null){
					//We have a part, now time to spawn it.
					try{
						Constructor<? extends EntityMultipartChild> construct = childClassToSpawn.getConstructor(World.class, EntityMultipartParent.class, String.class, float.class, float.class, float.class, int.class);
						EntityMultipartChild newChild = construct.newInstance(worldObj, this, this.UUID, dataToSpawn.offsetX, dataToSpawn.offsetY, dataToSpawn.offsetZ, ItemStackHelper.getItemDamage(heldStack));
						newChild.setNBTFromStack(heldStack);
						newChild.setTurnsWithMover(dataToSpawn.rotatesWithYaw);
						newChild.setController(dataToSpawn.isController);
						this.addChild(newChild.UUID, newChild, true);
						if(!PlayerHelper.isPlayerCreative(player)){
							PlayerHelper.removeItemFromHand(player, 1);
						}
						return true;
					}catch(Exception e){
						System.err.println("ERROR SPAWING PART!");
						e.printStackTrace();
					}
				}
			}
		}else{
			ItemStack heldStack = PlayerHelper.getHeldStack(player);
			if(heldStack != null){
				if(ItemStackHelper.getItemFromStack(heldStack).equals(MTSRegistry.wrench)){
					MTS.proxy.openGUI(this, player);
					return true;
				}
			}
		}
		return false;
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
				if(!AABBHelper.getCollidingBlockBoxes(worldObj, AABBHelper.getEntityBoundingBox(child), child.collidesWithLiquids()).isEmpty()){
					float boost = Math.max(0, -child.offsetY);
					this.rotationRoll = 0;
					this.setPositionAndRotation(posX, posY + boost, posZ, rotationYaw, 0);
					child.setPosition(posX + child.offsetX, posY + child.offsetY + boost, posZ + child.offsetZ);
					
					//Sometimes children can break off if the vehicle rotates and shoves something under the ground.
					for(EntityMultipartChild testChild : this.children.values()){
						if(!AABBHelper.getCollidingBlockBoxes(worldObj, AABBHelper.getOffsetEntityBoundingBox(testChild, 0, boost, 0), testChild.collidesWithLiquids()).isEmpty()){
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
				Entity rider = EntityHelper.getRider(child);
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
	public void writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setByte("numberChildren", this.numberChildren);
		tagCompound.setFloat("rotationRoll", this.rotationRoll);
	}
	
	/**This class contains data for parts that can be attached to or are attached to this parent.
	 * A set of these classes must be added upon init of the parent.
	 * The parent then looks to see if any linked parts match this list, and note it as so.
	 * 
	 *@author don_bruce
	 */
	protected class PartData{
		public final boolean rotatesWithYaw;
		public final boolean isController;
		public final float offsetX;
		public final float offsetY;
		public final float offsetZ;
		public final Class<? extends EntityMultipartChild>[] acceptableClasses;
		
		public PartData(float offsetX, float offsetY, float offsetZ, boolean rotatesWithYaw, boolean isController, Class<? extends EntityMultipartChild>... acceptableClasses){
			this.rotatesWithYaw = rotatesWithYaw;
			this.isController = isController;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
			this.offsetZ = offsetZ;
			this.acceptableClasses = acceptableClasses;
		}
		
		public PartData(float offsetX, float offsetY, float offsetZ, Class<? extends EntityMultipartChild>... acceptableClasses){
			this(offsetX, offsetY, offsetZ, false, false, acceptableClasses);
		}
	}
}
