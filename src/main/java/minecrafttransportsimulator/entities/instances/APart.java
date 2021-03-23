package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityD_Interactable;
import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.rendering.components.DurationDelayClock;
import minecrafttransportsimulator.rendering.instances.AnimationsPart;
import minecrafttransportsimulator.rendering.instances.RenderPart;

/**This class is the base for all parts and should be extended for any entity-compatible parts.
 * Use {@link AEntityE_Multipart#addPart(APart, boolean)} to add parts 
 * and {@link AEntityE_Multipart#removePart(APart, Iterator)} to remove them.
 * You may extend {@link AEntityE_Multipart} to get more functionality with those systems.
 * If you need to keep extra data ensure it is packed into whatever NBT is returned in item form.
 * This NBT will be fed into the constructor when creating this part, so expect it and ONLY look for it there.
 * 
 * @author don_bruce
 */
public abstract class APart extends AEntityD_Interactable<JSONPart>{
	private static final Point3d ZERO_POINT = new Point3d();
	private static final AnimationsPart animator = new AnimationsPart();
	private static RenderPart renderer;
	
	//JSON properties.
	public final JSONPartDefinition placementDefinition;
	public final Point3d placementOffset;
	public final Point3d placementAngles;
	public final boolean disableMirroring;
	
	//Instance properties.
	/**The entity this part has been placed on.*/
	public final AEntityE_Multipart<?> entityOn;
	/**The vehicle this part has been placed on, or null if it wasn't placed on a vehicle.*/
	protected final EntityVehicleF_Physics vehicleOn;
	/**The parent of this part, if this part is a sub-part of a part or an additional part for an entity.*/
	public final APart parentPart;
	/**Children to this part.  Can be either additional parts or sub-parts.*/
	public final List<APart> childParts = new ArrayList<APart>();
	
	//Runtime variables.
	private final List<DurationDelayClock> clocks = new ArrayList<DurationDelayClock>();
	public boolean isDisabled;
	public final Point3d localOffset;
	public final Point3d prevLocalOffset;
	public final Point3d localAngles;
	public final BoundingBox boundingBox;
		
	public APart(AEntityE_Multipart<?> entityOn, JSONPartDefinition placementDefinition, WrapperNBT data, APart parentPart){
		super(entityOn.world, data);
		this.entityOn = entityOn;
		this.vehicleOn = entityOn instanceof EntityVehicleF_Physics ? (EntityVehicleF_Physics) entityOn : null;
		this.placementOffset = placementDefinition.pos;
		this.localOffset = placementOffset.copy();
		this.prevLocalOffset = localOffset.copy();
		this.placementDefinition = placementDefinition;
		this.boundingBox = new BoundingBox(placementOffset, position, getWidth()/2D, getHeight()/2D, getWidth()/2D, definition.ground != null ? definition.ground.canFloat : false, false, false, 0);
		this.placementAngles = placementDefinition.rot != null ? placementDefinition.rot : new Point3d();
		this.localAngles = placementAngles.copy();
		
		//If we are an additional part or sub-part, link ourselves now.
		//If we are a fake part, don't even bother checking.
		if(!isFake() && parentPart != null){
			this.parentPart = parentPart;
			parentPart.childParts.add(this);
			if(placementDefinition.isSubPart){
				this.disableMirroring = parentPart.disableMirroring || definition.generic.disableMirroring;
			}else{
				this.disableMirroring = definition.generic.disableMirroring;
			}
		}else{
			this.disableMirroring = definition.generic.disableMirroring;
			this.parentPart = null;
		}
		
		//If we are a fake part, remove collisions, doors, and the like.
		if(isFake()){
			collisionBoxes.clear();
			blockCollisionBoxes.clear();
			doorBoxes.clear();
			interactionBoxes.clear();
		}else{
			interactionBoxes.add(boundingBox);
		}
		
		//Create movement animation clocks.
		createMovementClocks();
		
		//Set initial position and rotation.
		position.setTo(localOffset).rotateFine(entityOn.angles).add(entityOn.position);
		angles.setTo(localAngles).add(entityOn.angles);
		angles.setTo(placementAngles);
		prevAngles.setTo(angles);
	}
	
	/**
	 *  Helper method for creating duration/delay clocks.
	 */
	public void createMovementClocks(){
		clocks.clear();
		if(placementDefinition.animations != null){
			for(JSONAnimationDefinition animation : placementDefinition.animations){
				clocks.add(new DurationDelayClock(animation));
			}
		}
	}
	
