package minecrafttransportsimulator.jsondefs;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.packs.AItemPack;
import minecrafttransportsimulator.items.packs.parts.AItemPartEngine;
import minecrafttransportsimulator.systems.ConfigSystem;

/**Custom Config class.  This contains all fields used in config operation,
 * and should be assigned to a saved object when {@link ConfigSystem#loadFromDisk(File)}
 * is called.  Default values are assigned in the field declaration, while
 * comments are simply fields of their own.  Note that sub-classes MUST 
 * be static to use their default values!
 * 
 * @author don_bruce
 */
public class JSONConfig{
	public ConfigGeneral general = new ConfigGeneral();
	public ConfigDamage damage = new ConfigDamage();
	public ConfigFuel fuel = new ConfigFuel();
	public ConfigCrafting crafting = new ConfigCrafting();
	public ConfigClient client = new ConfigClient();
	public ConfigControls controls = new ConfigControls();
	
	public static class ConfigGeneral{
		public ConfigBoolean opSignEditingOnly = new ConfigBoolean(false, "If true, only OPs will be able to edit signs on servers.  Does not affect client worlds.");
		public ConfigBoolean opPickupVehiclesOnly = new ConfigBoolean(false, "If true, only OPs will be able to pick up vehicles with wrenches.  Does not affect client worlds.");
		public ConfigBoolean disableSurvivalVehiclePickup = new ConfigBoolean(false, "If true, vehicles can only be picked up in creative mode.");
		public ConfigDouble speedFactor = new ConfigDouble(0.35D, "Factor to apply to vehicle movement.  1 is the realistic value, but this makes vehicles move too fast for Minecraft. Adjust with caution.");
		public ConfigDouble fuelUsageFactor = new ConfigDouble(1.0D, "Factor times which engines use fuel.  Change this if you think engines use fuel too fast or slow.");
		public ConfigDouble engineHoursFactor = new ConfigDouble(1.0D, "Factor times which engines hours accumulate.  Change this if you want to adjust how fast engines wear out.");
		public ConfigDouble clingSpeed = new ConfigDouble(0.25D, "Speed (in BLK/S) at which players start to slide off vehicles due to wind.  Does not affect collision boxes set as interior in the vehicle JSON.");
		public ConfigItemWeights itemWeights = new ConfigItemWeights();
		
		public static class ConfigItemWeights{
			public String comment1 = "The following section is used for calculating item weights.  Any item that contains the text bits will have its weight multiplied by the following factor.";
			public String comment2 = "This mass factors into vehicle performance, with particular emphasis on vehicle payloads.  Some defaults are presented here, but you can add/remove to your liking.";
			public String comment3 = "Note that the part of the name searched is the item's registry name, NOT the in-game display name.  This is similar to what is in the /give command.";
			public Map<String, Double> weights = populateDefaultWeights();
			
			private static Map<String, Double> populateDefaultWeights(){
				Map<String, Double> weights = new HashMap<String, Double>();
				weights.put("diamond", 4.0);
				weights.put("iron", 4.0);
				weights.put("gold", 4.0);
				weights.put("coal", 2.0);
				weights.put("ore", 2.5);
				weights.put("stone", 1.5);
				return weights;
			}
		}
	}
	
	public static class ConfigDamage{
		public ConfigBoolean explosions = new ConfigBoolean(true, "Whether or not vehicles explode when crashed or shot down.");
		public ConfigBoolean blockBreakage = new ConfigBoolean(true, "Whether or not vehicles can break blocks when they hit them.  If false, vehicles will simply stop when they hit blocks.");
		public ConfigBoolean wheelBreakage = new ConfigBoolean(true, "Whether or not wheels can be broken (go flat).");
		public ConfigBoolean wheelDamageIgnoreVelocity = new ConfigBoolean(false, "Whether or not velocity is ignored when calculating wheel damage.");
		public ConfigDouble propellerDamageFactor = new ConfigDouble(1.0D, "Factor for damage caused by a propeller.");
		public ConfigDouble jetDamageFactor = new ConfigDouble(1.0D, "Factor for damage caused by a jet engine.");
		public ConfigDouble wheelDamageFactor = new ConfigDouble(1.0D, "Factor for damage caused by wheels on vehicles.");
		public ConfigDouble crashDamageFactor = new ConfigDouble(1.0D, "Factor for damage caused by crashes.");
		public ConfigDouble bulletDamageFactor = new ConfigDouble(1.0D, "Factor for damage caused by bullets on vehicles.");
		public ConfigDouble engineLeakProbability = new ConfigDouble(0.01D, "Chance an engine will spring a leak if hit.  Explosions cause 10x this chance.");
		public ConfigDouble crashItemDropPercentage = new ConfigDouble(0.75D, "Percent that a crafting ingredient will be dropped when a vehicle is crashed.  Note that fire/explosions may destroy these items if enabled, so just because they drop does not mean you will get all of them.");
	}
	
