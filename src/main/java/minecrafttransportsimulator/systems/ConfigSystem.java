package minecrafttransportsimulator.systems;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;

import minecrafttransportsimulator.jsondefs.PackPartObject;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;

/**Class that handles all configuration settings.
 * Methods are separated into client and server configs
 * for easier config file editing.
 * 
 * @author don_bruce
 */
public final class ConfigSystem{
	
	//-----------------NEW CODE---------------------
	private static File configFile;
	public static ConfigObject configObject;
	
	
	/**Custom Config class.  This contains all fields used in config operation,
	 * and should be assigned to config when {@link ConfigSystem#load(File)}
	 * is called.  Default values are assigned in the field declaration, while
	 * comments are simply fields of their own.  This class may (and probably should)
	 * be moved to the dataclasses package with other XXXXObject classes.
	 * Note that sub-classes MUST be static to use their default values!
	 */
	public static class ConfigObject{
		public DamageConfig general;
		public DamageConfig damage;
		
		public static class GeneralConfig{
			//TODO add general config.
		}
		
		public static class DamageConfig{
			public boolean explosions = true;
			public String explosionsComment = "Whether or not vehicles explode when crashed or shot down.";
			public boolean blockBreakage = true;
			public String blockBreakageComment = "Whether or not vehicles can break blocks when they hit them.  If false, vehicles will simply stop when they hit blocks.";
			public double propellerDamageFactor = 1.0;
			public String propellerDamageFactorComment = "Factor for damage caused by a propeller.";
			//TODO add rest of damage configs.
		}
		
		//TODO add other config sections.
	}
	
	/**Called to load this class from the passed-in config file.
	 * File should be a valid path where the config file resides,
	 * or should reside if no file is present.  If no file is present,
	 * one will be created at the end of the loading phase.
	 */
	public static void load(File file){
		//Set the current config file location to the passed-in file.
		configFile = file;
				
	    //If we have a config file already, parse it into Java.
		if(configFile.exists()){
			try{
				configObject = new Gson().fromJson(new FileReader(configFile), ConfigObject.class);
			}catch(Exception e){
				System.err.println("ERROR: ConfigSystem failed to parse config file JSON.  Reverting to defauts.");
				e.printStackTrace();
			}
		}
		
		//Check to see if our config object is valid.  If not, we must not have a file to parse, or we failed
		//to parse the file due to an error.  Create a new object and use that instead.
		if(configObject == null){
			configObject = new ConfigObject();
			//TODO add sub-config constructors here.  Perhaps we can do this in a loop?
			
			//If we don't have a file on disk, make one now for editing by the user.
			if(!configFile.exists()){
				try{
					new Gson().toJson(configObject, new FileWriter(configFile));
				}catch(Exception e){
					System.err.println("ERROR: ConfigSystem failed to save inital config file.  Report to the mod author!");
					e.printStackTrace();
				}
			}
		}
	}
	
	/**Called to save this class as a config File.  File
	 * the class is saved to is the same one passed in
	 * during {@link #load(File)}.  Call this whenever
	 * configs are edited to ensure they are saved, as
	 * the system does not do this automatically.
	 */
	public static void save(){
		try{
			new Gson().toJson(configObject, new FileWriter(configFile));
		}catch(Exception e){
			System.err.println("ERROR: ConfigSystem failed to save modified config file.  Report to the mod author!");
			e.printStackTrace();
		}
	}
	
	
	
	
	
	
	//---------------OLD METHODS ARE BELOW THIS LINE--------------------
	public static Configuration config;	
	 
	private static Map<String, Boolean> booleanConfigMap = new HashMap<String, Boolean>();
	private static Map<String, Integer> integerConfigMap = new HashMap<String, Integer>();
	private static Map<String, Double> doubleConfigMap = new HashMap<String, Double>();
	private static Map<String, String> stringConfigMap = new HashMap<String, String>();
	private static Map<String, Map<String, Double>> fuelConfigMaps = new HashMap<String, Map<String, Double>>();
	
	private static final String COMMON_CONFIG = "general";
	private static final String DAMAGE_CONFIG = "damage";
	private static final String CLIENT_CONFIG = "clientconfig";
	private static final String FUEL_CONFIG = "fuel";
	
