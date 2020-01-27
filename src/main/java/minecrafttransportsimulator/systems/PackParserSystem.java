package minecrafttransportsimulator.systems;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import minecrafttransportsimulator.dataclasses.CreativeTabPack;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.packs.AItemPack;
import minecrafttransportsimulator.items.packs.ItemBooklet;
import minecrafttransportsimulator.items.packs.ItemDecor;
import minecrafttransportsimulator.items.packs.ItemInstrument;
import minecrafttransportsimulator.items.packs.ItemItem;
import minecrafttransportsimulator.items.packs.ItemVehicle;
import minecrafttransportsimulator.items.packs.parts.AItemPart;
import minecrafttransportsimulator.items.packs.parts.ItemPartBarrel;
import minecrafttransportsimulator.items.packs.parts.ItemPartBullet;
import minecrafttransportsimulator.items.packs.parts.ItemPartCrate;
import minecrafttransportsimulator.items.packs.parts.ItemPartCustom;
import minecrafttransportsimulator.items.packs.parts.ItemPartEngineAircraft;
import minecrafttransportsimulator.items.packs.parts.ItemPartEngineBoat;
import minecrafttransportsimulator.items.packs.parts.ItemPartEngineCar;
import minecrafttransportsimulator.items.packs.parts.ItemPartEngineJet;
import minecrafttransportsimulator.items.packs.parts.ItemPartGeneric;
import minecrafttransportsimulator.items.packs.parts.ItemPartGroundDevicePontoon;
import minecrafttransportsimulator.items.packs.parts.ItemPartGroundDeviceSkid;
import minecrafttransportsimulator.items.packs.parts.ItemPartGroundDeviceTread;
import minecrafttransportsimulator.items.packs.parts.ItemPartGroundDeviceWheel;
import minecrafttransportsimulator.items.packs.parts.ItemPartGun;
import minecrafttransportsimulator.items.packs.parts.ItemPartPropeller;
import minecrafttransportsimulator.jsondefs.AJSONCraftable;
import minecrafttransportsimulator.jsondefs.JSONBooklet;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.jsondefs.JSONInstrument;
import minecrafttransportsimulator.jsondefs.JSONItem;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONSign;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleDefinition;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Air;
import minecrafttransportsimulator.vehicles.main.EntityVehicleG_Blimp;
import minecrafttransportsimulator.vehicles.main.EntityVehicleG_Boat;
import minecrafttransportsimulator.vehicles.main.EntityVehicleG_Car;
import minecrafttransportsimulator.vehicles.main.EntityVehicleG_Plane;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartBarrel;
import minecrafttransportsimulator.vehicles.parts.PartBrewingStand;
import minecrafttransportsimulator.vehicles.parts.PartCraftingTable;
import minecrafttransportsimulator.vehicles.parts.PartCrate;
import minecrafttransportsimulator.vehicles.parts.PartCustom;
import minecrafttransportsimulator.vehicles.parts.PartEngineAircraft;
import minecrafttransportsimulator.vehicles.parts.PartEngineBoat;
import minecrafttransportsimulator.vehicles.parts.PartEngineCar;
import minecrafttransportsimulator.vehicles.parts.PartEngineJet;
import minecrafttransportsimulator.vehicles.parts.PartFertilizer;
import minecrafttransportsimulator.vehicles.parts.PartFurnace;
import minecrafttransportsimulator.vehicles.parts.PartGroundDevicePontoon;
import minecrafttransportsimulator.vehicles.parts.PartGroundDeviceSkid;
import minecrafttransportsimulator.vehicles.parts.PartGroundDeviceTread;
import minecrafttransportsimulator.vehicles.parts.PartGroundDeviceWheel;
import minecrafttransportsimulator.vehicles.parts.PartGunFixed;
import minecrafttransportsimulator.vehicles.parts.PartGunTripod;
import minecrafttransportsimulator.vehicles.parts.PartGunTurret;
import minecrafttransportsimulator.vehicles.parts.PartHarvester;
import minecrafttransportsimulator.vehicles.parts.PartPlanter;
import minecrafttransportsimulator.vehicles.parts.PartPlow;
import minecrafttransportsimulator.vehicles.parts.PartPropeller;
import minecrafttransportsimulator.vehicles.parts.PartSeat;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**
 * Class responsible for parsing content pack data.  Gets properties from the text files that other parts
 * of the code can use.
 *
 * @author don_bruce
 */