	public static class ConfigFuel{
		public String comment1 = "The following section is used for fuel configs for engines.  Each entry here is a single fuel type for engines.";
		public String comment2 = "Inside each entry exist values for the potency of the fluids that can be considered to be that fuel type.";
		public String comment3 = "You can make engines take different types of fluids for their fuels by editing these values.  See the in-game handbook for more information.";
		public Map<String, Map<String, Double>> fuels = getDefaultFuels();
		
		public static Map<String, Map<String, Double>> getDefaultFuels(){
			Map<String, Map<String, Double>> fuels = new HashMap<String, Map<String, Double>>();
			for(String packID : MTSRegistry.packItemMap.keySet()){
				for(AItemPack<? extends AJSONItem<?>> item : MTSRegistry.packItemMap.get(packID).values()){
					if(item instanceof AItemPartEngine){
						AItemPartEngine itemEngine = (AItemPartEngine) item;
						if(itemEngine.definition.general.type.startsWith("engine")){
							//For old packs, if we don't have a fuelType set it to diesel.
							//This is because it's the most versatile fuel, and all the old packs have heavy equipment.
							if(itemEngine.definition.engine.fuelType == null){
								itemEngine.definition.engine.fuelType = "diesel";
							}
							
							//If we don't have the fuel in the fuel map, add it.
							//Default fuel list depends on the fuel name.
							if(!fuels.containsKey(itemEngine.definition.engine.fuelType)){
								Map<String, Double> fluids = new HashMap<String, Double>();
								switch(itemEngine.definition.engine.fuelType){
									case "gasoline" :{
										fluids.put("lava", 1.0);
										fluids.put("gasoline", 1.0);
										fluids.put("ethanol", 0.85);
										break;
									}
									case "diesel" :{
										fluids.put("lava", 1.0);
										fluids.put("diesel", 1.0);
										fluids.put("biodiesel", 0.8);
										fluids.put("oil", 0.5);
										break;
									}
									case "avgas" :{
										fluids.put("lava", 1.0);
										fluids.put("gasoline", 1.0);
										break;
									}
									case "redstone" :{
										fluids.put("lava", 1.0);
										fluids.put("redstone", 1.0);
										fluids.put("moltenredstone", 1.0);
										fluids.put("molten_redstone", 1.0);
										fluids.put("redstonemolten", 1.0);
										fluids.put("redstone_fluid", 1.0);
										fluids.put("fluidredstone", 1.0);
										fluids.put("fluid_redstone", 1.0);
										fluids.put("destabilized_redstone", 1.0);
										break;
									}
									default: fluids.put("lava", 1.0); break;
								}
								fuels.put(itemEngine.definition.engine.fuelType, fluids);
							}
						}
					}
				}
			}
			return fuels;
		}
	}
	
	public static class ConfigCrafting{
		public String comment1 = "The following section is used for overriding crafting recipes from packs for use in modpacks and servers.";
		public String comment2 = "Everything that is crafted on one of the benches can be modified here to include any item, including modded ones.";
		public String comment3 = "The format is one of [modID]:[ItemName]:[Metadata]:[Qty], with the name being the same as the in-game /give command.";
		public String comment4 = "A sample override config is given here for reference.  Modify as you see fit.";
		public Map<String, Map<String, String[]>> overrides = populateDefaultOverrides();
		
		private static Map<String, Map<String, String[]>> populateDefaultOverrides(){
			Map<String, Map<String, String[]>> sampleOverrides = new HashMap<String, Map<String, String[]>>();
			Map<String, String[]> packSampleOverride = new HashMap<String, String[]>();
			packSampleOverride.put("wheellarge", new String[]{"minecraft:dye:0:3", "minecraft:iron_ingot:0:3", "minecraft:wool:15:3"});
			packSampleOverride.put("engineamci4", new String[]{"minecraft:piston:0:4", "minecraft:obsidian:0:3", "minecraft:iron_ingot:0:1"});
			sampleOverrides.put("mtsofficialpack", packSampleOverride);
			
			packSampleOverride = new HashMap<String, String[]>();
			packSampleOverride.put("unuparts_decor_unu_cutout_derrick", new String[]{"minecraft:heavy_weighted_pressure_plate:0:1", "minecraft:dye:6:2"});
			sampleOverrides.put("unuparts", packSampleOverride);
			return sampleOverrides;
		}
	}
	
