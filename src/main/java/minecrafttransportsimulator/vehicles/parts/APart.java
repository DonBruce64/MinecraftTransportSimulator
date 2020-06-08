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
import minecrafttransportsimulator.systems.VehicleAnimationSystem;
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
	//JSON properties.
	public final JSONPart definition;
	public final VehiclePart vehicleDefinition;
	public final Vec3d placementOffset;
	public final Vec3d placementRotation;
	public final boolean disableMirroring;
	
	//Instance properties.
	public final EntityVehicleE_Powered vehicle;
	/**The parent of this part, if this part is a sub-part of a part or an additional part for a vehicle.*/
	public final APart parentPart;
	/**Children to this part.  Can be either additional parts or sub-parts.*/
	public final List<APart> childParts = new ArrayList<APart>();
	
	//Runtime variables.
	private final FloatBuffer soundPosition = ByteBuffer.allocateDirect(3*Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
	public Vec3d totalOffset;
	public Vec3d movementRotation;
	public Vec3d worldPos;
	private boolean isValid;
		
	public APart(EntityVehicleE_Powered vehicle, VehiclePart packVehicleDef, JSONPart definition, NBTTagCompound dataTag){
		this.vehicle = vehicle;
		this.placementOffset = new Vec3d(packVehicleDef.pos[0], packVehicleDef.pos[1], packVehicleDef.pos[2]);
		this.totalOffset = placementOffset;
		this.definition = definition;;
		this.vehicleDefinition = packVehicleDef;
		this.worldPos = RotationSystem.getRotatedPoint(this.placementOffset, vehicle.rotationPitch, vehicle.rotationYaw, vehicle.rotationRoll).add(this.vehicle.getPositionVector());
		this.placementRotation = packVehicleDef.rot != null ? new Vec3d(packVehicleDef.rot[0], packVehicleDef.rot[1], packVehicleDef.rot[2]) : Vec3d.ZERO;
		this.isValid = true;
		
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
		//Set the updated totalOffset and worldPos.  This is used for part position, but not rendering.
		if(parentPart != null && vehicleDefinition.isSubPart){
			//Get the relative distance between our offset and our parent's offset.
			Vec3d relativeOffset = placementOffset.add(getPositionOffset(0)).subtract(parentPart.placementOffset);
			
			//Rotate by the parent's rotation to match orientation.
			Vec3d parentRotation = parentPart.getPositionRotation(0);
			relativeOffset = RotationSystem.getRotatedPoint(relativeOffset, (float) parentRotation.x, (float) parentRotation.y, (float) parentRotation.z);
			
			//Rotate again to take the action rotation into account.
			parentRotation = parentPart.getActionRotation(0);
			relativeOffset = RotationSystem.getRotatedPoint(relativeOffset, (float) parentRotation.x, (float) parentRotation.y, (float) parentRotation.z);
			
			//Add parent offset to our offset to get actual point.
			totalOffset = parentPart.placementOffset.add(parentPart.getPositionOffset(0)).add(relativeOffset);
		}else{
			totalOffset = placementOffset.add(getPositionOffset(0));
		}
		worldPos = RotationSystem.getRotatedPoint(totalOffset, vehicle.rotationPitch, vehicle.rotationYaw, vehicle.rotationRoll).add(vehicle.getPositionVector());

		//Update sound variables.
		soundPosition.rewind();
		soundPosition.put((float) worldPos.x);
		soundPosition.put((float) worldPos.y);
		soundPosition.put((float) worldPos.z);
		soundPosition.flip();
	}
	
	public final VehicleAxisAlignedBB getAABBWithOffset(Vec3d boxOffset){
		return new VehicleAxisAlignedBB(Vec3d.ZERO.equals(boxOffset) ? worldPos : worldPos.add(boxOffset), this.placementOffset.add(getPositionOffset(0)), this.getWidth(), this.getHeight(), false, false);
	}
	
	/**Gets the movement position offset for the part as a vector.
	 * This offset is an addition to the main placement offset defined by the JSON.
	 */
	public final Vec3d getPositionOffset(float partialTicks){
		//First rotate about the rotation point and angles.
		Vec3d rotationAngles = getPositionRotation(partialTicks);
		Vec3d rotationOffset = Vec3d.ZERO;
		if(!rotationAngles.equals(Vec3d.ZERO)){
			rotationOffset = RotationSystem.getRotatedPoint(new Vec3d(-vehicleDefinition.rotationPosition[0], -vehicleDefinition.rotationPosition[1], -vehicleDefinition.rotationPosition[2]), (float) rotationAngles.x, (float) rotationAngles.y, (float) rotationAngles.z);
			rotationOffset = rotationOffset.addVector(vehicleDefinition.rotationPosition[0], vehicleDefinition.rotationPosition[1], vehicleDefinition.rotationPosition[2]);
		}
		
		//Now translate.  This may incorporate rotation angles.
		Vec3d translationOffset = Vec3d.ZERO;
		if(vehicleDefinition.translationVariable != null){
			double translationValue = VehicleAnimationSystem.getVariableValue(vehicleDefinition.translationVariable, 1, 0, vehicleDefinition.translationClampMin, vehicleDefinition.translationClampMax, vehicleDefinition.translationAbsolute, partialTicks, vehicle, this);
			translationOffset = new Vec3d(translationValue*vehicleDefinition.translationPosition[0], translationValue*vehicleDefinition.translationPosition[1], translationValue*vehicleDefinition.translationPosition[2]); 
			if(!rotationAngles.equals(Vec3d.ZERO)){
				translationOffset = RotationSystem.getRotatedPoint(translationOffset, (float) rotationAngles.x, (float) rotationAngles.y, (float) rotationAngles.z);
			}
		}
		
		//Add rotation and translation offset and return net offset.
		return rotationOffset.add(translationOffset);
	}
	
	/**Gets the rotation angles for the part as a vector.
	 * This rotation is used to rotate the part prior to translation.
	 * It may be used for stacked rotations, and should return the final
	 * rotation vector for all operations.
	 */
	public final Vec3d getPositionRotation(float partialTicks){
		if(vehicleDefinition.rotationVariable != null){
			double rotationValue = VehicleAnimationSystem.getVariableValue(vehicleDefinition.rotationVariable, 1, 0, vehicleDefinition.rotationClampMin, vehicleDefinition.rotationClampMax, vehicleDefinition.rotationAbsolute, partialTicks, vehicle, this);
			return new Vec3d(rotationValue*vehicleDefinition.rotationAngles[0], rotationValue*vehicleDefinition.rotationAngles[1], rotationValue*vehicleDefinition.rotationAngles[2]);
		}else{
			return Vec3d.ZERO;
		}
	}
	
	/**Gets the rotation angles for the part as a vector.
	 * This rotation is based on the internal part state, and cannot be modified via JSON.
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
