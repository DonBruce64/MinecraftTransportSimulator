package minecrafttransportsimulator.jsondefs;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.instances.ItemPartEngine;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.PackParserSystem;

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
	public ConfigClientRendering clientRendering = new ConfigClientRendering();
	public ConfigClientControls clientControls = new ConfigClientControls();
	public ConfigControls controls = new ConfigControls();
	
	public static class ConfigGeneral{
		public JSONConfigEntry<Boolean> dumpCraftingConfig = new JSONConfigEntry<Boolean>(false, "If true, then all recipes for all items in packs will be dumped into the config file at boot.  WARNING: this will overwrite your existing crafting overrides file!");
		public JSONConfigEntry<Boolean> opSignEditingOnly = new JSONConfigEntry<Boolean>(false, "If true, only OPs will be able to edit signs on servers.  Does not affect client worlds.");
		public JSONConfigEntry<Boolean> opPickupVehiclesOnly = new JSONConfigEntry<Boolean>(false, "If true, only OPs will be able to pick up vehicles with wrenches.  Does not affect client worlds.");
		public JSONConfigEntry<Boolean> creativePickupVehiclesOnly = new JSONConfigEntry<Boolean>(false, "If true, vehicles can only be picked up in creative mode.");
		public JSONConfigEntry<Boolean> blockBreakage = new JSONConfigEntry<Boolean>(true, "Whether or not vehicles and guns can break blocks when they hit them.  If false, vehicles will simply stop when they hit blocks, and guns won't break blocks when they hit them.  This will also disable explosive block breakage, though not explosions themselves.");
		public JSONConfigEntry<Boolean> vehicleDestruction = new JSONConfigEntry<Boolean>(true, "Whether or not vehicles can be destroyed by ramming into blocks at high speeds.  Set this to false if you don't like loosing vehicles to badly-placed walls.");
		public JSONConfigEntry<Boolean> noclipVehicles = new JSONConfigEntry<Boolean>(false, "If true, vehicles will not be able to collide with blocks.  This also prevents them from moving if they do not have wheels on them, as otherwise they would phase through the ground into the void.");
		public JSONConfigEntry<Boolean> creativeDamage = new JSONConfigEntry<Boolean>(false, "If true, damage from vehicles and guns will be applied to creative players.");
		public JSONConfigEntry<Boolean> chunkloadVehicles = new JSONConfigEntry<Boolean>(true, "If true, vehicles will be chunkloaded when on roads/tracks.  This allows them to travel into unloaded chunks.");
		public JSONConfigEntry<Boolean> doLegacyLightCompats = new JSONConfigEntry<Boolean>(true, "If true, legacy compatibility code will be performed on all models to make their lights work with newer versions.  This code will significantly slow down boot times due to needing to parse all models on boot, however, packs may not have functional lights without it.  Choose wisely if you want speed or features.");
		public JSONConfigEntry<Boolean> useHSV = new JSONConfigEntry<Boolean>(false, "If enabled, the MTS dev mode will export color values as HSV instead of hex.");
		public JSONConfigEntry<Boolean> allPlanesWithNav = new JSONConfigEntry<Boolean>(false, "If true, all planes will have the ability to use beacons for navigation.  This is false to allow pack authors to define new/old planes that have this capability, but can be turned on if you don't care about realism.");
		public JSONConfigEntry<Double> speedFactor = new JSONConfigEntry<Double>(0.35D, "Factor to apply to vehicle movement.  1 is the realistic value, but this makes vehicles move too fast for Minecraft. Adjust with caution.");
		public JSONConfigEntry<Double> fuelUsageFactor = new JSONConfigEntry<Double>(1.0D, "Factor times which engines use fuel.  Change this if you think engines use fuel too fast or slow.");
		public JSONConfigEntry<Double> engineHoursFactor = new JSONConfigEntry<Double>(1.0D, "Factor times which engines hours accumulate.  Change this if you want to adjust how fast engines wear out.");
		public JSONConfigEntry<Double> climbSpeed = new JSONConfigEntry<Double>(0.125D, "How far a vehicle will 'climb' blocks every tick when the wheels go into the ground.  Higher values make vehicles climb blocks quicker at the cost of smooth movement.");
		public JSONConfigEntry<Double> gravityFactor = new JSONConfigEntry<Double>(1.0D, "Factor for gravitational forces applied to vehicles.  Can be adjusted if you think cars are too 'floaty'.  Does not affect aircraft.");
		public JSONConfigEntry<Double> engineSpeedTempFactor = new JSONConfigEntry<Double>(1.0D, "Factor for how RPM affects engine temp.  Higher values will make engines heat up quicker at higher RPMs.");
		public JSONConfigEntry<Double> engineBiomeTempFactor = new JSONConfigEntry<Double>(1.0D, "Factor for how biome temp affects engine temp.  Higher values will make engines heat up quicker in hotter biomes.");
		public JSONConfigEntry<Set<String>> joinedPlayers = new JSONConfigEntry<Set<String>>(new HashSet<String>(), "Listing of players that have joined this world.  Players will be given the manual on their first join.");
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
		public JSONConfigEntry<Boolean> explosions = new JSONConfigEntry<Boolean>(true, "Whether or not vehicles explode when crashed or shot down.");
		public JSONConfigEntry<Boolean> wheelBreakage = new JSONConfigEntry<Boolean>(true, "Whether or not wheels can be broken (go flat).");
		public JSONConfigEntry<Boolean> wheelDamageIgnoreVelocity = new JSONConfigEntry<Boolean>(false, "Whether or not velocity is ignored when calculating wheel damage.");
		public JSONConfigEntry<Double> propellerDamageFactor = new JSONConfigEntry<Double>(1.0D, "Factor for damage caused by a propeller.");
		public JSONConfigEntry<Double> jetDamageFactor = new JSONConfigEntry<Double>(1.0D, "Factor for damage caused by a jet engine.");
		public JSONConfigEntry<Double> wheelDamageFactor = new JSONConfigEntry<Double>(1.0D, "Factor for damage caused by wheels on vehicles.");
		public JSONConfigEntry<Double> crashDamageFactor = new JSONConfigEntry<Double>(1.0D, "Factor for damage caused by crashes.");
		public JSONConfigEntry<Double> bulletDamageFactor = new JSONConfigEntry<Double>(1.0D, "Factor for damage caused by bullets on vehicles.");
		public JSONConfigEntry<Double> engineLeakProbability = new JSONConfigEntry<Double>(0.01D, "Chance an engine will spring a leak if hit.  Explosions cause 10x this chance.");
		public JSONConfigEntry<Double> crashItemDropPercentage = new JSONConfigEntry<Double>(0.75D, "Percent that a crafting ingredient will be dropped when a vehicle is crashed.  Note that fire/explosions may destroy these items if enabled, so just because they drop does not mean you will get all of them.");
		public JSONConfigEntry<Double> wheelDamageMinimumVelocity = new JSONConfigEntry<Double>(0.2D, "Minimum velocity (blocks/second) which vehicles must be going to damage entities with their wheels.");
	}
	
	public static class ConfigFuel{
		public String comment1 = "The following section is used for fuel configs for engines.  Each entry here is a single fuel type for engines.";
		public String comment2 = "Inside each entry exist values for the potency of the fluids that can be considered to be that fuel type.";
		public String comment3 = "You can make engines take different types of fluids for their fuels by editing these values.  See the in-game handbook for more information.";
		public String comment4 = "If you are having troubles getting the name for a particular fluid, run the game with the mods you want and check the 'lastLoadedFluids' list.  This contains all the fluids that were present during the last run of the game.";
		public Map<String, Map<String, Double>> fuels;
		public Map<String, String> lastLoadedFluids;
		
		public static Map<String, Map<String, Double>> getDefaultFuels(){
			Map<String, Map<String, Double>> fuels = new HashMap<String, Map<String, Double>>();
			for(AItemPack<?> packItem : PackParserSystem.getAllPackItems()){
				if(packItem instanceof ItemPartEngine){
					ItemPartEngine engine = (ItemPartEngine) packItem;
					//For old packs, if we don't have a fuelType set it to diesel.
					//This is because it's the most versatile fuel, and all the old packs have heavy equipment.
					if(engine.definition.engine.fuelType == null){
						engine.definition.engine.fuelType = "diesel";
					}
					
					//If we don't have the fuel in the fuel map, add it.
					//Default fuel list depends on the fuel name.
					if(!fuels.containsKey(engine.definition.engine.fuelType)){
						Map<String, Double> fluids = new HashMap<String, Double>();
						try{
							switch(FuelDefaults.valueOf(engine.definition.engine.fuelType.toUpperCase())){
								case GASOLINE :{
									fluids.put("lava", 1.0);
									fluids.put("gasoline", 1.0);
									fluids.put("ethanol", 0.85);
									break;
								}
								case DIESEL :{
									fluids.put("lava", 1.0);
									fluids.put("diesel", 1.0);
									fluids.put("biodiesel", 0.8);
									fluids.put("creosote", 0.7);
									fluids.put("oil", 0.5);
									break;
								}
								case AVGAS :{
									fluids.put("lava", 1.0);
									fluids.put("gasoline", 1.0);
									break;
								}
								case REDSTONE :{
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
								default: fluids.put("lava", 1.0);
							}
						}catch(Exception e){
							fluids.put("lava", 1.0);
						}
						fuels.put(engine.definition.engine.fuelType, fluids);
					}
				}
			}
			return fuels;
		}
		
		public static enum FuelDefaults{
			GASOLINE,
			DIESEL,
			AVGAS,
			REDSTONE;
		}
	}
	
	public static class ConfigClientRendering{
		public JSONConfigEntry<Boolean> renderHUD_1P = new JSONConfigEntry<Boolean>(true, "If false, the HUD in vehicles will not render in 1st-person mode.");
		public JSONConfigEntry<Boolean> renderHUD_3P = new JSONConfigEntry<Boolean>(true, "If false, the HUD in vehicles will not render in 3rd-person mode.");
		
		public JSONConfigEntry<Boolean> fullHUD_1P = new JSONConfigEntry<Boolean>(false, "If true, the full-size HUD will render in 1st-person rather than the half-size HUD.");
		public JSONConfigEntry<Boolean> fullHUD_3P = new JSONConfigEntry<Boolean>(false, "If true, the full-size HUD will render in 3rd-person rather than the half-size HUD.");
		
		public JSONConfigEntry<Boolean> transpHUD_1P = new JSONConfigEntry<Boolean>(false, "If true, the background textures for the HUD will not be rendered in 1st-person.");
		public JSONConfigEntry<Boolean> transpHUD_3P = new JSONConfigEntry<Boolean>(false, "If true, the background textures for the HUD will not be rendered in 1st-person.");
		
		public JSONConfigEntry<Boolean> renderWindows = new JSONConfigEntry<Boolean>(true, "Should the glass on windows be rendered on vehicles?");
		public JSONConfigEntry<Boolean> innerWindows = new JSONConfigEntry<Boolean>(false, "Should the glass on windows be rendered on the inside of the vehicle?  Note: if renderWindows is false, this config has no effect.");
		
		public JSONConfigEntry<Boolean> vehicleBeams = new JSONConfigEntry<Boolean>(true, "If false, beams on vehicles will not render.");
		public JSONConfigEntry<Boolean> blockBeams = new JSONConfigEntry<Boolean>(true, "If false, beams on blocks will not render.");
		
		public JSONConfigEntry<Boolean> brightLights = new JSONConfigEntry<Boolean>(true, "If false, lights from vehicles and blocks will not do brightness blending and will render as if they were part of the model at that same brightness.  Useful if you have shaders and this is causing troubles.");
		public JSONConfigEntry<Boolean> blendedLights = new JSONConfigEntry<Boolean>(true, "If false, beam-based lights from vehicles and blocks will not do brightness blending.  This is different from the general brightness setting as this will do OpenGL blending on the world to make it brighter, not just the beams themselves.");
		
		public JSONConfigEntry<Boolean> playerTweaks = new JSONConfigEntry<Boolean>(true, "If true, player hands will be modified when holding guns, and hands and legs will be modified when riding in vehicles.  Set this to false if mods cause issues, like two-hand rendering or player model issues.");
		
	}
	
	public static class ConfigClientControls{
		public JSONConfigEntry<Boolean> mouseYoke = new JSONConfigEntry<Boolean>(false, "Enable mouse yoke for vehicles? Prevents looking around unless unlocked.  Think MCHeli controls.");
		public JSONConfigEntry<Boolean> kbOverride = new JSONConfigEntry<Boolean>(true, "Should keyboard controls be ignored when a joystick control is mapped?  Leave true to free up the keyboard while using a joysick.");
		
		public JSONConfigEntry<Boolean> simpleThrottle = new JSONConfigEntry<Boolean>(true, "If true, then vehicles will automatically go into reverse after stopped with the brake rather than staying stopped and waiting for you to shift.  When going in reverse, the opposite is true: the vehicle will shift into forwards when pressing forwards when stopped.  Additionally, the parking brake will automatically be set when leaving the vehicle.");
		public JSONConfigEntry<Boolean> halfThrottle = new JSONConfigEntry<Boolean>(false, "If true, then the gas key will only be a half-throttle, with the MOD+Throttle key becoming the full-speed control.  Useful if you want a more controlled vehicle experience.  Only valid on car/boat types with on-off throttles, and does not work in conjunction with simpleThrottle as that changes how the MOD key works with gas and brake keys.");
		
		public JSONConfigEntry<Boolean> autostartEng = new JSONConfigEntry<Boolean>(true, "If true, engines will automatically start when a driver enters a vehicle, and will turn off when they leave.  The parking brake will also be applied when leaving the vehicle.  Note: this does not bypass the fuel or electrical system.");
		public JSONConfigEntry<Boolean> autoTrnSignals = new JSONConfigEntry<Boolean>(true, "If true, turns signals will come on automatically when you start a turn, and will turn off when the turn completes.  If this is false, then they will only be able to be activated with the keybinds or via the panel.");
		
		public JSONConfigEntry<Boolean> north360 = new JSONConfigEntry<Boolean>(false, "If true, instruments will represent North as 360 degrees, instead of the Minecraft default of 180. Allows using the heading system that real-world pilots and militaries do.");
		public JSONConfigEntry<Boolean> classicJystk = new JSONConfigEntry<Boolean>(false, "If true, the classic controller code will be used.  Note: THIS CODE MAY CRASH MOBILE DEVICES!  Also note that switching will probably mess up your keybinds.  Only do this if you are having issues with a joystick or controller not being recognized.  After changing this setting, reboot the game to make it take effect.");
		
		public JSONConfigEntry<Boolean> devMode = new JSONConfigEntry<Boolean>(false, "If enabled, MTS will allow access to the dev mode GUI while sitting in vehicles.  The GUI allows editing JSONs in-game without the need to restart.");

		public JSONConfigEntry<Integer> controlSurfaceCooldown = new JSONConfigEntry<Integer>(4, "How long (in ticks) it takes before control surfaces try to return to their natural angle.  This is not used when using a joystick.");
		public JSONConfigEntry<Integer> steeringIncrement = new JSONConfigEntry<Integer>(20, "How many units (1/10 of a degree) to turn the wheels on vehicles for every tick the button is held down.  This is not used when using a joystick.");
		public JSONConfigEntry<Integer> flightIncrement = new JSONConfigEntry<Integer>(6, "How many units (1/10 of a degree) to move the elevators and ailerons on aircraft for every tick the button is held down.  This is not used when using a joystick.");
		public JSONConfigEntry<Double> joystickDeadZone = new JSONConfigEntry<Double>(0.03D, "Dead zone for joystick axis.  This is NOT joystick specific.");
	}
	
	public static class ConfigControls{
		public Map<String, ConfigKeyboard> keyboard = new HashMap<String, ConfigKeyboard>();
		public Map<String, ConfigJoystick>  joystick = new HashMap<String, ConfigJoystick>();
	}
	
	
	public static class JSONConfigEntry<ConfigType>{
		public ConfigType value;
		public String comment;
		
		public JSONConfigEntry(ConfigType defaultValue, String comment){
			this.value = defaultValue;
			this.comment = comment;
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
