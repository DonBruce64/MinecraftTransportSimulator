package minecrafttransportsimulator.systems;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.CreativeTabPack;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.dataclasses.PackMultipartObject;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackFileDefinitions;
import minecrafttransportsimulator.dataclasses.PackPartObject;
import minecrafttransportsimulator.items.parts.AItemPart;
import minecrafttransportsimulator.items.parts.ItemPartCrate;
import minecrafttransportsimulator.items.parts.ItemPartEngineAircraft;
import minecrafttransportsimulator.items.parts.ItemPartEngineCar;
import minecrafttransportsimulator.items.parts.ItemPartGroundDevice;
import minecrafttransportsimulator.items.parts.ItemPartPropeller;
import minecrafttransportsimulator.items.parts.ItemPartSeat;
import minecrafttransportsimulator.multipart.main.EntityMultipartD_Moving;
import minecrafttransportsimulator.multipart.main.EntityMultipartF_Car;
import minecrafttransportsimulator.multipart.main.EntityMultipartF_Plane;
import minecrafttransportsimulator.multipart.parts.APart;
import minecrafttransportsimulator.multipart.parts.PartCrate;
import minecrafttransportsimulator.multipart.parts.PartEngineAircraft;
import minecrafttransportsimulator.multipart.parts.PartEngineCar;
import minecrafttransportsimulator.multipart.parts.PartGroundDevice;
import minecrafttransportsimulator.multipart.parts.PartPropeller;
import minecrafttransportsimulator.multipart.parts.PartSeat;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Class responsible for parsing content pack data.  Gets properties from the text files that other parts
 * of the code can use.
 *
 * @author don_bruce
 */
public final class PackParserSystem{
	/**Map that keys the unique name of a multipart to it's pack.  Used for associating packs with saved multiparts.**/
    private static final Map<String, PackMultipartObject> multipartPackMap = new LinkedHashMap<String, PackMultipartObject>();
   
    /**Map that keys the unique name of a multipart to it's JSON file name.**/
    private static final Map<String, String> multipartJSONMap = new HashMap<String, String>();
   
    /**Same function as the multipart map, just for parts.**/
    private static final Map<String, PackPartObject> partPackMap = new LinkedHashMap<String, PackPartObject>();
    
	/**Maps multipart and part names to their crafting ingredients.*/
	private static final Map<String, List<ItemStack>> craftingItemMap = new HashMap<String, List<ItemStack>>();
  
    /**Listing of log messages.  Stored here on bootstrap and outputted once the logging system comes online.**/
    private static final List<String> logList = new ArrayList<String>();
    
    
    //-----START OF INIT LOGIC-----
    
    /**Packs should call this upon load to add their multiparts to the mod.**/
    public static void addMultipartDefinition(InputStreamReader jsonReader, String jsonFileName, String modID){
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
    				
    				//Now that the multipart is registered, set the crafting.
    				List<String> materials = new ArrayList<String>();
    				for(String material : pack.general.materials){
    					materials.add(material);
    				}
    				for(String material : definition.extraMaterials){
    					materials.add(material);
    				}
    				final List<ItemStack> materialList = new ArrayList<ItemStack>();
    				for(String itemText : materials){
    					int itemQty = Integer.valueOf(itemText.substring(itemText.lastIndexOf(':') + 1));
    					itemText = itemText.substring(0, itemText.lastIndexOf(':'));
    					
    					int itemMetadata = Integer.valueOf(itemText.substring(itemText.lastIndexOf(':') + 1));
    					itemText = itemText.substring(0, itemText.lastIndexOf(':'));
    					
    					materialList.add(new ItemStack(Item.getByNameOrId(itemText), itemQty, itemMetadata));
    				}
    				craftingItemMap.put(multipartName, materialList);
    			}
    		}
    	}catch(Exception e){
    		logList.add("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + modID + ":" + jsonFileName);
    		logList.add(e.getMessage());
    	}
    }
    
    /**Packs should call this upon load to add their parts to the mod.  This should be the location of the JSON file.**/
    public static void addPartDefinition(InputStreamReader jsonReader, String jsonFileName, String modID){
    	try{
	    	PackPartObject pack =  new Gson().fromJson(jsonReader, PackPartObject.class);
	    	String partName = modID + ":" + jsonFileName;
	    	partPackMap.put(partName, pack);
	    	
			//Now that the part is registered, set the crafting.
			final List<ItemStack> materialList = new ArrayList<ItemStack>();
			for(String itemText : pack.general.materials){
				int itemQty = Integer.valueOf(itemText.substring(itemText.lastIndexOf(':') + 1));
				itemText = itemText.substring(0, itemText.lastIndexOf(':'));
				
				int itemMetadata = Integer.valueOf(itemText.substring(itemText.lastIndexOf(':') + 1));
				itemText = itemText.substring(0, itemText.lastIndexOf(':'));
				
				materialList.add(new ItemStack(Item.getByNameOrId(itemText), itemQty, itemMetadata));
			}
			craftingItemMap.put(partName, materialList);
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
    
    public static Class<? extends APart> getPartPartClass(String partName){
    	switch(getPartPack(partName).general.type){
			case "crate": return PartCrate.class;
			case "engine_aircraft": return PartEngineAircraft.class;
			case "engine_car": return PartEngineCar.class;
			case "ground_device": return PartGroundDevice.class;
			case "propeller": return PartPropeller.class;
			case "seat": return PartSeat.class;
			default: return null;
		}
    }
    
    public static Class<? extends AItemPart> getPartItemClass(String partName){
    	switch(getPartPack(partName).general.type){
			case "crate": return ItemPartCrate.class;
			case "engine_aircraft": return ItemPartEngineAircraft.class;
			case "engine_car": return ItemPartEngineCar.class;
			case "ground_device": return ItemPartGroundDevice.class;
			case "propeller": return ItemPartPropeller.class;
			case "seat": return ItemPartSeat.class;
			default: return null;
		}
    }
}
