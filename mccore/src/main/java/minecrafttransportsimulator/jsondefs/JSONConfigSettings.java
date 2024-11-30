package minecrafttransportsimulator.jsondefs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import minecrafttransportsimulator.entities.instances.EntityBrewer;
import minecrafttransportsimulator.entities.instances.EntityFurnace;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.instances.ItemPartEngine;
import minecrafttransportsimulator.packloading.PackParser;

/**
 * Config class for game settings.  This is for internal code logic and is used on both
 * the server and client.  While this can be modified in-game, it needs to be done so
 * on both the client and server at the same time.
 *
 * @author don_bruce
 */
public class JSONConfigSettings {
    public ConfigGeneral general = new ConfigGeneral();
    public ConfigDamage damage = new ConfigDamage();
    public ConfigFuel fuel = new ConfigFuel();

    public static class ConfigGeneral {
        public JSONConfigEntry<Boolean> generateOverrideConfigs = new JSONConfigEntry<>(false, "If true, then crafting and damage override config files, as well as language template files, will be dumped into the config folder at boot.  WARNING: this will overwrite your existing override config files!  Language files must be put into packs to function and will NOT overwrite anything.");
        public JSONConfigEntry<Boolean> opSignEditingOnly = new JSONConfigEntry<>(false, "If true, only OPs will be able to edit signs on servers.  Does not affect client worlds.");
        public JSONConfigEntry<Boolean> opPickupVehiclesOnly = new JSONConfigEntry<>(false, "If true, only OPs will be able to pick up vehicles with wrenches.  Does not affect client worlds.");
        public JSONConfigEntry<Boolean> creativePickupVehiclesOnly = new JSONConfigEntry<>(false, "If true, vehicles can only be picked up in creative mode.");
        public JSONConfigEntry<Boolean> keyRequiredToStartVehicles = new JSONConfigEntry<>(false, "If true, vehicles can only be started if the player has the key in their hand.");
        public JSONConfigEntry<Boolean> noclipVehicles = new JSONConfigEntry<>(false, "If true, vehicles will not be able to collide with blocks.  This also prevents them from moving if they do not have wheels on them, as otherwise they would phase through the ground into the void.");
        public JSONConfigEntry<Boolean> chunkloadVehicles = new JSONConfigEntry<>(true, "If true, vehicles will be chunkloaded when on roads/tracks.  This allows them to travel into unloaded chunks.");
        public JSONConfigEntry<Boolean> doLegacyLightCompats = new JSONConfigEntry<>(true, "If true, legacy compatibility code will be performed on all models to make their lights work with newer versions.  This code will significantly slow down boot times due to needing to parse all models on boot, however, packs may not have functional lights without it.  Choose wisely if you want speed or features.");
        public JSONConfigEntry<Boolean> useHSV = new JSONConfigEntry<>(false, "If enabled, the MTS dev mode will export color values as HSV instead of hex.");
        public JSONConfigEntry<Boolean> allPlanesWithNav = new JSONConfigEntry<>(false, "If true, all planes will have the ability to use beacons for navigation.  This is false to allow pack authors to define new/old planes that have this capability, but can be turned on if you don't care about realism.");
        public JSONConfigEntry<Boolean> giveManualsOnJoin = new JSONConfigEntry<>(true, "If true, then whenever a new player joins a world, they will be given a copy of the mod manuals.");
        public JSONConfigEntry<Boolean> performModCompatFunctions = new JSONConfigEntry<>(true, "If true, mod compatibility functions will be executed.  Should be true unless you're configuring a modpack and know and have addressed the various incompatibilities.");
        public JSONConfigEntry<Boolean> forceRenderLastSolid = new JSONConfigEntry<>(false, "If enabled, MTS will do rendering on the world-last Forge event.  This is not normally something you want, but can be used if some mods make things invisible.  Replaymod being one such example.");
        public JSONConfigEntry<Boolean> devMode = new JSONConfigEntry<>(false, "If enabled, MTS will allow access to the dev mode GUI while sitting in vehicles.  The GUI allows editing JSONs in-game without the need to restart.");
        public JSONConfigEntry<Integer> roadMaxLength = new JSONConfigEntry<>(32, "How long a single segment of road can be, in blocks.  While you can have longer segements, be aware they have more collision boxes and will generate more lag when broken or interacted with, and may not render if outside the chunk they are in.  Must be a whole number.");
        public JSONConfigEntry<Double> aircraftSpeedFactor = new JSONConfigEntry<>(0.35D, "Factor to apply to aircraft movement.  1 is the realistic value, but this makes vehicles move too fast for Minecraft. Adjust with caution.");
        public JSONConfigEntry<Double> carSpeedFactor = new JSONConfigEntry<>(0.35D, "Factor to apply to car movement.  1 is the realistic value, but this makes vehicles move too fast for Minecraft. Adjust with caution.");
        public JSONConfigEntry<Double> fuelUsageFactor = new JSONConfigEntry<>(1.0D, "Factor times which engines use fuel.  Change this if you think engines use fuel too fast or slow.  Setting it to 0 will disable fuel and allow engines to run without it.");
        public JSONConfigEntry<Double> engineHoursFactor = new JSONConfigEntry<>(1.0D, "Factor times which engines hours accumulate.  Change this if you want to adjust how fast engines wear out.");
        public JSONConfigEntry<Double> climbSpeed = new JSONConfigEntry<>(0.125D, "How far a vehicle will 'climb' blocks every tick when the wheels go into the ground.  Higher values make vehicles climb blocks quicker at the cost of smooth movement.");
        public JSONConfigEntry<Double> gravityFactor = new JSONConfigEntry<>(1.0D, "Factor for gravitational forces applied to vehicles.  Can be adjusted if you think cars are too 'floaty'.  Does not affect aircraft.");
        public JSONConfigEntry<Double> maxFlightHeight = new JSONConfigEntry<>(0.0D, "How high planes can fly.  Setting this to 0 (default) will allow planes to fly as high as their natural physics allows.  Useful if you want to cap vertical height for some reason.");
        public JSONConfigEntry<Double> engineSpeedTempFactor = new JSONConfigEntry<>(1.0D, "Factor for how RPM affects engine temp.  Higher values will make engines heat up quicker at higher RPMs.");
        public JSONConfigEntry<Double> engineBiomeTempFactor = new JSONConfigEntry<>(1.0D, "Factor for how biome temp affects engine temp.  Higher values will make engines heat up quicker in hotter biomes.");
        public JSONConfigEntry<Double> rfToElectricityFactor = new JSONConfigEntry<>(0.02D, "Factor for converting RF to internal electicity for vehicles.  Default value is 1/100, but can be adjusted.");
        public JSONConfigEntry<Double> vehicleDeathDespawnTime = new JSONConfigEntry<>(0.0D, "Time (in seconds) between when vehicles reach 0 health and they de-spawn.  Normally 0, which means they never de-spawn.");
        public JSONConfigEntry<Integer> seaLevel = new JSONConfigEntry<>(63,"The Y-Level that will be used to base altitude off of. Will also be factored in for engine performance calculations. Change only if you know what you're doing/ why this matters to engines/flying.");
        public JSONConfigEntry<List<String>> defaultVehicleRepairMaterials = new JSONConfigEntry<>(new ArrayList<>(), "List of items that can be used to repair vehicles by default, if no repair recipe is set in the vehicle's JSON.");
        public JSONConfigEntry<List<String>> defaultPartRepairMaterials = new JSONConfigEntry<>(new ArrayList<>(), "List of items that can be used to repair part by default, if no repair recipe is set in the part's JSON.");
        public JSONConfigEntry<Set<String>> engineDimensionBlacklist = new JSONConfigEntry<>(new HashSet<>(), "Blacklist of dimension names where engines will be prevented from being started.  Can be used to disable vehicles in specific dimensions.  Think Galacticraft, where you don't want folks flying planes on the moon.");
        public JSONConfigEntry<Set<String>> engineDimensionWhitelist = new JSONConfigEntry<>(new HashSet<>(), "Whitelist of dimension names where engines will only be alowed to work.  Overrides the blacklist if this exists.");
        public JSONConfigEntry<Map<String, Double>> packVehicleScales = new JSONConfigEntry<>(new HashMap<>(), "Scale of all vehicles for this pack.  You probably won't want to change this, but if you do want the vehicles to be smaller for some reason, you can.");
        public JSONConfigEntry<Map<String, Double>> packSpeedFactors = new JSONConfigEntry<>(new HashMap<>(), "A mapping of pack-speciifc speed factors.  These values will only apply to the vehicles in the pack modified.  This allows for speeding up packs with slower vehicles, or slowing down packs with fast vehicles for a more even play experience.  Note that this applies on top of the global speed factor.  So if that is set to 0.30, and a pack is set to 0.5, then the total speed factor will be 0.15");
        public ConfigItemWeights itemWeights = new ConfigItemWeights();
        public JSONConfigEntry<List<UUID>> joinedPlayers = new JSONConfigEntry<>(new ArrayList<>(), "Listing of players that have joined this world.  Players will be given the manual on their first join.");

