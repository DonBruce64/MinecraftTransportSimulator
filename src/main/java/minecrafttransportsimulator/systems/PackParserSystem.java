package minecrafttransportsimulator.systems;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.Gson;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.CreativeTabPack;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.dataclasses.PackDecorObject;
import minecrafttransportsimulator.dataclasses.PackInstrumentObject;
import minecrafttransportsimulator.dataclasses.PackItemObject;
import minecrafttransportsimulator.dataclasses.PackPartObject;
import minecrafttransportsimulator.dataclasses.PackSignObject;
import minecrafttransportsimulator.dataclasses.PackVehicleObject;
import minecrafttransportsimulator.dataclasses.PackVehicleObject.PackFileDefinitions;
import minecrafttransportsimulator.items.parts.AItemPart;
import minecrafttransportsimulator.items.parts.ItemPartBarrel;
import minecrafttransportsimulator.items.parts.ItemPartBullet;
import minecrafttransportsimulator.items.parts.ItemPartCrate;
import minecrafttransportsimulator.items.parts.ItemPartCustom;
import minecrafttransportsimulator.items.parts.ItemPartEngineAircraft;
import minecrafttransportsimulator.items.parts.ItemPartEngineBoat;
import minecrafttransportsimulator.items.parts.ItemPartEngineCar;
import minecrafttransportsimulator.items.parts.ItemPartEngineJet;
import minecrafttransportsimulator.items.parts.ItemPartGeneric;
import minecrafttransportsimulator.items.parts.ItemPartGroundDevicePontoon;
import minecrafttransportsimulator.items.parts.ItemPartGroundDeviceSkid;
import minecrafttransportsimulator.items.parts.ItemPartGroundDeviceTread;
import minecrafttransportsimulator.items.parts.ItemPartGroundDeviceWheel;
import minecrafttransportsimulator.items.parts.ItemPartGun;
import minecrafttransportsimulator.items.parts.ItemPartPropeller;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
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
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * Class responsible for parsing content pack data.  Gets properties from the text files that other parts
 * of the code can use.
 *
 * @author don_bruce
 */
public final class PackParserSystem{
	/**Map that keys the unique name of a vehicle to its pack.  Used for associating packs with saved vehicles.**/
    private static final Map<String, PackVehicleObject> vehiclePackMap = new LinkedHashMap<String, PackVehicleObject>();
   
    /**Map that keys the unique name of a vehicle to its JSON file name.**/
    private static final Map<String, String> vehicleJSONMap = new HashMap<String, String>();
   
    /**Same function as the vehicle map, just for parts.**/
    private static final Map<String, PackPartObject> partPackMap = new LinkedHashMap<String, PackPartObject>();
    
    /**Same function as the vehicle map, just for instruments.**/
    private static final Map<String, PackInstrumentObject> partInstrumentMap = new LinkedHashMap<String, PackInstrumentObject>();
    
    /**Map that keys the unique name of a sign to its pack.*/
    private static final Map<String, PackSignObject> signPackMap = new LinkedHashMap<String, PackSignObject>();
    
    /**Map that keys the unique name of a decor block to its pack.*/
    private static final Map<String, PackDecorObject> decorPackMap = new LinkedHashMap<String, PackDecorObject>();
    
    /**Map that keys the unique name of an item to its pack.*/
    private static final Map<String, PackItemObject> itemPackMap = new LinkedHashMap<String, PackItemObject>();
    
	/**Maps all things craftable on benches to their crafting ingredients.*/
	private static final Map<String, String[]> craftingItemMap = new HashMap<String, String[]>();
  
