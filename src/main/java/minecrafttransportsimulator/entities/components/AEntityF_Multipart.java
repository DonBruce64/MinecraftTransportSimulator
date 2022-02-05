package minecrafttransportsimulator.entities.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.jsondefs.AJSONPartProvider;
import minecrafttransportsimulator.jsondefs.JSONItem.ItemComponentType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.InterfacePacket;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.instances.PacketPartChange;
import minecrafttransportsimulator.rendering.components.RenderableObject;
import minecrafttransportsimulator.systems.PackParserSystem;

/**Base class for multipart entities.  These entities hold other, part-based entities.  These part
 * entities may be added or removed from this entity based on the implementation, but assurances
 * are made with how they are stored and how they are accessed.
 * 
 * @author don_bruce
 */
public abstract class AEntityF_Multipart<JSONDefinition extends AJSONPartProvider> extends AEntityE_Interactable<JSONDefinition>{
	
	/**This list contains all parts this entity has.  Do NOT directly modify this list.  Instead,
	 * call {@link #addPart}, {@link #addPartFromItem}, or {@link #removePart} to ensure all sub-classed
	 * operations are performed.  Note that if you are iterating over this list when you call one of those
	 * methods, and you don't pass the method an iterator instance, you will get a CME!.
	 */
	public final List<APart> parts = new ArrayList<APart>();
	
	/**This map is similar to {@link #parts}, except it's keyed by the part item.  It serves as a way
	 * to obtain all parts of a specific type on this entity in cases where such information is needed.
	 * Note that the CME/iterator rules do not apply to this map as it's not used for iterative operations.
	 */
	public final HashMap<AItemPart, List<APart>> partsByItem = new LinkedHashMap<AItemPart, List<APart>>();
	
	/**A companion map to {@link #partsByItem}, except this map indicates the last "primary" part for a given
	 * part item, with said part being a value in the list that has the same key as this map entry.
	 * Used mostly for guns, but may be extended for other uses in the future.  If there is no primary part,
	 * then there will not be a key/value mapping for a given item.
	 */
	public final HashMap<AItemPart, APart> lastPrimaryPart = new LinkedHashMap<AItemPart, APart>();
	
	/**List for parts loaded from NBT.  We can't add these parts on construction as we'd error out
	 * due to the potential of various sub-class variables not being ready at construction time.  To compensate, 
	 * we add the parts we wish to add to this list.  Post-construction these will be added to this entity, preventing NPEs.
	 * If you want to add parts to the entity in part constructors, they MUST be in this list.
	 */
	public final List<APart> partsFromNBT = new ArrayList<APart>();
	
	/**List of block collision boxes, with all part block collision boxes included.**/
	public final List<BoundingBox> allBlockCollisionBoxes = new ArrayList<BoundingBox>();
	
	/**List of entity collision boxes, with all part collision boxes included.**/
	public final List<BoundingBox> allEntityCollisionBoxes = new ArrayList<BoundingBox>();
	
	/**List of interaction boxes, plus all part boxes included.**/
	public final List<BoundingBox> allInteractionBoxes = new ArrayList<BoundingBox>();
	
	/**Map of part slot boxes.  Key is the box, value is the definition for that slot.**/
	public final Map<BoundingBox, JSONPartDefinition> allPartSlotBoxes = new HashMap<BoundingBox, JSONPartDefinition>();
	
	/**Map of active part slot boxes.  Contains {@link #allPartSlotBoxes}, though may not contain all of them due to them not being active.**/
	public final Map<BoundingBox, JSONPartDefinition> activePartSlotBoxes = new HashMap<BoundingBox, JSONPartDefinition>();
	
	/**Map of part slot renderables.  Contains {@link #allPartSlotBoxes} boxes.  Rendering is left up to calling functions.**/
	public final Map<BoundingBox, RenderableObject> allPartSlotRenderables = new HashMap<BoundingBox, RenderableObject>();
	
	//Constants
	private final float PART_SLOT_HITBOX_WIDTH = 0.75F;
	private final float PART_SLOT_HITBOX_HEIGHT = 2.25F;
	
	public AEntityF_Multipart(WrapperWorld world, WrapperPlayer placingPlayer, WrapperNBT data){
		super(world, placingPlayer, data);
		//Add parts.
		//Also Replace ride-able locations with seat locations.
		//This ensures we use the proper location for mapping operations.
		for(int i=0; i<data.getInteger("totalParts"); ++i){
			//Use a try-catch for parts in case they've changed since this entity was last placed.
			//Don't want crashes due to pack updates.
			try{
				WrapperNBT partData = data.getData("part_" + i);
				AItemPart partItem = PackParserSystem.getItem(partData.getString("packID"), partData.getString("systemName"), partData.getString("subName"));
				Point3d partOffset = partData.getPoint3d("offset");
				addPartFromItem(partItem, placingPlayer, partData, partOffset, true);
			}catch(Exception e){
				InterfaceCore.logError("Could not load part from NBT.  Did you un-install a pack?");
			}
		}
		
		//Create the initial boxes and slots.
		recalculatePartSlots();
	}
	
