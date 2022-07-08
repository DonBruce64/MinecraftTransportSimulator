package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.AnimationSwitchbox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage.LanguageEntry;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packloading.JSONParser;
import minecrafttransportsimulator.rendering.instances.RenderPart;

/**This class is the base for all parts and should be extended for any entity-compatible parts.
 * Use {@link AEntityF_Multipart#addPart(APart, boolean)} to add parts 
 * and {@link AEntityF_Multipart#removePart(APart, Iterator)} to remove them.
 * You may extend {@link AEntityF_Multipart} to get more functionality with those systems.
 * If you need to keep extra data ensure it is packed into whatever NBT is returned in item form.
 * This NBT will be fed into the constructor when creating this part, so expect it and ONLY look for it there.
 * 
 * @author don_bruce
 */
public abstract class APart extends AEntityE_Interactable<JSONPart>{
	private static RenderPart renderer;
	
	//JSON properties.
	public final JSONPartDefinition placementDefinition;
	public final Point3D placementOffset;
	
	//Instance properties.
	/**The entity this part has been placed on.*/
	public final AEntityF_Multipart<?> entityOn;
	/**The vehicle this part has been placed on, or null if it wasn't placed on a vehicle.*/
	public final EntityVehicleF_Physics vehicleOn;
	/**The parent of this part, if this part is a sub-part of a part or an additional part for an entity.*/
	public final APart parentPart;
	/**Children to this part.  Can be either additional parts or sub-parts.*/
	public final List<APart> childParts = new ArrayList<APart>();
	
	/**Cached pack definition mappings for sub-part packs.  First key is the parent part definition, which links to a map.
	 * This second map is keyed by a part definition, with the value equal to a corrected definition.  This means that
	 * in total, this object contains all sub-packs created on any entity for any part with sub-packs.  This is done as parts with
	 * sub-parts use relative locations, and thus we need to ensure we have the correct position for them on any entity part location.*/
	private final Map<JSONPartDefinition, JSONPartDefinition> subpackMappings = new HashMap<JSONPartDefinition, JSONPartDefinition>();
	public boolean isInvisible = false;
	public boolean isActive = true;
	public boolean isMirrored;
	public final Point3D localOffset;
	public final RotationMatrix localOrientation;
	public final RotationMatrix zeroReferenceOrientation;
	public final RotationMatrix prevZeroReferenceOrientation;
	private AnimationSwitchbox placementActiveSwitchbox;
	private AnimationSwitchbox internalActiveSwitchbox;
	private AnimationSwitchbox placementMovementSwitchbox;
	private AnimationSwitchbox internalMovementSwitchbox;
		
	public APart(AEntityF_Multipart<?> entityOn, IWrapperPlayer placingPlayer, JSONPartDefinition placementDefinition, IWrapperNBT data, APart parentPart){
		super(entityOn.world, placingPlayer, data);
		this.entityOn = entityOn;
		this.vehicleOn = entityOn instanceof EntityVehicleF_Physics ? (EntityVehicleF_Physics) entityOn : null;
		this.placementDefinition = placementDefinition;
		this.placementOffset = placementDefinition.pos;
		
		this.localOffset = placementOffset.copy();
		this.localOrientation = new RotationMatrix();
		this.zeroReferenceOrientation = new RotationMatrix();
		this.prevZeroReferenceOrientation = new RotationMatrix();
		
		//If we are an additional part or sub-part, link ourselves now.
		//If we are a fake part, don't even bother checking.
		if(!isFake() && parentPart != null){
			this.parentPart = parentPart;
			parentPart.childParts.add(this);
		}else{
			this.parentPart = null;
		}
		
		//Set initial position and rotation.  This ensures part doesn't "warp" the first tick.
		//Note that this isn't exact, as we can't calculate the exact locals until after the first tick.
		//This is why it does not take into account parent part positions.
		position.set(localOffset).add(entityOn.position);
		prevPosition.set(position);
		orientation.set(entityOn.orientation);
		prevOrientation.set(orientation);
		
		//Set mirrored state.
		this.isMirrored = (placementOffset.x < 0 && !placementDefinition.inverseMirroring) || (placementOffset.x >= 0 && placementDefinition.inverseMirroring);
	}
	
