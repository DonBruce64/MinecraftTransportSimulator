package minecrafttransportsimulator.vehicles.parts;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import minecrafttransportsimulator.baseclasses.AEntityC_Definable;
import minecrafttransportsimulator.baseclasses.AEntityE_Multipart;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.rendering.components.ARenderEntity;
import minecrafttransportsimulator.rendering.components.DurationDelayClock;
import minecrafttransportsimulator.rendering.instances.AnimationsPart;
import minecrafttransportsimulator.systems.PackParserSystem;

/**This class is the base for all parts and should be extended for any entity-compatible parts.
 * Use {@link AEntityE_Multipart#addPart(APart)} to add parts 
 * and {@link AEntityE_Multipart#removePart(APart, Iterator)} to remove them.
 * You may extend {@link AEntityE_Multipart} to get more functionality with those systems.
 * If you need to keep extra data ensure it is packed into whatever NBT is returned in item form.
 * This NBT will be fed into the constructor when creating this part, so expect it and ONLY look for it there.
 * 
 * @author don_bruce
 */
public abstract class APart extends AEntityC_Definable<JSONPart>{
	private static final Point3d ZERO_POINT = new Point3d();
	private static final AnimationsPart animator = new AnimationsPart();
	
	//JSON properties.
	public final JSONPartDefinition partDefinition;
	public final Point3d placementOffset;
	public final Point3d placementRotation;
	public final boolean disableMirroring;
	
	//Instance properties.
	public final AEntityE_Multipart<?> entityOn;
	/**The parent of this part, if this part is a sub-part of a part or an additional part for an entity.*/
	public final APart parentPart;
	/**Children to this part.  Can be either additional parts or sub-parts.*/
	public final List<APart> childParts = new ArrayList<APart>();
	
	//Runtime variables.
	private final List<DurationDelayClock> clocks = new ArrayList<DurationDelayClock>();
	public final Point3d totalOffset;
	public final Point3d prevTotalOffset;
	public final Point3d totalRotation;
	public final Point3d prevTotalRotation;
	public final Point3d worldPos;
	public final BoundingBox boundingBox;
		
	public APart(AEntityE_Multipart<?> entityOn, JSONPartDefinition packVehicleDef, WrapperNBT data, APart parentPart){
		super(entityOn.world, null, data);
		this.entityOn = entityOn;
		this.placementOffset = packVehicleDef.pos;
		this.totalOffset = placementOffset.copy();
		this.prevTotalOffset = totalOffset.copy();
		this.partDefinition = packVehicleDef;
		this.worldPos = placementOffset.copy().rotateFine(entityOn.angles).add(entityOn.position);
		this.boundingBox = new BoundingBox(placementOffset, worldPos, getWidth()/2D, getHeight()/2D, getWidth()/2D, definition.ground != null ? definition.ground.canFloat : false, false, false, 0);
		this.placementRotation = packVehicleDef.rot != null ? packVehicleDef.rot : new Point3d();
		this.totalRotation = placementRotation.copy();
		this.prevTotalRotation = totalRotation.copy();
		
		//If we are an additional part or sub-part, link ourselves now.
		//If we are a fake part, don't even bother checking.
		if(!isFake() && parentPart != null){
			this.parentPart = parentPart;
			parentPart.childParts.add(this);
			if(packVehicleDef.isSubPart){
				this.disableMirroring = parentPart.disableMirroring || definition.generic.disableMirroring;
			}else{
				this.disableMirroring = definition.generic.disableMirroring;
			}
		}else{
			this.disableMirroring = definition.generic.disableMirroring;
			this.parentPart = null;
		}
		
		//Create movement animation clocks.
		if(partDefinition.animations != null){
			for(JSONAnimationDefinition animation : partDefinition.animations){
				clocks.add(new DurationDelayClock(animation));
			}
		}
	}
	
	/**
	 * This is called during part save/load calls.  Fakes parts are
	 * added to entities, but they aren't saved with the NBT.  Rather, 
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
	 * Called when the entity this part is on sees this part being attacked.
	 * Only called on the server.
	 */
	public void attack(Damage damage){}
	
	/**
	 * This gets called every tick by the entity after it finishes its update loop.
	 * Use this for reactions that this part can take based on its surroundings if need be.
	 * Do NOT remove the part from the entity in this loop.  Instead, set it to invalid.
	 * Removing the part during this loop will earn you a CME.
	 */
	@Override
	public void update(){
		prevTotalOffset.setTo(totalOffset);
		prevTotalRotation.setTo(totalRotation);
		updatePositionAndRotation();
		//If we have a parent part, we need to change our offsets to be relative to it.
		if(parentPart != null){
			//Get parent offset and rotation.  The parent will have been updated already as it has
			//to be placed on the vehicle before us, and as such will be before us in the parts list.
			//Our initial offset needs to be relative to the position of the part on the parent, so 
			//we need to start with that delta.
			totalOffset.subtract(parentPart.placementOffset);
			
			//Rotate our current relative offset by the rotation of the parent to get the correct
			//offset between us and our parent's position in our parent's coordinate system.
			totalOffset.rotateFine(parentPart.totalRotation);
			
			//Add our parent's rotation to our own so we have a cumulative rotation.
			//This has the potential for funny rotations if we're both rotated, as we should
			//apply this rotation about our parent's rotated axis, not our own, but for most situations,
			//it's close enough.
			totalRotation.add(parentPart.totalRotation);
			
			//Now that we have the proper relative offset, add our parent's offset to get our next offset.
			//This is our final offset point.
			totalOffset.add(parentPart.totalOffset);
		}
		
		//Set worldpos to our net offset pos on the entity.
		worldPos.setTo(totalOffset).rotateFine(entityOn.angles).add(entityOn.position);
	}
	
