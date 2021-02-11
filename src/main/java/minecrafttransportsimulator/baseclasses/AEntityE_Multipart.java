package minecrafttransportsimulator.baseclasses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.jsondefs.AJSONPartProvider;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketPartChange;
import minecrafttransportsimulator.packloading.JSONParser;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartSeat;

/**Base class for multipart entities.  These entities hold other, part-based entities.  These part
 * entities may be added or removed from this entity based on the implementation, but assurances
 * are made with how they are stored and how they are accessed.
 * 
 * @author don_bruce
 */
public abstract class AEntityE_Multipart<JSONDefinition extends AJSONPartProvider> extends AEntityD_Interactable<JSONDefinition>{
	
	/**This list contains all parts this entity has.  Do NOT directly modify this list.  Instead,
	 * call {@link #addPart}, {@link #addPartFromItem}, or {@link #removePart} to ensure all sub-classed
	 * operations are performed.  Note that if you are iterating over this list when you call one of those
	 * methods, and you don't pass the method an iterator instance, you will get a CME!.
	 */
	public final List<APart> parts = new ArrayList<APart>();
	
	/**List for parts loaded from NBT.  We can't add these parts on construction as we'd error out
	 * due to the potential of various sub-class variables not being ready at construction time.  To compensate, we add the parts we
	 * wish to add to this list.  Post-construction these will be added to this entity, preventing NPEs.
	 */
	private final List<APart> partsFromNBT = new ArrayList<APart>();
	
	/**Cached pack definition mappings for sub-part packs.  First key is the parent part definition, which links to a map.
	 * This second map is keyed by a part definition, with the value equal to a corrected definition.  This means that
	 * in total, this object contains all sub-packs created on any entity for any part with sub-packs.  This is done as parts with
	 * sub-parts use relative locations, and thus we need to ensure we have the correct position for them on any entity part location.*/
	private static final Map<JSONPartDefinition, Map<JSONPartDefinition, JSONPartDefinition>> SUBPACK_MAPPINGS = new HashMap<JSONPartDefinition, Map<JSONPartDefinition, JSONPartDefinition>>();
	
	public AEntityE_Multipart(WrapperWorld world, WrapperNBT data){
		super(world, data);
		//Add parts.
		//Also Replace ride-able locations with seat locations.
		//This ensures we use the proper location for mapping operations.
		for(int i=0; i<data.getInteger("totalParts"); ++i){
			//Use a try-catch for parts in case they've changed since this entity was last placed.
			//Don't want crashes due to pack updates.
			try{
				WrapperNBT partData = data.getData("part_" + i);
				ItemPart partItem = PackParserSystem.getItem(partData.getString("packID"), partData.getString("systemName"), partData.getString("subName"));
				Point3d partOffset = partData.getPoint3d("offset");
				addPartFromItem(partItem, partData, partOffset, true);
			}catch(Exception e){
				InterfaceCore.logError("Could not load part from NBT.  Did you un-install a pack?");
			}
		}
	}
	