	@Override
	protected void initializeDefinition(){
		super.initializeDefinition();
		AEntityD_Definable<?> entityPlacedOn = parentPart != null && placementDefinition.isSubPart ? parentPart : entityOn;
		if(placementDefinition.animations != null || placementDefinition.applyAfter != null){
			List<JSONAnimationDefinition> animations = new ArrayList<JSONAnimationDefinition>();
			if(placementDefinition.animations != null){
				animations.addAll(placementDefinition.animations);
			}
			placementMovementSwitchbox = new AnimationSwitchbox(entityPlacedOn, animations, placementDefinition.applyAfter);
		}
		if(definition.generic.movementAnimations != null){
			internalMovementSwitchbox = new AnimationSwitchbox(this, definition.generic.movementAnimations, null);
		}
		if(placementDefinition.activeAnimations != null){
			placementActiveSwitchbox = new AnimationSwitchbox(entityPlacedOn, placementDefinition.activeAnimations, null);
		}
		if(definition.generic.activeAnimations != null){
			internalActiveSwitchbox = new AnimationSwitchbox(this, definition.generic.activeAnimations, null);
		}
	}
	
	@Override
	public void update(){
		super.update();
		//Update active state.
		isActive = placementDefinition.isSubPart ? parentPart.isActive : true;
		if(isActive && placementActiveSwitchbox != null){
			isActive = placementActiveSwitchbox.runSwitchbox(0, false);
		}
		if(isActive && internalActiveSwitchbox != null){
			isActive = internalActiveSwitchbox.runSwitchbox(0, false);
		}
		
		//Set initial offsets.
		if(parentPart != null && placementDefinition.isSubPart){
			motion.set(parentPart.motion);
			position.set(parentPart.position);
			orientation.set(parentPart.orientation);
			localOffset.set(placementOffset).subtract(parentPart.placementOffset);
		}else{
			motion.set(entityOn.motion);
			position.set(entityOn.position);
			orientation.set(entityOn.orientation);
			localOffset.set(placementOffset);
		}
		
		//Update zero-reference.
		prevZeroReferenceOrientation.set(zeroReferenceOrientation);
		zeroReferenceOrientation.set(entityOn.orientation);
		if(placementDefinition.rot != null){
			zeroReferenceOrientation.multiply(placementDefinition.rot);
		}
		
		//Update local position, orientation, scale, and enabled state.
		isInvisible = false;
		scale.set(placementDefinition.isSubPart && parentPart != null ? parentPart.scale : entityOn.scale);
		localOrientation.setToZero();
        
        //Internal movement uses local coords.
        //First rotate orientation to face rotated state.
        if(placementDefinition.rot != null){
            localOrientation.set(placementDefinition.rot);
        }
        //Also apply part scale, so everything stays local.
        //We will still have to multiply the translation by this scale though.
        if(placementDefinition.partScale != null){
            scale.multiply(placementDefinition.partScale);
        }
        if(internalMovementSwitchbox != null){
            isInvisible = !internalMovementSwitchbox.runSwitchbox(0, false) || isInvisible;
            //Offset here is local and just needs translation, as it's
            //assuming that we are the origin.
            localOffset.add(internalMovementSwitchbox.translation.multiply(scale).rotate(localOrientation));
            localOrientation.multiply(internalMovementSwitchbox.rotation);
        }
		
		//Placement movement uses the coords of the thing we are on,
        //so we transform our points by the matrix resultant.
		if(placementMovementSwitchbox != null){
			isInvisible = !placementMovementSwitchbox.runSwitchbox(0, false);
			//Offset needs to move according to full transform.
			//This is because these coords are from what we are on.
			//Orientation just needs to update according to new rotation.
			localOffset.transform(placementMovementSwitchbox.netMatrix);
			localOrientation.multiply(placementMovementSwitchbox.rotation);
		}
		//Offset now needs to be multiplied by the scale, as that's the scale of what we are on.
		//This ensures that we're offset relative the proper amount.
		localOffset.multiply(scale);
		
		//Now that locals are set, set globals to reflect them.
		Point3D localPositionDelta = new Point3D().set(localOffset).rotate(orientation);
		position.add(localPositionDelta);
		orientation.multiply(localOrientation);
		
		//Update bounding box, as scale changes width/height.
		boundingBox.widthRadius = getWidth()/2D*scale.x;
		boundingBox.heightRadius = getHeight()/2D*scale.y;
		boundingBox.depthRadius = getWidth()/2D*scale.z;
		
		//Add-back parent offset to our locals if we have one.
		//We don't use this value in any of our interim calculations as we do everything relative to the parent.
		//However, external calls expect this to be relative to the entity we are on, which is NOT our parent.
		if(parentPart != null && placementDefinition.isSubPart){
			localOffset.add(parentPart.localOffset);
		}
	}
	
