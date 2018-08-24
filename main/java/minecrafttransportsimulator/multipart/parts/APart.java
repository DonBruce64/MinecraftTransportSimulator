package minecrafttransportsimulator.multipart.parts;

import minecrafttransportsimulator.baseclasses.MultipartAxisAlignedBB;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackPart;
import minecrafttransportsimulator.dataclasses.PackPartObject;
import minecrafttransportsimulator.multipart.main.EntityMultipartD_Moving;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.systems.RotationSystem;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

/**This class is the base for all parts and should be
 * extended for any multipart-compatible parts.
 * Use {@link EntityMultipart#addPart(EntityPart, boolean)} to add parts 
 * and {@link EntityMultipart#removePart(EntityPart, boolean)} to remove them.
 * You may extend {@link EntityMultipart} to get more functionality with those systems.
 * If you need to keep extra data ensure it is packed into whatever NBT is returned in item form.
 * This NBT will be fed into the constructor when creating this part, so expect it and ONLY look for it there.
 * 
 * @author don_bruce
 */
public abstract class APart{	
	/** Can a rider of this part send inputs to the multipart this is a part of.*/
	public final boolean isController;
	/** Does this part rotate in-sync with the yaw changes of the multipart.*/
	public final boolean turnsWithSteer;
	public final Vec3d offset;
	public final EntityMultipartD_Moving multipart;
	public final String partName;
	public final PackPartObject pack;
	public final Vec3d partRotation;
	public final boolean overrideMirror;
	
	public Vec3d partPos;
	
	private boolean isValid;
	private ResourceLocation modelLocation;
		
	public APart(EntityMultipartD_Moving multipart, Vec3d offset, boolean isController, boolean turnsWithSteer, String partName, NBTTagCompound dataTag){
		this.isController = isController;
		this.turnsWithSteer = turnsWithSteer;
		this.offset = offset;
		this.multipart = multipart;
		this.partName = partName;
		this.pack = PackParserSystem.getPartPack(partName);
		this.partPos = RotationSystem.getRotatedPoint(this.offset, multipart.rotationPitch, multipart.rotationYaw, multipart.rotationRoll).add(this.multipart.getPositionVector());
		PackPart packPart = multipart.getPackDefForLocation(offset.xCoord, offset.yCoord, offset.zCoord);
		this.partRotation = packPart.rot != null ? new Vec3d(packPart.rot[0], packPart.rot[1], packPart.rot[2]) : Vec3d.ZERO;
		this.overrideMirror = packPart.overrideMirror;
		this.isValid = true;
	}
	
	/**Called right before this part is added to the multipart.
	 * Should this be false, the part will not be added.
	 * This is also called during save operations to see if the part
	 * is still valid and should be saved.
	 */
	public boolean isValid(){
		return this.isValid;
	}

	/**Called when checking if this part can be interacted with.
	 * If a part does interctions it should do so and then return true.
	 */
	public boolean interactPart(EntityPlayer player){
		return false;
	}
	
	/**Called when the master multipart sees this part being attacked.
	 */
	public void attackPart(DamageSource source, float damage){}
	
	/**This gets called every tick by the multipart after it finishes its update loop.
	 * Use this for reactions that this part can take based on its surroundings if need be.
	 */
	public void updatePart(){
		this.partPos = RotationSystem.getRotatedPoint(this.offset, multipart.rotationPitch, multipart.rotationYaw, multipart.rotationRoll).add(this.multipart.getPositionVector());
	}
	
	/**Called when the master multipart removes this part.
	 * Allows for parts to trigger logic that happens when they are removed.
	 * By default, this removes all sub-parts from the multipart.
	 */
	public void removePart(){
		this.isValid = false;
		if(pack.subParts != null){
			for(PackPart packPart : pack.subParts){
				APart subPart = multipart.getPartAtLocation(this.offset.xCoord + packPart.pos[0], this.offset.yCoord + packPart.pos[1], this.offset.zCoord + packPart.pos[2]);
				if(subPart != null){
					multipart.removePart(subPart, false);
					subPart.removePart();
					Item droppedItem = subPart.getItemForPart();
					if(droppedItem != null){
						ItemStack droppedStack = new ItemStack(droppedItem);
						droppedStack.setTagCompound(subPart.getPartNBTTag());
						multipart.worldObj.spawnEntityInWorld(new EntityItem(multipart.worldObj, subPart.partPos.xCoord, subPart.partPos.yCoord, subPart.partPos.zCoord, droppedStack));
					}
				}
			}
		}
	}
	
	/**Return the part data in NBT form.
	 * This is called when removing the part from a multipart to return an item.
	 * This is also called when saving this part, so ensure EVERYTHING you need to make this
	 * part back into an part again is returned in the NBT of this stack.
	 * This does not include the part offsets, as those are re-calculated every time the part is attached
	 * and are saved separately from the item NBT data in the multipart.
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
	
	public final MultipartAxisAlignedBB getAABBWithOffset(Vec3d boxOffset){
		Vec3d totalOffset = partPos.add(boxOffset);
		return new MultipartAxisAlignedBB(totalOffset, this.offset, this.getWidth(), this.getHeight());
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
		return !multipart.worldObj.getCollisionBoxes(this.getAABBWithOffset(collisionOffset)).isEmpty();
    }
}
