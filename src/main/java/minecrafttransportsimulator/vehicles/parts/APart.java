package minecrafttransportsimulator.vehicles.parts;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import mcinterface.BuilderEntity;
import mcinterface.WrapperNBT;
import mcinterface.WrapperPlayer;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.sound.ISoundProvider;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.systems.VehicleAnimationSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import net.minecraft.item.Item;

/**This class is the base for all parts and should be extended for any vehicle-compatible parts.
 * Use {@link BuilderEntity#addPart(APart, boolean)} to add parts 
 * and {@link BuilderEntity#removePart(APart, boolean)} to remove them.
 * You may extend {@link EntityVehicleE_Powered} to get more functionality with those systems.
 * If you need to keep extra data ensure it is packed into whatever NBT is returned in item form.
 * This NBT will be fed into the constructor when creating this part, so expect it and ONLY look for it there.
 * 
 * @author don_bruce
 */
public abstract class APart implements ISoundProvider{
	private static final Point3d ZERO_POINT = new Point3d(0, 0, 0);
	
	//JSON properties.
	public final JSONPart definition;
	public final VehiclePart vehicleDefinition;
	public final Point3d placementOffset;
	public final Point3d placementRotation;
	public final boolean disableMirroring;
	
	//Instance properties.
	public final EntityVehicleF_Physics vehicle;
	/**The parent of this part, if this part is a sub-part of a part or an additional part for a vehicle.*/
	public final APart parentPart;
	/**Children to this part.  Can be either additional parts or sub-parts.*/
	public final List<APart> childParts = new ArrayList<APart>();
	/**List containing text lines for saved text.**/
	public final List<String> textLines = new ArrayList<String>();
	