    /**Listing of log messages.  Stored here on bootstrap and outputted once the logging system comes online.**/
    private static final List<String> logList = new ArrayList<String>();
    
    
    //-----START OF INIT LOGIC-----
    /**Packs should call this upon load to add their content to the mod.
     * This will return an array of strings that correspond to content types.
     * These content types will be content that has items in the jsondefs folder
     * that the pack should send to MTS.  The pack should only send the ResourceLocation
     * of such an item as it will allow MTS to load the information from ResourcePacks in modpacks.
     * This is done to allow server owners to modify pack JSONs to their liking (say for crafting recipes)
     * and distribute them in their modpacks without having to modify the actual pack JSON.**/
    public static String[] getValidPackContentNames(){
    	return new String[]{"vehicle", "part", "instrument", "sign", "decor", "item"};
    }
    
    /**Packs should call this upon load to add their vehicles to the mod.**/
    public static void addVehicleDefinition(InputStreamReader jsonReader, String jsonFileName, String modID){
    	try{
    		PackVehicleObject pack = new Gson().fromJson(jsonReader, PackVehicleObject.class);
    		for(PackFileDefinitions definition : pack.definitions){
    			if(definition != null){
    				String vehicleName = modID + ":" + jsonFileName + definition.subName;
    				vehiclePackMap.put(vehicleName, pack);
    				vehicleJSONMap.put(vehicleName, jsonFileName);
    				if(!MTSRegistry.packTabs.containsKey(modID)){
    					MTSRegistry.packTabs.put(modID, new CreativeTabPack(modID));
    				}
    				
    				List<String> materials = new ArrayList<String>();
    				for(String material : pack.general.materials){
    					materials.add(material);
    				}
    				for(String material : definition.extraMaterials){
    					materials.add(material);
    				}
    				craftingItemMap.put(vehicleName, materials.toArray(new String[materials.size()]));
    			}
    		}
    	}catch(Exception e){
    		logList.add("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + modID + ":" + jsonFileName);
    		logList.add(e.getMessage());
    		e.printStackTrace();
    	}
    }
    
    /**Packs should call this upon load to add their parts to the mod.**/
    public static void addPartDefinition(InputStreamReader jsonReader, String jsonFileName, String modID){
    	try{
	    	PackPartObject pack =  new Gson().fromJson(jsonReader, PackPartObject.class);
	    	String partName = modID + ":" + jsonFileName;
	    	partPackMap.put(partName, pack);
	    	if(!MTSRegistry.packTabs.containsKey(modID)){
				MTSRegistry.packTabs.put(modID, new CreativeTabPack(modID));
			}
	    	craftingItemMap.put(partName, pack.general.materials);
    	}catch(Exception e){
    		logList.add("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + modID + ":" + jsonFileName);
    		logList.add(e.getMessage());
    	}
    }
    
    /**Packs should call this upon load to add their instrument set to the mod.**/
    public static void addInstrumentDefinition(InputStreamReader jsonReader, String jsonFileName, String modID){
    	try{
	    	PackInstrumentObject pack =  new Gson().fromJson(jsonReader, PackInstrumentObject.class);
	    	String instrumentName = modID + ":" + jsonFileName;
    		partInstrumentMap.put(instrumentName, pack);
    		craftingItemMap.put(instrumentName, pack.general.materials);
    	}catch(Exception e){
    		logList.add("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + modID + ":" + jsonFileName);
    		logList.add(e.getMessage());
    	}
    }
    
    /**Packs should call this upon load to add their signs to the mod.**/
    public static void addSignDefinition(InputStreamReader jsonReader, String jsonFileName, String modID){
    	try{
	    	PackSignObject pack =  new Gson().fromJson(jsonReader, PackSignObject.class);
	    	String signName = modID + ":" + jsonFileName;
    		signPackMap.put(signName, pack);
    	}catch(Exception e){
    		logList.add("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + modID + ":" + jsonFileName);
    		logList.add(e.getMessage());
    	}
    }
    
    /**Packs should call this upon load to add their decor blocks to the mod.**/
    public static void addDecorDefinition(InputStreamReader jsonReader, String jsonFileName, String modID){
    	try{
	    	PackDecorObject pack =  new Gson().fromJson(jsonReader, PackDecorObject.class);
	    	String decorName = modID + ":" + jsonFileName;
    		decorPackMap.put(decorName, pack);
    		craftingItemMap.put(decorName, pack.general.materials);
    	}catch(Exception e){
    		logList.add("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + modID + ":" + jsonFileName);
    		logList.add(e.getMessage());
    	}
    }
    
    /**Packs should call this upon load to add their crafting items to the mod.**/
    public static void addItemDefinition(InputStreamReader jsonReader, String jsonFileName, String modID){
    	try{
    		PackItemObject pack =  new Gson().fromJson(jsonReader, PackItemObject.class);
	    	String itemName = modID + ":" + jsonFileName;
	    	itemPackMap.put(itemName, pack);
	    	craftingItemMap.put(itemName, pack.general.materials);
    	}catch(Exception e){
    		logList.add("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + modID + ":" + jsonFileName);
    		logList.add(e.getMessage());
    	}
    }

    public static void outputLog(){
    	for(String logLine : logList){
    		MTS.MTSLog.error(logLine);
    	}
    	logList.clear();
    }
    
    
    //-----START OF RELOAD LOGIC-----
    public static void reloadPackData(){
    	try{
	    	//We need to shove the strings into a list to keep us from getting CMEs while iterating the map.
	    	List<String> jsonFilesToReload = new ArrayList<String>();
	    	for(Entry<String, String> vehicleJSONEntry : vehicleJSONMap.entrySet()){
	    		jsonFilesToReload.add(vehicleJSONEntry.getKey().substring(0, vehicleJSONEntry.getKey().indexOf(':') + 1) + vehicleJSONEntry.getValue());
	    	}
	    	for(String jsonFile : jsonFilesToReload){
	    		ResourceLocation jsonResource = new ResourceLocation(jsonFile.substring(0, jsonFile.indexOf(':')), "jsondefs/vehicles/" + jsonFile.substring(jsonFile.indexOf(':') + 1) + ".json");
	    		addVehicleDefinition(new InputStreamReader(Minecraft.getMinecraft().getResourceManager().getResource(jsonResource).getInputStream()), jsonFile.substring(jsonFile.indexOf(':') + 1), jsonFile.substring(0, jsonFile.indexOf(':')));
	    	}
	    	jsonFilesToReload.clear();
	    	
	    	for(String partJSONFile : partPackMap.keySet()){
	    		jsonFilesToReload.add(partJSONFile);
	    	}
	    	for(String jsonFile : jsonFilesToReload){
	    		ResourceLocation jsonResource = new ResourceLocation(jsonFile.substring(0, jsonFile.indexOf(':')), "jsondefs/parts/" + jsonFile.substring(jsonFile.indexOf(':') + 1) + ".json");
	    		addPartDefinition(new InputStreamReader(Minecraft.getMinecraft().getResourceManager().getResource(jsonResource).getInputStream()), jsonFile.substring(jsonFile.indexOf(':') + 1), jsonFile.substring(0, jsonFile.indexOf(':')));
	    	}
	    	jsonFilesToReload.clear();
	    	
	    	for(String instrumentJSONFile : partInstrumentMap.keySet()){
	    		jsonFilesToReload.add(instrumentJSONFile);
	    	}
	    	for(String jsonFile : jsonFilesToReload){
	    		ResourceLocation jsonResource = new ResourceLocation(jsonFile.substring(0, jsonFile.indexOf(':')), "jsondefs/instruments/" + jsonFile.substring(jsonFile.indexOf(':') + 1) + ".json");
	    		addInstrumentDefinition(new InputStreamReader(Minecraft.getMinecraft().getResourceManager().getResource(jsonResource).getInputStream()), jsonFile.substring(jsonFile.indexOf(':') + 1), jsonFile.substring(0, jsonFile.indexOf(':')));
	    	}
	    	jsonFilesToReload.clear();
	    	
	    	for(String signJSONFile : signPackMap.keySet()){
	    		jsonFilesToReload.add(signJSONFile);
	    	}
	    	for(String jsonFile : jsonFilesToReload){
	    		ResourceLocation jsonResource = new ResourceLocation(jsonFile.substring(0, jsonFile.indexOf(':')), "jsondefs/signs/" + jsonFile.substring(jsonFile.indexOf(':') + 1) + ".json");
	    		addSignDefinition(new InputStreamReader(Minecraft.getMinecraft().getResourceManager().getResource(jsonResource).getInputStream()), jsonFile.substring(jsonFile.indexOf(':') + 1), jsonFile.substring(0, jsonFile.indexOf(':')));
	    	}
	    	jsonFilesToReload.clear();
	    	
	    	for(String decorJSONFile : decorPackMap.keySet()){
	    		jsonFilesToReload.add(decorJSONFile);
	    	}
	    	for(String jsonFile : jsonFilesToReload){
	    		ResourceLocation jsonResource = new ResourceLocation(jsonFile.substring(0, jsonFile.indexOf(':')), "jsondefs/decors/" + jsonFile.substring(jsonFile.indexOf(':') + 1) + ".json");
	    		addDecorDefinition(new InputStreamReader(Minecraft.getMinecraft().getResourceManager().getResource(jsonResource).getInputStream()), jsonFile.substring(jsonFile.indexOf(':') + 1), jsonFile.substring(0, jsonFile.indexOf(':')));
	    	}
    	}catch(Exception e){
    		logList.add("AN I/O ERROR WAS ENCOUNTERED WHEN TRYING TO RELOAD PACK DATA");
    		e.printStackTrace();
    	}
    	outputLog();
    }
    
    
    //-----START OF GENERAL LOOKUP LOGIC-----
    public static List<ItemStack> getMaterials(String componentName){
    	final List<ItemStack> materialList = new ArrayList<ItemStack>();
		for(String itemText : craftingItemMap.get(componentName)){
			int itemQty = Integer.valueOf(itemText.substring(itemText.lastIndexOf(':') + 1));
			itemText = itemText.substring(0, itemText.lastIndexOf(':'));
			
			int itemMetadata = Integer.valueOf(itemText.substring(itemText.lastIndexOf(':') + 1));
			itemText = itemText.substring(0, itemText.lastIndexOf(':'));
			materialList.add(new ItemStack(Item.getByNameOrId(itemText), itemQty, itemMetadata));
		}
    	return materialList;
    }
    
    
    //-----START OF VEHICLE LOOKUP LOGIC-----
    public static PackVehicleObject getVehiclePack(String name){
        return vehiclePackMap.get(name);
    }
    
    public static Set<String> getAllVehiclePackNames(){
        return vehiclePackMap.keySet();
    }
    
    public static String getVehicleJSONName(String name){
    	return vehicleJSONMap.get(name);
    }
    
    public static Class<? extends EntityVehicleE_Powered> getVehicleClass(String vehicleName){
    	switch(getVehiclePack(vehicleName).general.type){
			case "plane": return EntityVehicleG_Plane.class;
			case "car": return EntityVehicleG_Car.class;
			case "blimp": return EntityVehicleG_Blimp.class;
			case "boat": return EntityVehicleG_Boat.class;
			default: return null;
		}
    }
    
    
    //-----START OF PART LOOKUP LOGIC-----
    public static PackPartObject getPartPack(String name){
        return partPackMap.get(name);
    }
    
    public static Set<String> getAllPartPackNames(){
        return partPackMap.keySet();
    }
    
    
    //-----START OF INSTRUMENT LOOKUP LOGIC-----
    public static PackInstrumentObject getInstrument(String name){
        return partInstrumentMap.get(name);
    }
    
    public static Set<String> getAllInstruments(){
        return partInstrumentMap.keySet();
    }
    
    
    //-----START OF SIGN LOOKUP LOGIC-----
    public static PackSignObject getSign(String name){
        return signPackMap.get(name);
    }
    
    public static Set<String> getAllSigns(){
        return signPackMap.keySet();
    }
    
    
    //-----START OF DECOR LOOKUP LOGIC-----
    public static PackDecorObject getDecor(String name){
        return decorPackMap.get(name);
    }
    
    public static Set<String> getAllDecor(){
        return decorPackMap.keySet();
    }
    
    
    //-----START OF ITEM LOOKUP LOGIC-----
    public static PackItemObject getItem(String name){
        return itemPackMap.get(name);
    }
    
    public static Set<String> getAllItems(){
        return itemPackMap.keySet();
    }
    
    
    //-----START OF CONSTANTS AND SWITCHES-----
    public static Class<? extends APart> getPartPartClass(String partName){
    	switch(getPartPack(partName).general.type){
			case "crate": return PartCrate.class;
			case "barrel": return PartBarrel.class;
			case "crafting_table": return PartCraftingTable.class;
			case "furnace": return PartFurnace.class;
			case "brewing_stand": return PartBrewingStand.class;
			case "plow": return PartPlow.class;
			case "planter": return PartPlanter.class;
			case "fertilizer": return PartFertilizer.class;
			case "harvester": return PartHarvester.class;
			case "engine_aircraft": return PartEngineAircraft.class;
			case "engine_jet": return PartEngineJet.class;
			case "engine_car": return PartEngineCar.class;
			case "engine_boat": return PartEngineBoat.class;
			case "wheel": return PartGroundDeviceWheel.class;
			case "skid": return PartGroundDeviceSkid.class;
			case "pontoon": return PartGroundDevicePontoon.class;
			case "tread": return PartGroundDeviceTread.class;
			case "propeller": return PartPropeller.class;
			case "seat": return PartSeat.class;
			case "gun_fixed": return PartGunFixed.class;
			case "gun_tripod": return PartGunTripod.class;
			case "gun_turret": return PartGunTurret.class;
			//Note that this case is invalid, as bullets are NOT parts that can be placed on vehicles.
			//Rather, they are items that get loaded into the gun, so they never actually become parts themselves.
			//case "bullet": return PartBullet.class;
			case "custom": return PartCustom.class;
			default: return null;
		}
    }
    
    public static Class<? extends AItemPart> getPartItemClass(String partName){
    	switch(getPartPack(partName).general.type){
			case "crate": return ItemPartCrate.class;
			case "barrel": return ItemPartBarrel.class;
			case "crafting_table": return ItemPartGeneric.class;
			case "furnace": return ItemPartGeneric.class;
			case "brewing_stand": return ItemPartGeneric.class;
			case "plow": return ItemPartGeneric.class;
			case "planter": return ItemPartGeneric.class;
			case "fertilizer": return ItemPartGeneric.class;
			case "harvester": return ItemPartGeneric.class;
			case "engine_aircraft": return ItemPartEngineAircraft.class;
			case "engine_jet": return ItemPartEngineJet.class;
			case "engine_car": return ItemPartEngineCar.class;
			case "engine_boat": return ItemPartEngineBoat.class;
			case "wheel": return ItemPartGroundDeviceWheel.class;
			case "skid": return ItemPartGroundDeviceSkid.class;
			case "pontoon": return ItemPartGroundDevicePontoon.class;
			case "tread": return ItemPartGroundDeviceTread.class;
			case "propeller": return ItemPartPropeller.class;
			case "seat": return ItemPartGeneric.class;
			case "gun_fixed": return ItemPartGun.class;
			case "gun_tripod": return ItemPartGun.class;
			case "gun_turret": return ItemPartGun.class;
			case "bullet": return ItemPartBullet.class;
			case "custom": return ItemPartCustom.class;
			default: return null;
		}
    }
}