	public static void initCommon(File configFile){
		config = new Configuration(configFile);
		config.load();
		
		//COMMON_CONFIG
		booleanConfigMap.put("OPSignEditingOnly", config.get(COMMON_CONFIG, "OPSignEditingOnly", false, "If true, only OPs will be able to edit signs on servers.  Does not affect client worlds.").getBoolean());
		booleanConfigMap.put("OPPickupVehiclesOnly", config.get(COMMON_CONFIG, "OPPickupVehiclesOnly", false, "If true, only OPs will be able to pick up vehicles with wrenches.  Does not affect client worlds.").getBoolean());
		integerConfigMap.put("RenderReductionHeight", config.get(COMMON_CONFIG, "RenderReductionHeight", 250, "When riding in a vehicle above this height MTS will reduce the render distance to 1.  \nThis provides a significant speedup for worldgen and render lag.  \nNote that this is only active on Singleplayer.").getInt());
		doubleConfigMap.put("SpeedFactor", config.get(COMMON_CONFIG, "SpeedFactor", 0.35D, "Factor to apply to vehicle movement.  \n1 is the realistic value, but this makes vehicles move too fast for Minecraft. Adjust with caution.").getDouble());
		doubleConfigMap.put("FuelUsageFactor", config.get(COMMON_CONFIG, "FuelUsageFactor", 1.0D, "Factor times which engines use fuel.  \nChange this if you think engines use fuel too fast or slow.").getDouble());
		doubleConfigMap.put("EngineHoursFactor", config.get(COMMON_CONFIG, "EngineHoursFactor", 1.0D, "Factor times which engines hours accumulate.  \nChange this if you want to adjust how fast engines wear out.").getDouble());
		doubleConfigMap.put("ClingSpeed", config.get(COMMON_CONFIG, "ClingSpeed", 0.25D, "Speed (in BLK/S) at which players start to slide off vehicles due to wind.  \nDoes not affect collision boxes set as interior in the vehicle JSON.").getDouble());
		stringConfigMap.put("HeavyItems", config.get(COMMON_CONFIG, "HeavyItems", "diamond, iron, gold, coal, ore, stone", "Any item that contains these words will be counted as heavy (double mass) when considering plane mass.  \nChange and tweak to your liking.").getString());
		
		//DAMAGE_CONFIG
		booleanConfigMap.put("Explosions", config.get(DAMAGE_CONFIG, "Explosions", true, "Whether or not vehicles explode when crashed or shot down.").getBoolean());
		booleanConfigMap.put("BlockBreakage", config.get(DAMAGE_CONFIG, "BlockBreakage", true, "Whether or not vehicles can break blocks when they hit them.  If false, vehicles will simply stop when they hit blocks.").getBoolean());
		doubleConfigMap.put("PropellerDamageFactor", config.get(DAMAGE_CONFIG, "PropellerDamageFactor", 1.0D, "Factor for damage caused by a propeller.").getDouble());
		doubleConfigMap.put("JetDamageFactor", config.get(DAMAGE_CONFIG, "JetDamageFactor", 1.0D, "Factor for damage caused by a jet engine.").getDouble());
		doubleConfigMap.put("WheelDamageFactor", config.get(DAMAGE_CONFIG, "WheelDamageFactor", 1.0D, "Factor for damage caused by wheels on vehicles.").getDouble());
		doubleConfigMap.put("CrashDamageFactor", config.get(DAMAGE_CONFIG, "CrashDamageFactor", 1.0D, "Factor for damage caused by crashes.").getDouble());
		doubleConfigMap.put("BulletDamageFactor", config.get(DAMAGE_CONFIG, "BulletDamageFactor", 1.0D, "Factor for damage caused by bullets on vehicles.").getDouble());
		doubleConfigMap.put("EngineLeakProbability", config.get(DAMAGE_CONFIG, "EngineLeakProbability", 0.01D, "Chance an engine will spring a leak if hit.  \nExplosions cause 10x this chance.").getDouble());
		doubleConfigMap.put("CrashItemDropPercentage", config.get(DAMAGE_CONFIG, "CrashItemDropPercentage", 0.75D, "Percent that a crafting ingredient will be dropped when a vehicle is crashed.  \nNote that fire/explosions may destroy these items if enabled, so just because they drop does not mean you will get all of them.").getDouble());
		config.save();
	}
	