	//Runtime variables.
	private final FloatBuffer soundPosition = ByteBuffer.allocateDirect(3*Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
	public final Point3d totalOffset;
	public final Point3d totalRotation;
	public final Point3d worldPos;
	public final BoundingBox boundingBox;
	public boolean isValid = true;
		
	public APart(EntityVehicleF_Physics vehicle, VehiclePart packVehicleDef, JSONPart definition, WrapperNBT data, APart parentPart){
		this.vehicle = vehicle;
		this.placementOffset = new Point3d(packVehicleDef.pos[0], packVehicleDef.pos[1], packVehicleDef.pos[2]);
		this.totalOffset = placementOffset.copy();
		this.definition = definition;;
		this.vehicleDefinition = packVehicleDef;
		this.worldPos = placementOffset.copy().rotateFine(vehicle.angles).add(vehicle.position);
		this.boundingBox = new BoundingBox(getPositionOffset(0).add(placementOffset), worldPos, getWidth()/2D, getHeight()/2D, getWidth()/2D, definition.ground != null ? definition.ground.canFloat : false, false);
		this.placementRotation = packVehicleDef.rot != null ? new Point3d(packVehicleDef.rot[0], packVehicleDef.rot[1], packVehicleDef.rot[2]) : new Point3d(0, 0, 0);
		this.totalRotation = placementRotation;
		this.isValid = true;
		
		//Load text.
		if(definition.rendering != null && definition.rendering.textObjects != null){
			for(byte i=0; i<definition.rendering.textObjects.size(); ++i){
				textLines.add(data.getString("textLine" + i));
			}
		}
		
		//If we are an additional part or sub-part, link ourselves now.
		//If we are a fake part, don't even bother checking.
		if(!isFake() && parentPart != null){
			this.parentPart = parentPart;
			parentPart.childParts.add(this);
			if(packVehicleDef.isSubPart){
				this.disableMirroring = parentPart.disableMirroring || definition.general.disableMirroring;
			}else{
				this.disableMirroring = definition.general.disableMirroring;
			}
		}else{
			this.disableMirroring = definition.general.disableMirroring;
			this.parentPart = null;
		}
	}
	
	/**
	 * This is called during part save/load calls.  Fakes parts are
	 * added to vehicles, but they aren't saved with the NBT.  Rather, 
	 * they should be re-created in the constructor of the part that added
	 * them in the first place.
	 */
	public boolean isFake(){
		return false;
	}

	/**
	 * Called when checking if this part can be interacted with.
	 * If a part does interactions it should do so and then return true.
	 * Call this ONLY from the server-side!  The server will handle the
	 * interaction by notifying the client via packet if appropriate.
	 */
	public boolean interact(WrapperPlayer player){
		return false;
	}
	
	/**
	 * Called when the vehicle sees this part being attacked.
	 */
	public void attack(Damage damage){}
	
	/**
	 * This gets called every tick by the vehicle after it finishes its update loop.
	 * Use this for reactions that this part can take based on its surroundings if need be.
	 * Do NOT remove the part from the vehicle in this loop.  Instead, set it to invalid.
	 * Removing the part during this loop will earn you a CME.
	 */
	public void update(){
		//Set the updated totalOffset and worldPos.  This is used for part position, but not rendering.
		if(parentPart != null && vehicleDefinition.isSubPart){
			//First, get the relative distance between our offset and our parent's offset.
			totalOffset.setTo(getPositionOffset(0)).add(placementOffset).subtract(parentPart.placementOffset);
			
			//Now get our parent's rotation contribution.
			totalRotation.setTo(parentPart.getPositionRotation(0)).add(parentPart.placementRotation);
			
			//Rotate our current relative offset by the rotation of the parent to get the correct
			//offset between us and our paren't position in our parent's coordinate system.
			totalOffset.rotateFine(totalRotation);
			
			//Now, get the parent's action rotation, and rotate again to take that rotation into account.
			Point3d parentActionRotation = parentPart.getActionRotation(0);
			totalOffset.rotateFine(parentActionRotation);
			
			//Now that we have the proper relative offset, add our placement and position offsets.
			//This is our final offset point.
			totalOffset.add(parentPart.placementOffset).add(parentPart.getPositionOffset(0));
			
			//Now that we have our offset, add the parent's rotations with our rotations to get the total rotation. 
			//FIXME this may be wrong, but it may also be right?
			totalRotation.add(parentActionRotation);
			//totalRotation.add(parentActionRotation).add(getPositionRotation(0)).add(placementRotation);
		}else{
			totalOffset.setTo(getPositionOffset(0)).add(placementOffset);
			totalRotation.setTo(getPositionRotation(0)).add(placementRotation);
		}
		worldPos.setTo(totalOffset).rotateFine(vehicle.angles).add(vehicle.position);

		//Update sound variables.
		soundPosition.rewind();
		soundPosition.put((float) worldPos.x);
		soundPosition.put((float) worldPos.y);
		soundPosition.put((float) worldPos.z);
		soundPosition.flip();
	}
	
	/**
	 * Gets the movement position offset for the part as a vector.
	 * This offset is an addition to the main placement offset defined by the JSON.
	 */
	public final Point3d getPositionOffset(float partialTicks){
		Point3d positionOffset;
		
		//First rotate about the rotation point and angles.
		Point3d rotationAngles = getPositionRotation(partialTicks);
		if(!rotationAngles.isZero()){
			positionOffset = new Point3d(-vehicleDefinition.rotationPosition[0], -vehicleDefinition.rotationPosition[1], -vehicleDefinition.rotationPosition[2]).rotateFine(rotationAngles);
			positionOffset.add(vehicleDefinition.rotationPosition[0], vehicleDefinition.rotationPosition[1], vehicleDefinition.rotationPosition[2]);
		}else{
			positionOffset = new Point3d(0D, 0D, 0D);
		}
		
		//Now translate.  This may incorporate rotation angles.
		if(vehicleDefinition.translationVariable != null){
			double translationValue = VehicleAnimationSystem.getVariableValue(vehicleDefinition.translationVariable, 1, 0, vehicleDefinition.translationClampMin, vehicleDefinition.translationClampMax, vehicleDefinition.translationAbsolute, partialTicks, vehicle, this);
			Point3d translationOffset = new Point3d(translationValue*vehicleDefinition.translationPosition[0], translationValue*vehicleDefinition.translationPosition[1], translationValue*vehicleDefinition.translationPosition[2]); 
			if(!rotationAngles.isZero()){
				translationOffset.rotateFine(rotationAngles);
			}
			positionOffset.add(translationOffset);
		}
		
		//Return the net offset from rotation and translation.
		return positionOffset;
	}
	
	/**
	 * Gets the rotation angles for the part as a vector.
	 * This rotation is used to rotate the part prior to translation.
	 * It may be used for stacked rotations, and should return the final
	 * rotation vector for all operations.
	 */
	public final Point3d getPositionRotation(float partialTicks){
		if(vehicleDefinition.rotationVariable != null){
			double rotationValue = VehicleAnimationSystem.getVariableValue(vehicleDefinition.rotationVariable, 1, 0, vehicleDefinition.rotationClampMin, vehicleDefinition.rotationClampMax, vehicleDefinition.rotationAbsolute, partialTicks, vehicle, this);
			if(rotationValue != 0){
				return new Point3d(rotationValue*vehicleDefinition.rotationAngles[0], rotationValue*vehicleDefinition.rotationAngles[1], rotationValue*vehicleDefinition.rotationAngles[2]);
			}
		}
		return new Point3d(0, 0, 0);
	}
	
	/**
	 * Gets the rotation angles for the part as a vector.
	 * This rotation is based on the internal part state, and cannot be modified via JSON.
	 */
	public Point3d getActionRotation(float partialTicks){
		return ZERO_POINT;
	}
	
	/**
	 * Returns true if this part is in liquid.
	 */
	public boolean isInLiquid(){
		return vehicle.world.isBlockLiquid(new Point3i(worldPos));
	}
	
	/**
	 * Called when the vehicle removes this part.
	 * Allows for parts to trigger logic that happens when they are removed.
	 * Note that hitboxes are configured to not allow this part to be
	 * wrenched if it has children, so it may be assumed that no child
	 * parts are present when this action occurs.  Do note that it's possible
	 * this part is a child to another part, so you will need to remove this
	 * part as the child from its parent if is has one.  Also note that you may
	 * NOT remove any other parts in this method.  Doing so will get you a CME.
	 * If you need to remove another part, set it to invalid instead.  This will
	 * have it be removed at the end of the update loop.
	 */
	public void remove(){
		isValid = false;
		if(parentPart != null){
			parentPart.childParts.remove(this);
		}
	}
	
	/**
	 * Gets the item for this part.  If the part should not return an item 
	 * (either due to damage or other reasons) make this method return null.
	 */
	public Item getItem(){
		return MTSRegistry.packItemMap.get(definition.packID).get(definition.systemName);
	}
	
	/**
	 * Return the part data in NBT form.
	 * This is called when removing the part from a vehicle to return an item.
	 * This is also called when saving this part, so ensure EVERYTHING you need to make this
	 * part back into an part again is packed into the NBT tag that is returned.
	 * This does not include the part offsets, as those are re-calculated every time the part is attached
	 * and are saved separately from the item NBT data in the vehicle.
	 */
	public WrapperNBT getData(){
		WrapperNBT data = new WrapperNBT();
		if(definition.rendering != null && definition.rendering.textObjects != null){
			for(byte i=0; i<definition.rendering.textObjects.size(); ++i){
				data.setString("textLine" + i, textLines.get(i));
			}
		}
		return data;
	}
	
	public abstract float getWidth();
	
	public abstract float getHeight();
	

	
	//--------------------START OF CLIENT-SPECIFIC CODE--------------------
	/**
	 * Gets the location of the model for this part. 
	 */
	public String getModelLocation(){
		return "objmodels/parts/" + (definition.general.modelName != null ? definition.general.modelName : definition.systemName) + ".obj";
	}
	
	/**
	 * Gets the location of the texture for this part.
	 * This can be changed for data-dependent part texture. 
	 */
	public String getTextureLocation(){
		return "textures/parts/" + definition.systemName + ".png";
	}
	
	@Override
	public void updateProviderSound(SoundInstance sound){
		if(!this.isValid || !vehicle.isValid){
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
