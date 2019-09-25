package minecrafttransportsimulator.systems;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fluids.FluidRegistry;

/**Class that handles all configuration settings.
 * Methods are separated into client and server configs
 * for easier config file editing.
 * 
 * @author don_bruce
 */
public final class ConfigSystem{
	public static Configuration config;	
	 
	protected static Map<String, Boolean> booleanConfigMap = new HashMap<String, Boolean>();
	protected static Map<String, Integer> integerConfigMap = new HashMap<String, Integer>();
	protected static Map<String, Double> doubleConfigMap = new HashMap<String, Double>();
	protected static Map<String, String> stringConfigMap = new HashMap<String, String>();

	private static Map<String, Double> fluidValues = new HashMap<String, Double>();
	private static final String COMMON_CONFIG = "general";
	private static final String DAMAGE_CONFIG = "damage";
	private static final String CLIENT_CONFIG = "clientconfig";
	private static final String FUEL_CONFIG = "fuels";
	
	public static void initCommon(File configFile){
		config = new Configuration(configFile);
		config.load();
		
		//COMMON_CONFIG
		booleanConfigMap.put("OPSignEditingOnly", config.get(COMMON_CONFIG, "OPSignEditingOnly", false, "If true, only OPs will be able to edit signs on servers.  Does not affect client worlds.").getBoolean());
		integerConfigMap.put("RenderReductionHeight", config.get(COMMON_CONFIG, "RenderReductionHeight", 250, "When riding in a vehicle above this height MTS will reduce the render distance to 1.  \nThis provides a significant speedup for worldgen and render lag.  \nNote that this is only active on Singleplayer.").getInt());
		doubleConfigMap.put("SpeedFactor", config.get(COMMON_CONFIG, "SpeedFactor", 0.35D, "Factor to apply to vehicle movement.  \n1 is the realistic value, but this makes vehicles move too fast for Minecraft. Adjust with caution.").getDouble());
		doubleConfigMap.put("FuelUsageFactor", config.get(COMMON_CONFIG, "FuelUsageFactor", 1.0D, "Factor times which engines use fuel.  \nChange this if you think engines use fuel too fast or slow.").getDouble());
		doubleConfigMap.put("ClingSpeed", config.get(COMMON_CONFIG, "ClingSpeed", 0.25D, "Speed (in BLK/S) at which players start to slide off vehicles due to wind.  \nDoes not affect collision boxes set as interior in the vehicle JSON.").getDouble());
		stringConfigMap.put("ItemWeights", config.get(COMMON_CONFIG, "ItemWeights", "diamond_block:10;iron_block:780;gold_block:1930;coal_block:640;diamond_ore:8;iron_ore:77;gold_ore:140;coal_ore:68;iron:90;gold:215;coal:72;stone:64;cobblestone:64;ore:10", "Custom item weights written in pattern \"name:weight\" and separated by semicolon (;). Name is the word that item unlocalized name must contain to match, weight is weight defined in kilos. The earlier the pattern is written in string, the more priority it has").getString());
		
		//DAMAGE_CONFIG
		booleanConfigMap.put("Explosions", config.get(DAMAGE_CONFIG, "Explosions", true, "Whether or not vehicles explode when crashed or shot down.").getBoolean());
		booleanConfigMap.put("BlockBreakage", config.get(DAMAGE_CONFIG, "BlockBreakage", true, "Whether or not vehicles can break blocks when they hit them.  If false, vehicles will simply stop when they hit blocks.").getBoolean());
		booleanConfigMap.put("WheelDamageIgnoreSpeed", config.get(DAMAGE_CONFIG, "WheelDamageIgnoreSpeed", false, "Exclude vehicle speed from damage amount caused by wheels calculation").getBoolean());
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
		for(String fluidName : FluidRegistry.getRegisteredFluids().keySet()){
			fluidValues.put(fluidName, config.get(FUEL_CONFIG, fluidName, fluidName.equals(FluidRegistry.LAVA.getName()) ? 1.0F : 0.0F).getDouble());
		}
		config.save();
	}
	
	public static void initClient(File configFile){
		initCommon(configFile);
		booleanConfigMap.put("DevMode", config.get(CLIENT_CONFIG, "DevMode", false, "If enabled, MTS will re-load all resources every time the config key (P) is pressed.  \nThis includes textures for vehicles and parts, JSON files, and OBJ models.  \nThis is intended for use in pack creation with pack components  \nbeing placed in an un-zipped resource pack.  \nNote that every re-load will also re-load EVERY resource, not just MTS resources.  \nMake sure not to have lots of mods installed when you are doing this!").getBoolean());
		booleanConfigMap.put("SeaLevelOffset", config.get(CLIENT_CONFIG, "SeaLevelOffset", false, "Does altimiter read zero at average sea level instead of Y=0?").getBoolean());
		booleanConfigMap.put("MouseYoke", config.get(CLIENT_CONFIG, "MouseYoke", false, "Enable mouse yoke for vehicles? \nPrevents looking around unless unlocked.  Think MCHeli controls.").getBoolean());
		booleanConfigMap.put("InnerWindows", config.get(CLIENT_CONFIG, "InnerWindows", true, "Render the insides of windows on vehicles?").getBoolean());
		booleanConfigMap.put("KeyboardOverride", config.get(CLIENT_CONFIG, "KeyboardOverride", true, "Should keyboard controls be overriden when a joystick control is mapped?  \nLeave true to free up the keyboard while using a joysick.").getBoolean());
		integerConfigMap.put("ControlSurfaceCooldown", config.get(CLIENT_CONFIG, "ControlSurfaceCooldown", 4, "How long (in ticks) it takes before control surfaces try to return to their natural angle.  \nThis is not used when using a joystick.", 0, Short.MAX_VALUE).getInt());
		doubleConfigMap.put("JoystickDeadZone", config.get(CLIENT_CONFIG, "JoystickDeadZone", 0.03D, "Dead zone for joystick axis.  NOT joystick specific.").getDouble());
		config.save();
	}
	
	public static double getFuelValue(String liquidName){
		return fluidValues.get(liquidName);
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
