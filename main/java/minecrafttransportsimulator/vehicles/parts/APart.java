package minecrafttransportsimulator.vehicles.parts;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.VehicleAxisAlignedBB;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.dataclasses.PackPartObject;
import minecrafttransportsimulator.dataclasses.PackVehicleObject.PackPart;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.systems.RotationSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleA_Base;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

/**This class is the base for all parts and should be
 * extended for any vehicle-compatible parts.
 * Use {@link EntityVehicleA_Base#addPart(APart, boolean)} to add parts 
 * and {@link EntityVehicleA_Base#removePart(APart, boolean)} to remove them.
 * You may extend {@link EntityVehicleA_Base} to get more functionality with those systems.
 * If you need to keep extra data ensure it is packed into whatever NBT is returned in item form.
 * This NBT will be fed into the constructor when creating this part, so expect it and ONLY look for it there.
 * 
 * @author don_bruce
 */
public abstract class APart{	
	/** Can a rider of this part send inputs to the vehicle this is a part of.*/
	public final boolean isController;
	/** Does this part rotate in-sync with the yaw changes of the vehicle.*/
	public final boolean turnsWithSteer;
	public final Vec3d offset;
	public final EntityVehicleE_Powered vehicle;
	public final String partName;
	public final PackPartObject pack;
	public final Vec3d partRotation;
	public final boolean overrideMirror;
	/**The parent of this part, if this part is a sub-part of a part or an additional part for a vehicle.*/
	public final APart parentPart;
	/**Children to this part.  Can be either additional parts or sub-parts.*/
	public final List<APart> childParts = new ArrayList<APart>();
	
	public Vec3d partPos;
	
	private boolean isValid;
	private ResourceLocation modelLocation;
		
	public APart(EntityVehicleE_Powered vehicle, PackPart packPart, String partName, NBTTagCompound dataTag){
		this.vehicle = vehicle;
		this.offset = new Vec3d(packPart.pos[0], packPart.pos[1], packPart.pos[2]);
		this.partName = partName;
		this.pack = PackParserSystem.getPartPack(partName);
		this.partPos = RotationSystem.getRotatedPoint(this.offset, vehicle.rotationPitch, vehicle.rotationYaw, vehicle.rotationRoll).add(this.vehicle.getPositionVector());
		this.partRotation = packPart.rot != null ? new Vec3d(packPart.rot[0], packPart.rot[1], packPart.rot[2]) : Vec3d.ZERO;
		this.isController = packPart.isController;
		this.turnsWithSteer = packPart.turnsWithSteer;
		this.overrideMirror = packPart.overrideMirror;
		this.isValid = true;
		
		//Check to see if we are an additional part to a part on our parent.
		//If we are not valid due to us being fake, don't add ourselves.
		if(this.isValid()){
			for(PackPart parentPackPart : vehicle.pack.parts){
				if(packPart.equals(parentPackPart.additionalPart)){
					parentPart = vehicle.getPartAtLocation(parentPackPart.pos[0], parentPackPart.pos[1], parentPackPart.pos[2]);
					parentPart.childParts.add(this);
					return;
				}
			}
			
			//If we aren't an additional part, see if we are a sub-part.
			for(APart part : vehicle.getVehicleParts()){
				if(part.pack.subParts != null){
					for(PackPart partSubPartPack : part.pack.subParts){
						if((float) part.offset.xCoord + partSubPartPack.pos[0] == (float) this.offset.xCoord && (float) part.offset.yCoord + partSubPartPack.pos[1] == (float) this.offset.yCoord && (float) part.offset.zCoord + partSubPartPack.pos[2] == (float) this.offset.zCoord){
							parentPart = part;
							parentPart.childParts.add(this);
							return;
						}
					}
				}
			}
		}
		parentPart = null;
	}
	
	/**Called right before this part is added to the vehicle.
	 * Should this be false, the part will not be added.
	 * This is also called during save operations to see if the part
	 * is still valid and should be saved.
	 */
	public boolean isValid(){
		return this.isValid;
	}

	/**Called when checking if this part can be interacted with.
	 * If a part does interactions it should do so and then return true.
	 */
	public boolean interactPart(EntityPlayer player){
		return false;
	}
	
	/**Called when the vehicle sees this part being attacked.
	 */
	public void attackPart(DamageSource source, float damage){}
	