	@Override
	public void update(){
		super.update();
		prevMotion.setTo(entityOn.prevMotion);
		motion.setTo(entityOn.motion);
		prevLocalOffset.setTo(localOffset);
		variablesOn.clear();
		variablesOn.addAll(entityOn.variablesOn);
		isDisabled = updateLocals();
		//If we have a parent part, we need to change our offsets to be relative to it.
		if(parentPart != null && placementDefinition.isSubPart){
			//Get parent offset and rotation.  The parent will have been updated already as it has
			//to be placed on the vehicle before us, and as such will be before us in the parts list.
			//Our initial offset needs to be relative to the position of the part on the parent, so 
			//we need to start with that delta.
			localOffset.subtract(parentPart.placementOffset);
			
			//Rotate our current relative offset by the rotation of the parent to get the correct
			//offset between us and our parent's position in our parent's coordinate system.
			localOffset.rotateFine(parentPart.localAngles);
			
			//Add our parent's angles to our own so we have a cumulative rotation.
			//This has the potential for funny rotations if we're both rotated, as we should
			//apply this rotation about our parent's rotated axis, not our own, but for most situations,
			//it's close enough.
			localAngles.add(parentPart.localAngles);
			
			//Now that we have the proper relative offset, add our parent's offset to get our net offset.
			//This is our final offset point.
			localOffset.add(parentPart.localOffset);
		}
		
		//Set position and rotation to our net offset pos on the entity.
		position.setTo(localOffset).rotateFine(entityOn.angles).add(entityOn.position);
		angles.setTo(localAngles).add(entityOn.angles);
	}
	
	@Override
	public void remove(){
		super.remove();
		if(parentPart != null){
			parentPart.childParts.remove(this);
		}
	}
	
	@Override
	public boolean shouldSavePosition(){
		return false;
	}
	
	/**
	 * Updates the passed-in position and angles to the current position and rotation, 
	 * as defined by the various animations and offsets defined in the passed-in JSON.
	 * This is a local offset, and should be used to get the part's position and angles relative
	 * to the parent entity.  If the part should be invisible, given the animations, true is returned.
	 * This can be used to disable both the part and the hitbox, if desired.
	 */
	protected boolean updateLocals(){
		boolean inhibitAnimations = false;
		boolean disablePart = false;
		localOffset.set(0D, 0D, 0D);
		localAngles.set(0D, 0D, 0D);
		if(!clocks.isEmpty()){
			for(DurationDelayClock clock : clocks){
				JSONAnimationDefinition animation = clock.animation;
				switch(animation.animationType){
					case TRANSLATION :{
						if(!inhibitAnimations){
							//Found translation.  This gets applied in the translation axis direction directly.
							//This axis needs to be rotated by the rollingRotation to ensure it's in the correct spot.
							double variableValue = animator.getAnimatedVariableValue(this, animation, 0, clock, 0);
							Point3d appliedTranslation = animation.axis.copy().normalize().multiply(variableValue);
							localOffset.add(appliedTranslation.rotateFine(localAngles));
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
								localOffset.add(animation.centerPoint.copy().multiply(-1D).rotateFine(appliedRotation).add(animation.centerPoint).rotateFine(localAngles));
							}
							
							//Apply rotation.  We need to do this after translation operations to ensure proper offsets.
							localAngles.add(appliedRotation);
						}
						break;
					}
					case SCALING :{
						//Do nothing.
						break;
					}
					case VISIBILITY :{
						if(!inhibitAnimations){
							double variableValue = animator.getAnimatedVariableValue(this, animation, 0, clock, 0);
							if(variableValue < animation.clampMin || variableValue > animation.clampMax){
								disablePart = true;
							}
						}
						break;
					}
					case INHIBITOR :{
						if(!inhibitAnimations){
							double variableValue = animator.getAnimatedVariableValue(this, animation, 0, clock, 0);
							if(variableValue >= animation.clampMin && variableValue <= animation.clampMax){
								inhibitAnimations = true;
							}
						}
						break;
					}
					case ACTIVATOR :{
						if(inhibitAnimations){
							double variableValue = animator.getAnimatedVariableValue(this, animation, 0, clock, 0);
							if(variableValue >= animation.clampMin && variableValue <= animation.clampMax){
								inhibitAnimations = false;
							}
						}
						break;
					}
				}
				if(disablePart){
					break;
				}
			}
		}
		
		//Add on the placement offset and angles now that we have our dynamic values.
		localOffset.add(placementOffset);
		localAngles.add(placementAngles);
		return disablePart;
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
		return world.isBlockLiquid(position);
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
	
	public float getWidth(){
		return definition.generic.width != 0 ? definition.generic.width : 0.75F;
	}
	
	public float getHeight(){
		return definition.generic.height != 0 ? definition.generic.height : 0.75F;
	}

	
	//--------------------START OF SOUND AND ANIMATION CODE--------------------
	@Override
	public float getLightProvided(){
		return entityOn.getLightProvided();
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
	@SuppressWarnings("unchecked")
	public AnimationsPart getAnimator(){
		return animator;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public RenderPart getRenderer(){
		if(renderer == null){
			renderer = new RenderPart();
		}
		return renderer;
	}
}