	public static class ConfigClient{
		public ConfigBoolean devMode = new ConfigBoolean(false, "If enabled, MTS will re-load all resources every time the config key (P) is pressed.  This includes textures for vehicles and parts, JSON files, and OBJ models.  This is intended for use in pack creation with pack components being placed in an un-zipped resource pack.  Note that every re-load will also re-load EVERY resource, not just MTS resources.  Make sure not to have lots of mods installed when you are doing this!");
		public ConfigBoolean innerWindows = new ConfigBoolean(false, "Should the glass on windows be rendered on the inside of the vehicle?");
		public ConfigBoolean mouseYoke = new ConfigBoolean(false, "Enable mouse yoke for vehicles? Prevents looking around unless unlocked.  Think MCHeli controls.");
		public ConfigBoolean kbOverride = new ConfigBoolean(true, "Should keyboard controls be ignored when a joystick control is mapped?  Leave true to free up the keyboard while using a joysick.");
		public ConfigBoolean flareBlending = new ConfigBoolean(false, "If true, light flares from vehicles and lamps will not do brightness blending.  Normally false, but can be set to true if it makes shaders better.");
		public ConfigBoolean beamBlending = new ConfigBoolean(true, "If false, beam-based lights from vehicles and lamps will not do brightness blending.  Useful if you have shaders and this is causing troubles.");
		public ConfigBoolean lightsPass0 = new ConfigBoolean(false, "If true, light rendering (and possibly blending) will happen on the first (solid) render pass.  For performance and compatibility, this should normally be false, but may be set to true to get proper light blending with shaders.");
		public ConfigBoolean vehicleBlklt = new ConfigBoolean(false, "If true, vehicles will spawn invisible light blocks to force shaders to render them brighter.  Use this only if the other light options fail to work.");
		public ConfigBoolean renderHUD_1P = new ConfigBoolean(true, "If false, the HUD in vehicles will not render in 1st-person mode.");
		public ConfigBoolean renderHUD_3P = new ConfigBoolean(true, "If false, the HUD in vehicles will not render in 3rd-person mode.");
		public ConfigBoolean fullHUD_1P = new ConfigBoolean(false, "If true, the full-size HUD will render in 1st-person rather than the half-size HUD.");
		public ConfigBoolean fullHUD_3P = new ConfigBoolean(false, "If true, the full-size HUD will render in 3rd-person rather than the half-size HUD.");
		public ConfigBoolean transpHUD_1P = new ConfigBoolean(false, "If true, the background textures for the HUD will not be rendered in 1st-person.");
		public ConfigBoolean transpHUD_3P = new ConfigBoolean(false, "If true, the background textures for the HUD will not be rendered in 1st-person.");
		
		public ConfigInteger renderReductionHeight = new ConfigInteger(250, "When riding in a vehicle above this height MTS will reduce the render distance to 1.  This provides a significant speedup for worldgen and render lag.  Note that this is only active on Singleplayer.");
		public ConfigInteger controlSurfaceCooldown = new ConfigInteger(4, "How long (in ticks) it takes before control surfaces try to return to their natural angle.  This is not used when using a joystick.");
		public ConfigInteger steeringIncrement = new ConfigInteger(20, "How many units (1/10 of a degree) to turn the wheels on vehicles for every tick the button is held down.  This is not used when using a joystick.");
		public ConfigInteger flightIncrement = new ConfigInteger(6, "How many units (1/10 of a degree) to move the elevators and ailerons on aircraft for every tick the button is held down.  This is not used when using a joystick.");
		public ConfigDouble joystickDeadZone = new ConfigDouble(0.03D, "Dead zone for joystick axis.  This is NOT joystick specific.");
	}
	
	public static class ConfigControls{
		public Map<String, ConfigKeyboard> keyboard = new HashMap<String, ConfigKeyboard>();
		public Map<String, ConfigJoystick>  joystick = new HashMap<String, ConfigJoystick>();
	}
	
	
	public static class Config<ConfigType>{
		public ConfigType value;
		public String comment;
		
		public Config(ConfigType defaultValue, String comment){
			this.value = defaultValue;
			this.comment = comment;
		}
	}
	
	public static class ConfigBoolean extends Config<Boolean>{
		public ConfigBoolean(Boolean defaultValue, String comment){
			super(defaultValue, comment);
		}
	}
	
	public static class ConfigInteger extends Config<Integer>{
		public ConfigInteger(Integer defaultValue, String comment){
			super(defaultValue, comment);
		}
	}
	
	public static class ConfigDouble extends Config<Double>{
		public ConfigDouble(Double defaultValue, String comment){
			super(defaultValue, comment);
		}
	}
	
	public static class ConfigString extends Config<String>{
		public ConfigString(String defaultValue, String comment){
			super(defaultValue, comment);
		}
	}
	
	public static class ConfigStringArray extends Config<String[]>{
		public ConfigStringArray(String[] defaultValue, String comment){
			super(defaultValue, comment);
		}
	}
	
	public static class ConfigKeyboard{
		public int keyCode;
	}
	
	public static class ConfigJoystick{
		public String joystickName;
		public int buttonIndex;
		public boolean invertedAxis;
		public double axisMinTravel;
		public double axisMaxTravel;
	}
}
