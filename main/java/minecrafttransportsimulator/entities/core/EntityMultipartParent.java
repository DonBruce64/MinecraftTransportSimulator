package minecrafttransportsimulator.entities.core;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import minecrafttransportsimulator.MTS;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**Main parent class.  All entities that have parts should extend this class.
 * It is primarily responsible for the linking of children and parents.
 * It is NOT responsible for custom data sets, spawnable operations, or movement and the like.
 * That should be done in sub-classes as different implementations may be desired.
 * 
 * @author don_bruce
 */
public abstract class EntityMultipartParent extends EntityMultipartBase{
	public byte numberChildren;
	public float rotationRoll;
	public float prevRotationRoll;
	
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
		}
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