	public static void initFuels(){
		//First get all valid fuel names from engines.
		List<String> fuelNames = new ArrayList<String>();
		for(String packPartName : PackParserSystem.getAllPartPackNames()){
			PackPartObject packPart = PackParserSystem.getPartPack(packPartName);
			if(packPart.general.type.startsWith("engine")){
				//For old packs, if we don't have a fuelType set it to diesel.
				//This is because it's the most versatile fuel, and all the old packs have heavy equipment.
				if(packPart.engine.fuelType == null){
					packPart.engine.fuelType = "diesel";
				}
				if(!fuelNames.contains(packPart.engine.fuelType)){
					fuelNames.add(packPart.engine.fuelType);
				}
			}
		}
		
		//Now load the config with the fuel->fluid linkings and their values.
		//We have some pre-configured defaults here for some fuel types.
		for(String fuelName : fuelNames){
			String[] defaultValues;
			switch(fuelName){
				case "gasoline": defaultValues = new String[]{"lava:1.0", "gasoline:1.0", "ethanol:0.85"}; break;
				case "diesel": defaultValues = new String[]{"lava:1.0", "diesel:1.0", "biodiesel:0.8", "oil:0.5"}; break;
				case "avgas": defaultValues = new String[]{"lava:1.0", "gasoline:1.0"}; break;
				case "redstone": defaultValues = new String[]{"lava:1.0", "redstone:1.0", "moltenredstone:1.0", "molten_redstone:1.0", "redstonemolten:1.0", "redstone_fluid:1.0", "fluidredstone:1.0", "fluid_redstone:1.0", "destabilized_redstone:1.0"}; break;
				default: defaultValues = new String[]{"lava:1.0"}; break;
			}

			Map<String, Double> fluidPotencies = new HashMap<String, Double>();
			for(String configEntry : config.get(FUEL_CONFIG, fuelName, defaultValues).getStringList()){
				String fluidName = configEntry.substring(0, configEntry.indexOf(':'));
				double fluidPotency = Double.valueOf(configEntry.substring(configEntry.indexOf(':') + 1));
				fluidPotencies.put(fluidName, fluidPotency);
			}
			fuelConfigMaps.put(fuelName, fluidPotencies);
		}
		config.save();
	}
	
	public static void initClient(File configFile){
		initCommon(configFile);
		booleanConfigMap.put("DevMode", config.get(CLIENT_CONFIG, "DevMode", false, "If enabled, MTS will re-load all resources every time the config key (P) is pressed.  \nThis includes textures for vehicles and parts, JSON files, and OBJ models.  \nThis is intended for use in pack creation with pack components  \nbeing placed in an un-zipped resource pack.  \nNote that every re-load will also re-load EVERY resource, not just MTS resources.  \nMake sure not to have lots of mods installed when you are doing this!").getBoolean());
		booleanConfigMap.put("SeaLevelOffset", config.get(CLIENT_CONFIG, "SeaLevelOffset", false, "Does altimiter read zero at average sea level instead of Y=0?").getBoolean());
		booleanConfigMap.put("MouseYoke", config.get(CLIENT_CONFIG, "MouseYoke", false, "Enable mouse yoke for vehicles? \nPrevents looking around unless unlocked.  Think MCHeli controls.").getBoolean());
		booleanConfigMap.put("KeyboardOverride", config.get(CLIENT_CONFIG, "KeyboardOverride", true, "Should keyboard controls be overriden when a joystick control is mapped?  \nLeave true to free up the keyboard while using a joysick.").getBoolean());
		integerConfigMap.put("ControlSurfaceCooldown", config.get(CLIENT_CONFIG, "ControlSurfaceCooldown", 4, "How long (in ticks) it takes before control surfaces try to return to their natural angle.  \nThis is not used when using a joystick.", 0, Short.MAX_VALUE).getInt());
		doubleConfigMap.put("JoystickDeadZone", config.get(CLIENT_CONFIG, "JoystickDeadZone", 0.03D, "Dead zone for joystick axis.  NOT joystick specific.").getDouble());
		config.save();
	}
	
	public static double getFuelValue(String fuelName, String fluidName){
		return fuelConfigMaps.get(fuelName).containsKey(fluidName) ? fuelConfigMaps.get(fuelName).get(fluidName) : 0;
	}
	
	public static Set<String> getAllFuels(){
		return fuelConfigMaps.keySet();
	}
	
	public static boolean getBooleanConfig(String configName){
		return booleanConfigMap.get(configName);
	}
	
	public static int getIntegerConfig(String configName){
		return integerConfigMap.get(configName);
	}

	public static double getDoubleConfig(String configName){
		return doubleConfigMap.get(configName);
	}
	
	public static String getStringConfig(String configName){
		return stringConfigMap.get(configName);
	}
	
	public static void setCommonConfig(String configName, Object value){
		setConfig(configName, String.valueOf(value), COMMON_CONFIG);
	}
	
	public static void setClientConfig(String configName, Object value){
		setConfig(configName, String.valueOf(value), CLIENT_CONFIG);
	}
	
	private static void setConfig(String configName, String value, String categoryName){
		ConfigCategory category = config.getCategory(categoryName);
		if(category.containsKey(configName)){
			if(booleanConfigMap.containsKey(configName)){
				booleanConfigMap.put(configName, Boolean.valueOf(value));
			}else if(integerConfigMap.containsKey(configName)){
				integerConfigMap.put(configName, Integer.valueOf(value));
			}else if(doubleConfigMap.containsKey(configName)){
				doubleConfigMap.put(configName, Double.valueOf(value));
			}else if(stringConfigMap.containsKey(configName)){
				stringConfigMap.put(configName, value);
			}else{
				return;
			}
			category.get(configName).set(value);
			config.save();
		}
	}
}