	@Override
	protected void updateCollisionBoxes(){
		//Add collision if we aren't a fake part.
		if(!isFake()){
			super.updateCollisionBoxes();
			interactionBoxes.add(boundingBox);
		}
	}
	
	@Override
	public void attack(Damage damage){
		//Check if we can be removed by this attack.
		if(!placementDefinition.isPermanent && definition.generic.canBeRemovedByHand && damage.isHand){
			//Attacked a removable part, remove us to the player's inventory.
			//If the inventory can't fit us, don't remove us.
			IWrapperPlayer player = (IWrapperPlayer) damage.entityResponsible;
			if(entityOn.locked){
				player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_VEHICLE_LOCKED));
			}else{
				if(player.getInventory().addStack(getItem().getNewStack(save(InterfaceManager.coreInterface.getNewNBTWrapper())))){
					entityOn.removePart(this, null);
				}
			}
		}else{
			//Not a removable part, or is an actual attack.
			super.attack(damage);
		}
	}
	
	@Override
	public PlayerOwnerState getOwnerState(IWrapperPlayer player){
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
			correctedPartDef.pos.add(placementDefinition.pos);
			
			//Use the parent's turnsWithSteer, mirroring, and isSpare variables, as that's based on the vehicle, not the part.
			correctedPartDef.turnsWithSteer = placementDefinition.turnsWithSteer;
			correctedPartDef.inverseMirroring = placementDefinition.inverseMirroring;
			correctedPartDef.isSpare = placementDefinition.isSpare;
			
			//Save the corrected pack into the mappings for later use.
	        subpackMappings.put(subPartDef, correctedPartDef);
		}
		return subpackMappings.get(subPartDef);
	}
	
	
	
	/**
	 * Returns true if this part is in liquid.
	 */
	public boolean isInLiquid(){
		return world.isBlockLiquid(position);
	}
	
	/**
	 * Checks if this part can be removed with a wrench.  If so, then null is returned.
	 * If not, a {@link LanguageEntry} is returned with the message of why it cannot be.
	 * 
	 */
	public LanguageEntry checkForRemoval(){
		return null;
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
	
	public double getWidth(){
		return definition.generic.width != 0 ? definition.generic.width : 0.75F;
	}
	
	public double getHeight(){
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
	public String getTexture(){
		return definition.generic.useVehicleTexture ? entityOn.getTexture() : super.getTexture();
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
				return AEntityF_Multipart.getSpecificPartAnimation(this, variable, partNumber, partialTicks);
			}
		}
		
		//Check for generic part variables.
		switch(variable){
			case("part_present"): return 1;
			case("part_ismirrored"): return isMirrored ? 1 : 0;
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
