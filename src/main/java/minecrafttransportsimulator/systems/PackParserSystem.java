package minecrafttransportsimulator.systems;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import mcinterface.BuilderItem;
import mcinterface.WrapperNBT;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.instances.ItemBooklet;
import minecrafttransportsimulator.items.instances.ItemDecor;
import minecrafttransportsimulator.items.instances.ItemInstrument;
import minecrafttransportsimulator.items.instances.ItemItem;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.items.instances.ItemPole;
import minecrafttransportsimulator.items.instances.ItemPoleComponent;
import minecrafttransportsimulator.items.instances.ItemVehicle;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.jsondefs.JSONBooklet;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.jsondefs.JSONInstrument;
import minecrafttransportsimulator.jsondefs.JSONItem;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleAnimatedObject;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleDefinition;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart.ExhaustObject;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleRendering;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartCustom;
import minecrafttransportsimulator.vehicles.parts.PartEngine;
import minecrafttransportsimulator.vehicles.parts.PartGroundDevice;
import minecrafttransportsimulator.vehicles.parts.PartGroundEffector;
import minecrafttransportsimulator.vehicles.parts.PartGun;
import minecrafttransportsimulator.vehicles.parts.PartInteractable;
import minecrafttransportsimulator.vehicles.parts.PartPropeller;
import minecrafttransportsimulator.vehicles.parts.PartSeat;

/**
 * Class responsible for parsing content pack data.  Gets properties from the text files that other parts
 * of the code can use.
 *
 * @author don_bruce
 */
public final class PackParserSystem{
	/**All registered pack items are stored in this map as they are added.  Used to sort items in the creative tab,
	 * and will be sent to packs for item registration when so asked via {@link #getItemsForPack(String)}.  May also
	 * be used if we need to lookup a registered part item.  Map is keyed by packID to allow sorting for items from 
	 * different packs, while the sub-map is keyed by the part's {@link AJSONItem#systemName}.**/
	private static TreeMap<String, LinkedHashMap<String, AItemPack<?>>> packItemMap = new TreeMap<String, LinkedHashMap<String, AItemPack<?>>>();

	/**Custom Gson instance for parsing packs.*/
	public static final Gson packParser = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().registerTypeAdapter(Point3d.class, Point3d.adapter).create();
	
	/**List of log entries to be added to the log.  Saved here as the log won't be ready till preInit, which
	 * runs after this parsing operation.*/
	public static List<String> logEntries = new ArrayList<String>();
	
    
    //-----START OF INIT LOGIC-----
    /**Packs should call this upon load to add their content to the mod.
     * This will return an array of strings that correspond to content types.
     * These content types will be content that has items in the jsondefs folder
     * that the pack should send to MTS.  The pack should only send the location
     * of such an item as it will allow MTS to load the information in modpacks.**/
    public static String[] getValidPackContentNames(){
    	return ItemClassification.getAllTypesAsStrings().toArray(new String[ItemClassification.values().length]);
    }
    
