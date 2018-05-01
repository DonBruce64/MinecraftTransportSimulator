package minecrafttransportsimulator.entities.core;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.PackPartObject;
import minecrafttransportsimulator.multipart.parts.AMultipartPart;
import minecrafttransportsimulator.packets.general.EntityClientRequestDataPacket;
import minecrafttransportsimulator.packets.general.EntityMultipartChild;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**Base multipart class.  All multipart entities should extend this class.
 * It is primarily responsible for the adding and removal of multiparts,
 * as well as dealing with what happens when this part is killed.
 * It is NOT responsible for custom data sets, sounds, or movement.
 * That should be done in sub-classes to keep methods segregated.
 * 
 * @author don_bruce
 */
public abstract class EntityMultipartA_Base extends Entity{
	private final List<AMultipartPart> parts = new ArrayList<AMultipartPart>();

	public EntityMultipartA_Base(World world){
		super(world);
		//This constructor is called on clients and servers.
		//If this is a client, we need to request NBT data from the server to get our parts.
		if(world.isRemote){
			MTS.MTSNet.sendToServer(new EntityClientRequestDataPacket(this.getEntityId()));
		}
	}
	
	public EntityMultipartA_Base(World world, float posX, float posY, float posZ, float playerRotation){
		this(world);
		//Set position to the spot that was clicked by the player.
		//Add a -90 rotation offset so the multipart is facing perpendicular.
		//Makes placement easier and is less likely for players to get stuck.
		this.setPositionAndRotation(posX, posY, posZ, playerRotation-90, 0);
	}
	
	@Override
	public void setDead(){
		super.setDead();
		//If we are set dead, make sure to drop all the parts we have on the ground.
		for(AMultipartPart part : getMultipartParts()){
			if(part.getItemForPart() != null){
				ItemStack partStack = new ItemStack(part.getItemForPart());
				NBTTagCompound stackTag = part.getPartNBTTag();
				if(stackTag != null){
					partStack.setTagCompound(stackTag);
				}
				Vec3d partPosition = this.getPositionVector().add(part.offset);
				worldObj.spawnEntityInWorld(new EntityItem(worldObj, partPosition.xCoord, partPosition.yCoord, partPosition.zCoord, partStack));
			}
		}
	}
	
    @Override
    @SideOnly(Side.CLIENT)
    public void setPositionAndRotationDirect(double posX, double posY, double posZ, float yaw, float pitch, int posRotationIncrements, boolean teleport){
    	//Overridden due to stupid tracker behavior.
    	//Client-side render changes calls put in its place.
    	this.setRenderDistanceWeight(100);
    	this.ignoreFrustumCheck = true;
    }
	
	public void addPart(AMultipartPart part){
		if(part.isPartCollidingWithBlocks(Vec3d.ZERO)){
			Vec3d boost = new Vec3d(0, Math.max(0, -part.offset.yCoord), 0);
			this.rotationRoll = 0;
			this.setPositionAndRotation(posX, posY + boost.yCoord, posZ, rotationYaw, 0);
			
			//Sometimes parts can break off if the multipart rotates and shoves something under the ground.
			//Check to make sure we don't do this.
			for(AMultipartPart testPart : this.getMultipartParts()){
				if(testPart.isPartCollidingWithBlocks(boost)){
					this.setPositionAndRotation(posX, posY + 1, posZ, rotationYaw, 0);
					break;
				}
			}
		}
		//TODO add a new packet for add/remove part events.
		this.sendDataToClient();
	}
	
	/**
	 * Removes a part from this multipart. Optionally plays a breaking sound.
	 */
	public void removePart(AMultipartPart part, boolean playBreakSound){
		parts.remove(part);
		if(playBreakSound){
			this.playSound(SoundEvents.ITEM_SHIELD_BREAK, 2.0F, 1.0F);
		}
	}
	
	public AMultipartPart[] getMultipartParts(){
		return ImmutableList.copyOf(parts).toArray(new AMultipartPart[parts.size()]);
	}
			
    @Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		if(this.parts.size() == 0){
			NBTTagList partTagList = tagCompound.getTagList("Parts", 10);
			for(byte i=0; i<partTagList.tagCount(); ++i){
				try{
					NBTTagCompound partTag = partTagList.getCompoundTagAt(i);
					Class partClass = Class.forName(partTag.getString("ClassName"));
					partTag.removeTag("className");
					Constructor<? extends AMultipartPart> construct = partClass.getConstructor(EntityMultipartA_Base.class, Vec3d.class, boolean.class, boolean.class, PackPartObject.class, NBTTagCompound.class);
					AMultipartPart part = construct.newInstance(this, , mover, mover.UUID, packPart.pos[0], packPart.pos[1], packPart.pos[2], heldStack.getItemDamage());
				}catch(Exception e){
					MTS.MTSLog.error("ERROR IN LOADING PART FROM NBT!");
					e.printStackTrace();
				}
			}
		}
	}
    
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		NBTTagList partTagList = new NBTTagList();
		for(AMultipartPart part : this.getMultipartParts()){
			NBTTagCompound partTag = part.getPartNBTTag();
			//We need to set some extra data here for the part to allow this multipart to know where it went.
			//This only gets set here during saving/loading, and is NOT returned in the item that comes from the part.
			partTag.setString("className", part.getClass().getName());
			partTagList.appendTag(part.getPartNBTTag());
		}
		tagCompound.setTag("Parts", partTagList);
		return tagCompound;
	}
	
	//Junk methods, forced to pull in.
	protected void entityInit(){}
	protected void readEntityFromNBT(NBTTagCompound p_70037_1_){}
	protected void writeEntityToNBT(NBTTagCompound p_70014_1_){}
}
