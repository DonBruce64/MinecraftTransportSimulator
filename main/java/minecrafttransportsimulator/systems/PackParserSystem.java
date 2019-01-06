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
import minecrafttransportsimulator.dataclasses.PackInstrumentObject;
import minecrafttransportsimulator.dataclasses.PackMultipartObject;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackFileDefinitions;
import minecrafttransportsimulator.dataclasses.PackPartObject;
import minecrafttransportsimulator.dataclasses.PackSignObject;
import minecrafttransportsimulator.items.parts.AItemPart;
import minecrafttransportsimulator.items.parts.ItemPartCrate;
import minecrafttransportsimulator.items.parts.ItemPartCustom;
import minecrafttransportsimulator.items.parts.ItemPartEngineAircraft;
import minecrafttransportsimulator.items.parts.ItemPartEngineCar;
import minecrafttransportsimulator.items.parts.ItemPartGroundDevicePontoon;
import minecrafttransportsimulator.items.parts.ItemPartGroundDeviceSkid;
import minecrafttransportsimulator.items.parts.ItemPartGroundDeviceTread;
import minecrafttransportsimulator.items.parts.ItemPartGroundDeviceWheel;
import minecrafttransportsimulator.items.parts.ItemPartPropeller;
import minecrafttransportsimulator.items.parts.ItemPartSeat;
import minecrafttransportsimulator.multipart.main.EntityMultipartD_Moving;
import minecrafttransportsimulator.multipart.main.EntityMultipartF_Car;
import minecrafttransportsimulator.multipart.main.EntityMultipartF_Plane;
import minecrafttransportsimulator.multipart.parts.APart;
import minecrafttransportsimulator.multipart.parts.PartCrate;
import minecrafttransportsimulator.multipart.parts.PartCustom;
import minecrafttransportsimulator.multipart.parts.PartEngineAircraft;
import minecrafttransportsimulator.multipart.parts.PartEngineCar;
import minecrafttransportsimulator.multipart.parts.PartGroundDevicePontoon;
import minecrafttransportsimulator.multipart.parts.PartGroundDeviceSkid;
import minecrafttransportsimulator.multipart.parts.PartGroundDeviceTread;
import minecrafttransportsimulator.multipart.parts.PartGroundDeviceWheel;
import minecrafttransportsimulator.multipart.parts.PartPropeller;
import minecrafttransportsimulator.multipart.parts.PartSeat;
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
	/**Map that keys the unique name of a multipart to its pack.  Used for associating packs with saved multiparts.**/
    private static final Map<String, PackMultipartObject> multipartPackMap = new LinkedHashMap<String, PackMultipartObject>();
   
    /**Map that keys the unique name of a multipart to its JSON file name.**/
    private static final Map<String, String> multipartJSONMap = new HashMap<String, String>();
   
    /**Same function as the multipart map, just for parts.**/
    private static final Map<String, PackPartObject> partPackMap = new LinkedHashMap<String, PackPartObject>();
    
    /**Same function as the multipart map, just for instruments.**/
    private static final Map<String, PackInstrumentObject> partInstrumentMap = new LinkedHashMap<String, PackInstrumentObject>();
    
    /**Map that keys the unique name of a sign to its pack.*/
    private static final Map<String, PackSignObject> signPackMap = new LinkedHashMap<String, PackSignObject>();
    
	/**Maps multipart, part, and instrument names to their crafting ingredients.*/
	private static final Map<String, List<ItemStack>> craftingItemMap = new HashMap<String, List<ItemStack>>();
  
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
    	return new String[]{"vehicle", "part", "instrument", "sign"};
    }
    
    /**Packs should call this upon load to add their vehicles to the mod.**/
    public static void addVehicleDefinition(InputStreamReader jsonReader, String jsonFileName, String modID){
    	try{
    		PackMultipartObject pack = new Gson().fromJson(jsonReader, PackMultipartObject.class);
    		for(PackFileDefinitions definition : pack.definitions){
    			if(definition != null){
    				String multipartName = modID + ":" + jsonFileName + definition.subName;
    				multipartPackMap.put(multipartName, pack);
    				multipartJSONMap.put(multipartName, jsonFileName);
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
    				registerCrafting(multipartName, materials.toArray(new String[materials.size()]));
    			}
    		}
    	}catch(Exception e){
    		logList.add("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + modID + ":" + jsonFileName);
    		logList.add(e.getMessage());
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
	    	
	    	registerCrafting(partName, pack.general.materials);
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
    		registerCrafting(instrumentName, pack.general.materials.toArray(new String[pack.general.materials.size()]));
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
    
    /**Helper method to parse crafting strings and register items in the internal MTS crafting system.**/
    private static void registerCrafting(String itemName, String[] materials){
		final List<ItemStack> materialList = new ArrayList<ItemStack>();
		for(String itemText : materials){
			int itemQty = Integer.valueOf(itemText.substring(itemText.lastIndexOf(':') + 1));
			itemText = itemText.substring(0, itemText.lastIndexOf(':'));
			
			int itemMetadata = Integer.valueOf(itemText.substring(itemText.lastIndexOf(':') + 1));
			itemText = itemText.substring(0, itemText.lastIndexOf(':'));
			
			materialList.add(new ItemStack(Item.getByNameOrId(itemText), itemQty, itemMetadata));
		}
		craftingItemMap.put(itemName, materialList);
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
	    	for(Entry<String, String> multipartJSONEntry : multipartJSONMap.entrySet()){
	    		jsonFilesToReload.add(multipartJSONEntry.getKey().substring(0, multipartJSONEntry.getKey().indexOf(':') + 1) + multipartJSONEntry.getValue());
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
    	}catch(Exception e){
    		logList.add("AN I/O ERROR WAS ENCOUNTERED WHEN TRYING TO RELOAD PACK DATA");
    		e.printStackTrace();
    	}
    	outputLog();
    }
    
    
    //-----START OF GENERAL LOOKUP LOGIC-----
    public static List<ItemStack> getMaterials(String componentName){
    	return craftingItemMap.get(componentName);
    }
    
    
    //-----START OF MULTIPART LOOKUP LOGIC-----
    public static PackMultipartObject getMultipartPack(String name){
        return multipartPackMap.get(name);
    }
    
    public static Set<String> getAllMultipartPackNames(){
        return multipartPackMap.keySet();
    }
    
    public static String getMultipartJSONName(String name){
    	return multipartJSONMap.get(name);
    }
    
    public static Class<? extends EntityMultipartD_Moving> getMultipartClass(String multipartName){
    	switch(getMultipartPack(multipartName).general.type){
			case "car": return EntityMultipartF_Car.class;
			case "plane": return EntityMultipartF_Plane.class;
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
    
    
    //-----START OF CONSTANTS AND SWITCHES-----
    public static Class<? extends APart> getPartPartClass(String partName){
    	switch(getPartPack(partName).general.type){
			case "crate": return PartCrate.class;
			case "engine_aircraft": return PartEngineAircraft.class;
			case "engine_car": return PartEngineCar.class;
			case "wheel": return PartGroundDeviceWheel.class;
			case "skid": return PartGroundDeviceSkid.class;
			case "pontoon": return PartGroundDevicePontoon.class;
			case "tread": return PartGroundDeviceTread.class;
			case "propeller": return PartPropeller.class;
			case "seat": return PartSeat.class;
			case "custom": return PartCustom.class;
			default: return null;
		}
    }
    
    public static Class<? extends AItemPart> getPartItemClass(String partName){
    	switch(getPartPack(partName).general.type){
			case "crate": return ItemPartCrate.class;
			case "engine_aircraft": return ItemPartEngineAircraft.class;
			case "engine_car": return ItemPartEngineCar.class;
			case "wheel": return ItemPartGroundDeviceWheel.class;
			case "skid": return ItemPartGroundDeviceSkid.class;
			case "pontoon": return ItemPartGroundDevicePontoon.class;
			case "tread": return ItemPartGroundDeviceTread.class;
			case "propeller": return ItemPartPropeller.class;
			case "seat": return ItemPartSeat.class;
			case "custom": return ItemPartCustom.class;
			default: return null;
		}
    }
}