    /**Packs should call this upon load to add their vehicles to the mod.**/
    public static void addVehicleDefinition(InputStreamReader jsonReader, String jsonFileName, String packID){
    	try{
    		JSONVehicle mainDefinition = packParser.fromJson(jsonReader, JSONVehicle.class);
    		if(mainDefinition.rendering != null){
    			doAnimationLegacyCompats(mainDefinition.rendering, mainDefinition);
    		}
    		mainDefinition.genericName = jsonFileName;
	    	for(VehicleDefinition subDefinition : mainDefinition.definitions){
	    		try{
	    			//Need to copy the JSON into a new instance to allow differing systemNames.
	    			JSONVehicle mainDefinitionCopy = new JSONVehicle();
	    			mainDefinitionCopy.packID = mainDefinition.packID;
	    			mainDefinitionCopy.classification = mainDefinition.classification;
	    			mainDefinitionCopy.genericName = mainDefinition.genericName;
	    			
	    			//Need to copy general too, as we need to set the name for each general section to be unique.
	    			//For the materials we copy the strings and add the extra materials.
	    			mainDefinitionCopy.general = mainDefinition.new VehicleGeneral();
	    			mainDefinitionCopy.general.name = subDefinition.name;
	    			mainDefinitionCopy.general.description = mainDefinition.general.description;
	    			mainDefinitionCopy.general.materials = new String[mainDefinition.general.materials.length + subDefinition.extraMaterials.length];
	    			for(int i=0; i<mainDefinition.general.materials.length; ++i){
	    				mainDefinitionCopy.general.materials[i] = mainDefinition.general.materials[i];
	    			}
	    			for(int i=0; i<subDefinition.extraMaterials.length; ++i){
	    				mainDefinitionCopy.general.materials[mainDefinition.general.materials.length + i] = subDefinition.extraMaterials[i]; 
	    			}
	    			mainDefinitionCopy.general.openTop = mainDefinition.general.openTop;
	    			mainDefinitionCopy.general.emptyMass = mainDefinition.general.emptyMass;
	    			mainDefinitionCopy.general.type = mainDefinition.general.type;
	    			
	    			//Copy the rest of the parameters as-is.
	    			mainDefinitionCopy.definitions = mainDefinition.definitions;
	    			mainDefinitionCopy.motorized = mainDefinition.motorized;
	    			mainDefinitionCopy.plane = mainDefinition.plane;
	    			mainDefinitionCopy.blimp = mainDefinition.blimp;
	    			mainDefinitionCopy.car = mainDefinition.car;
	    			mainDefinitionCopy.parts = mainDefinition.parts;
	    			mainDefinitionCopy.collision = mainDefinition.collision;
	    			mainDefinitionCopy.doors = mainDefinition.doors;
	    			mainDefinitionCopy.rendering = mainDefinition.rendering;
	    			
	    			performLegacyCompats(mainDefinitionCopy);
	    			setupItem(new ItemVehicle(mainDefinitionCopy, subDefinition.subName), jsonFileName + subDefinition.subName, packID, ItemClassification.VEHICLE);
	    		}catch(Exception e){
	    			throw new NullPointerException("Unable to parse definition #" + (mainDefinition.definitions.indexOf(subDefinition) + 1) + " due to a formatting error.");
	    		}
    		}
    	}catch(Exception e){
    		logEntries.add("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + packID + ":" + jsonFileName);
    		logEntries.add(e.getMessage());
    		e.printStackTrace();
    	}
    }
    
    /**Packs should call this upon load to add their parts to the mod.**/
    public static void addPartDefinition(InputStreamReader jsonReader, String jsonFileName, String packID){
    	try{
    		JSONPart definition = packParser.fromJson(jsonReader, JSONPart.class);
    		performLegacyCompats(definition);
    		setupItem(new ItemPart(definition), jsonFileName, packID, ItemClassification.PART);
    	}catch(Exception e){
    		logEntries.add("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + packID + ":" + jsonFileName);
    		logEntries.add(e.getMessage());
    	}
    }
    
    /**Packs should call this upon load to add their instrument set to the mod.**/
    public static void addInstrumentDefinition(InputStreamReader jsonReader, String jsonFileName, String packID){
    	try{
    		setupItem(new ItemInstrument(packParser.fromJson(jsonReader, JSONInstrument.class)), jsonFileName, packID, ItemClassification.INSTRUMENT);
    	}catch(Exception e){
    		logEntries.add("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + packID + ":" + jsonFileName);
    		logEntries.add(e.getMessage());
    	}
    }
    
    /**Packs should call this upon load to add their pole components to the mod.**/
    public static void addPoleDefinition(InputStreamReader jsonReader, String jsonFileName, String packID){
    	try{
    		JSONPoleComponent definition = packParser.fromJson(jsonReader, JSONPoleComponent.class);
    		performLegacyCompats(definition);
	    	setupItem(definition.general.type.equals("core") ? new ItemPole(definition) : new ItemPoleComponent(definition), jsonFileName, packID, ItemClassification.POLE);
    	}catch(Exception e){
    		logEntries.add("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + packID + ":" + jsonFileName);
    		logEntries.add(e.getMessage());
    	}
    }
    
    /**Packs should call this upon load to add their decor blocks to the mod.**/
    public static void addDecorDefinition(InputStreamReader jsonReader, String jsonFileName, String packID){
    	try{
    		JSONDecor definition = packParser.fromJson(jsonReader, JSONDecor.class);
    		performLegacyCompats(definition);
    		setupItem(new ItemDecor(definition), jsonFileName, packID, ItemClassification.DECOR);
    	}catch(Exception e){
    		logEntries.add("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + packID + ":" + jsonFileName);
    		logEntries.add(e.getMessage());
    	}
    }
    
    /**Packs should call this upon load to add their crafting items to the mod.**/
    public static void addItemDefinition(InputStreamReader jsonReader, String jsonFileName, String packID){
    	try{
	    	setupItem(new ItemItem(packParser.fromJson(jsonReader, JSONItem.class)), jsonFileName, packID, ItemClassification.ITEM);
    	}catch(Exception e){
    		logEntries.add("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + packID + ":" + jsonFileName);
    		logEntries.add(e.getMessage());
    	}
    }
    
    /**Packs should call this upon load to add their booklets to the mod.**/
    public static void addBookletDefinition(InputStreamReader jsonReader, String jsonFileName, String packID){
    	try{
    		setupItem(new ItemBooklet(packParser.fromJson(jsonReader, JSONBooklet.class)), jsonFileName, packID, ItemClassification.BOOKLET);
    	}catch(Exception e){
    		logEntries.add("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + packID + ":" + jsonFileName);
    		logEntries.add(e.getMessage());
    	}
    }
    
    /**Sets up the item in the system. Item must be created prior to this as we can't use generics for instantiation.**/
    public static <ItemInstance extends AItemPack<? extends AJSONItem<?>>> void setupItem(AItemPack<? extends AJSONItem<?>> item, String systemName, String packID, ItemClassification classification){
    	//Set code-based definition values.
    	item.definition.packID = packID;
    	item.definition.classification = classification;
    	item.definition.systemName = systemName;
    	
    	//Add the item to the builder system.
    	BuilderItem.createItem(item);
    	
    	//Put the item in the map in the registry.
    	if(!packItemMap.containsKey(packID)){
    		packItemMap.put(packID, new LinkedHashMap<String, AItemPack<? extends AJSONItem<? extends AJSONItem<?>.General>>>());
    	}
    	packItemMap.get(packID).put(item.definition.systemName, item);
    }
    
    /**
     * Perform legacy compats.  This is used to allow older packs to remain compatible.
     * Legacy compats may be removed ONLY when older packs have updated!
     * 
     */
    private static <JSONDefinition extends AJSONItem<?>> void performLegacyCompats(JSONDefinition definition){
    	if(definition instanceof JSONPart){
    		JSONPart partDef = (JSONPart) definition;
    		if(partDef.engine != null){
    			//If we are an engine_jet part, and our jetPowerFactor is 0, we are a legacy jet engine.
    			if(partDef.general.type.equals("engine_jet") && partDef.engine.jetPowerFactor == 0){
    				partDef.engine.jetPowerFactor = 1.0F;
    				partDef.engine.bypassRatio = partDef.engine.gearRatios[0];
    				partDef.engine.gearRatios[0] = 1.0F;
    			}
    			
    			//If we only have one gearRatio, add two more gears as we're a legacy propeller-based engine.
    			if(partDef.engine.gearRatios.length == 1){
    				partDef.engine.propellerRatio = 1/partDef.engine.gearRatios[0];
    				partDef.engine.gearRatios = new float[]{-1, 0, 1};
    			}
    			
    			//If our shiftSpeed is 0, we are a legacy engine that didn't set a shift speed.
    			if(partDef.engine.shiftSpeed == 0){
    				partDef.engine.shiftSpeed = 20;
    			}
    		}else if(partDef.gun != null){
    			//Make sure turrets are set as turrets.
    			if(partDef.general.type.equals("gun_turret")){
    				partDef.gun.isTurret = true;
    			}
    		}else{
    			//Check for old ground devices, crates, barrels, and effectors.
    			switch(partDef.general.type){
    				case("wheel"):{
    					partDef.general.type = "ground_" + partDef.general.type;
    					partDef.ground = partDef.new JSONPartGroundDevice();
    					partDef.ground.isWheel = true;
    					partDef.ground.width = partDef.wheel.diameter/2F;
    					partDef.ground.height = partDef.wheel.diameter;
    					partDef.ground.lateralFriction = partDef.wheel.lateralFriction;
    					partDef.ground.motiveFriction = partDef.wheel.motiveFriction;
    					break;
    				}case("skid"):{
    					partDef.general.type = "ground_" + partDef.general.type;
    					partDef.ground = partDef.new JSONPartGroundDevice();
    					partDef.ground.width = partDef.skid.width;
    					partDef.ground.height = partDef.skid.width;
    					partDef.ground.lateralFriction = partDef.skid.lateralFriction;
    					break;
    				}case("pontoon"):{
    					partDef.general.type = "ground_" + partDef.general.type;
    					partDef.ground = partDef.new JSONPartGroundDevice();
    					partDef.ground.canFloat = true;
    					partDef.ground.width = partDef.pontoon.width;
    					partDef.ground.height = partDef.pontoon.width;
    					partDef.ground.lateralFriction = partDef.pontoon.lateralFriction;
    					partDef.ground.extraCollisionBoxOffset = partDef.pontoon.extraCollisionBoxOffset;
    					break;
    				}case("tread"):{
    					partDef.general.type = "ground_" + partDef.general.type;
    					partDef.ground = partDef.new JSONPartGroundDevice();
    					partDef.ground.isTread = true;
    					partDef.ground.width = partDef.tread.width;
    					partDef.ground.height = partDef.tread.width;
    					partDef.ground.lateralFriction = partDef.tread.lateralFriction;
    					partDef.ground.motiveFriction = partDef.tread.motiveFriction;
    					partDef.ground.extraCollisionBoxOffset = partDef.tread.extraCollisionBoxOffset;
    					partDef.ground.spacing = partDef.tread.spacing;
    					break;
    				}case("crate"):{
    					partDef.general.type = "interactable_crate";
    					partDef.interactable = partDef.new JSONPartInteractable();
    					partDef.interactable.interactionType = "crate";
    					partDef.interactable.inventoryUnits = 1;
    					partDef.interactable.feedsVehicles = true;
    					break;
    				}case("barrel"):{
    					partDef.general.type = "interactable_barrel";
    					partDef.interactable = partDef.new JSONPartInteractable();
    					partDef.interactable.interactionType = "barrel";
    					partDef.interactable.inventoryUnits = 1;
    					break;
    				}case("crafting_table"):{
    					partDef.general.type = "interactable_crafting_table";
    					partDef.interactable = partDef.new JSONPartInteractable();
    					partDef.interactable.interactionType = "crafting_table";
    					break;
    				}case("furnace"):{
    					partDef.general.type = "interactable_furnace";
    					partDef.interactable = partDef.new JSONPartInteractable();
    					partDef.interactable.interactionType = "furnace";
    					break;
    				}case("brewing_stand"):{
    					partDef.general.type = "interactable_brewing_stand";
    					partDef.interactable = partDef.new JSONPartInteractable();
    					partDef.interactable.interactionType = "brewing_stand";
    					break;
    				}case("fertilizer"):{
    					partDef.general.type = "effector_fertilizer";
    					partDef.effector = partDef.new JSONPartEffector();
    					partDef.effector.type = "fertilizer";
    					partDef.effector.blocksWide = 1;
    					break;
    				}case("harvester"):{
    					partDef.general.type = "effector_harvester";
    					partDef.effector = partDef.new JSONPartEffector();
    					partDef.effector.type = "harvester";
    					partDef.effector.blocksWide = 1;
    					break;
    				}case("planter"):{
    					partDef.general.type = "effector_planter";
    					partDef.effector = partDef.new JSONPartEffector();
    					partDef.effector.type = "planter";
    					partDef.effector.blocksWide = 1;
    					break;
    				}case("plow"):{
    					partDef.general.type = "effector_plow";
    					partDef.effector = partDef.new JSONPartEffector();
    					partDef.effector.type = "plow";
    					partDef.effector.blocksWide = 1;
    					break;
    				}
    			}
    		}
    		
    		if(partDef.subParts != null){
	    		//Check all part slots for ground device names and update them.
	    		//Also check if we define an additional part, and make it a list instead.
	    		//Finally, switch all crates and barrels to effectors with the appropriate type.
	    		for(VehiclePart subPartDef : partDef.subParts){
	    			if(subPartDef.additionalPart != null){
	    				subPartDef.additionalParts = new ArrayList<VehiclePart>();
	    				subPartDef.additionalParts.add(subPartDef.additionalPart);
	    			}
	    			for(byte i=0; i<subPartDef.types.size(); ++i){
	    				String subPartName = subPartDef.types.get(i);
	    				if(subPartName.equals("wheel") || subPartName.equals("skid") || subPartName.equals("pontoon") || subPartName.equals("tread")){
	    					if(subPartName.equals("tread")){
	    						subPartDef.turnsWithSteer = true;
	    					}
	    					subPartDef.types.set(i, "ground_" + subPartName);
	    				}else if(subPartName.equals("crate") || subPartName.equals("barrel") || subPartName.equals("crafting_table") || subPartName.equals("furnace") || subPartName.equals("brewing_stand")){
	    					subPartDef.types.set(i, "interactable_" + subPartName);
	    				}else if(subPartName.equals("fertilizer") || subPartName.equals("harvester") || subPartName.equals("planter") || subPartName.equals("plow")){
	    					subPartDef.types.set(i, "effector_" + subPartName);
	    				}
	    				//If we have additional parts, check those too.
	    				if(subPartDef.additionalParts != null){
	    					for(VehiclePart additionalPart : subPartDef.additionalParts){
		    					for(byte j=0; j<additionalPart.types.size(); ++j){
		    	    				subPartName = additionalPart.types.get(j);
		    	    				if(subPartName.equals("wheel") || subPartName.equals("skid") || subPartName.equals("pontoon") || subPartName.equals("tread")){
		    	    					if(subPartName.equals("tread")){
		    	    						additionalPart.turnsWithSteer = true;
		    	    					}
		    	    					additionalPart.types.set(j, "ground_" + subPartName);
		    	    				}else if(subPartName.equals("crate") || subPartName.equals("barrel") || subPartName.equals("crafting_table") || subPartName.equals("furnace") || subPartName.equals("brewing_stand")){
		    	    					subPartDef.types.set(i, "interactable_" + subPartName);
		    	    				}else if(subPartName.equals("fertilizer") || subPartName.equals("harvester") || subPartName.equals("planter") || subPartName.equals("plow")){
		    	    					subPartDef.types.set(i, "effector_" + subPartName);
		    	    				}
		    	    			}
	    					}
	    				}
	    			}
	    		}
    		}
    		
    		if(partDef.rendering != null){
    			doAnimationLegacyCompats(partDef.rendering, new JSONVehicle());
    		}
    	}else if(definition instanceof JSONVehicle){
    		JSONVehicle vehicleDef = (JSONVehicle) definition;
    		//Move vehicle parameters to the motorized section.
    		if(vehicleDef.car != null){
    			vehicleDef.motorized.isBigTruck = vehicleDef.car.isBigTruck;
    			vehicleDef.motorized.isFrontWheelDrive = vehicleDef.car.isFrontWheelDrive;
    			vehicleDef.motorized.isRearWheelDrive = vehicleDef.car.isRearWheelDrive;
    			vehicleDef.motorized.hasCruiseControl = vehicleDef.car.hasCruiseControl;
    			vehicleDef.motorized.axleRatio = vehicleDef.car.axleRatio;
    			vehicleDef.motorized.dragCoefficient = vehicleDef.car.dragCoefficient;
    			vehicleDef.car = null;
    		}
    		
    		//If we still have the old type parameter and are an aircraft, set the flag to true.
    		if(vehicleDef.general.type != null){
    			if(vehicleDef.general.type.equals("plane") || vehicleDef.general.type.equals("blimp") || vehicleDef.general.type.equals("helicopter")){
    				vehicleDef.general.isAircraft = true;
    			}
    		}
    		
    		if(vehicleDef.plane != null){
    			vehicleDef.general.isAircraft = true;
    			vehicleDef.motorized.hasFlaps = vehicleDef.plane.hasFlaps;
    			vehicleDef.motorized.hasAutopilot = vehicleDef.plane.hasAutopilot;
    			vehicleDef.motorized.wingSpan = vehicleDef.plane.wingSpan;
    			vehicleDef.motorized.wingArea = vehicleDef.plane.wingArea;
    			vehicleDef.motorized.tailDistance = vehicleDef.plane.tailDistance;
    			vehicleDef.motorized.aileronArea = vehicleDef.plane.aileronArea;
    			vehicleDef.motorized.elevatorArea = vehicleDef.plane.elevatorArea;
    			vehicleDef.motorized.rudderArea = vehicleDef.plane.rudderArea;
    			vehicleDef.plane = null;
    			
    			//If aileronArea is 0, we're a legacy plane and need to adjust.
    			if(vehicleDef.motorized.aileronArea == 0){
    				vehicleDef.motorized.aileronArea = vehicleDef.motorized.wingArea/5F;
    			}
    		}
    		
    		if(vehicleDef.blimp != null){
    			vehicleDef.general.isAircraft = true;
    			vehicleDef.general.isBlimp = true;
    			vehicleDef.motorized.crossSectionalArea = vehicleDef.blimp.crossSectionalArea;
    			vehicleDef.motorized.tailDistance = vehicleDef.blimp.tailDistance;
    			vehicleDef.motorized.rudderArea = vehicleDef.blimp.rudderArea;
    			vehicleDef.motorized.ballastVolume = vehicleDef.blimp.ballastVolume;
    			vehicleDef.blimp = null;
    		}
    		
    		//Check all part slots for ground device names and update them.
    		//Also check if we define an additional part, and make it a list instead.
    		//Finally, switch all crates and barrels to effectors with the appropriate type.
    		for(VehiclePart part : vehicleDef.parts){
    			if(part.additionalPart != null){
    				part.additionalParts = new ArrayList<VehiclePart>();
    				part.additionalParts.add(part.additionalPart);
    			}
    			if(part.exhaustPos != null){
    				part.exhaustObjects = new ArrayList<ExhaustObject>();
    				for(int i=0; i<part.exhaustPos.length; i+=3){
    					ExhaustObject exhaust = part.new ExhaustObject();
    					exhaust.pos = new Point3d(part.exhaustPos[i], part.exhaustPos[i+1], part.exhaustPos[i+2]);
    					exhaust.velocity = new Point3d(part.exhaustVelocity[i], part.exhaustVelocity[i+1], part.exhaustVelocity[i+2]);
    					exhaust.scale = 1.0F;
    					part.exhaustObjects.add(exhaust);
    				}
    			}
    			for(byte i=0; i<part.types.size(); ++i){
    				String partName = part.types.get(i);
    				if(partName.equals("wheel") || partName.equals("skid") || partName.equals("pontoon") || partName.equals("tread")){
    					if(partName.equals("tread")){
    						part.turnsWithSteer = true;
    					}
    					part.types.set(i, "ground_" + partName);
    				}else if(partName.equals("crate") || partName.equals("barrel") || partName.equals("crafting_table") || partName.equals("furnace") || partName.equals("brewing_stand")){
    					part.types.set(i, "interactable_" + partName);
    				}else if(partName.equals("fertilizer") || partName.equals("harvester") || partName.equals("planter") || partName.equals("plow")){
    					part.types.set(i, "effector_" + partName);
    				}
    				//If we have additional parts, check those too.
    				if(part.additionalParts != null){
    					for(VehiclePart additionalPart : part.additionalParts){
	    					for(byte j=0; j<additionalPart.types.size(); ++j){
	    	    				partName = additionalPart.types.get(j);
	    	    				if(partName.equals("wheel") || partName.equals("skid") || partName.equals("pontoon") || partName.equals("tread")){
	    	    					if(partName.equals("tread")){
	    	    						additionalPart.turnsWithSteer = true;
	    	    					}
	    	    					additionalPart.types.set(j, "ground_" + partName);
	    	    				}else if(partName.equals("crate") || partName.equals("barrel") || partName.equals("crafting_table") || partName.equals("furnace") || partName.equals("brewing_stand")){
	    	    					additionalPart.types.set(i, "interactable_" + partName);
	    	    				}else if(partName.equals("fertilizer") || partName.equals("harvester") || partName.equals("planter") || partName.equals("plow")){
	    	    					additionalPart.types.set(i, "effector_" + partName);
	    	    				}
	    	    			}
    					}
    				}
    			}
    		}
    	}else if(definition instanceof JSONPoleComponent){
    		JSONPoleComponent pole = (JSONPoleComponent) definition;
    		//If we are a sign using the old textlines, update them.
    		if(pole.general.textLines != null){
    			pole.general.textObjects = new ArrayList<JSONText>();
    			for(minecrafttransportsimulator.jsondefs.JSONPoleComponent.TextLine line : pole.general.textLines){
    				JSONText object = new JSONText();
    				object.color = line.color;
    				object.scale = line.scale;
    				object.maxLength = line.characters;
    				object.pos = new Point3d(line.xPos, line.yPos, line.zPos + 0.01D);
    				object.rot = new Point3d(0, 0, 0);
    				object.fieldName = "TextLine #" + (pole.general.textObjects.size() + 1);
    				pole.general.textObjects.add(object);
    			}
    			pole.general.textLines = null;
    		}
    	}else if(definition instanceof JSONDecor){
    		JSONDecor decor = (JSONDecor) definition;
    		//If we are a decor using the old textlines, update them.
    		if(decor.general.textLines != null){
    			decor.general.textObjects = new ArrayList<JSONText>();
    			int lineNumber = 0;
    			for(minecrafttransportsimulator.jsondefs.JSONDecor.TextLine line : decor.general.textLines){
    				JSONText object = new JSONText();
    				object.lightsUp = true;
    				object.color = line.color;
    				object.scale = line.scale;
    				if(lineNumber++ < 3){
    					object.pos = new Point3d(line.xPos, line.yPos, line.zPos + 0.0001D);
    					object.rot = new Point3d(0, 0, 0);
    				}else{
    					object.pos = new Point3d(line.xPos, line.yPos, line.zPos - 0.0001D);
    					object.rot = new Point3d(0, 180, 0);
    				}
    				object.fieldName = "TextLine #" + (decor.general.textObjects.size() + 1);
    				decor.general.textObjects.add(object);
    			}
    			 decor.general.textLines = null;
    		}
    	}
    }
    
    private static void doAnimationLegacyCompats(VehicleRendering rendering, JSONVehicle jsonInstance){
    	if(rendering.textMarkings != null){
    		rendering.textObjects = new ArrayList<JSONText>();
    		for(minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleDisplayText marking : rendering.textMarkings){
				JSONText object = new JSONText();
				object.lightsUp = rendering.textLighted;
				object.color = marking.color;
				object.scale = marking.scale;
				object.maxLength = rendering.displayTextMaxLength;
				object.pos = marking.pos;
				object.rot = marking.rot;
				object.fieldName = "Text";
				object.defaultText = rendering.defaultDisplayText;
				rendering.textObjects.add(object);
			}
    		rendering.textMarkings = null;
    	}
    	if(rendering.rotatableModelObjects != null){
    		if(rendering.animatedObjects == null){
    			rendering.animatedObjects = new ArrayList<VehicleAnimatedObject>();
    		}
    		for(minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleRotatableModelObject rotatable : rendering.rotatableModelObjects){
    			VehicleAnimatedObject object = null;
    			for(VehicleAnimatedObject testObject : rendering.animatedObjects){
    				if(testObject.objectName.equals(rotatable.partName)){
    					object = testObject;
    					break;
    				}
    			}
    			if(object == null){
    				object = jsonInstance.new VehicleAnimatedObject();
    				object.objectName = rotatable.partName;
    				object.animations = new ArrayList<VehicleAnimationDefinition>();
    				rendering.animatedObjects.add(object);
    			}
    			
    			VehicleAnimationDefinition animation = jsonInstance.new VehicleAnimationDefinition();
    			animation.animationType = "rotation";
    	    	animation.variable = rotatable.rotationVariable;
    	    	animation.centerPoint = rotatable.rotationPoint;
    	    	animation.axis = rotatable.rotationAxis;
    	    	animation.clampMin = rotatable.rotationClampMin;
    	    	animation.clampMax = rotatable.rotationClampMax;
    	    	animation.absolute = rotatable.absoluteValue;
    	    	if(rotatable.rotationVariable.equals("steering_wheel")){
    	    		animation.variable = "rudder";
    	    		animation.axis.multiply(-1D);
    	    	}
    	    	if(rotatable.rotationVariable.equals("door")){
    	    		animation.duration = 30;
    	    	}
    	    	object.animations.add(animation);
			}
    		rendering.rotatableModelObjects = null;
    	}
    	if(rendering.translatableModelObjects != null){
    		if(rendering.animatedObjects == null){
    			rendering.animatedObjects = new ArrayList<VehicleAnimatedObject>();
    		}
    		for(minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleTranslatableModelObject translatable : rendering.translatableModelObjects){
    			VehicleAnimatedObject object = null;
    			for(VehicleAnimatedObject testObject : rendering.animatedObjects){
    				if(testObject.objectName.equals(translatable.partName)){
    					object = testObject;
    					break;
    				}
    			}
    			if(object == null){
    				object = jsonInstance.new VehicleAnimatedObject();
    				object.objectName = translatable.partName;
    				object.animations = new ArrayList<VehicleAnimationDefinition>();
    				rendering.animatedObjects.add(object);
    			}
    			
    			VehicleAnimationDefinition animation = jsonInstance.new VehicleAnimationDefinition();
    			animation.animationType = "translation";
    	    	animation.variable = translatable.translationVariable;
    	    	animation.axis = translatable.translationAxis;
    	    	animation.clampMin = translatable.translationClampMin;
    	    	animation.clampMax = translatable.translationClampMax;
    	    	animation.absolute = translatable.absoluteValue;
    	    	if(translatable.translationVariable.equals("steering_wheel")){
    	    		animation.variable = "rudder";
    	    		animation.axis.multiply(-1D);
    	    	}
    	    	if(translatable.translationVariable.equals("door")){
    	    		animation.duration = 30;
    	    	}
    	    	object.animations.add(animation);
			}
    		rendering.translatableModelObjects = null;
    	}
    }
    
    //--------------------START OF HELPER METHODS--------------------	
    public static <JSONDefinition extends AJSONItem<?>> JSONDefinition getDefinition(String packID, String systemName){
    	if(packItemMap.containsKey(packID)){
    		if(packItemMap.get(packID).containsKey(systemName)){
    			return (JSONDefinition) packItemMap.get(packID).get(systemName).definition;
    		}
    	}
    	return null;
    }
    
    public static <PackItem extends AItemPack<JSONDefinition>, JSONDefinition extends AJSONItem<?>> PackItem getItem(JSONDefinition definition){
    	return getItem(definition.packID, definition.systemName);
    }
    
    public static <PackItem extends AItemPack<JSONDefinition>, JSONDefinition extends AJSONItem<?>> PackItem getItem(String packID, String systemName){
    	return getItem(packID, systemName, null);
    }
    
    public static <PackItem extends AItemPack<JSONDefinition>, JSONDefinition extends AJSONItem<?>> PackItem getItem(String packID, String systemName, String subName){
    	if(packItemMap.containsKey(packID)){
    		return (PackItem) packItemMap.get(packID).get(systemName);
    	}
    	return null;
    }
    
    public static boolean arePacksPresent(){
    	//We always have 1 pack: the core pack.
    	return packItemMap.size() > 1;
    }
    
    public static Set<String> getAllPackIDs(){
    	return packItemMap.keySet();
    }
    
    public static List<AItemPack<?>> getAllItemsForPack(String packID){
    	return new ArrayList<AItemPack<?>>(packItemMap.get(packID).values());
    }
    
    public static List<AItemPack<?>> getAllPackItems(){
    	List<AItemPack<?>> packItems = new ArrayList<AItemPack<?>>();
    	for(String packID : packItemMap.keySet()){
    		packItems.addAll(getAllItemsForPack(packID));
    	}
    	return packItems;
    }
    
    public static APart createPart(EntityVehicleF_Physics vehicle, VehiclePart packVehicleDef, JSONPart definition, WrapperNBT partData, APart parentPart){
    	if(definition.general.type.startsWith("engine_")){
    		return new PartEngine(vehicle, packVehicleDef, definition, partData, parentPart);
    	}else if(definition.general.type.startsWith("gun_")){
    		return new PartGun(vehicle, packVehicleDef, definition, partData, parentPart);
    	}else if(definition.general.type.startsWith("ground_")){
    		return new PartGroundDevice(vehicle, packVehicleDef, definition, partData, parentPart);
    	}else if(definition.general.type.startsWith("interactable_")){
        	return new PartInteractable(vehicle, packVehicleDef, definition, partData, parentPart);
    	}else if(definition.general.type.startsWith("effector_")){
           	return new PartGroundEffector(vehicle, packVehicleDef, definition, partData, parentPart);
    	}else{
	    	switch(definition.general.type){
				case "propeller": return new PartPropeller(vehicle, packVehicleDef, definition, partData, parentPart);
				case "seat": return new PartSeat(vehicle, packVehicleDef, definition, partData, parentPart);
				//Note that this case is invalid, as bullets are NOT parts that can be placed on vehicles.
				//Rather, they are items that get loaded into the gun, so they never actually become parts themselves.
				//case "bullet": return new PartBullet(vehicle, packVehicleDef, definition, partData, parentPart);
				case "custom": return new PartCustom(vehicle, packVehicleDef, definition, partData, parentPart);
			}
    	}
    	throw new IllegalArgumentException(definition.general.type + " is not a valid type for creating a part.");
    }
    
    public enum ItemClassification{
    	VEHICLE,
    	PART,
    	INSTRUMENT,
    	POLE,
    	DECOR,
    	ITEM,
    	BOOKLET;
    	
    	public final String assetFolder;
    	
    	private ItemClassification(){
    		this.assetFolder = this.name().toLowerCase() + "s";
    	}
    	
    	public static List<String> getAllTypesAsStrings(){
        	List<String> assetTypes = new ArrayList<String>();
        	for(ItemClassification classification : ItemClassification.values()){
        		assetTypes.add(classification.name().toLowerCase());
        	}
        	return assetTypes;
    	}
    }
}