	/**This gets called every tick by the vehicle after it finishes its update loop.
	 * Use this for reactions that this part can take based on its surroundings if need be.
	 */
	public void updatePart(){
		this.partPos = RotationSystem.getRotatedPoint(this.offset, vehicle.rotationPitch, vehicle.rotationYaw, vehicle.rotationRoll).add(vehicle.getPositionVector());
	}
	
	/**Called when the vehicle removes this part.
	 * Allows for parts to trigger logic that happens when they are removed.
	 * By default, this removes all sub-parts from the vehicle.
	 * It also removes any extra parts as defined in the vehicle JSON.
	 * Make sure to call the part's removal methods PRIOR to removing them
	 * from their vehicle as they need to be set invalid to prevent
	 * bad packets from arriving on the client.
	 */
	public void removePart(){
		this.isValid = false;
		while(childParts.size() > 0){
			APart childPart = childParts.get(0);
			childPart.removePart();
			vehicle.removePart(childPart, false);
			if(!vehicle.worldObj.isRemote){
				Item droppedItem = childPart.getItemForPart();
				if(droppedItem != null){
					ItemStack droppedStack = new ItemStack(droppedItem);
					droppedStack.setTagCompound(childPart.getPartNBTTag());
					vehicle.worldObj.spawnEntityInWorld(new EntityItem(vehicle.worldObj, childPart.partPos.xCoord, childPart.partPos.yCoord, childPart.partPos.zCoord, droppedStack));
				}
			}
		}
		if(this.parentPart != null){
			this.parentPart.childParts.remove(this);
		}
	}
	
	/**Return the part data in NBT form.
	 * This is called when removing the part from a vehicle to return an item.
	 * This is also called when saving this part, so ensure EVERYTHING you need to make this
	 * part back into an part again is returned in the NBT of this stack.
	 * This does not include the part offsets, as those are re-calculated every time the part is attached
	 * and are saved separately from the item NBT data in the vehicle.
	 */
	public abstract NBTTagCompound getPartNBTTag();
	
	public abstract float getWidth();
	
	public abstract float getHeight();
	
	/**Gets the item for this part.  If the part should not return an item 
	 * (either due to damage or other reasons) make this method return null.
	 */
	public Item getItemForPart(){
		return MTSRegistry.partItemMap.get(this.partName);
	}
	
	/**Gets the location of the model for this part. 
	 */
	public ResourceLocation getModelLocation(){
		if(modelLocation == null){
			if(pack.general.modelName != null){
				modelLocation = new ResourceLocation(partName.substring(0, partName.indexOf(':')), "objmodels/parts/" + pack.general.modelName + ".obj");
			}else{
				modelLocation = new ResourceLocation(partName.substring(0, partName.indexOf(':')), "objmodels/parts/" + partName.substring(partName.indexOf(':') + 1) + ".obj");
			}
		}
		return modelLocation;
	}
	
	/**Gets the location of the texture for this part.
	 * This can be changed for data-dependent part texture. 
	 */
	public ResourceLocation getTextureLocation(){
		return new ResourceLocation(partName.substring(0, partName.indexOf(':')), "textures/parts/" + partName.substring(partName.indexOf(':') + 1) + ".png");
	}
	
	public final VehicleAxisAlignedBB getAABBWithOffset(Vec3d boxOffset){
		Vec3d totalOffset = partPos.add(boxOffset);
		return new VehicleAxisAlignedBB(totalOffset, this.offset, this.getWidth(), this.getHeight(), false, false);
	}
	
	/**Gets the rotation vector for rendering.
	 * This comes from the part itself and is only
	 * changed on the client for animation purposes.
	 * Both this and partRotation are used
	 * to determine the final rotation of a part
	 * during rendering.
	 */
	public Vec3d getActionRotation(float partialTicks){
		return Vec3d.ZERO;
	}

	/**Checks to see if this part is collided with any collidable blocks.
	 * Uses a regular Vanilla check, as well as a liquid check for applicable parts.
	 * Can be given an offset vector to check for potential collisions. 
	 */
	public boolean isPartCollidingWithBlocks(Vec3d collisionOffset){
		return !vehicle.worldObj.getCollisionBoxes(this.getAABBWithOffset(collisionOffset)).isEmpty();
    }
}