        public static class ConfigItemWeights {
            public String comment1 = "The following section is used for calculating item weights.  Any item that contains the text bits will have its weight multiplied by the following factor.";
            public String comment2 = "This mass factors into vehicle performance, with particular emphasis on vehicle payloads.  Some defaults are presented here, but you can add/remove to your liking.";
            public String comment3 = "Note that the part of the name searched is the item's registry name, NOT the in-game display name.  This is similar to what is in the /give command.";
            public Map<String, Double> weights = populateDefaultWeights();

            private static Map<String, Double> populateDefaultWeights() {
                Map<String, Double> weights = new HashMap<>();
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

    public static class ConfigDamage {
        public JSONConfigEntry<Boolean> bulletBlockBreaking = new JSONConfigEntry<>(true, "Whether or not bullets/bombs can break blocks when they hit them.  This will also disable explosive block breakage (though not explosions themselves) and prevent incendiary bullets from setting blocks on fire.");
        public JSONConfigEntry<Boolean> bulletExplosions = new JSONConfigEntry<>(true, "Whether or not bullets/bombs will blow up.  This does not disable block breakage, but it will pretty much nerf all block mass-destruction weapons.");
        public JSONConfigEntry<Boolean> vehicleBlockBreaking = new JSONConfigEntry<>(true, "Whether or not vehicles can break blocks when they run into them.  If false, vehicles will simply stop when they hit blocks.");
        public JSONConfigEntry<Boolean> vehicleDestruction = new JSONConfigEntry<>(true, "Whether or not vehicles can be destroyed by ramming into blocks at high speeds.  Set this to false if you don't like loosing vehicles to badly-placed walls.");
        public JSONConfigEntry<Boolean> vehicleExplosions = new JSONConfigEntry<>(true, "Whether or not vehicles explode when crashed or shot down.  If this is false, vehicles will simply poof when they crash.");
        public JSONConfigEntry<Boolean> creativePlayerDamage = new JSONConfigEntry<>(false, "If true, damage from vehicles and guns will be applied to creative players.  Not sure why you'd want this, but it's here.");
        public JSONConfigEntry<Boolean> wheelBreakage = new JSONConfigEntry<>(true, "Whether or not wheels can be broken (go flat).");
        public JSONConfigEntry<Boolean> wheelDamageIgnoreVelocity = new JSONConfigEntry<>(false, "Whether or not velocity is ignored when calculating wheel damage.");
        public JSONConfigEntry<Boolean> allowExternalDamage = new JSONConfigEntry<>(false, "Whether or not non-IV things can damage vehicles.  This is normally false, as external damage is a hassle for most people, but can be true if you want other mod's guns to be able to attack vehicles in addition to IV's.");
        public JSONConfigEntry<Double> propellerDamageFactor = new JSONConfigEntry<>(1.0D, "Factor for damage caused by a propeller.");
        public JSONConfigEntry<Double> jetDamageFactor = new JSONConfigEntry<>(1.0D, "Factor for damage caused by a jet engine.");
        public JSONConfigEntry<Double> wheelDamageFactor = new JSONConfigEntry<>(1.0D, "Factor for damage caused by wheels on vehicles.");
        public JSONConfigEntry<Double> crashDamageFactor = new JSONConfigEntry<>(1.0D, "Factor for damage caused by crashes.");
        public JSONConfigEntry<Double> bulletDamageFactor = new JSONConfigEntry<>(1.0D, "Factor for damage caused by bullets on vehicles.");
        public JSONConfigEntry<Double> wheelDamageMinimumVelocity = new JSONConfigEntry<>(0.2D, "Minimum velocity (blocks/second) which vehicles must be going to damage entities with their wheels.");
        public JSONConfigEntry<Map<String, Double>> packBulletDamageFactors = new JSONConfigEntry<>(new HashMap<>(), "A mapping of pack-speciifc bullet damage factors.  These values will apply to all bullets in a pack when they hit something, be it a vehicle or entity.");
    }

    public static class ConfigFuel {
        public String comment1 = "The following section is used for fuel configs for engines.  Each entry here is a single fuel type for engines.";
        public String comment2 = "Inside each entry exist values for the potency of the fluids that can be considered to be that fuel type.";
        public String comment3 = "You can make engines take different types of fluids for their fuels by editing these values.  See the in-game handbook for more information.";
        public String comment4 = "If you are having troubles getting the name for a particular fluid, run the game with the mods you want and check the 'lastLoadedFluids' list.  This contains all the fluids that were present during the last run of the game.";
        public String comment5 = "The first entry in the list is the registered fluid name, while the second entry will be the displayed name.  Only use the first name, as the second name changes based on game language settings.";
        public Map<String, Map<String, Double>> fuels;
        public Map<String, String> lastLoadedFluids;

        public static Map<String, Map<String, Double>> getDefaultFuels() {
            Map<String, Map<String, Double>> fuels = new HashMap<>();
            for (AItemPack<?> packItem : PackParser.getAllPackItems()) {
                if (packItem instanceof ItemPartEngine) {
                    ItemPartEngine engine = (ItemPartEngine) packItem;

                    //If we don't have the fuel in the fuel map, add it.
                    //Default fuel list depends on the fuel name.
                    if (!fuels.containsKey(engine.definition.engine.fuelType)) {
                        Map<String, Double> fluids = new HashMap<>();
                        try {
                            switch (FuelDefaults.valueOf(engine.definition.engine.fuelType.toUpperCase(Locale.ROOT))) {
                                case GASOLINE: {
                                    fluids.put("lava", 1.0);
                                    fluids.put("gasoline", 1.0);
                                    fluids.put("ethanol", 0.85);
                                    break;
                                }
                                case DIESEL: {
                                    fluids.put("lava", 1.0);
                                    fluids.put("diesel", 1.0);
                                    fluids.put("biodiesel", 0.8);
                                    fluids.put("creosote", 0.7);
                                    fluids.put("oil", 0.5);
                                    break;
                                }
                                case AVGAS: {
                                    fluids.put("lava", 1.0);
                                    fluids.put("gasoline", 1.0);
                                    break;
                                }
                                case REDSTONE: {
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
                                case NOTHING: {
                                    break;
                                }
                                default:
                                    fluids.put("lava", 1.0);
                            }
                        } catch (Exception e) {
                            fluids.put("lava", 1.0);
                        }
                        fuels.put(engine.definition.engine.fuelType, fluids);
                    }
                }

                //Also add furnace and brewing stand fuel.
                if (!fuels.containsKey(EntityFurnace.FURNACE_FUEL_NAME)) {
                    Map<String, Double> fluids = new HashMap<>();
                    fluids.put("lava", 1.0);
                    fuels.put(EntityFurnace.FURNACE_FUEL_NAME, fluids);
                }
                if (!fuels.containsKey(EntityBrewer.BREWER_FUEL_NAME)) {
                    Map<String, Double> fluids = new HashMap<>();
                    fluids.put("lava", 1.0);
                    fuels.put(EntityBrewer.BREWER_FUEL_NAME, fluids);
                }
            }
            return fuels;
        }
    }

    public enum FuelDefaults {
        GASOLINE,
        DIESEL,
        AVGAS,
        REDSTONE,
        NOTHING;
    }
}