public final class PackParserSystem{
    
    /**Map that keys the unique name of a sign to its pack.*/
    private static final Map<String, JSONSign> signPackMap = new LinkedHashMap<String, JSONSign>();
    
    
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
    		JSONVehicle mainDefinition = new Gson().fromJson(jsonReader, JSONVehicle.class);
    		mainDefinition.genericName = jsonFileName;
    		for(VehicleDefinition subDefinition : mainDefinition.definitions){
    			//Need to copy the JSON into a new instance to allow differing systemNames.
    			JSONVehicle mainDefinitionCopy = new JSONVehicle();
    			mainDefinitionCopy.packID = mainDefinition.packID;
    			mainDefinitionCopy.classification = mainDefinition.classification;
    			mainDefinitionCopy.genericName = mainDefinition.genericName;
    			mainDefinitionCopy.general = mainDefinition.general;
    			mainDefinitionCopy.definitions = mainDefinition.definitions;
    			mainDefinitionCopy.motorized = mainDefinition.motorized;
    			mainDefinitionCopy.plane = mainDefinition.plane;
    			mainDefinitionCopy.blimp = mainDefinition.blimp;
    			mainDefinitionCopy.car = mainDefinition.car;
    			mainDefinitionCopy.parts = mainDefinition.parts;
    			mainDefinitionCopy.collision = mainDefinition.collision;
    			mainDefinitionCopy.rendering = mainDefinition.rendering;
    			
    			ItemVehicle vehicle = new ItemVehicle(mainDefinitionCopy, subDefinition.subName);
    			setupItem(vehicle, jsonFileName + subDefinition.subName, packID, ItemClassification.VEHICLE);
    			List<String> materials = new ArrayList<String>();
				for(String material : mainDefinitionCopy.general.materials){
					materials.add(material);
				}
				for(String material : subDefinition.extraMaterials){
					materials.add(material);
				}
				//Need to set this again to account for the extraMaterials.
				MTSRegistry.packCraftingMap.put(vehicle, materials.toArray(new String[materials.size()]));
    		}
    		
    	}catch(Exception e){
    		System.err.println("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + packID + ":" + jsonFileName);
    		e.printStackTrace();
    	}
    }
    
    /**Packs should call this upon load to add their parts to the mod.**/
    public static void addPartDefinition(InputStreamReader jsonReader, String jsonFileName, String packID){
    	try{
    		setupItem(createPartItem(new Gson().fromJson(jsonReader, JSONPart.class)), jsonFileName, packID, ItemClassification.PART);
    	}catch(Exception e){
    		System.err.println("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + packID + ":" + jsonFileName);
    		e.printStackTrace();
    	}
    }
    
    /**Packs should call this upon load to add their instrument set to the mod.**/
    public static void addInstrumentDefinition(InputStreamReader jsonReader, String jsonFileName, String packID){
    	try{
    		setupItem(new ItemInstrument(new Gson().fromJson(jsonReader, JSONInstrument.class)), jsonFileName, packID, ItemClassification.INSTRUMENT);
    	}catch(Exception e){
    		System.err.println("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + packID + ":" + jsonFileName);
    		e.printStackTrace();
    	}
    }
    
    /**Packs should call this upon load to add their signs to the mod.**/
    public static void addSignDefinition(InputStreamReader jsonReader, String jsonFileName, String packID){
    	try{
    		//Signs are a special-case as they don't have items or crafting materials.
	    	//Instead, they are just definitions that sit by themselves.
    		JSONSign definition =  new Gson().fromJson(jsonReader, JSONSign.class);
	    	definition.packID = packID;
	    	definition.classification = ItemClassification.SIGN;
	    	definition.systemName = jsonFileName;
	    	
	    	if(!MTSRegistry.packSignMap.containsKey(packID)){
	    		MTSRegistry.packSignMap.put(packID, new LinkedHashMap<String, JSONSign>());
	    	}
	    	MTSRegistry.packSignMap.get(packID).put(jsonFileName, definition);
    	}catch(Exception e){
    		System.err.println("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + packID + ":" + jsonFileName);
    		e.printStackTrace();
    	}
    }
    
    /**Packs should call this upon load to add their decor blocks to the mod.**/
    public static void addDecorDefinition(InputStreamReader jsonReader, String jsonFileName, String packID){
    	try{
    		setupItem(new ItemDecor(new Gson().fromJson(jsonReader, JSONDecor.class)), jsonFileName, packID, ItemClassification.DECOR);
    	}catch(Exception e){
    		System.err.println("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + packID + ":" + jsonFileName);
    		e.printStackTrace();
    	}
    }
    
    /**Packs should call this upon load to add their crafting items to the mod.**/
    public static void addItemDefinition(InputStreamReader jsonReader, String jsonFileName, String packID){
    	try{
	    	setupItem(new ItemItem(new Gson().fromJson(jsonReader, JSONItem.class)), jsonFileName, packID, ItemClassification.ITEM);
    	}catch(Exception e){
    		System.err.println("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + packID + ":" + jsonFileName);
    		e.printStackTrace();
    	}
    }
    
    /**Packs should call this upon load to add their booklets to the mod.**/
    public static void addBookletDefinition(InputStreamReader jsonReader, String jsonFileName, String packID){
    	try{
    		setupItem(new ItemBooklet(new Gson().fromJson(jsonReader, JSONBooklet.class)), jsonFileName, packID, ItemClassification.BOOKLET);
    	}catch(Exception e){
    		System.err.println("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + packID + ":" + jsonFileName);
    		e.printStackTrace();
    	}
    }
    
    /**Sets up the item in the system. Item must be created prior to this as we can't use generics for instantiation.**/
    public static <ItemInstance extends AItemPack> void setupItem(AItemPack item, String systemName, String packID, ItemClassification classification){
    	//Set code-based definition values.
    	item.definition.packID = packID;
    	item.definition.classification = classification;
    	item.definition.systemName = systemName;
    	
		//Set the unlocalized name.  The packs use this to register the items on their side,
    	//so the format needs to be standard.
		item.setUnlocalizedName(packID + "." + systemName);
    	
    	//Put the item in the map in the registry.
    	if(!MTSRegistry.packItemMap.containsKey(packID)){
    		MTSRegistry.packItemMap.put(packID, new LinkedHashMap<String, AItemPack>());
    	}
    	MTSRegistry.packItemMap.get(packID).put(item.definition.systemName, item);
    	
    	//If we are craftable, put us in the crafting map.
    	if(item.definition.general instanceof AJSONCraftable.General){
    		MTSRegistry.packCraftingMap.put(item, ((AJSONCraftable.General) item.definition.general).materials);
    	}
    	
    	//Set the creative tab.  Need to check if we're an internal item or not.
    	if(item.definition.packID.equals("mts")){
    		item.setCreativeTab(MTSRegistry.coreTab);
		}else{
			if(!MTSRegistry.packTabs.containsKey(packID)){
				MTSRegistry.packTabs.put(packID, new CreativeTabPack(packID));
			}
			item.setCreativeTab(MTSRegistry.packTabs.get(packID));
		}
    }
    
    public static EntityVehicleE_Powered createVehicle(World world, float posX, float posY, float posZ, float playerRotation, JSONVehicle definition, String subName){
    	switch(definition.general.type){
			case "plane": return new EntityVehicleG_Plane(world, posX, posY, posZ, playerRotation, definition);
			case "car": return new EntityVehicleG_Car(world, posX, posY, posZ, playerRotation, definition);
			case "blimp": return new EntityVehicleG_Blimp(world, posX, posY, posZ, playerRotation, definition);
			case "boat": return new EntityVehicleG_Boat(world, posX, posY, posZ, playerRotation, definition);
			
			default: throw new IllegalArgumentException(definition.general.type + " is not a valid type for creating a vehicle.");
		}
    }    
    
    public static APart createPart(EntityVehicleE_Powered vehicle, VehiclePart packVehicleDef, JSONPart definition, NBTTagCompound dataTag){
    	switch(definition.general.type){
			case "crate": return new PartCrate(vehicle, packVehicleDef, definition, dataTag);
			case "barrel": return new PartBarrel(vehicle, packVehicleDef, definition, dataTag);
			case "crafting_table": return new PartCraftingTable(vehicle, packVehicleDef, definition, dataTag);
			case "furnace": return new PartFurnace(vehicle, packVehicleDef, definition, dataTag);
			case "brewing_stand": return new PartBrewingStand(vehicle, packVehicleDef, definition, dataTag);
			case "plow": return new PartPlow(vehicle, packVehicleDef, definition, dataTag);
			case "planter": return new PartPlanter(vehicle, packVehicleDef, definition, dataTag);
			case "fertilizer": return new PartFertilizer(vehicle, packVehicleDef, definition, dataTag);
			case "harvester": return new PartHarvester(vehicle, packVehicleDef, definition, dataTag);
			case "engine_aircraft": return new PartEngineAircraft((EntityVehicleF_Air) vehicle, packVehicleDef, definition, dataTag);
			case "engine_jet": return new PartEngineJet((EntityVehicleG_Plane) vehicle, packVehicleDef, definition, dataTag);
			case "engine_car": return new PartEngineCar((EntityVehicleG_Car) vehicle, packVehicleDef, definition, dataTag);
			case "engine_boat": return new PartEngineBoat((EntityVehicleG_Boat) vehicle, packVehicleDef, definition, dataTag);
			case "wheel": return new PartGroundDeviceWheel(vehicle, packVehicleDef, definition, dataTag);
			case "skid": return new PartGroundDeviceSkid(vehicle, packVehicleDef, definition, dataTag);
			case "pontoon": return new PartGroundDevicePontoon(vehicle, packVehicleDef, definition, dataTag);
			case "tread": return new PartGroundDeviceTread(vehicle, packVehicleDef, definition, dataTag);
			case "propeller": return new PartPropeller((EntityVehicleF_Air) vehicle, packVehicleDef, definition, dataTag);
			case "seat": return new PartSeat(vehicle, packVehicleDef, definition, dataTag);
			case "gun_fixed": return new PartGunFixed(vehicle, packVehicleDef, definition, dataTag);
			case "gun_tripod": return new PartGunTripod(vehicle, packVehicleDef, definition, dataTag);
			case "gun_turret": return new PartGunTurret(vehicle, packVehicleDef, definition, dataTag);
			//Note that this case is invalid, as bullets are NOT parts that can be placed on vehicles.
			//Rather, they are items that get loaded into the gun, so they never actually become parts themselves.
			//case "bullet": return PartBullet.class;
			case "custom": return new PartCustom(vehicle, packVehicleDef, definition, dataTag);
			
			default: throw new IllegalArgumentException(definition.general.type + " is not a valid type for creating a part.");
		}
    }
    
    public static AItemPart createPartItem(JSONPart definition){
    	switch(definition.general.type){
	    	case "crate": return new ItemPartCrate(definition);
			case "barrel": return new ItemPartBarrel(definition);
			case "crafting_table": return new ItemPartGeneric(definition);
			case "furnace": return new ItemPartGeneric(definition);
			case "brewing_stand": return new ItemPartGeneric(definition);
			case "plow": return new ItemPartGeneric(definition);
			case "planter": return new ItemPartGeneric(definition);
			case "fertilizer": return new ItemPartGeneric(definition);
			case "harvester": return new ItemPartGeneric(definition);
			case "engine_aircraft": return new ItemPartEngineAircraft(definition);
			case "engine_jet": return new ItemPartEngineJet(definition);
			case "engine_car": return new ItemPartEngineCar(definition);
			case "engine_boat": return new ItemPartEngineBoat(definition);
			case "wheel": return new ItemPartGroundDeviceWheel(definition);
			case "skid": return new ItemPartGroundDeviceSkid(definition);
			case "pontoon": return new ItemPartGroundDevicePontoon(definition);
			case "tread": return new ItemPartGroundDeviceTread(definition);
			case "propeller": return new ItemPartPropeller(definition);
			case "seat": return new ItemPartGeneric(definition);
			case "gun_fixed": return new ItemPartGun(definition);
			case "gun_tripod": return new ItemPartGun(definition);
			case "gun_turret": return new ItemPartGun(definition);
			case "bullet": return new ItemPartBullet(definition);
			case "custom": return new ItemPartCustom(definition);
			
			default: throw new IllegalArgumentException(definition.general.type + " is not a valid type for creating a part item.");
		}
    }
    
    public enum ItemClassification{
    	VEHICLE,
    	PART,
    	INSTRUMENT,
    	SIGN,
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
