package minecrafttransportsimulator.vehicles.parts;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.VehicleAxisAlignedBB;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.sound.ISoundProvider;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.systems.RotationSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**This class is the base for all parts and should be extended for any vehicle-compatible parts.
 * Use {@link EntityVehicleA_Base#addPart(APart, boolean)} to add parts 
 * and {@link EntityVehicleA_Base#removePart(APart, boolean)} to remove them.
 * You may extend {@link EntityVehicleE_Powered} to get more functionality with those systems.
 * If you need to keep extra data ensure it is packed into whatever NBT is returned in item form.
 * This NBT will be fed into the constructor when creating this part, so expect it and ONLY look for it there.
 * 
 * @author don_bruce
 */
public abstract class APart implements ISoundProvider{	
	/** Can a rider of this part send inputs to the vehicle this is a part of.*/
	public final boolean isController;
	/** Does this part rotate in-sync with the yaw changes of the vehicle.*/
	public final boolean turnsWithSteer;
	/**The offset from the center of the vehicle where this part is placed.*/
	public final Vec3d placementOffset;
	public final EntityVehicleE_Powered vehicle;
	public final VehiclePart packVehicleDef;
	public final JSONPart definition;
	/**The placement rotation for this part, as defined by the pack definition.*/
	public final Vec3d placementRotation;
	public final boolean inverseMirroring;
	public final boolean disableMirroring;
	private final FloatBuffer soundPosition = ByteBuffer.allocateDirect(3*Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
	
	/**The parent of this part, if this part is a sub-part of a part or an additional part for a vehicle.*/
	public final APart parentPart;
	/**Children to this part.  Can be either additional parts or sub-parts.*/
	public final List<APart> childParts = new ArrayList<APart>();
	/**The part's current position, in world coordinates.*/
	public Vec3d worldPos;
	
	//Internal cached variables.
	private boolean isValid;
		
	public APart(EntityVehicleE_Powered vehicle, VehiclePart packVehicleDef, JSONPart definition, NBTTagCompound dataTag){
		this.vehicle = vehicle;
		this.placementOffset = new Vec3d(packVehicleDef.pos[0], packVehicleDef.pos[1], packVehicleDef.pos[2]);
		this.definition = definition;;
		this.packVehicleDef = packVehicleDef;
		this.worldPos = RotationSystem.getRotatedPoint(this.placementOffset, vehicle.rotationPitch, vehicle.rotationYaw, vehicle.rotationRoll).add(this.vehicle.getPositionVector());
		this.placementRotation = packVehicleDef.rot != null ? new Vec3d(packVehicleDef.rot[0], packVehicleDef.rot[1], packVehicleDef.rot[2]) : Vec3d.ZERO;
		this.isController = packVehicleDef.isController;
		this.turnsWithSteer = packVehicleDef.turnsWithSteer;
		this.isValid = true;
		this.inverseMirroring = packVehicleDef.inverseMirroring;
		
		//Check to see if we are an additional part to a part on our parent.
		//If we are not valid due to us being fake, don't add ourselves.
		if(this.isValid()){
			for(VehiclePart parentPackPart : vehicle.definition.parts){
				if(packVehicleDef.equals(parentPackPart.additionalPart)){
					parentPart = vehicle.getPartAtLocation(parentPackPart.pos[0], parentPackPart.pos[1], parentPackPart.pos[2]);
					parentPart.childParts.add(this);
					this.disableMirroring = definition.general.disableMirroring;
					return;
				}
			}
			
			//If we aren't an additional part, see if we are a sub-part.
			for(APart part : vehicle.getVehicleParts()){
				if(part.definition.subParts != null){
					for(VehiclePart partSubPartPack : part.definition.subParts){
						if((float) part.placementOffset.x + partSubPartPack.pos[0] == (float) this.placementOffset.x && (float) part.placementOffset.y + partSubPartPack.pos[1] == (float) this.placementOffset.y && (float) part.placementOffset.z + partSubPartPack.pos[2] == (float) this.placementOffset.z){
							parentPart = part;
							parentPart.childParts.add(this);
							this.disableMirroring = parentPart.disableMirroring || definition.general.disableMirroring;
							return;
						}
					}
				}
			}
		}
		this.disableMirroring = definition.general.disableMirroring;
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
	 * Call this ONLY from the server-side!  The server will handle the
	 * interaction by notifying the client via packet if appropriate.
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
		//Set position depending on if we are on a rotated parent or not.
		//If we are on a rotated part, set the movementOffset and the partPos to reflect our rotated position.
		Vec3d parentActionRotation = packVehicleDef.isSubPart && parentPart != null ? parentPart.getActionRotation(0) : Vec3d.ZERO;
		if(!parentActionRotation.equals(Vec3d.ZERO)){
			Vec3d partRelativeOffset = placementOffset.subtract(parentPart.placementOffset);
			Vec3d movementOffset = parentPart.placementOffset.add(RotationSystem.getRotatedPoint(partRelativeOffset, (float) parentActionRotation.x, (float) parentActionRotation.y, (float) parentActionRotation.z));
			worldPos = RotationSystem.getRotatedPoint(movementOffset, vehicle.rotationPitch, vehicle.rotationYaw, vehicle.rotationRoll).add(vehicle.getPositionVector());
		}else{
			worldPos = RotationSystem.getRotatedPoint(placementOffset, vehicle.rotationPitch, vehicle.rotationYaw, vehicle.rotationRoll).add(vehicle.getPositionVector());
		}

		//Update sound variables.
		soundPosition.rewind();
		soundPosition.put((float) worldPos.x);
		soundPosition.put((float) worldPos.y);
		soundPosition.put((float) worldPos.z);
		soundPosition.flip();
	}
	
	public final VehicleAxisAlignedBB getAABBWithOffset(Vec3d boxOffset){
		return new VehicleAxisAlignedBB(Vec3d.ZERO.equals(boxOffset) ? worldPos : worldPos.add(boxOffset), this.placementOffset, this.getWidth(), this.getHeight(), false, false);
	}
	
	/**Gets the rotation vector for the part.
	 * This comes from the part itself and is used to determine the angle of the part for rendering
	 * and for translation of sub-parts.  This rotation is variable and depends on what the part is doing.
	 * This is different from partRotation, which is a fixed rotation component that comes from the vehicle JSONs.
	 */
	public Vec3d getActionRotation(float partialTicks){
		return Vec3d.ZERO;
	}

	/**Checks to see if this part is collided with any collidable blocks.
	 * Uses a regular Vanilla check: liquid checks may be done by overriding this method.
	 * Can be given an offset vector to check for potential collisions. 
	 */
	public boolean isPartCollidingWithBlocks(Vec3d collisionOffset){
		return !vehicle.world.getCollisionBoxes(null, this.getAABBWithOffset(collisionOffset)).isEmpty();
    }
	
	/**Checks to see if this part is collided with any liquid blocks.
	 * Can be given an offset vector to check for potential collisions. 
	 */
	public boolean isPartCollidingWithLiquids(Vec3d collisionOffset){
		VehicleAxisAlignedBB collisionBox = this.getAABBWithOffset(collisionOffset);
		int minX = (int) Math.floor(collisionBox.minX);
    	int maxX = (int) Math.floor(collisionBox.maxX + 1.0D);
    	int minY = (int) Math.floor(collisionBox.minY);
    	int maxY = (int) Math.floor(collisionBox.maxY + 1.0D);
    	int minZ = (int) Math.floor(collisionBox.minZ);
    	int maxZ = (int) Math.floor(collisionBox.maxZ + 1.0D);
    	
    	for(int i = minX; i < maxX; ++i){
    		for(int j = minY; j < maxY; ++j){
    			for(int k = minZ; k < maxZ; ++k){
    				BlockPos checkPos = new BlockPos(i, j, k);
    				if(vehicle.world.isBlockLoaded(checkPos)){
	    				if(vehicle.world.getBlockState(checkPos).getMaterial().isLiquid()){
	    					return true;
	    				}
    				}
    			}
    		}
    	}
		return false;
    }
	
	/**Called when the vehicle removes this part.
	 * Allows for parts to trigger logic that happens when they are removed.
	 * Note that hitboxes are configured to not allow this part to be
	 * wrenched if it has children, so it may be assumed that no child
	 * parts are present when this action occurs.  Do note that it's possible
	 * this part is a child to another part, so you will need to remove this
	 * part as the child from its parent if is has one.
	 */
	public void removePart(){
		this.isValid = false;
		if(this.parentPart != null){
			this.parentPart.childParts.remove(this);
		}
	}
	
	/**Gets the item for this part.  If the part should not return an item 
	 * (either due to damage or other reasons) make this method return null.
	 */
	public Item getItemForPart(){
		return MTSRegistry.packItemMap.get(definition.packID).get(definition.systemName);
	}
	
	/**Return the part data in NBT form.
	 * This is called when removing the part from a vehicle to return an item.
	 * This is also called when saving this part, so ensure EVERYTHING you need to make this
	 * part back into an part again is packed into the NBT tag that is returned.
	 * This does not include the part offsets, as those are re-calculated every time the part is attached
	 * and are saved separately from the item NBT data in the vehicle.
	 */
	public abstract NBTTagCompound getPartNBTTag();
	
	public abstract float getWidth();
	
	public abstract float getHeight();
	

	
	//--------------------START OF CLIENT-SPECIFIC CODE--------------------
	/**
	 * Gets the location of the model for this part. 
	 */
	public String getModelLocation(){
		return "objmodels/parts/" + (definition.general.modelName != null ? definition.general.modelName : definition.systemName) + ".obj";
	}
	
	/**Gets the location of the texture for this part.
	 * This can be changed for data-dependent part texture. 
	 */
	public String getTextureLocation(){
		return "textures/parts/" + definition.systemName + ".png";
	}
	
	@Override
	public void updateProviderSound(SoundInstance sound){
		if(!this.isValid || vehicle.isDead){
			sound.stop();
		}
	}
	
	@Override
	public void restartSound(SoundInstance sound){}
    
	@Override
    public FloatBuffer getProviderPosition(){
		return soundPosition;
	}
    
	@Override
    public FloatBuffer getProviderVelocity(){
		return vehicle.getProviderVelocity();
	}
	
	@Override
    public int getProviderDimension(){
		return vehicle.getProviderDimension();
	}
}