	@Override
	public void update(){
		//If we have any NBT parts, add them now.
		if(!partsFromNBT.isEmpty()){
			for(APart part : partsFromNBT){
				addPart(part);
			}
			partsFromNBT.clear();
		}
		
		//Send update call down to all parts.
		//They need to get processed first to handle hitbox logic, or removal based on damage.
		//We call this before we call the super as they need to know the new statuses.
		Iterator<APart> iterator = parts.iterator();
		while(iterator.hasNext()){
			APart part = iterator.next();
			part.update();
			if(!part.isValid){
				removePart(part, iterator);
			}
		}
		
		//Now call the super to update prev variables.
		super.update();
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
    
    /**
	 * Adds the passed-part to this entity.  This method will check at the passed-in point
	 * if the item-based part can go to this entity.  If so, it is constructed and added,
	 * and a packet is sent to all clients to inform them of this change.  Returns true
	 * if all operations completed, false if the part was not able to be added.
	 * If the part is being added during construction, set doingConstruction to true to
	 * prevent calling the lists, maps, and other systems that aren't set up yet.
	 * This method returns true if the part was able to be added, false if something prevented it.
	 */
    public boolean addPartFromItem(ItemPart partItem, WrapperNBT partData, Point3d offset, boolean doingConstruction){
    	//Get the part pack to add.
		JSONPartDefinition newPartDef = getPackDefForLocation(offset);
		APart partToAdd = null;
		APart parentPart = null;
		//Check to make sure the spot is free.
		if(getPartAtLocation(offset) == null){
			//Check to make sure the part is valid.
			if(partItem.isPartValidForPackDef(newPartDef)){
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
							JSONPartDefinition correctedDef = getPackForSubPart(part.partDefinition, subPartDef);
							if(offset.equals(correctedDef.pos)){
								parentPart = part;
								break;
							}
							
							//Check sub-part additional parts.
							if(subPartDef.additionalParts != null){
								for(JSONPartDefinition additionalPartDef : subPartDef.additionalParts){
									JSONPartDefinition correctedAdditionalDef = getPackForSubPart(part.partDefinition, additionalPartDef);
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
				//FIXME need to pack in definition with NBT here.
				partToAdd = partItem.createPart(this, newPartDef, partData != null ? partData : new WrapperNBT(), parentPart); 
			}
		}
    	
    	//If the part isn't null, add it to the entity.
		//If we're in construction, it goes in the NBT maps and we need to add a rider position if it's a seat.
		//Otherwise, we use the regular add method.
    	if(partToAdd != null){
    		if(doingConstruction){
    			partsFromNBT.add(partToAdd);
				if(partToAdd instanceof PartSeat){
					ridableLocations.add(partToAdd.placementOffset);
				}
    		}else{
	    		addPart(partToAdd);
				
				//If we are a new part, we need to add text.
	    		boolean newPart = partData.getString("packID").isEmpty();
	    		if(newPart){
					if(partToAdd.definition.rendering != null && partToAdd.definition.rendering.textObjects != null){
						for(JSONText textObject : partToAdd.definition.rendering.textObjects){
							partToAdd.text.put(textObject, textObject.defaultText);
						}
					}
					partData = partToAdd.getData();
	    		}
				
				//Send packet to client with part data.
				InterfacePacket.sendToAllClients(new PacketPartChange(this, offset, partItem, partData, partToAdd.parentPart));
				
				//If we are a new part, add default parts.  We need to do this after we send a packet.
				//We need to make sure to convert them to the right type as they're offset.
				if(newPart && partToAdd.definition.parts != null){
					List<JSONPartDefinition> subPartsToAdd = new ArrayList<JSONPartDefinition>();
					for(JSONPartDefinition subPartPack : partToAdd.definition.parts){
						subPartsToAdd.add(this.getPackForSubPart(partToAdd.partDefinition, subPartPack));
					}
					addDefaultParts(subPartsToAdd, partToAdd, true);
				}
    		}
			return true;
    	}else{
    		return false;
    	}
    }
	
    /**
   	 * Adds the passed-in part to the entity.  Also is responsible for modifying
   	 * and lists or maps that may have changed from adding the part.
   	 */
	public void addPart(APart part){
		parts.add(part);
		
		//Add a ride-able location.
		if(part instanceof PartSeat){
			ridableLocations.add(part.placementOffset);
		}
	}
	
	/**
   	 * Removes the passed-in part from the entity.  Calls the part's {@link APart#remove()} method to
   	 * let the part handle removal code.  Iterator is optional, but if you're in any code block that
   	 * is iterating over the parts list, and you don't pass that iterator in, you'll get a CME.
   	 */
	public void removePart(APart part, Iterator<APart> iterator){
		if(parts.contains(part)){
			//Remove part from main list of parts.
			if(iterator != null){
				iterator.remove();
			}else{
				parts.remove(part);
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
		}
		
		//Remove a ride-able location.
		if(part instanceof PartSeat){
			ridableLocations.remove(part.placementOffset);
		}
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
				JSONPartDefinition parentPartDef = getPackDefForLocation(part.placementOffset);
				for(JSONPartDefinition subPartDef : part.definition.parts){
					JSONPartDefinition correctedPartDef = getPackForSubPart(parentPartDef, subPartDef);
					partDefs.put(correctedPartDef.pos, correctedPartDef);
					
					//Check to see if we can put a additional parts in this location.
					//If a part is present at a location that can have an additional parts, we allow them to be placed.
					if(subPartDef.additionalParts != null){
						for(APart part2 : parts){
							if(part2.placementOffset.equals(correctedPartDef.pos)){
								for(JSONPartDefinition additionalPartDef : subPartDef.additionalParts){
									correctedPartDef = getPackForSubPart(parentPartDef, additionalPartDef);
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
				JSONPartDefinition parentPartDef = getPackDefForLocation(part.placementOffset);
				for(JSONPartDefinition subPartDef : part.definition.parts){
					JSONPartDefinition correctedPartDef = getPackForSubPart(parentPartDef, subPartDef);
					if(correctedPartDef.pos.equals(offset)){
						return correctedPartDef;
					}
					
					//Check additional part definitions.
					if(subPartDef.additionalParts != null){
						for(JSONPartDefinition additionalPartDef : subPartDef.additionalParts){
							correctedPartDef = getPackForSubPart(parentPartDef, additionalPartDef);
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
	 * Returns a PackPart with the correct properties for a SubPart.  This is because
	 * subParts inherit some properties from their parent parts.  All created sub-part
	 * packs are cached locally once created, as they need to not create new Point3d instances.
	 * If they did, then the lookup relation between them and their spot in the vehicle would
	 * get broken for maps on each reference.
	 */
	public JSONPartDefinition getPackForSubPart(JSONPartDefinition parentPack, JSONPartDefinition subPack){
		if(!SUBPACK_MAPPINGS.containsKey(parentPack)){
			SUBPACK_MAPPINGS.put(parentPack, new HashMap<JSONPartDefinition, JSONPartDefinition>());
		}
		
		JSONPartDefinition correctedPartDef = SUBPACK_MAPPINGS.get(parentPack).get(subPack);
		if(correctedPartDef == null){
			//Use GSON to make a deep copy of the current pack definition.
			//Set the sub-part flag to ensure we know this is a subPart for rendering operations.
			correctedPartDef = JSONParser.duplicateJSON(subPack);
			correctedPartDef.isSubPart = true;
			
			//Now set parent-specific properties.  These pertain to position, rotation, mirroring, and the like.
			//First add the parent pack's position to the sub-pack.
			//We don't add rotation, as we need to stay relative to the parent part, as the parent part will rotate us.
			correctedPartDef.pos.add(parentPack.pos);
			
			//If the parent pack is mirrored, we need to invert our X-position to match.
			if(parentPack.pos.x < 0 ^ parentPack.inverseMirroring){
				correctedPartDef.pos.x -= 2*subPack.pos.x;
			}
			
			//Use the parent's turnsWithSteer variable, as that's based on the vehicle, not the part.
			correctedPartDef.turnsWithSteer = parentPack.turnsWithSteer;
			
			//Save the corrected pack into the mappings for later use.
	        SUBPACK_MAPPINGS.get(parentPack).put(subPack, correctedPartDef);
		}
		return correctedPartDef;
	}
	
	/**
	 * Helper method to allow for recursion when adding default parts.
	 * This method adds all default parts for the passed-in list of parts.
	 * The part list should consist of a "parts" JSON definition, and can either
	 * be on the main entity, or a part on this entity.
	 * This method should only be called when the entity or part with the
	 * passed-in definition is first placed, not when it's being loaded from saved data.
	 */
	public void addDefaultParts(List<JSONPartDefinition> partsToAdd, APart parentPart, boolean sendPacket){
		for(JSONPartDefinition partDef : partsToAdd){
			if(partDef.defaultPart != null){
				try{
					String partPackID = partDef.defaultPart.substring(0, partDef.defaultPart.indexOf(':'));
					String partSystemName = partDef.defaultPart.substring(partDef.defaultPart.indexOf(':') + 1);
					try{
						//FIXME call item adding methods here.
						ItemPart partItem = PackParserSystem.getItem(partPackID, partSystemName);
						APart newPart = partItem.createPart(this, partDef, new WrapperNBT(), parentPart);
						addPart(newPart);
						
						//Set default text for the new part, if we have any.
						if(newPart.definition.rendering != null && newPart.definition.rendering.textObjects != null){
							for(JSONText textObject : newPart.definition.rendering.textObjects){
								newPart.text.put(textObject, textObject.defaultText);
							}
						}
						
						//Send a packet if required.
						if(sendPacket){
							InterfacePacket.sendToAllClients(new PacketPartChange(this, newPart.placementOffset, newPart.getItem(), newPart.getData(), parentPart));
						}
						
						//Check if we have an additional parts.
						//If so, we need to check that for default parts.
						if(partDef.additionalParts != null){
							addDefaultParts(partDef.additionalParts, newPart, sendPacket);
						}
						
						//Check all sub-parts, if we have any.
						//We need to make sure to convert them to the right type as they're offset.
						if(newPart.definition.parts != null){
							List<JSONPartDefinition> subPartsToAdd = new ArrayList<JSONPartDefinition>();
							for(JSONPartDefinition subPartPack : newPart.definition.parts){
								subPartsToAdd.add(getPackForSubPart(partDef, subPartPack));
							}
							addDefaultParts(subPartsToAdd, newPart, sendPacket);
						}
					}catch(NullPointerException e){
						throw new IllegalArgumentException("Attempted to add defaultPart: " + partPackID + ":" + partSystemName + " to: " + definition.packID + ":" + definition.systemName + " but that part doesn't exist in the pack item registry.");
					}
				}catch(IndexOutOfBoundsException e){
					throw new IllegalArgumentException("Could not parse defaultPart definition: " + partDef.defaultPart + ".  Format should be \"packId:partName\"");
				}
			}
		}
	}
	
	@Override
	public void save(WrapperNBT data){
		super.save(data);
		int totalParts = 0;
		for(APart part : parts){
			//Don't save the part if it's not valid or a fake part.
			if(part.isValid && !part.isFake()){
				WrapperNBT partData = part.getData();
				//We need to set some extra data here for the part to allow this entity to know where it went.
				//This only gets set here during saving/loading, and is NOT returned in the item that comes from the part.
				partData.setString("packID", part.definition.packID);
				partData.setString("systemName", part.definition.systemName);
				partData.setString("subName", part.subName);
				partData.setPoint3d("offset", part.placementOffset);
				data.setData("part_" + totalParts, partData);
				++totalParts;
			}
		}
		data.setInteger("totalParts", totalParts);
	}
	
	//FIXME need to have the remove method call all part removes so they get their removal states set.
}
