package minecrafttransportsimulator.vehicles.parts;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.rendering.components.DurationDelayClock;
import minecrafttransportsimulator.rendering.components.IAnimationProvider;
import minecrafttransportsimulator.rendering.components.ITextProvider;
import minecrafttransportsimulator.rendering.instances.AnimationsPart;
import minecrafttransportsimulator.sound.ISoundProviderComplex;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

/**This class is the base for all parts and should be extended for any vehicle-compatible parts.
 * Use {@link EntityVehicleF_Physics#addPart(APart)} to add parts 
 * and {@link EntityVehicleF_Physics#removePart(APart, Iterator)} to remove them.
 * You may extend {@link EntityVehicleE_Powered} to get more functionality with those systems.
 * If you need to keep extra data ensure it is packed into whatever NBT is returned in item form.
 * This NBT will be fed into the constructor when creating this part, so expect it and ONLY look for it there.
 * 
 * @author don_bruce
 */
public abstract class APart implements ISoundProviderComplex, IAnimationProvider, ITextProvider{
	private static final Point3d ZERO_POINT = new Point3d(0, 0, 0);
	private static final AnimationsPart animator = new AnimationsPart();
	
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
	/**Map containing text objects and their current associated text.**/
	public final Map<JSONText, String> text = new LinkedHashMap<JSONText, String>();
	
	//Runtime variables.
	private final List<DurationDelayClock> clocks = new ArrayList<DurationDelayClock>();
	public final Point3d totalOffset;
	public final Point3d totalRotation;
	public final Point3d worldPos;
	public final BoundingBox boundingBox;
	public String currentSubName;
	public boolean isValid = true;
		
