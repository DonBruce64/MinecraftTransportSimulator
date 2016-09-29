package minecraftflightsimulator.utilities;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fluids.FluidRegistry;

/**Class that handles all regular configuration settings.
 * 
 * @author don_bruce
 */
public class CommonConfig{
	public static Configuration config;	
	 
	protected static CommonConfig instance;
	protected static Map<String, Boolean> booleanConfigMap = new HashMap<String, Boolean>();
	protected static Map<String, Integer> integerConfigMap = new HashMap<String, Integer>();
	protected static Map<String, Double> doubleConfigMap = new HashMap<String, Double>();
	protected static Map<String, String> stringConfigMap = new HashMap<String, String>();

	private static Map<String, Double> fluidValues = new HashMap<String, Double>();
	private static final String COMMON_CONFIG = "general";
	private static final String FUEL_CONFIG = "fuels";
	
	public static void init(File configFile){
		instance = new CommonConfig();
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
	
	public static void setBooleanConfig(String configName, boolean value){
		if(booleanConfigMap.containsKey(configName)){
			booleanConfigMap.put(configName, value);
			setProperty(configName, String.valueOf(value));
		}
	}
	
	public static void setIntgerConfig(String configName, int value){
		if(integerConfigMap.containsKey(configName)){
			integerConfigMap.put(configName, value);
			setProperty(configName, String.valueOf(value));
		}
	}
	
	public static void setDoubleConfig(String configName, double value){
		if(doubleConfigMap.containsKey(configName)){
			doubleConfigMap.put(configName, value);
			setProperty(configName, String.valueOf(value));
		}
	}
	
	public static void setStringConfig(String configName, String value){
		if(stringConfigMap.containsKey(configName)){
			stringConfigMap.put(configName, value);
			setProperty(configName, value);
		}
	}
	
	public static double getFuelValue(String liquidName){
		return fluidValues.get(liquidName);
	}
	
	protected static void setProperty(String propertyName, String value){
		ConfigCategory category = instance.getConfigCategory();
		if(category.containsKey(propertyName)){
			Property property = category.get(propertyName);
			if(property.isBooleanValue()){
				property.set(Boolean.valueOf(value));
			}else if(property.isIntValue()){
				property.set(Integer.valueOf(value));
			}else if(property.isDoubleValue()){
				property.set(Double.valueOf(value));
			}else{
				property.set(value);
			}
		}
		config.save();
	}
	
	protected ConfigCategory getConfigCategory(){
		return config.getCategory(COMMON_CONFIG);
	}
	
	protected enum ConfigTypes{
		BOOLEAN,
		INTEGER,
		DOUBLE,
		STRING;
	}
}
