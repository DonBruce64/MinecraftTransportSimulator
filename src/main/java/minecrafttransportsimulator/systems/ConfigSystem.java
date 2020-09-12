package minecrafttransportsimulator.systems;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.jsondefs.JSONConfig;
import minecrafttransportsimulator.jsondefs.JSONCraftingOverrides;


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
	public static JSONConfig configObject;
	
	/**Called to load this class from the passed-in config file.
	 * File should be a valid path where the config file resides,
	 * or should reside if no file is present.  If no file is present,
	 * one will be created at the end of the loading phase.
	 */
	public static void loadFromDisk(File file){
		//Set the current config file location to the passed-in file.
		configFile = file;
				
	    //If we have a config file already, parse it into Java.
		//Otherwise, make a new one.
		if(configFile.exists()){
			try{
				configObject = new Gson().fromJson(new FileReader(configFile), JSONConfig.class);
				//Check to make sure we have all our fuels.  We may have loaded a new engine type this launch.
				for(Entry<String, Map<String, Double>> fuelValues : JSONConfig.ConfigFuel.getDefaultFuels().entrySet()){
					if(!configObject.fuel.fuels.containsKey(fuelValues.getKey())){
						configObject.fuel.fuels.put(fuelValues.getKey(), fuelValues.getValue());
					}
				}
			}catch(Exception e){
				MTS.MTSLog.error("ERROR: ConfigSystem failed to parse config file JSON.  Reverting to defauts.");
				MTS.MTSLog.error(e.getMessage());
			}
		}
		
		//If we don't have a valid configObject, we must not have a file or have a corrupted file.
		//In either case, make a fresh object now.
		if(configObject == null){
			configObject = new JSONConfig();
		}
		
		//After parsing the config save it.  This allows new entries to be populated.
		saveToDisk();
		
		
		//Now parse the crafting override file.
		File craftingFile = new File(configFile.getParentFile(), "mtscraftingoverrides.json");
		if(craftingFile.exists() && !configObject.general.dumpCraftingConfig.value){
			try{
				JSONCraftingOverrides craftingOverridesObject = new Gson().fromJson(new FileReader(craftingFile), JSONCraftingOverrides.class);
				for(String craftingOverridePackID : craftingOverridesObject.overrides.keySet()){
					if(MTSRegistry.packItemMap.containsKey(craftingOverridePackID)){
						for(String craftingOverrideSystemName : craftingOverridesObject.overrides.get(craftingOverridePackID).keySet()){
							if(MTSRegistry.packItemMap.get(craftingOverridePackID).containsKey(craftingOverrideSystemName)){
								MTSRegistry.packCraftingMap.put(MTSRegistry.packItemMap.get(craftingOverridePackID).get(craftingOverrideSystemName), craftingOverridesObject.overrides.get(craftingOverridePackID).get(craftingOverrideSystemName));
							}
						}
					}
				}
			}catch(Exception e){
				MTS.MTSLog.error("ERROR: ConfigSystem failed to parse crafting override file JSON.  Crafting overrides will not be applied.");
				MTS.MTSLog.error(e.getMessage());
			}
		}else{
			//Make the default override file and save it.
			try{
				FileWriter writer = new FileWriter(craftingFile);
				new GsonBuilder().setPrettyPrinting().create().toJson(new JSONCraftingOverrides(), JSONCraftingOverrides.class, writer);
				writer.flush();
				writer.close();
			}catch(Exception e){
				System.err.println("ERROR: ConfigSystem failed to create fresh crafting overridesg file.  Report to the mod author!");
				e.printStackTrace();
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
			new GsonBuilder().setPrettyPrinting().create().toJson(configObject, JSONConfig.class, writer);
			writer.flush();
			writer.close();
		}catch(Exception e){
			System.err.println("ERROR: ConfigSystem failed to save modified config file.  Report to the mod author!");
			e.printStackTrace();
		}
	}
}
