package minecrafttransportsimulator.multipart.parts;

import minecrafttransportsimulator.baseclasses.MultipartAxisAlignedBB;
import minecrafttransportsimulator.dataclasses.PackPartObject;
import minecrafttransportsimulator.multipart.main.EntityMultipartD_Moving;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
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
	//This can't be final as it doesn't allow us to change this in dev mode on a reload.
	public PackPartObject pack;
	
	public Vec3d partPos;
		
	public APart(EntityMultipartD_Moving multipart, Vec3d offset, boolean isController, boolean turnsWithSteer, String partName, NBTTagCompound dataTag){
		this.isController = isController;
		this.turnsWithSteer = turnsWithSteer;
		this.offset = offset;
		this.multipart = multipart;
		this.partName = partName;
		this.pack = PackParserSystem.getPartPack(partName);
		this.partPos = multipart.getPositionVector().add(offset);
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
		this.partPos = this.multipart.getPositionVector().add(this.offset);
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
		//TODO add method to return registered part items.
		return null;
	}
	
	/**Gets the location of the model for this part. 
	 */
	public final ResourceLocation getModelLocation(){
		return new ResourceLocation(this.pack.general.packID, "objmodels/parts/" + this.pack.general.partUniqueName + ".obj");
	}
	
	/**Gets the location of the texture for this part.
	 * This can be changed for data-dependent part texture. 
	 */
	public final ResourceLocation getTextureLocation(){
		return new ResourceLocation(this.pack.general.packID, "textures/parts/" + this.pack.general.partUniqueName + ".png");
	}
	
	public final MultipartAxisAlignedBB getAABBWithOffset(Vec3d boxOffset){
		Vec3d totalOffset = partPos.add(boxOffset);
		return new MultipartAxisAlignedBB(totalOffset, this.offset, this.getWidth(), this.getHeight());
	}
	
	/**Gets the current rotation for rendering.
	 * This is used for rotating this part by the rendering
	 * system and also represents the general "state" of this
	 * part on it's offset point.  Used for multiple things
	 * in multiple places. 
	 */
	public Vec3d getRotation(float partialTicks){
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
