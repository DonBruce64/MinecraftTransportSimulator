package minecrafttransportsimulator.systems;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import minecrafttransportsimulator.jsondefs.JSONConfig;


/**Class that handles all configuration settings. This file is responsible for saving and loading
 * the config, and representing that config as an instance object of type {@link JSONConfig} for access in the code.
 * This class is NOT responsible for detecting config changes.  It is up to the code that calls this class to ensure the 
 * changes made are valid and can be saved to the disk.  This also cuts down on saves in some instances where configs
 * cam be saved/modified in a batch rather than as single values.
 * 
 * @author don_bruce
 */
public final class ConfigSystem{
	
	//-----------------NEW CODE---------------------
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
			}catch(Exception e){
				System.err.println("ERROR: ConfigSystem failed to parse config file JSON.  Reverting to defauts.");
				e.printStackTrace();
			}
		}
		
		//If we don't have a valid configObject, we must not have a file or have a corrupted file.
		//In either case, make a fresh object now.
		if(configObject == null){
			configObject = new JSONConfig();
		}
		
		//After parsing the config save it.  This allows new entries to be populated.
		saveToDisk();
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