	/**
	 * Updates the position and rotation totals to the current position and rotation, 
	 * as defined by the various animations and offsets defined in the JSON.
	 * This may be extended by parts to modify this behavior.
	 */
	protected void updatePositionAndRotation(){
		boolean inhibitAnimations = false;
		totalOffset.set(0D, 0D, 0D);
		totalRotation.set(0D, 0D, 0D);
		if(!clocks.isEmpty()){
			for(DurationDelayClock clock : clocks){
				JSONAnimationDefinition animation = clock.definition;
				switch(animation.animationType){
					case TRANSLATION :{
						if(!inhibitAnimations){
							//Found translation.  This gets applied in the translation axis direction directly.
							//This axis needs to be rotated by the rollingRotation to ensure it's in the correct spot.
							double variableValue = animator.getAnimatedVariableValue(this, animation, 0, clock, 0);
							Point3d appliedTranslation = animation.axis.copy().normalize().multiply(variableValue);
							totalOffset.add(appliedTranslation.rotateFine(totalRotation));
						}
						break;
					}
					case ROTATION :{
						if(!inhibitAnimations){
							//Found rotation.  Get angles that needs to be applied.
							double variableValue = animator.getAnimatedVariableValue(this, animation, 0, clock, 0);
							Point3d appliedRotation = animation.axis.copy().normalize().multiply(variableValue);
							
							//Check if we need to apply a translation based on this rotation.
							if(!animation.centerPoint.isZero()){
								//Use the center point as a vector we rotate to get the applied offset.
								//We need to take into account the rolling rotation here, as we might have rotated on a prior call.
								totalOffset.add(animation.centerPoint.copy().multiply(-1D).rotateFine(appliedRotation).add(animation.centerPoint).rotateFine(totalRotation));
							}
							
							//Apply rotation.  We need to do this after translation operations to ensure proper offsets.
							totalRotation.add(appliedRotation);
						}
						break;
					}
					case VISIBILITY :{
						//Do nothing.
						break;
					}
					case INHIBITOR :{
						double variableValue = animator.getAnimatedVariableValue(this, animation, 0, clock, 0);
						if(variableValue >= animation.clampMin && variableValue <= animation.clampMax){
							inhibitAnimations = true;
						}
						break;
					}
					case ACTIVATOR :{
						double variableValue = animator.getAnimatedVariableValue(this, animation, 0, clock, 0);
						if(variableValue >= animation.clampMin && variableValue <= animation.clampMax){
							inhibitAnimations = false;
						}
						break;
					}
				}
			}
		}
		
		//Add on the placement offset and rotation  now that we have our dynamic values.
		totalOffset.add(placementOffset);
		totalRotation.add(placementRotation);
	}
	
	/**
	 * Gets the rotation angles for the part as a vector.
	 * This rotation is only for custom rendering operations, and cannot be modified via JSON.
	 * If we have a parent part and this part is on it, use its rotation.
	 */
	public Point3d getRenderingRotation(float partialTicks, boolean animationValue){
		return parentPart != null ? parentPart.getRenderingRotation(partialTicks, animationValue) : ZERO_POINT;
	}
	
	/**
	 * Returns true if this part is in liquid.
	 */
	public boolean isInLiquid(){
		return world.isBlockLiquid(new Point3i(worldPos));
	}
	
	/**
	 * Called when the part is removed from the entity.
	 * Allows for parts to trigger logic that happens when they are removed.
	 * Note that hitboxes are configured to not allow this part to be
	 * removed if it has children, so it may be assumed that no child
	 * parts are present when this action occurs.  Do note that it's possible
	 * this part is a child to another part, so you will need to remove this
	 * part as the child from its parent if is has one.  Also note that you may
	 * NOT remove any other parts in this method.  Doing so will get you a CME.
	 * If you need to remove another part, set it to invalid instead.  This will
	 * have it be removed at the end of the update loop.
	 */
	@Override
	public void remove(){
		super.remove();
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
	 * This is called when removing the part from an entity to return an item.
	 * This is also called when saving this part, so ensure EVERYTHING you need to make this
	 * part back into an part again is packed into the NBT tag that is returned.
	 * This does not include the part offsets, as those are re-calculated every time the part is attached
	 * and are saved separately from the item NBT data in the entity.
	 */
	public WrapperNBT getData(){
		WrapperNBT data = new WrapperNBT();
		int lineNumber = 0;
		for(String textLine : text.values()){
			data.setString("textLine" + lineNumber++, textLine);
		}
		return data;
	}
	
	public float getWidth(){
		return definition.generic != null ? definition.generic.width : 0.75F;
	}
	
	public float getHeight(){
		return definition.generic != null ? definition.generic.height : 0.75F;
	}

	
	//--------------------START OF SOUND AND ANIMATION CODE--------------------
	@Override
	public boolean isLitUp(){
		return entityOn.isLitUp();
	}
	
	@Override
	public float getLightPower(){
		return entityOn.getLightPower();
	}
	
	@Override
	public boolean shouldRenderBeams(){
		return entityOn.shouldRenderBeams();
	}
	
	@Override
	public boolean renderTextLit(){
		return entityOn.renderTextLit();
	}
	
	@Override
	public <AnimationEntity extends AEntityC_Definable<JSONPart>> ARenderEntity<AnimationEntity> getRenderer(){
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public double getAnimationValue(JSONAnimationDefinition animation, double offset, DurationDelayClock clock, float partialTicks){
		return animator.getAnimatedVariableValue(this, animation, offset, clock, partialTicks);
	}
}
