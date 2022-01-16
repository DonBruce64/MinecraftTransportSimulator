package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.vecmath.Vector3d;

import minecrafttransportsimulator.baseclasses.AnimationSwitchbox;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Orientation3d;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.TrailerConnection;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
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
	public final Point3d placementOffset;
	public final boolean disableMirroring;
	
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
	public final Point3d localOffset;
	public final Orientation3d localOrientation;
	private AnimationSwitchbox placementActiveSwitchbox;
	private AnimationSwitchbox internalActiveSwitchbox;
	private AnimationSwitchbox placementMovementSwitchbox;
	private AnimationSwitchbox internalMovementSwitchbox;
		
	public APart(AEntityF_Multipart<?> entityOn, WrapperPlayer placingPlayer, JSONPartDefinition placementDefinition, WrapperNBT data, APart parentPart){
		super(entityOn.world, placingPlayer, data);
		this.boundingBox = new BoundingBox(placementDefinition.pos, position, getWidth()/2D, getHeight()/2D, getWidth()/2D, definition.ground != null ? definition.ground.canFloat : false);
		this.entityOn = entityOn;
		this.vehicleOn = entityOn instanceof EntityVehicleF_Physics ? (EntityVehicleF_Physics) entityOn : null;
		this.placementDefinition = placementDefinition;
		this.placementOffset = placementDefinition.pos;
		
		this.localOffset = placementOffset.copy();
		this.localOrientation = placementDefinition.rot != null ? new Orientation3d(placementDefinition.rot) : new Orientation3d(new Point3d(0, 0, 0));
		
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
		
		//Set initial position and rotation.  This ensures part doesn't "warp" the first tick.
		//Note that this isn't exact, as we can't calculate the exact locals until after the first tick.
		//This is why it does not take into account parent part positions.
		localOrientation.rotatePoint(position.setTo(localOffset)).add(entityOn.position);
		orientation.setTo(entityOn.orientation).multiplyBy(localOrientation);
		prevPosition.setTo(position);
		prevOrientation.setTo(orientation);
		
		//Set mirrored state.
		this.mirrored = ((placementOffset.x < 0 && !placementDefinition.inverseMirroring) || (placementOffset.x >= 0 && placementDefinition.inverseMirroring)) && !disableMirroring;
	}
	
	@Override
	protected void initializeDefinition(){
		super.initializeDefinition();
		if(placementDefinition.animations != null){
			placementMovementSwitchbox = new AnimationSwitchbox(this, placementDefinition.animations);
		}
		if(definition.generic.movementAnimations != null){
			internalMovementSwitchbox = new AnimationSwitchbox(this, definition.generic.movementAnimations);
		}
		if(placementDefinition.activeAnimations != null){
			placementActiveSwitchbox = new AnimationSwitchbox(this, placementDefinition.activeAnimations);
		}
		if(definition.generic.activeAnimations != null){
			internalActiveSwitchbox = new AnimationSwitchbox(this, definition.generic.activeAnimations);
		}
	}
	
	@Override
	public boolean update(){
		if(super.update()){
			//Update active state.
			isActive = placementDefinition.isSubPart ? parentPart.isActive : true;
			if(isActive && placementActiveSwitchbox != null){
				isActive = placementActiveSwitchbox.runSwitchbox(0);
			}
			if(isActive && internalActiveSwitchbox != null){
				isActive = internalActiveSwitchbox.runSwitchbox(0);
			}
			
			//Set initial offsets.
			if(parentPart != null && placementDefinition.isSubPart){
				prevMotion.setTo(parentPart.prevMotion);
				motion.setTo(parentPart.motion);
				position.setTo(parentPart.position);
				orientation.setTo(parentPart.orientation);
				localOffset.setTo(placementOffset);
			}else{
				prevMotion.setTo(entityOn.prevMotion);
				motion.setTo(entityOn.motion);
				position.setTo(entityOn.position);
				orientation.setTo(entityOn.orientation);
				localOffset.setTo(placementOffset);
			}
			
			//Update local position, orientation, scale, and enabled state.
			isInvisible = false;
			scale = placementDefinition.isSubPart && parentPart != null ? parentPart.scale : 1.0F;
			localOffset.multiply(scale);
			localOrientation.setRotation(0);
			
			//Placement movement uses the coords of the thing we are on.
			if(placementMovementSwitchbox != null && definition.systemName.contains("invi")){
				isInvisible = !placementMovementSwitchbox.runSwitchbox(0);
				scale *= placementMovementSwitchbox.animationScale;
				
				javax.vecmath.Point3d helper = new javax.vecmath.Point3d(localOffset.x, localOffset.y, localOffset.z);
				placementMovementSwitchbox.netMatrix.transform(helper);
				localOffset.set(helper.x, helper.y, helper.z);
				
				//Update local orientation to match rotated.
				//localOffset.setTo(placementMovementSwitchbox.animationOffset);
				//localOrientation.multiplyBy(placementMovementSwitchbox.animationOrientation);
			}
			
			//Internal movement uses local coords.
			//First rotate orientation to face rotated state.
			localOrientation.multiplyBy(placementDefinition.rot);
			if(internalMovementSwitchbox != null){
				isInvisible = !internalMovementSwitchbox.runSwitchbox(0) || isInvisible;
				scale *= internalMovementSwitchbox.animationScale;
				//Need to update offset to account for offset found in the animation.
				//This doesn't apply to the local offset, as this is in the part's coodrinate
				//system, not the one that the part is placed on.
				//localOrientation.rotateAndAddTo(internalMovementSwitchbox.animationOffset, localOffset);
				//Update local orientation to match rotated.
				//localOrientation.multiplyBy(internalMovementSwitchbox.animationOrientation);
			}
			
			//Now that locals are set, set globals to reflect them.
			orientation.rotateAndAddTo(localOffset, position);
			orientation.multiplyBy(localOrientation);
			
			//Update bounding box, as scale changes width/height.
			boundingBox.widthRadius = getWidth()/2D;
			boundingBox.heightRadius = getHeight()/2D;
			boundingBox.depthRadius = getWidth()/2D;
			
			//Update post-movement things.
			updatePostMovement();
			return true;
		}else{
			return false;
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
		if(definition.generic.canBeRemovedByHand && !placementDefinition.isPermanent && damage.entityResponsible instanceof WrapperPlayer){
			//Attacked a removable part, remove us to the player's inventory.
			//If the inventory can't fit us, don't remove us.
			WrapperPlayer player = (WrapperPlayer) damage.entityResponsible;
			if(entityOn.locked){
				player.sendPacket(new PacketPlayerChatMessage(player, "interact.failure.vehiclelocked"));
			}else{
				if(player.getInventory().addStack(getItem().getNewStack(save(new WrapperNBT())))){
					disconnectAllConnections();
					entityOn.removePart(this, null);
				}
			}
		}else{
			//Not a removable part, or is an actual attack.
			super.attack(damage);
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
			
			//If the parent part is mirrored, we need to invert our X-position to match.
			if(mirrored){
				correctedPartDef.pos.x -= 2*subPartDef.pos.x;
			}
			
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
	 * This is called during part save/load calls.  Fakes parts are
	 * added to entities, but they aren't saved with the NBT.  Rather, 
	 * they should be re-created in the constructor of the part that added
	 * them in the first place.
	 */
	public boolean isFake(){
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
