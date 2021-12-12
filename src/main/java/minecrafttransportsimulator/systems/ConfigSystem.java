package minecrafttransportsimulator.systems;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.jsondefs.JSONConfig;
import minecrafttransportsimulator.jsondefs.JSONCraftingOverrides;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.packloading.JSONParser;


/**Class that handles all configuration settings. This file is responsible for saving and loading
 * the config, and representing that config as an instance object of type {@link JSONConfig} for access in the code.
 * This class is NOT responsible for detecting config changes.  It is up to the code that calls this class to ensure the 
 * changes made are valid and can be saved to the disk.  This also cuts down on saves in some instances where configs
 * cam be saved/modified in a batch rather than as single values.
 * 
 * @author don_bruce
 */
public final class ConfigSystem{
	private static File configFile;
	private static File craftingFile;
	public static JSONConfig configObject;
	
	/**Called to load this class from the passed-in config file.
	 * File should be a valid path where the config file resides,
	 * or should reside if no file is present.  If no file is present,
	 * one will be created at the end of the loading phase.
	 */
	public static void loadFromDisk(File configDirectory){
		//Set the current config file location to the passed-in file.
		configFile = new File(configDirectory, "mtsconfig.json");
		craftingFile = new File(configFile.getParentFile(), "mtscraftingoverrides.json");
				
	    //If we have a config file already, parse it into Java.
		//Otherwise, make a new one.
		if(configFile.exists()){
			try{
				configObject = JSONParser.parseStream(new FileReader(configFile), JSONConfig.class, null, null);
			}catch(Exception e){
				InterfaceCore.logError("ConfigSystem failed to parse config file JSON.  Reverting to defauts.");
				InterfaceCore.logError(e.getMessage());
			}
		}
		
		//If we don't have a valid configObject, we must not have a file or have a corrupted file.
		//In either case, make a fresh object now.
		if(configObject == null){
			configObject = new JSONConfig();
		}
		
		//After parsing the config save it.  This allows new entries to be populated.
		saveToDisk();
		
		//If we have the old config file, delete it.
		File oldConfigFile = new File(configDirectory, "mts.cfg");
		if(oldConfigFile.exists()){
			oldConfigFile.delete();
		}
	}
	
	/**Called to do crafting overrides.  Must be called after all packs are loaded.
	 */
	public static void initCraftingOverrides(){
		if(configObject.general.dumpCraftingConfig.value){
			//Make the default override file and save it.
			try{
				FileWriter writer = new FileWriter(craftingFile);
				JSONCraftingOverrides craftingOverridesObject = new JSONCraftingOverrides();
				craftingOverridesObject.overrides = new LinkedHashMap<String, Map<String, List<String>>>();
				for(AItemPack<?> packItem : PackParserSystem.getAllPackItems()){
					if(!craftingOverridesObject.overrides.containsKey(packItem.definition.packID)){
						craftingOverridesObject.overrides.put(packItem.definition.packID, new LinkedHashMap<String, List<String>>());
					}
					if(packItem instanceof AItemSubTyped){
						List<String> materials = new ArrayList<String>();
						materials.addAll(packItem.definition.general.materials);
						materials.addAll(((AItemSubTyped<?>) packItem).getExtraMaterials());
						craftingOverridesObject.overrides.get(packItem.definition.packID).put(packItem.definition.systemName + ((AItemSubTyped<?>) packItem).subName, materials);
					}else{
						craftingOverridesObject.overrides.get(packItem.definition.packID).put(packItem.definition.systemName, packItem.definition.general.materials);
					}
					if(packItem.definition.general.repairMaterials != null){
						craftingOverridesObject.overrides.get(packItem.definition.packID).put(packItem.definition.systemName + "_repair", packItem.definition.general.repairMaterials);
					}
				}
				JSONParser.exportStream(craftingOverridesObject, writer);
				writer.flush();
				writer.close();
			}catch(Exception e){
				InterfaceCore.logError("ConfigSystem failed to create fresh crafting overrides file.  Report to the mod author!");
			}
		}else if(craftingFile.exists()){
			try{
				JSONCraftingOverrides craftingOverridesObject = new Gson().fromJson(new FileReader(craftingFile), JSONCraftingOverrides.class);
				for(String craftingOverridePackID : craftingOverridesObject.overrides.keySet()){
					for(String craftingOverrideSystemName : craftingOverridesObject.overrides.get(craftingOverridePackID).keySet()){
						AItemPack<? extends AJSONItem> item = PackParserSystem.getItem(craftingOverridePackID, craftingOverrideSystemName);
						if(item instanceof AItemSubTyped){
							List<String> extraMaterials = ((AItemSubTyped<?>) item).getExtraMaterials();
							extraMaterials.clear();
							extraMaterials.addAll(craftingOverridesObject.overrides.get(craftingOverridePackID).get(craftingOverrideSystemName));
							item.definition.general.materials.clear();
						}else if(item != null){
							item.definition.general.materials = craftingOverridesObject.overrides.get(craftingOverridePackID).get(craftingOverrideSystemName);
							item.definition.general.repairMaterials = craftingOverridesObject.overrides.get(craftingOverridePackID).get(craftingOverrideSystemName + "_repair");
						}
					}
				}
			}catch(Exception e){
				InterfaceCore.logError("ConfigSystem failed to parse crafting override file JSON.  Crafting overrides will not be applied.");
				InterfaceCore.logError(e.getMessage());
			}
		}
	}
	
	/**Called to save this class as a config File.  File
	 * the class is saved to is the same one passed in
	 * during {@link #loadFromDisk(File)}.  Call this whenever
	 * configs are edited to ensure they are saved, as
	 * the system does not do this automatically.
	 */
	public static void saveToDisk(){
		try{
			FileWriter writer = new FileWriter(configFile);
			JSONParser.exportStream(configObject, writer);
			writer.flush();
			writer.close();
		}catch(Exception e){
			InterfaceCore.logError("ConfigSystem failed to save modified config file.  Report to the mod author!");
		}
	}
}