	public APart(EntityVehicleF_Physics vehicle, VehiclePart packVehicleDef, ItemPart item, WrapperNBT data, APart parentPart){
		this.vehicle = vehicle;
		this.placementOffset = packVehicleDef.pos;
		this.totalOffset = placementOffset.copy();
		this.definition = item.definition;;
		this.vehicleDefinition = packVehicleDef;
		this.worldPos = placementOffset.copy().rotateFine(vehicle.angles).add(vehicle.position);
		this.boundingBox = new BoundingBox(placementOffset, worldPos, getWidth()/2D, getHeight()/2D, getWidth()/2D, definition.ground != null ? definition.ground.canFloat : false, false, false, 0);
		this.placementRotation = packVehicleDef.rot != null ? packVehicleDef.rot : new Point3d(0, 0, 0);
		this.totalRotation = placementRotation.copy();
		this.currentSubName = item.subName;
		this.isValid = true;
		
		//Load text.
		if(definition.rendering != null && definition.rendering.textObjects != null){
			for(int i=0; i<definition.rendering.textObjects.size(); ++i){
				text.put(definition.rendering.textObjects.get(i), data.getString("textLine" + i));
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
		
		//Create movement animation clocks.
		if(vehicleDefinition.animations != null){
			for(JSONAnimationDefinition animation : vehicleDefinition.animations){
				clocks.add(new DurationDelayClock(animation));
			}
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
	 * Only called on the server.
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
			
			//Now that we have the proper relative offset, add our parent's placement and position offsets.
			//This is our final offset point.
			totalOffset.add(parentPart.placementOffset).add(parentPart.getPositionOffset(0));
			
			//Also add our own rotation to our cumulative rotation we got from our parent.
			totalRotation.add(getPositionRotation(0)).add(placementRotation);
		}else{
			totalOffset.setTo(getPositionOffset(0)).add(placementOffset);
			totalRotation.setTo(getPositionRotation(0)).add(placementRotation);
		}
		worldPos.setTo(totalOffset).rotateFine(vehicle.angles).add(vehicle.position);
	}
	
	/**
	 * Gets the movement position offset for the part as a vector.
	 * This offset is an addition to the main placement offset defined by the JSON.
	 */
	public final Point3d getPositionOffset(float partialTicks){
		boolean inhibitAnimations = false;
		Point3d rollingOffset = new Point3d(0D, 0D, 0D);
		if(!clocks.isEmpty()){
			Point3d rollingRotation = new Point3d(0D, 0D, 0D);
			for(DurationDelayClock clock : clocks){
				JSONAnimationDefinition animation = clock.definition;
				if(animation.animationType.equals("inhibitor")){
					double variableValue = animator.getAnimatedVariableValue(this, animation, 0, clock, partialTicks);
					if(variableValue >= animation.clampMin && variableValue <= animation.clampMax){
						inhibitAnimations = true;
					}
				}else if(animation.animationType.equals("activator")){
					double variableValue = animator.getAnimatedVariableValue(this, animation, 0, clock, partialTicks);
					if(variableValue >= animation.clampMin && variableValue <= animation.clampMax){
						inhibitAnimations = false;
					}
				}else if(!inhibitAnimations){
					if(animation.animationType.equals("rotation")){
						//Found rotation.  Get angles that needs to be applied.
						double variableValue = animator.getAnimatedVariableValue(this, animation, 0, clock, partialTicks);
						Point3d appliedRotation = animation.axis.copy().multiply(variableValue);
						
						//Check if we need to apply a translation based on this rotation.
						if(!animation.centerPoint.isZero()){
							//Use the center point as a vector we rotate to get the applied offset.
							//We need to take into account the rolling rotation here, as we might have rotated on a prior call.
							rollingOffset.add(animation.centerPoint.copy().multiply(-1D).rotateFine(appliedRotation).add(animation.centerPoint).rotateFine(rollingRotation));
						}
						
						//Apply rotation.
						rollingRotation.add(appliedRotation);
					}else if(animation.animationType.equals("translation")){
						//Found translation.  This gets applied in the translation axis direction directly.
						//This axis needs to be rotated by the rollingRotation to ensure it's in the correct spot.
						double variableValue = animator.getAnimatedVariableValue(this, animation, 0, clock, partialTicks);
						Point3d appliedTranslation = animation.axis.copy().multiply(variableValue);
						rollingOffset.add(appliedTranslation.rotateFine(rollingRotation));
					}
				}
			}
		}
		
		//Return the net offset from rotation and translation.
		return rollingOffset;
	}
	
	/**
	 * Gets the rotation angles for the part as a vector.
	 * This rotation is used to rotate the part prior to translation.
	 * It may be used for stacked rotations, and should return the final
	 * rotation angles for all operations.
	 */
	public final Point3d getPositionRotation(float partialTicks){
		boolean inhibitAnimations = false;
		Point3d rollingRotation = new Point3d(0D, 0D, 0D);
		if(!clocks.isEmpty()){
			for(DurationDelayClock clock : clocks){
				JSONAnimationDefinition animation = clock.definition;
				if(animation.animationType.equals("inhibitor")){
					double variableValue = animator.getAnimatedVariableValue(this, animation, 0, clock, partialTicks);
					if(variableValue >= animation.clampMin && variableValue <= animation.clampMax){
						inhibitAnimations = true;
					}
				}else if(animation.animationType.equals("activator")){
					double variableValue = animator.getAnimatedVariableValue(this, animation, 0, clock, partialTicks);
					if(variableValue >= animation.clampMin && variableValue <= animation.clampMax){
						inhibitAnimations = false;
					}
				}
				if(!inhibitAnimations && animation.animationType.equals("rotation")){
					double variableValue = animator.getAnimatedVariableValue(this, animation, 0, clock, partialTicks);
					rollingRotation.add(animation.axis.x*variableValue, animation.axis.y*variableValue, animation.axis.z*variableValue);
				}
			}
		}
		return rollingRotation;
	}
	
	/**
	 * Gets the rotation angles for the part as a vector.
	 * This rotation is only for custom rendering operations, and cannot be modified via JSON.
	 */
	public Point3d getRenderingRotation(float partialTicks){
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
	public ItemPart getItem(){
		ItemPart item = PackParserSystem.getItem(definition.packID, definition.systemName, currentSubName);
		return item;
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
			for(int i=0; i<definition.rendering.textObjects.size(); ++i){
				data.setString("textLine" + i, text.get(definition.rendering.textObjects.get(i)));
			}
		}
		return data;
	}
	
	public abstract float getWidth();
	
	public abstract float getHeight();
	

	
	//--------------------START OF SOUND AND ANIMATION CODE--------------------
	@Override
	public void updateProviderSound(SoundInstance sound){
		if(!this.isValid || !vehicle.isValid){
			sound.stop();
		}
	}
	
	@Override
	public void startSounds(){}
	
	@Override
    public Point3d getProviderPosition(){
		return worldPos;
	}
    
	@Override
    public Point3d getProviderVelocity(){
		return vehicle.getProviderVelocity();
	}
	
	@Override
    public WrapperWorld getProviderWorld(){
		return vehicle.getProviderWorld();
	}
	
	@Override
    public AnimationsPart getAnimationSystem(){
		return animator;
	}
	
	@Override
	public float getLightPower(){
		return vehicle.getLightPower();
	}
	
	@Override
	public Set<String> getActiveVariables(){
		return vehicle.getActiveVariables();
	}
	
	@Override
	public Map<JSONText, String> getText(){
		return text;
	}
	
	@Override
	public String getSecondaryTextColor(){
		return vehicle.getSecondaryTextColor();
	}
	
	@Override
	public boolean renderTextLit(){
		return vehicle.renderTextLit();
	}
}
