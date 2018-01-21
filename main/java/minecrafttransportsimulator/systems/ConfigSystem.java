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
	 
	protected static ConfigSystem instance;
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
		instance = new ConfigSystem();
		config = new Configuration(configFile);
		config.load();
		
		doubleConfigMap.put("SpeedFactor", config.get(COMMON_CONFIG, "SpeedFactor", 0.35F, "Factor to apply to vehicle movement.  \n1 is the realistic value, but this makes vehicles move too fast for Minecraft. Adjust with caution.").getDouble());
		doubleConfigMap.put("FuelUsageFactor", config.get(COMMON_CONFIG, "FuelUsageFactor", 1.0F, "Factor times which engines use fuel.  \nChange this if you think engines use fuel too fast or slow.").getDouble());
		stringConfigMap.put("HeavyItems", config.get(COMMON_CONFIG, "HeavyItems", "diamond, iron, gold, coal, ore, stone", "Any item that contains these words will be counted as heavy (double mass) when considering plane mass.  \nChange and tweak to your liking.").getString());
		
		
		booleanConfigMap.put("Explosions", config.get(DAMAGE_CONFIG, "Explosions", true, "Whether or not vehicles explode when crashed or shot down.  \nDoes not disable engine explosions as they're usually too small to cause much damage.").getBoolean());
		doubleConfigMap.put("PropellerDamageFactor", config.get(DAMAGE_CONFIG, "PropellerDamageFactor", 1.0F, "Factor for damage caused by a propeller.").getDouble());
		doubleConfigMap.put("CrashDamageFactor", config.get(DAMAGE_CONFIG, "CrashDamageFactor", 1.0F, "Factor for damage caused by crashes.").getDouble());
		doubleConfigMap.put("EngineLeakProbability", config.get(DAMAGE_CONFIG, "EngineLeakProbability", 0.01F, "Chance an engine will spring a leak if hit.  \nExplosions cause 10x this chance.").getDouble());

		for(String fluidName : FluidRegistry.getRegisteredFluids().keySet()){
			fluidValues.put(fluidName, config.get(FUEL_CONFIG, fluidName, fluidName.equals(FluidRegistry.LAVA.getName()) ? 1.0F : 0.0F).getDouble());
		}
		config.save();
	}
	
	public static void initClient(File configFile){
		initCommon(configFile);
		booleanConfigMap.put("FirstRun", config.get(CLIENT_CONFIG, "FirstRun", true, "Credits screen is displayed if this is the first run.").getBoolean());
		booleanConfigMap.put("SeaLevelOffset", config.get(CLIENT_CONFIG, "SeaLevelOffset", false, "Does altimiter read zero at average sea level instead of Y=0?").getBoolean());
		booleanConfigMap.put("ElectricStart", config.get(CLIENT_CONFIG, "ElectricStart", true, "Is the starter enabled? \nIf not, players must hit propellers to start them").getBoolean());
		booleanConfigMap.put("XaerosCompatibility", config.get(CLIENT_CONFIG, "XaerosCompatibility", false, "Enable Xaeros Minimap compatibility? \nThis allows Xaeros Minimap to be shown, but makes the hotbar render over the HUD.").getBoolean());
		booleanConfigMap.put("MouseYoke", config.get(CLIENT_CONFIG, "MouseYoke", false, "Enable mouse yoke for planes? \nPrevents looking around unless unlocked.  Think MCHeli controls.").getBoolean());
		booleanConfigMap.put("InnerWindows", config.get(CLIENT_CONFIG, "InnerWindows", true, "Render the insides of windows on vehicles?").getBoolean());
		booleanConfigMap.put("KeyboardOverride", config.get(CLIENT_CONFIG, "KeyboardOverride", true, "Should keyboard controls be overriden when a joystick control is mapped?  \nLeave true to free up the keyboard while using a joysick.").getBoolean());
		integerConfigMap.put("JoystickForceFactor", config.get(CLIENT_CONFIG, "JoystickForceFactor", 15, "Factor by which joystick inputs are multiplied.  \nThis controls how quickly the control surfaces change.", 1, Byte.MAX_VALUE).getInt());
		integerConfigMap.put("ControlSurfaceCooldown", config.get(CLIENT_CONFIG, "ControlSurfaceCooldown", 4, "How long (in ticks) it takes before control surfaces try to return to their natural angle.  \nThis is not used when using a joystick.", 0, Short.MAX_VALUE).getInt());
		integerConfigMap.put("SyncThreshold", config.get(CLIENT_CONFIG, "SyncThreshold", 5, "How many blocks or degrees out-of-sync a plane can be on a client before it will be forced to re-sync.  \nHigher values cause less jittering but increase the chance that an entity can get out of sync.  \nYou can keep this value as it is, but consider making it lower if you are on a server with fast planes.", 1, Byte.MAX_VALUE).getInt());
		doubleConfigMap.put("IncrementalMovement", config.get(CLIENT_CONFIG, "IncrementalMovement", 0.01, "How much planes will try to correct themselves when they get a sync packet.  \nThis is intended to gradually sync airborne planes so they don't get out of the sync threshold and get force-synced.", 0, Short.MAX_VALUE).getDouble());
		doubleConfigMap.put("JoystickDeadZone", config.get(CLIENT_CONFIG, "JoystickDeadZone", 0.03D, "Dead zone for joystick axis.  \nMTS will always use the greater of this value and the value provided by the computer.").getDouble());
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