	@Override
	protected void initializeDefinition(){
    	super.initializeDefinition();
    	//Reset any parts on us.
    	//Don't reset sub-parts though, as they don't use the JSONs on us for movement.
    	for(APart part : parts){
    		if(!part.placementDefinition.isSubPart){
    			//Find the actual definition in the JSON and get the new animations to use.
    			for(JSONPartDefinition packDef : definition.parts){
    				if(packDef.pos.equals(part.placementDefinition.pos)){
    					part.placementDefinition.animations = packDef.animations;
    					part.animationsInitialized = false;
    					break;
    				}
    			}
    		}
    	}
    }
	
	@Override
	public boolean update(){
		if(super.update()){
			world.beginProfiling("EntityF_Level", true);
			
			//If we have any NBT parts, add them now.
			if(!partsFromNBT.isEmpty()){
				for(APart part : partsFromNBT){
					addPart(part, false);
				}
				partsFromNBT.clear();
			}
			
			//Update part slot box positions.
			world.beginProfiling("PartSlotPositions", true);
			for(BoundingBox box : allPartSlotBoxes.keySet()){
				JSONPartDefinition packVehicleDef = allPartSlotBoxes.get(box);
				boolean updatedToSubPart = false;
				for(APart part : parts){
					if(part.definition.parts != null){
						for(JSONPartDefinition subPartDef : part.definition.parts){
							if(packVehicleDef.equals(part.getPackForSubPart(subPartDef))){
								//Need to find the delta between our 0-degree position and our current position.
								Point3d delta = subPartDef.pos.copy().rotateFine(part.localAngles).subtract(subPartDef.pos);
								box.updateToEntity(this, delta);
								updatedToSubPart = true;
								break;
							}
						}
					}
				}
				if(!updatedToSubPart){
					box.updateToEntity(this, null);
				}
			}
			
			//Populate active part slot list.
			//Only do this on clients; servers reference the main list to handle clicks.
			//Boxes added on clients depend on what the player is holding.
			//We add these before part boxes so the player can click them before clicking a part.
			if(world.isClient()){
				world.beginProfiling("PartSlotActives", false);
				activePartSlotBoxes.clear();
				WrapperPlayer player = InterfaceClient.getClientPlayer();
				AItemBase heldItem = player.getHeldItem();
				if(heldItem instanceof AItemPart){
					for(Entry<BoundingBox, JSONPartDefinition> partSlotBoxEntry : allPartSlotBoxes.entrySet()){
						AItemPart heldPart = (AItemPart) heldItem;
						//Does the part held match this packPart?
						if(heldPart.isPartValidForPackDef(partSlotBoxEntry.getValue(), subName, false)){
							//Are there any doors blocking us from clicking this part?
							if(!areVariablesBlocking(partSlotBoxEntry.getValue(), player)){
								//Part matches.  Add the box.  Set the box bounds to the generic box, or the
								//special bounds of the generic part if we're holding one.
								BoundingBox box = partSlotBoxEntry.getKey();
								box.widthRadius = heldPart.definition.generic.width != 0 ? heldPart.definition.generic.width/2D : PART_SLOT_HITBOX_WIDTH/2D;
								box.heightRadius = heldPart.definition.generic.height != 0 ? heldPart.definition.generic.height/2D : PART_SLOT_HITBOX_HEIGHT/2D;
								box.depthRadius = heldPart.definition.generic.width != 0 ? heldPart.definition.generic.width/2D : PART_SLOT_HITBOX_WIDTH/2D;
								activePartSlotBoxes.put(partSlotBoxEntry.getKey(), partSlotBoxEntry.getValue());
							}
						}
					}
				}
			}
			
			world.endProfiling();
			world.endProfiling();
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public double getMass(){
		//Return our mass, plus our parts.
		double currentMass = super.getMass();
		for(APart part : parts){
			currentMass += part.getMass();
		}
		return currentMass;
	}
	
	@Override
	public boolean addRider(WrapperEntity rider, Point3d riderLocation){
		//Auto-close doors for the rider in the seat they are going in, if such doors exist.
		if(super.addRider(rider, riderLocation)){
			PartSeat seat = (PartSeat) getPartAtLocation(locationRiderMap.inverse().get(rider));
			if(seat != null && seat.placementDefinition.linkedVariables != null){
				for(String variable : seat.placementDefinition.linkedVariables){
					setVariable(variable, 0);
				}
			}
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public void removeRider(WrapperEntity rider, Iterator<WrapperEntity> iterator){
		//Auto-open doors for the rider in the seat they were in, if such doors exist.
		PartSeat seat = (PartSeat) getPartAtLocation(locationRiderMap.inverse().get(rider));
		if(seat != null && seat.placementDefinition.linkedVariables != null){
			for(String variable : seat.placementDefinition.linkedVariables){
				setVariable(variable, 1);
			}
		}
		super.removeRider(rider, iterator);
	}
	
	@Override
	public void attack(Damage damage){
		//If the bounding box attacked corresponds to a part, forward the attack to that part for calculation.
		//Otherwise, we allow ourselves to be attacked.
		APart part = getPartWithBox(damage.box);
		if(part != null && part.isValid){
			part.attack(damage);
		}else{
			super.attack(damage);
		}
	}
	
	@Override
	public void remove(){
		super.remove();
		//Call all the part removal methods to ensure they save their states properly.
		for(APart part : parts){
			part.remove();
		}
	}
	
	@Override
	public void updateText(List<String> textLines){
		//Multiparts also update their part's text.
		int linesChecked = 0;
		for(Entry<JSONText, String> textEntry : text.entrySet()){
			textEntry.setValue(textLines.get(linesChecked));
			++linesChecked;
		}
		for(APart part : parts){
			for(Entry<JSONText, String> textEntry : part.text.entrySet()){
				textEntry.setValue(textLines.get(linesChecked));
				++linesChecked;
			}
		}
	}
	
	@Override
    public Collection<BoundingBox> getCollisionBoxes(){
    	return allEntityCollisionBoxes;
    }
	
	@Override
    public Collection<BoundingBox> getInteractionBoxes(){
    	return allInteractionBoxes;
    }
	
	@Override
	public boolean canCollideWith(AEntityB_Existing entityToCollide){
		return !(entityToCollide instanceof APart);
	}
	
	@Override
	public void updatePostMovement(){
		//Update parts prior to doing our post-movements.
		//This is required for trailers, as they may attached to parts.
		world.beginProfiling("PartUpdates_" + parts.size(), true);
		Iterator<APart> iterator = parts.iterator();
		while(iterator.hasNext()){
			APart part = iterator.next();
			part.update();
			if(!part.isValid){
				removePart(part, iterator);
			}
		}
		world.endProfiling();
		super.updatePostMovement();
		
		//Update all-box lists now that all parts are updated.
		//If we don't do this, then the box size might get de-synced.
		world.beginProfiling("BoxAlignment_" + allInteractionBoxes.size(), true);
		sortBoxes();
		world.endProfiling();
	}
	
	@Override
	public double getRawVariableValue(String variable, float partialTicks){
		//If we have a variable with a suffix, we need to get that part first and pass
		//it into this method rather than trying to run through the code now.
		int partNumber = getVariableNumber(variable);
		if(partNumber != -1){
			return getSpecificPartAnimation(this, variable, partNumber, partialTicks);
		}else{
			return super.getRawVariableValue(variable, partialTicks);
		}
	}
	
	@Override
	public void toggleVariable(String variable){
		int partNumber = getVariableNumber(variable);
		if(partNumber != -1){
			APart foundPart = getSpecificPart(this, variable, partNumber);
			if(foundPart != null){
				variable = variable.substring(0, variable.lastIndexOf("_"));
				foundPart.toggleVariable(variable);
			}
		}else{
			super.toggleVariable(variable);
		}
	}
	
	@Override
	public void setVariable(String variable, double value){
		int partNumber = getVariableNumber(variable);
		if(partNumber != -1){
			APart foundPart = getSpecificPart(this, variable, partNumber);
			if(foundPart != null){
				foundPart.setVariable(variable.substring(0, variable.lastIndexOf("_")), value);
			}
		}else{
			super.setVariable(variable, value);
		}
	}
	
	/**
	 * Returns true if any linked variables are blocking the player from
	 * accessing the passed-in part slot.
	 */
	public boolean areVariablesBlocking(JSONPartDefinition partDef, WrapperPlayer player){
		if(partDef.linkedVariables != null){
			for(String variableName : partDef.linkedVariables){
				if(variableName.startsWith("!")){
					double value = getRawVariableValue(variableName.substring(1), 0);
					if(value == 0 || Double.isNaN(value)){
						//We have the variable, no need to check any further.
						return false;
					}
				}else{
					double value = getRawVariableValue(variableName, 0);
					if(value > 0 && !Double.isNaN(value)){
						//We have the variable, no need to check any further.
						return false;
					}
				}
			}
			return true;
		}else{
			return false;
		}
	}
    
    /**
	 * Adds the passed-part to this entity.  This method will check at the passed-in point
	 * if the item-based part can go to this entity.  If so, it is constructed and added,
	 * and a packet is sent to all clients to inform them of this change.  Returns true
	 * if all operations completed, false if the part was not able to be added.
	 * If the part is being added during construction, set doingConstruction to true to
	 * prevent calling the lists, maps, and other systems that aren't set up yet.
	 * This method returns the part if it was added, null if it wasn't.
	 */
    public APart addPartFromItem(AItemPart partItem, WrapperPlayer playerAdding, WrapperNBT partData, Point3d offset, boolean addedDuringConstruction){
    	//Get the part pack to add.
		JSONPartDefinition newPartDef = getPackDefForLocation(offset);
		APart partToAdd = null;
		APart parentPart = null;
		//Check to make sure the spot is free.
		if(getPartAtLocation(offset) == null){
			//Check to make sure the part is valid.
			if(partItem.isPartValidForPackDef(newPartDef, subName, true)){
				//Try to find the parent part, if this part would have one.
				for(JSONPartDefinition partDef : definition.parts){
					if(partDef.additionalParts != null){
						for(JSONPartDefinition additionalPartDef : partDef.additionalParts){
							if(offset.equals(additionalPartDef.pos)){
								parentPart = getPartAtLocation(partDef.pos);
								break;
							}
						}
					}
					if(parentPart != null){
						break;
					}
				}
				
				//If we aren't an additional part, see if we are a sub-part.
				//This consists of both existing and NBT parts.
				List<APart> partsToCheck = new ArrayList<APart>();
				partsToCheck.addAll(parts);
				partsToCheck.addAll(partsFromNBT);
				for(APart part : partsToCheck){
					if(part.definition.parts != null){
						for(JSONPartDefinition subPartDef : part.definition.parts){
							JSONPartDefinition correctedDef = part.getPackForSubPart(subPartDef);
							if(offset.equals(correctedDef.pos)){
								parentPart = part;
								break;
							}
							
							//Check sub-part additional parts.
							if(subPartDef.additionalParts != null){
								for(JSONPartDefinition additionalPartDef : subPartDef.additionalParts){
									JSONPartDefinition correctedAdditionalDef = part.getPackForSubPart(additionalPartDef);
									if(offset.equals(correctedAdditionalDef.pos)){
										parentPart = getPartAtLocation(correctedDef.pos);
										break;
									}
								}
								if(parentPart != null){
									break;
								}
							}
						}
						if(parentPart != null){
							break;
						}
					}
				}
				
				//Part is valid.  Create it.
				partItem.populateDefaultData(partData);
				partToAdd = partItem.createPart(this, playerAdding, newPartDef, partData, parentPart); 
			}
		}
    	
    	//If the part isn't null, add it to the entity.
		//If we're in construction, it goes in the NBT maps and we need to add a rider position if it's a seat.
		//Otherwise, we use the regular add method.
    	if(partToAdd != null){
    		if(addedDuringConstruction){
    			partsFromNBT.add(partToAdd);
				if(partToAdd instanceof PartSeat){
					ridableLocations.add(partToAdd.placementOffset);
				}
    		}else{
    			//Need to know if we are a new part or not.
    			boolean newPart = partData == null || partData.getString("uniqueUUID").isEmpty();
    			
    			//Now add the part as all instruments and variables are on it.
    			addPart(partToAdd, true);
				
				//Add default parts.  We need to do this after we send a packet so our slots are valid.
				//Need to make sure to convert the part placement defs to the right type as they're offset.
				if(partToAdd.definition.parts != null){
					for(JSONPartDefinition subPartPack : partToAdd.definition.parts){
						addDefaultPart(partToAdd.getPackForSubPart(subPartPack), playerAdding, partToAdd.definition, addedDuringConstruction, !newPart);
					}
				}
    		}
    	}
    	return partToAdd;
    }
	
    /**
   	 * Adds the passed-in part to the entity.  Also is responsible for modifying
   	 * and lists or maps that may have changed from adding the part.
   	 */
	public void addPart(APart part, boolean sendPacket){
		parts.add(part);
		AItemPart partItem = part.getItem();
		//Check for null, as the part may not have an item it will return, as is
		//the case for fake parts or flat wheels.
		if(partItem != null){
			if(!partsByItem.containsKey(partItem)){
				partsByItem.put(partItem, new ArrayList<APart>());
			}
			partsByItem.get(partItem).add(part);
		}
		
		//Add a ride-able location.
		if(part instanceof PartSeat){
			ridableLocations.add(part.placementOffset);
		}
		
		//Recalculate slots.
		recalculatePartSlots();
		
		//If we are on the server, and need to notify clients, do so.
		if(sendPacket && !world.isClient()){
			InterfacePacket.sendToAllClients(new PacketPartChange(this,  part));
		}
		
		//Add the part to the world.
		world.addEntity(part);
	}
	
	/**
   	 * Removes the passed-in part from the entity.  Calls the part's {@link APart#remove()} method to
   	 * let the part handle removal code.  Iterator is optional, but if you're in any code block that
   	 * is iterating over the parts list, and you don't pass that iterator in, you'll get a CME.
   	 */
	public void removePart(APart part, Iterator<APart> iterator){
		if(parts.contains(part)){
			//If the part has any parts, remove those first.
			if(!part.childParts.isEmpty()){
				if(iterator == null){
					while(!part.childParts.isEmpty()){
						removePart(part.childParts.get(0), null);
					}
				}else{
					//Flag all child parts for removal rather than remove them.
					//We're iterating here, so don't want to remove them right away.
					for(APart childPart : part.childParts){
						childPart.isValid = false;
					}
				}
			}
			
			//Remove part from main list of parts.
			if(iterator != null){
				iterator.remove();
			}else{
				parts.remove(part);
			}
			//Remove from mappings.
			AItemPart partItem = part.getItem();
			if(partsByItem.containsKey(partItem)){
				partsByItem.get(partItem).remove(part);
				if(part.equals(lastPrimaryPart.get(partItem))){
					lastPrimaryPart.remove(partItem);
				}
			}
			
			//Remove any riders riding this part from the riding map.
			if(locationRiderMap.containsKey(part.placementOffset)){
				removeRider(locationRiderMap.get(part.placementOffset), null);
			}
			//Call the part's removal code for it to process.
			part.remove();
			//If we are on the server, notify all clients of this change.
			if(!world.isClient()){
				InterfacePacket.sendToAllClients(new PacketPartChange(this, part.placementOffset));
			}
		}else if(partsFromNBT.contains(part)){
			partsFromNBT.remove(part);
		}
		
		//Remove a ride-able location.
		if(part instanceof PartSeat){
			ridableLocations.remove(part.placementOffset);
		}
		
		//Recalculate slots.
		recalculatePartSlots();
	}
	
	/**
	 * Gets the part at the specified location.
	 * This also checks NBT parts in case we are doing
	 * this check for parent-part lookups during construction.
	 */
	public APart getPartAtLocation(Point3d offset){
		for(APart part : parts){
			if(part.placementOffset.equals(offset)){
				return part;
			}
		}
		for(APart part : partsFromNBT){
			if(part.placementOffset.equals(offset)){
				return part;
			}
		}
		return null;
	}
	
	/**
	 * Gets the part that has the passed-in bounding box.
	 * Useful if we interacted with  a box on this multipart and need
	 * to know which part it went to.
	 */
	public APart getPartWithBox(BoundingBox box){
		for(APart part : parts){
			if(part.interactionBoxes.contains(box)){
				return part;
			}
		}
		return null;
	}
	
	/**
	 * Gets all possible pack parts.  This includes additional parts on the entity
	 * and extra parts of parts on other parts.  Map returned is the position of the
	 * part positions and the part pack information at those positions.
	 * Note that additional parts will not be added if no part is present
	 * in the primary location.
	 */
	public LinkedHashMap<Point3d, JSONPartDefinition> getAllPossiblePackParts(){
		LinkedHashMap<Point3d, JSONPartDefinition> partDefs = new LinkedHashMap<Point3d, JSONPartDefinition>();
		//First get all the regular part spots.
		for(JSONPartDefinition partDef : definition.parts){
			partDefs.put(partDef.pos, partDef);
			
			//Check to see if we can put a additional parts in this location.
			//If a part is present at a location that can have an additional parts, we allow them to be placed.
			if(partDef.additionalParts != null){
				for(APart part : parts){
					if(part.placementOffset.equals(partDef.pos)){
						for(JSONPartDefinition additionalPartDef : partDef.additionalParts){
							partDefs.put(additionalPartDef.pos, additionalPartDef);
						}
						break;
					}
				}
			}
		}
		
		//Next get any sub parts on parts that are present.
		for(APart part : parts){
			if(part.definition.parts != null){
				for(JSONPartDefinition subPartDef : part.definition.parts){
					JSONPartDefinition correctedPartDef = part.getPackForSubPart(subPartDef);
					partDefs.put(correctedPartDef.pos, correctedPartDef);
					
					//Check to see if we can put a additional parts in this location.
					//If a part is present at a location that can have an additional parts, we allow them to be placed.
					if(subPartDef.additionalParts != null){
						for(APart part2 : parts){
							if(part2.placementOffset.equals(correctedPartDef.pos)){
								for(JSONPartDefinition additionalPartDef : subPartDef.additionalParts){
									correctedPartDef = part.getPackForSubPart(additionalPartDef);
									partDefs.put(correctedPartDef.pos, correctedPartDef);
								}
								break;
							}
						}
					}
				}
			}
			
		}
		return partDefs;
	}
	
	/**
	 * Gets the pack definition at the specified location.
	 */
	public JSONPartDefinition getPackDefForLocation(Point3d offset){
		//Check to see if this is a main part.
		for(JSONPartDefinition partDef : definition.parts){
			if(partDef.pos.equals(offset)){
				return partDef;
			}
			
			//Not a main part.  Check if this is an additional part.
			if(partDef.additionalParts != null){
				for(JSONPartDefinition additionalPartDef : partDef.additionalParts){
					if(additionalPartDef.pos.equals(offset)){
						return additionalPartDef;
					}
				}
			}
		}
		
		//If this is not a main part or an additional part, check the sub-parts.
		//We check both the main parts, and those from NBT in case we're in a loading-loop.
		List<APart> allParts = new ArrayList<APart>();
		allParts.addAll(parts);
		allParts.addAll(partsFromNBT);
		for(APart part : allParts){
			if(part.definition.parts != null){
				for(JSONPartDefinition subPartDef : part.definition.parts){
					JSONPartDefinition correctedPartDef = part.getPackForSubPart(subPartDef);
					if(correctedPartDef.pos.equals(offset)){
						return correctedPartDef;
					}
					
					//Check additional part definitions.
					if(subPartDef.additionalParts != null){
						for(JSONPartDefinition additionalPartDef : subPartDef.additionalParts){
							correctedPartDef = part.getPackForSubPart(additionalPartDef);
							if(correctedPartDef.pos.equals(offset)){
								return correctedPartDef;
							}
						}
					}
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Helper method to allow for recursion when adding default parts.
	 * This method adds all default parts for the passed-in part entry.
	 * The entry can either be on the main entity, or a part on this entity.
	 * This method should only be called when the entity or part with the
	 * passed-in definition is placed on this entity, not when it's being loaded from saved data.
	 * The savedParent flag is to let this method add only default permanent parts, as they get
	 * removed with the part when wrenched, and added back when placed again, and don't save their states.
	 */
	public void addDefaultPart(JSONPartDefinition partDef, WrapperPlayer playerAdding, AJSONPartProvider providingDef, boolean addedDuringConstruction, boolean savedParent){
		if(partDef.defaultPart != null && (!savedParent || partDef.isPermanent)){
			try{
				String partPackID = partDef.defaultPart.substring(0, partDef.defaultPart.indexOf(':'));
				String partSystemName = partDef.defaultPart.substring(partDef.defaultPart.indexOf(':') + 1);
				try{
					APart addedPart = addPartFromItem(PackParserSystem.getItem(partPackID, partSystemName), playerAdding, new WrapperNBT(), partDef.pos, addedDuringConstruction);
					if(addedPart != null){
						//Set the default tone for the part, if it requests one and we can provide one.
						updatePartTone(addedPart);
						
						//Check if we have an additional parts.
						//If so, we need to check that for default parts.
						if(partDef.additionalParts != null){
							for(JSONPartDefinition additionalPartDef : partDef.additionalParts){
								addDefaultPart(addedPart.placementDefinition.isSubPart ? addedPart.parentPart.getPackForSubPart(additionalPartDef) : additionalPartDef, playerAdding, providingDef, addedDuringConstruction, savedParent);
							}
						}
						
						//Check all sub-parts, if we have any.
						//We need to make sure to convert them to the right type as they're offset.
						if(addedPart.definition.parts != null){
							for(JSONPartDefinition subPartPack : addedPart.definition.parts){
								addDefaultPart(addedPart.getPackForSubPart(subPartPack), playerAdding, addedPart.definition, addedDuringConstruction, savedParent);
							}
						}
					}
				}catch(NullPointerException e){
					throw new IllegalArgumentException("Attempted to add defaultPart: " + partPackID + ":" + partSystemName + " to: " + providingDef.packID + ":" + providingDef.systemName + " but that part doesn't exist in the pack item registry.");
				}
			}catch(IndexOutOfBoundsException e){
				throw new IllegalArgumentException("Could not parse defaultPart definition: " + partDef.defaultPart + ".  Format should be \"packId:partName\"");
			}
		}
	}
	
	/**
	 * Updates the tone of the passed-in part to its appropriate type.
	 * If the part can't match the tone of this vehicle, then it is not modified.
	 */
	public void updatePartTone(APart part){
		if(part.placementDefinition.toneIndex != 0){
			List<String> partTones = null;
			for(JSONSubDefinition subDefinition : definition.definitions){
				if(subDefinition.subName.equals(subName)){
					partTones = subDefinition.partTones;
				}
			}
			if(partTones != null && partTones.size() >= part.placementDefinition.toneIndex){
				String partTone = partTones.get(part.placementDefinition.toneIndex - 1);
				for(JSONSubDefinition subDefinition : part.definition.definitions){
					if(subDefinition.subName.equals(partTone)){
						part.subName = partTone;
						return;
					}
				}
			}
		}
	}
	
	/**
	 * Call to re-create the list of all valid part slot boxes.
	 * This should be called after part addition or part removal.
	 * Also must be called at construction time to create the initial slot set.
	 */
	private void recalculatePartSlots(){
		allPartSlotBoxes.clear();
		allPartSlotRenderables.clear();
		for(Entry<Point3d, JSONPartDefinition> packPartEntry : getAllPossiblePackParts().entrySet()){
			if(getPartAtLocation(packPartEntry.getKey()) == null){
				BoundingBox newSlotBox = new BoundingBox(packPartEntry.getKey(), packPartEntry.getKey().copy().rotateFine(angles).add(position), PART_SLOT_HITBOX_WIDTH/2D, PART_SLOT_HITBOX_HEIGHT/2D, PART_SLOT_HITBOX_WIDTH/2D, false);
				allPartSlotBoxes.put(newSlotBox, packPartEntry.getValue());
				allPartSlotRenderables.put(newSlotBox, new RenderableObject(newSlotBox, new ColorRGB(), true));
			}
		}
	}
	
	/**
	 * Call to re-create the lists of the collision and interaction boxes.
	 * This should be run at construction, and every tick so we have up-to-date lists.
	 */
	protected void sortBoxes(){
		//Set active collision box, door box, and interaction box lists to current boxes.
		allEntityCollisionBoxes.clear();
		allEntityCollisionBoxes.addAll(entityCollisionBoxes);
		allBlockCollisionBoxes.clear();
		allBlockCollisionBoxes.addAll(blockCollisionBoxes);
		allInteractionBoxes.clear();
		allInteractionBoxes.addAll(interactionBoxes);
		
		//Only add active slots on clients, but all slots on servers.
		//The only exception is if the player has a scanner, in which case we add them all to allow it to work.
		if(world.isClient() && !InterfaceClient.getClientPlayer().isHoldingItemType(ItemComponentType.SCANNER)){
			allInteractionBoxes.addAll(activePartSlotBoxes.keySet());
		}else{
			allInteractionBoxes.addAll(allPartSlotBoxes.keySet());
		}
		
		//Add all part boxes.
		for(APart part : parts){
			allEntityCollisionBoxes.addAll(part.entityCollisionBoxes);
			allBlockCollisionBoxes.addAll(part.blockCollisionBoxes);
			
			//Part interaction boxes are updated by the part, so we don't need to update those.
			//Rather, the part will update them on it's own update call.
			//However, we do need to decide if we need to add those boxes to the interaction list.
			//This is dependent on what the current player entity is holding.
			if(world.isClient()){
				WrapperPlayer clientPlayer = InterfaceClient.getClientPlayer();
				
				//If the part is a seat, and we are riding it, don't add it.
				//This keeps us from clicking our own seat when we want to click other things.
				if(part instanceof PartSeat){
					if(part.placementOffset.equals(locationRiderMap.inverse().get(clientPlayer))){
						continue;
					}
				}
				
				//If the part is linked to variables, and none are active, don't add it.
				//Exclude this if the part is a seat and we are riding this vehicle.
				//This lets players change seats without opening doors.
				if(!(locationRiderMap.inverse().containsKey(clientPlayer) && part instanceof PartSeat) && areVariablesBlocking(part.placementDefinition, clientPlayer)){
					continue;
				}
				
				//If we are holding a wrench, and the part has children, don't add it.  We can't wrench those parts.
				//The only exception are parts that have permanent-default parts on them.  These can be wrenched.
				if(clientPlayer.isHoldingItemType(ItemComponentType.WRENCH)){
					boolean partHasRemovablePart = false;
					for(APart childPart : part.childParts){
						if(!childPart.placementDefinition.isPermanent){
							partHasRemovablePart = true;
							break;
						}
					}
					
					if(partHasRemovablePart){
						continue;
					}
				}
			}
				
			//Conditions to add have been met, add boxes.
			allInteractionBoxes.addAll(part.interactionBoxes);
		}
		
		//Update encompassing bounding box to reflect all bounding boxes of all parts.
		for(APart part : parts){
			if(!part.isFake()){
	    		encompassingBox.widthRadius = (float) Math.max(encompassingBox.widthRadius, Math.abs(part.encompassingBox.globalCenter.x - position.x + part.encompassingBox.widthRadius));
	    		encompassingBox.heightRadius = (float) Math.max(encompassingBox.heightRadius, Math.abs(part.encompassingBox.globalCenter.y - position.y + part.encompassingBox.heightRadius));
	    		encompassingBox.depthRadius = (float) Math.max(encompassingBox.depthRadius, Math.abs(part.encompassingBox.globalCenter.z - position.z + part.encompassingBox.depthRadius));
			}
    	}
		encompassingBox.updateToEntity(this, null);
	}
	
	/**
	 * Helper method to return the part at the specific index for the passed-in variable.
	 * Returns null if the part doesn't exist.
	 */
	public static APart getSpecificPart(AEntityD_Definable<? extends AJSONPartProvider> entityAnimating, String variable, int partNumber){
		//Iterate through our parts to find the index of the pack def for the part we want.
		String partType = variable.substring(0, variable.indexOf("_"));
		JSONPartDefinition foundDef = null;
		for(JSONPartDefinition partDef : entityAnimating.definition.parts){
			//If this part is the one we want, get it or add to our index.
			for(String defPartType : partDef.types){
				if(partType.equals("part") || defPartType.startsWith(partType)){
					if(partNumber == 0){
						foundDef = partDef;
					}else{
						--partNumber;
					}
					break;
				}
			}
			
			//Also check additional parts if we have them.
			if(foundDef == null && partDef.additionalParts != null){
				for(JSONPartDefinition additionalDef : partDef.additionalParts){
					for(String defPartType : additionalDef.types){
						if(partType.equals("part") || defPartType.startsWith(partType)){
							if(partNumber == 0){
								foundDef = additionalDef;
							}else{
								--partNumber;
							}
							break;
						}
					}
					if(foundDef != null){
						break;
					}
				}
			}
			
			//If we found our part, try to get it.
			if(foundDef != null){
				//Get the part at this location.  If it's of the same type as what we need, return it.
				//If it's not, or it doesn't exist, return null as it hasn't been placed yet.
				if(entityAnimating instanceof APart){
					APart part = (APart) entityAnimating;
					return part.entityOn.getPartAtLocation(part.getPackForSubPart(foundDef).pos);
				}else{
					AEntityF_Multipart<?> provider = (AEntityF_Multipart<?>) entityAnimating;
					return provider.getPartAtLocation(foundDef.pos);
				}
			}
		}
		
		//No valid sub-part definitions found.  This is an error, but not one we should crash for.  Return null.
		return null;
	}
	
	/**
	 * Helper method to return the value of an animation for a specific part, as
	 * determined by the index of that part.
	 */
	public static double getSpecificPartAnimation(AEntityD_Definable<? extends AJSONPartProvider> entityAnimating, String variable, int partNumber, float partialTicks){
		APart foundPart = getSpecificPart(entityAnimating, variable, partNumber);
		if(foundPart != null){
			return foundPart.getRawVariableValue(variable.substring(0, variable.lastIndexOf("_")), partialTicks);
		}else{
			return 0;
		}
	}
		
	
	@Override
	public WrapperNBT save(WrapperNBT data){
		super.save(data);
		List<APart> allParts = new ArrayList<APart>();
		allParts.addAll(parts);
		allParts.addAll(partsFromNBT);
		int totalParts = 0;
		for(APart part : allParts){
			//Don't save the part if it's not valid or a fake part.
			if(part.isValid && !part.isFake()){
				WrapperNBT partData = part.save(new WrapperNBT());
				//We need to set some extra data here for the part to allow this entity to know where it went.
				//This only gets set here during saving/loading, and is NOT returned in the item that comes from the part.
				partData.setPoint3d("offset", part.placementOffset);
				data.setData("part_" + totalParts, partData);
				++totalParts;
			}
		}
		data.setInteger("totalParts", totalParts);
		return data;
	}
}
