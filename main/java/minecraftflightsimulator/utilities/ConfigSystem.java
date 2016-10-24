package minecraftflightsimulator.utilities;

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
public class ConfigSystem{
	public static Configuration config;	
	 
	protected static ConfigSystem instance;
	protected static Map<String, Boolean> booleanConfigMap = new HashMap<String, Boolean>();
	protected static Map<String, Integer> integerConfigMap = new HashMap<String, Integer>();
	protected static Map<String, Double> doubleConfigMap = new HashMap<String, Double>();
	protected static Map<String, String> stringConfigMap = new HashMap<String, String>();

	private static Map<String, Double> fluidValues = new HashMap<String, Double>();
	private static final String COMMON_CONFIG = "general";
	private static final String CLIENT_CONFIG = "clientconfig";
	private static final String FUEL_CONFIG = "fuels";
	
	public static void initCommon(File configFile){
		instance = new ConfigSystem();
		config = new Configuration(configFile);
		config.load();
		
		doubleConfigMap.put("PlaneSpeedFactor", config.get(COMMON_CONFIG, "PlaneSpeedFactor", 0.5F, "Factor to apply to plane movement.  \n1 is the realistic value, but this makes planes move too fast for Minecraft. Adjust with caution.").getDouble());
		doubleConfigMap.put("FuelUsageFactor", config.get(COMMON_CONFIG, "FuelUsageFactor", 1.0F, "Factor times which engines use fuel.  \nChange this if you think engines use fuel too fast or slow.").getDouble());
		doubleConfigMap.put("PropellerDamageFactor", config.get(COMMON_CONFIG, "PropellerDamageFactor", 1.0F, "Factor for damage caused by a propeller.").getDouble());
		doubleConfigMap.put("CrashDamageFactor", config.get(COMMON_CONFIG, "CrashDamageFactor", 1.0F, "Factor for damage caused by plane crashes.").getDouble());
		stringConfigMap.put("HeavyItems", config.get(COMMON_CONFIG, "HeavyItems", "diamond, iron, gold, coal, ore, stone", "Any item that contains these words will be counted as heavy (double mass) when considering plane mass.  \nChange and tweak to your liking.").getString());
		for(String fluidName : FluidRegistry.getRegisteredFluids().keySet()){
			fluidValues.put(fluidName, config.get(FUEL_CONFIG, fluidName, fluidName.equals(FluidRegistry.LAVA.getName()) ? 1.0F : 0.0F).getDouble());
		}
		config.save();
	}
	
	public static void initClient(){
		booleanConfigMap.put("FirstRun", config.get(CLIENT_CONFIG, "FirstRun", true, "Credits screen is displayed if this is the first run.").getBoolean());
		booleanConfigMap.put("ThrottleKills", config.get(CLIENT_CONFIG, "ThrottleKills", false, "Whether or not the throttle can be put to zero and kill the engine.  \nOnly valid for joysticks.").getBoolean());
		booleanConfigMap.put("SeaLevelOffset", config.get(CLIENT_CONFIG, "SeaLevelOffset", false, "Does altimiter read zero at Y=64 instead of Y=0?").getBoolean());
		booleanConfigMap.put("ElectricStart", config.get(CLIENT_CONFIG, "ElectricStart", true, "Is the starter enabled? \nIf not, players must hit propellers to start them").getBoolean());
		integerConfigMap.put("JoystickForceFactor", config.get(CLIENT_CONFIG, "JoystickForceFactor", 15, "Factor by which joystick inputs are multiplied.  \nThis controls how quickly the control surfaces change.", 1, Byte.MAX_VALUE).getInt());
		integerConfigMap.put("ControlSurfaceCooldown", config.get(CLIENT_CONFIG, "ControlSurfaceCooldown", 4, "How long (in ticks) it takes before control surfaces try to return to their natural angle.  \nThis is not used when using a joystick.", 0, Short.MAX_VALUE).getInt());
		doubleConfigMap.put("JoystickDeadZone", config.get(CLIENT_CONFIG, "DeadZone", 0.03D, "Dead zone for joystick axis.  \nMFS will always use the greater of this value and the value provided by the computer.").getDouble());
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
