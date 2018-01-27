package minecrafttransportsimulator.entities.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MTSVector;
import minecrafttransportsimulator.packets.general.ServerSyncPacket;
import minecrafttransportsimulator.packets.general.ServerTPSPacket;
import minecrafttransportsimulator.systems.RotationSystem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**Main parent class.  All entities that have parts should extend this class.
 * It is responsible for part management, checks, rendering and other functions.
 * It is NOT responsible for custom data sets, spawnable operations, and the like.
 * That should be done in sub-classes as different implementations may be desired.
 * 
 * @author don_bruce
 */
public abstract class EntityMultipartParent extends EntityMultipartBase{
	public boolean forceSyncThisTick;
	public byte numberChildren;
	public float rotationRoll;
	public float prevRotationRoll;
	public float pitchCorrection;
	public float yawCorrection;
	public float rollCorrection;
	
	/**
	 * Every update tick on the client this parent will look for cached packets in here.
	 * Used as network multi-threading causes packet dumps and packets are handled willy-nilly. 
	 */
	public List<ServerSyncPacket> cachedSyncPackets = new ArrayList<ServerSyncPacket>();
	
	public byte clientTicksToSkip = 0;
	
	/**
	 * Map that contains child mappings.  Keyed by child's UUID.
	 * Note that this is for moving and linking children, and will be empty until
	 * children get linked.
	 */
	private Map<String, EntityMultipartChild> children = new HashMap<String, EntityMultipartChild>();

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
					MTS.MTSLog.warn("A PART HAS FAILED TO LOAD!  SKIPPNG!");
				}else if(children.size() == numberChildren + 1){
					MTS.MTSLog.warn("AN EXTRA PART HAS BEEN LOADED!  ADDING!");
				}else{
					return;
				}
				numberChildren = (byte) children.size();
				linked = true;
			}	
		}else if(!worldObj.isRemote){
			MTS.MTSNet.sendToAll(new ServerSyncPacket(getEntityId(), worldObj.getTotalWorldTime(), posX, posY, posZ, motionX, motionY, motionZ, rotationYaw, rotationPitch, rotationRoll));
			//Every 100 ticks, check the TPS to see if we need to tell the client to skip a tick (or more).
			if(worldObj.getMinecraftServer().getTickCounter()%100==99){
				long tpsSum = 0;
				for(byte tick=0; tick < 100; ++tick){
					tpsSum += worldObj.getMinecraftServer().worldTickTimes.get(worldObj.provider.getDimension())[tick];
				}
				double tps = Math.min(1000.0/(tpsSum/100D * 1.0E-6D), 20);
				for(byte ticksToSkip=0; ticksToSkip < 100 - tps*5D; ++ticksToSkip){
					MTS.MTSNet.sendToAll(new ServerTPSPacket(worldObj.provider.getDimension()));
				}
			}
		}else{
			for(ServerSyncPacket packet : cachedSyncPackets){
				if(packet.timeStamp == worldObj.getTotalWorldTime()){
					handleServerSyncPacket(packet);
				}
			}
			cachedSyncPackets.clear();
			moveChildren();
			if(forceSyncThisTick){
				requestDataFromServer();
				forceSyncThisTick = false;
			}
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
				if(child.isChildOffsetBoxCollidingWithBlocks(child.getEntityBoundingBox())){
					float boost = Math.max(0, -child.offsetY);
					this.rotationRoll = 0;
					this.setPositionAndRotation(posX, posY + boost, posZ, rotationYaw, 0);
					child.setPosition(posX + child.offsetX, posY + child.offsetY + boost, posZ + child.offsetZ);
					
					//Sometimes children can break off if the parent rotates and shoves something under the ground.
					for(EntityMultipartChild testChild : this.children.values()){
						if(child.isChildOffsetBoxCollidingWithBlocks(testChild.getEntityBoundingBox().offset(0, boost, 0))){
							this.setPositionAndRotation(posX, posY + 1, posZ, rotationYaw, 0);
							break;
						}
					}
				}
				worldObj.spawnEntityInWorld(child);
				this.sendDataToClient();
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
			this.playSound(SoundEvents.ITEM_SHIELD_BREAK, 2.0F, 1.0F);
		}
	}

	public void moveChildren(){
		for(EntityMultipartChild child : getChildren()){
			if(child.isDead){
				removeChild(child.UUID, false);
			}else{
				MTSVector offset = RotationSystem.getRotatedPoint(child.offsetX, child.offsetY, child.offsetZ, rotationPitch, rotationYaw, rotationRoll);
				child.setPosition(posX + offset.xCoord, posY + offset.yCoord, posZ + offset.zCoord);
			}
		}
	}
	
	private void handleServerSyncPacket(final ServerSyncPacket message){
		byte syncThreshold = 5;
		posX = rectifyValue(posX, message.posX, syncThreshold);
		posY = rectifyValue(posY, message.posY, syncThreshold);
		posZ = rectifyValue(posZ, message.posZ, syncThreshold);
		
		motionX = rectifyValue(motionX, message.motionX, syncThreshold/25F);
		motionY = rectifyValue(motionY, message.motionY, syncThreshold/25F);
		motionZ = rectifyValue(motionZ, message.motionZ, syncThreshold/25F);
		
		yawCorrection = rotationYaw;
		rotationYaw = (float) rectifyValue(rotationYaw, message.yaw, syncThreshold);
		yawCorrection -= rotationYaw;
		
		rollCorrection = rotationRoll;
		rotationRoll = (float) rectifyValue(rotationRoll, message.roll, syncThreshold);
		rollCorrection -= rotationRoll;
		
		pitchCorrection = rotationPitch;
		rotationPitch = (float) rectifyValue(rotationPitch, message.pitch, syncThreshold);
		pitchCorrection -= rotationPitch; 
	}
	
	private double rectifyValue(double currentValue, double packetValue, double cutoff){
		if(currentValue > packetValue){
			if(currentValue - packetValue > cutoff){
				forceSyncThisTick = true;
				return packetValue;
			}else{
				return currentValue - Math.min(currentValue - packetValue, 0.01);
			}
		}else{
			if(packetValue - currentValue > cutoff){
				forceSyncThisTick = true;
				return packetValue;
			}else{
				return currentValue + Math.min(packetValue - currentValue, 0.01);
			}
		}
	}
	
	public EntityMultipartChild[] getChildren(){return ImmutableList.copyOf(children.values()).toArray(new EntityMultipartChild[children.size()]);}
	
	public abstract boolean processInitialInteractFromChild(EntityPlayer player, EntityMultipartChild childClicked, @Nullable ItemStack stack);
		
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
