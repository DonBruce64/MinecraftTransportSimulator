package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.TrailerConnection;
import minecrafttransportsimulator.entities.components.AEntityD_Interactable;
import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packloading.JSONParser;
import minecrafttransportsimulator.rendering.components.DurationDelayClock;
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
	private final List<DurationDelayClock> activeClocks = new ArrayList<DurationDelayClock>();
	private final List<DurationDelayClock> movementClocks = new ArrayList<DurationDelayClock>();;
	
	/**Cached pack definition mappings for sub-part packs.  First key is the parent part definition, which links to a map.
	 * This second map is keyed by a part definition, with the value equal to a corrected definition.  This means that
	 * in total, this object contains all sub-packs created on any entity for any part with sub-packs.  This is done as parts with
	 * sub-parts use relative locations, and thus we need to ensure we have the correct position for them on any entity part location.*/
	private final Map<JSONPartDefinition, JSONPartDefinition> subpackMappings = new HashMap<JSONPartDefinition, JSONPartDefinition>();
	public boolean isDisabled;
	public boolean isActive = true;
	public boolean prevActive = true;
	public double prevScale = 1.0;
	public double scale = 1.0;
	public final Point3d localOffset;
	public final Point3d prevLocalOffset;
	public final Point3d localAngles;
		
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
		
		//Set initial position and rotation.
		position.setTo(localOffset).rotateFine(entityOn.angles).add(entityOn.position);
		angles.setTo(localAngles).add(entityOn.angles);
		angles.setTo(placementAngles);
		prevAngles.setTo(angles);
	}
	
	@Override
	protected void initializeAnimations(){
		super.initializeAnimations();
		movementClocks.clear();
		if(definition.generic.movementAnimations != null){
			for(JSONAnimationDefinition animation : definition.generic.movementAnimations){
				movementClocks.add(new DurationDelayClock(animation));
			}
		}
		if(placementDefinition.animations != null){
			for(JSONAnimationDefinition animation : placementDefinition.animations){
				movementClocks.add(new DurationDelayClock(animation));
			}
		}
		activeClocks.clear();
		if(definition.generic.activeAnimations != null){
			for(JSONAnimationDefinition animation : definition.generic.activeAnimations){
				activeClocks.add(new DurationDelayClock(animation));
			}
		}
	}
	
	@Override
	public boolean update(){
		if(super.update()){
			//Update active state.
			prevActive = isActive;
			isActive = placementDefinition.isSubPart ? parentPart.isActive : true;
			if(isActive && !activeClocks.isEmpty()){
				boolean inhibitAnimations = false;
				for(DurationDelayClock clock : activeClocks){
					switch(clock.animation.animationType){
						case VISIBILITY :{
							if(!inhibitAnimations){
								double variableValue = getAnimatedVariableValue(clock, 0);
								if(variableValue < clock.animation.clampMin || variableValue > clock.animation.clampMax){
									isActive = false;
								}
							}
							break;
						}
						case INHIBITOR :{
							if(!inhibitAnimations){
								double variableValue = getAnimatedVariableValue(clock, 0);
								if(variableValue >= clock.animation.clampMin && variableValue <= clock.animation.clampMax){
									inhibitAnimations = true;
								}
							}
							break;
						}
						case ACTIVATOR :{
							if(inhibitAnimations){
								double variableValue = getAnimatedVariableValue(clock, 0);
								if(variableValue >= clock.animation.clampMin && variableValue <= clock.animation.clampMax){
									inhibitAnimations = false;
								}
							}
							break;
						}
						case TRANSLATION :{
							//Do nothing.
							break;
						}
						case ROTATION :{
							//Do nothing.
							break;
						}
						case SCALING :{
							//Do nothing.
							break;
						}
					}
				
					if(!isActive){
						//Don't need to process any further as we can't play.
						break;
					}
				}
			}
			
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
			
			//Update post-movement things.
			updatePostMovement();
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public void attack(Damage damage){
		//Check if we can be removed by this attack.
		if(definition.generic.canBeRemovedByHand && !placementDefinition.isPermanent && damage.entityResponsible instanceof WrapperPlayer){
			//Attacked a removable part, remove us to the player's inventory.
			//If the inventory can't fit us, don't remove us.
			WrapperPlayer player = (WrapperPlayer) damage.entityResponsible;
			if(entityOn.locked){
				player.sendPacket(new PacketPlayerChatMessage(player, "interact.failure.vehiclelocked"));
			}else{
				if(player.getInventory().addItem(getItem(), save(new WrapperNBT()))){
					disconnectAllConnections();
					entityOn.removePart(this, null);
				}
			}
		}
	}
	
	@Override
	public PlayerOwnerState getOwnerState(WrapperPlayer player){
		return entityOn.getOwnerState(player);
	}
	
	@Override
	public double getMass(){
		return definition.generic.mass;
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
	
	@Override
	public void connectTrailer(TrailerConnection connection){
		entityOn.connectTrailer(connection);
	}
	
	@Override
	public void disconnectTrailer(TrailerConnection connection){
		entityOn.disconnectTrailer(connection);
	}
	
	@Override
	public void connectAsTrailer(TrailerConnection connection){
		entityOn.connectAsTrailer(connection);
	}
	
	@Override
	public void disconnectAsTrailer(){
		entityOn.disconnectAsTrailer();
	}
	
	@Override
	public Set<TrailerConnection> getTowingConnections(){
		return entityOn.getTowingConnections();
	}
	
	/**
	 * Updates the passed-in position and angles to the current position and rotation, 
	 * as defined by the various animations and offsets defined in the passed-in JSON.
	 * This is a local offset, and should be used to get the part's position and angles relative
	 * to the parent entity.  If the part should be invisible, given the animations, true is returned.
	 * This can be used to disable both the part and the hitbox, if desired.
	 */
	private boolean updateLocals(){
		boolean inhibitAnimations = false;
		boolean disablePart = false;
		prevScale = scale;
		scale = placementDefinition.isSubPart && parentPart != null ? parentPart.scale : 1.0;
		localOffset.set(0D, 0D, 0D);
		localAngles.set(0D, 0D, 0D);
		if(!movementClocks.isEmpty()){
			for(DurationDelayClock clock : movementClocks){
				switch(clock.animation.animationType){
					case TRANSLATION :{
						if(!inhibitAnimations){
							//Found translation.  This gets applied in the translation axis direction directly.
							//This axis needs to be rotated by the rollingRotation to ensure it's in the correct spot.
							double magnitude = clock.animation.axis.length();
							double variableValue = getAnimatedVariableValue(clock, magnitude, 0);
							Point3d appliedTranslation = clock.animation.axis.copy().normalize().multiply(variableValue);
							localOffset.add(appliedTranslation.rotateFine(localAngles));
						}
						break;
					}
					case ROTATION :{
						if(!inhibitAnimations){
							//Found rotation.  Get angles that needs to be applied.
							double magnitude = clock.animation.axis.length();
							double variableValue = getAnimatedVariableValue(clock, magnitude, 0);
							Point3d appliedRotation = clock.animation.axis.copy().normalize().multiply(variableValue);
							
							//Check if we need to apply a translation based on this rotation.
							if(!clock.animation.centerPoint.isZero()){
								//Use the center point as a vector we rotate to get the applied offset.
								//We need to take into account the rolling rotation here, as we might have rotated on a prior call.
								localOffset.add(clock.animation.centerPoint.copy().multiply(-1D).rotateFine(appliedRotation).add(clock.animation.centerPoint).rotateFine(localAngles));
							}
							
							//Apply rotation.  We need to do this after translation operations to ensure proper offsets.
							localAngles.add(appliedRotation);
						}
						break;
					}
					case SCALING :{
						if(!inhibitAnimations){
							//Found scaling.  This gets applied during rendering, so we don't directly use the value here.
							//Instead, we save it and use it later.
							scale *= getAnimatedVariableValue(clock, clock.animation.axis.length(), 0);
							//Update bounding box, as scale changes width/height.
							boundingBox.widthRadius = getWidth()/2D;
							boundingBox.heightRadius = getHeight()/2D;
							boundingBox.depthRadius = getWidth()/2D;
						}
						break;
					}
					case VISIBILITY :{
						if(!inhibitAnimations){
							double variableValue = getAnimatedVariableValue(clock, 0);
							if(variableValue < clock.animation.clampMin || variableValue > clock.animation.clampMax){
								disablePart = true;
							}
						}
						break;
					}
					case INHIBITOR :{
						if(!inhibitAnimations){
							double variableValue = getAnimatedVariableValue(clock, 0);
							if(variableValue >= clock.animation.clampMin && variableValue <= clock.animation.clampMax){
								inhibitAnimations = true;
							}
						}
						break;
					}
					case ACTIVATOR :{
						if(inhibitAnimations){
							double variableValue = getAnimatedVariableValue(clock, 0);
							if(variableValue >= clock.animation.clampMin && variableValue <= clock.animation.clampMax){
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
		if(placementDefinition.isSubPart && parentPart != null && parentPart.scale != 1){
			localOffset.add(placementOffset.copy().multiply(parentPart.scale));
		}else{
			localOffset.add(placementOffset);
		}
		localAngles.add(placementAngles);
		return disablePart;
	}
	
	/**
	 * Returns a definition with the correct properties for a SubPart.  This is because
	 * subParts inherit some properties from their parent parts.  All created sub-part
	 * packs are cached locally once created, as they need to not create new instances.
	 * If they did, then the lookup relation between them and their spot in the vehicle would
	 * get broken for maps on each reference.
	 */
	public JSONPartDefinition getPackForSubPart(JSONPartDefinition subPartDef){
		if(!subpackMappings.containsKey(subPartDef)){
			//Use GSON to make a deep copy of the JSON-defined pack definition.
			//Set the sub-part flag to ensure we know this is a subPart for rendering operations.
			JSONPartDefinition correctedPartDef = JSONParser.duplicateJSON(subPartDef);
			correctedPartDef.isSubPart = true;
			
			//Now set parent-specific properties.  These pertain to position, rotation, mirroring, and the like.
			//First add the parent pack's position to the sub-pack.
			//We don't add rotation, as we need to stay relative to the parent part, as the parent part will rotate us.
			correctedPartDef.pos.add(placementOffset);
			
			//If the parent part is mirrored, we need to invert our X-position to match.
			if(placementOffset.x < 0 ^ disableMirroring){
				correctedPartDef.pos.x -= 2*subPartDef.pos.x;
			}
			
			//Use the parent's turnsWithSteer and isSpare variables, as that's based on the vehicle, not the part.
			correctedPartDef.turnsWithSteer = placementDefinition.turnsWithSteer;
			correctedPartDef.isSpare= placementDefinition.isSpare;
			
			//Save the corrected pack into the mappings for later use.
	        subpackMappings.put(subPartDef, correctedPartDef);
		}
		return subpackMappings.get(subPartDef);
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
	public boolean shouldRenderBeams(){
		return entityOn.shouldRenderBeams();
	}
	
	@Override
	public boolean renderTextLit(){
		return entityOn.renderTextLit();
	}
	
	@Override
	public double getRawVariableValue(String variable, float partialTicks){
		//If the variable is prefixed with "parent_", then we need to get our parent's value.
		if(variable.startsWith("parent_")){
			return parentPart.getRawVariableValue(variable.substring("parent_".length()), partialTicks);
		}else if(definition.parts != null){
			//Check sub-parts for the part with the specified index.
			int partNumber = getVariableNumber(variable);
			if(partNumber != -1){
				return AEntityE_Multipart.getSpecificPartAnimation(this, variable, partNumber, partialTicks);
			}
		}
		
		//Check for generic part variables.
		switch(variable){
			case("part_present"): return 1;
		}
		
		//No variables, check super variables before doing generic forwarding.
		//We need this here for position-specific values, as some of the
		//super variables care about position, so we can't forward those.
		double value = super.getRawVariableValue(variable, partialTicks);
		if(!Double.isNaN(value)){
			return value;
		}
		
		//We didn't find any part-specific or generic animations.
		//We could, however, be wanting the animations of our parent part, but didn't specify a _parent prefix.
		//If we have a parent part, get it, and try this loop again.
		if(parentPart != null){
			return parentPart.getRawVariableValue(variable, partialTicks);
		}

		//If we are down here, we must have not found a part variable, and don't have a parent part to do default forwarding.
		//This means we might be requesting a variable on the entity this part is placed on.
		//Try to get the parent variable, and return whatever we get, NaN or otherwise.
		return entityOn.getRawVariableValue(variable, partialTicks);
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
