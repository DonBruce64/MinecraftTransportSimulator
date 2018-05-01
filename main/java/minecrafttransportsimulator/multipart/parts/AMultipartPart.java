package minecrafttransportsimulator.multipart.parts;

import minecrafttransportsimulator.dataclasses.PackPartObject;
import minecrafttransportsimulator.entities.core.EntityMultipart;
import minecrafttransportsimulator.entities.core.EntityMultipartA_Base;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
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
public abstract class AMultipartPart{	
	/** Can a rider of this part send inputs to the multipart this is a part of.*/
	public final boolean isController;
	/** Does this part rotate in-sync with the yaw changes of the multipart.*/
	public final boolean turnsWithSteer;
	public final Vec3d offset;
	public final EntityMultipartA_Base multipart;
	public final PackPartObject packInfo;
		
	public AMultipartPart(EntityMultipartA_Base multipart, Vec3d offset, boolean isController, boolean turnsWithSteer, PackPartObject packInfo, NBTTagCompound dataTag){
		this.isController = isController;
		this.turnsWithSteer = turnsWithSteer;
		this.offset = offset;
		this.multipart = multipart;
		this.packInfo = packInfo;
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
	
	/**Gets the item for this part.
	 * If the part should not return an item (either due to damage or other reasons)
	 * make this method return null.
	 */
	public Item getItemForPart(){
		//TODO add method to return registered part items.
	}
	
	/**Gets the location of the model for this part. 
	 */
	public final ResourceLocation getModelLocation(){
		return new ResourceLocation(this.packInfo.general.packID, "objmodels/parts/" + this.packInfo.general.partUniqueName + ".obj");
	}
	
	/**Gets the location of the texture for this part.
	 * This can be changed for data-dependent part texture. 
	 */
	public final ResourceLocation getTextureLocation(){
		return new ResourceLocation(this.packInfo.general.packID, "textures/parts/" + this.packInfo.general.partUniqueName + ".png");
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
		Vec3d partPosition = this.multipart.getPositionVector().add(this.offset).add(collisionOffset);
		AxisAlignedBB collisionBox = new AxisAlignedBB(
				partPosition.xCoord - this.getWidth()/2F, 
				partPosition.yCoord - this.getHeight()/2F,
				partPosition.zCoord - this.getWidth()/2F,
				partPosition.xCoord + this.getWidth()/2F,
				partPosition.yCoord + this.getHeight()/2F,
				partPosition.zCoord + this.getWidth()/2F);
		
    	if(!multipart.worldObj.getCollisionBoxes(collisionBox).isEmpty()){
    		return true;
    	}else{
    		if(!collidesWithLiquids()){
    			return false;
    		}else{
    			int minX = (int) Math.floor(collisionBox.minX);
    	    	int maxX = (int) Math.floor(collisionBox.maxX + 1.0D);
    	    	int minY = (int) Math.floor(collisionBox.minY);
    	    	int maxY = (int) Math.floor(collisionBox.maxY + 1.0D);
    	    	int minZ = (int) Math.floor(collisionBox.minZ);
    	    	int maxZ = (int) Math.floor(collisionBox.maxZ + 1.0D);
    	    	
    	    	for(int i = minX; i < maxX; ++i){
    	    		for(int j = minY; j < maxY; ++j){
    	    			for(int k = minZ; k < maxZ; ++k){
    	    				if(multipart.worldObj.getBlockState(new BlockPos(i, j, k)).getMaterial().isLiquid()){
    	    					return true;
    	    				}
    	    			}
    	    		}
    	    	}
    	    	return false;
    		}
    	}
    }
}
