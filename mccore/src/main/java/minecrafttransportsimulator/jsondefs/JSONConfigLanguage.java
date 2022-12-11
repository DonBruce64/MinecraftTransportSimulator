package minecrafttransportsimulator.jsondefs;

import java.util.LinkedHashMap;
import java.util.Map;

import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packloading.PackParser;

/**
 * Config class for language interfacing.  This contains all default text strings, and will be loaded
 * by both the client and server.  When choosing a file to load, the current language suffix will be used
 * on clients, whereas servers will always use the default, english language.  If a file doesn't exist,
 * or is missing an entry, then the default value will be used instead.
 *
 * @author don_bruce
 */
public class JSONConfigLanguage {
    public Map<String, String> core = new LinkedHashMap<>();
    public Map<String, Map<String, JSONItemEntry>> packs = new LinkedHashMap<>();
    public static final Map<String, LanguageEntry> coreEntries = new LinkedHashMap<>();

    public void populateEntries(boolean isClient) {
        boolean overrideJSONWithDefinedValues = !isClient || InterfaceManager.clientInterface.usingDefaultLanguage();
        //First populate core entries.
        for (LanguageEntry entry : coreEntries.values()) {
            if (!core.containsKey(entry.key) || overrideJSONWithDefinedValues) {
                core.put(entry.key, entry.value);
            } else {
                entry.value = core.get(entry.key);
            }
        }

        //Populate pack entries.
        for (String packID : PackParser.getAllPackIDs()) {
            Map<String, JSONItemEntry> packMap = packs.computeIfAbsent(packID, k -> new LinkedHashMap<>());
            for (AItemPack<?> packItem : PackParser.getAllItemsForPack(packID, true)) {
                String itemKey = packItem.getRegistrationName();
                JSONItemEntry entry = packMap.get(itemKey);
                if (entry == null) {
                    entry = new JSONItemEntry();
                    packMap.put(itemKey, entry);
                }
                String itemName = null;
                if (packItem instanceof AItemSubTyped) {
                    AItemSubTyped<?> subTyped = (AItemSubTyped<?>) packItem;
                    for (JSONSubDefinition subDefinition : subTyped.definition.definitions) {
                        if (subDefinition.subName.equals(subTyped.subDefinition.subName)) {
                            itemName = subDefinition.name;
                            break;
                        }
                    }
                }
                if (itemName == null) {
                    itemName = packItem.definition.general.name != null ? packItem.definition.general.name : packItem.definition.systemName;
                }
                String itemDescription = packItem.definition.general.description != null ? packItem.definition.general.description : "";
                if (!itemName.equals(entry.name) || !itemDescription.equals(entry.description) || overrideJSONWithDefinedValues) {
                    if (entry.name == null || overrideJSONWithDefinedValues) {
                        entry.name = itemName;
                    }
                    if (entry.description == null || overrideJSONWithDefinedValues) {
                        entry.description = itemDescription;
                    }
                }
            }
        }
    }

    public static class LanguageEntry {
        public final String key;
        public String value;

        public LanguageEntry(String key, String defaultValue) {
            this.key = key;
            this.value = defaultValue;
            coreEntries.put(key, this);
        }
    }

    public static class JSONItemEntry {
        public String name;
        public String description;
    }

    //List of language entries are kept in this file, as it ensures we init them all when this class is loaded.
    //If we made them static in other classes, then they wouldn't all get created at one time and wouldn't load right.
    public static final LanguageEntry DEATH_BULLET_NULL = new LanguageEntry("death.bullet.null", "%s was shot by a spy");
    public static final LanguageEntry DEATH_BULLET_PLAYER = new LanguageEntry("death.bullet.player", "%s was shot by %s");

    public static final LanguageEntry DEATH_PROPELLER_NULL = new LanguageEntry("death.propeller.null", "%s was shredded by a propeller");
    public static final LanguageEntry DEATH_PROPELLER_PLAYER = new LanguageEntry("death.propeller.player", "%s was shredded by %s's propeller");

    public static final LanguageEntry DEATH_JETINTAKE_NULL = new LanguageEntry("death.jet_intake.null", "%s was sucked into a jet engine");
    public static final LanguageEntry DEATH_JETINTAKE_PLAYER = new LanguageEntry("death.jet_intake.player", "%s was sucked into %s's jet engine");

    public static final LanguageEntry DEATH_JETEXHAUST_NULL = new LanguageEntry("death.jet_exhaust.null", "%s was burnt to a crisp by a jet engine");
    public static final LanguageEntry DEATH_JETEXHAUST_PLAYER = new LanguageEntry("death.jet_exhaust.player", "%s was burnt to a crisp by %s's jet engine");

    public static final LanguageEntry DEATH_WHEEL_NULL = new LanguageEntry("death.wheel.null", "%s was run over");
    public static final LanguageEntry DEATH_WHEEL_PLAYER = new LanguageEntry("death.wheel.player", "%s was run over by %s");

    public static final LanguageEntry DEATH_CRASH_NULL = new LanguageEntry("death.crash.null", "%s died in a crash");
    public static final LanguageEntry DEATH_CRASH_PLAYER = new LanguageEntry("death.crash.player", "%s crashed their vehicle and killed %s");

    public static final LanguageEntry GUI_CONFIRM = new LanguageEntry("gui.confirm", "CONFIRM");
    public static final LanguageEntry GUI_MASTERCONFIG = new LanguageEntry("gui.masterconfig", "Open configuration screen");

    public static final LanguageEntry GUI_SIGNALCONTROLLER_SCAN = new LanguageEntry("gui.signalcontroller.scan", "Scan For Signals & Components");
    public static final LanguageEntry GUI_SIGNALCONTROLLER_SCANDISTANCE = new LanguageEntry("gui.signalcontroller.scandistance", "Radius: ");
    public static final LanguageEntry GUI_SIGNALCONTROLLER_SCANCENTER = new LanguageEntry("gui.signalcontroller.scancenter", "Center (X/Z): ");
    public static final LanguageEntry GUI_SIGNALCONTROLLER_SCANFOUND = new LanguageEntry("gui.signalcontroller.scanfound", "Found: ");
    public static final LanguageEntry GUI_SIGNALCONTROLLER_RIGHTHANDDRIVE = new LanguageEntry("gui.signalcontroller.righthanddrive", "Right-Hand Drive");
    public static final LanguageEntry GUI_SIGNALCONTROLLER_LEFTHANDDRIVE = new LanguageEntry("gui.signalcontroller.lefthanddrive", "Left-Hand Drive");
    public static final LanguageEntry GUI_SIGNALCONTROLLER_TIMEMODE = new LanguageEntry("gui.signalcontroller.timemode", "Time Delay");
    public static final LanguageEntry GUI_SIGNALCONTROLLER_TRIGGERMODE = new LanguageEntry("gui.signalcontroller.triggermode", "Vehicle Trigger");
    public static final LanguageEntry GUI_SIGNALCONTROLLER_PRIMARYAXIS = new LanguageEntry("gui.signalcontroller.primaryaxis", "Main Axis: ");
    public static final LanguageEntry GUI_SIGNALCONTROLLER_LANEWIDTH = new LanguageEntry("gui.signalcontroller.lanewidth", "Lane Width: ");
    public static final LanguageEntry GUI_SIGNALCONTROLLER_GREENMAINTIME = new LanguageEntry("gui.signalcontroller.greenmaintime", "Green Time (Main): ");
    public static final LanguageEntry GUI_SIGNALCONTROLLER_GREENCROSSTIME = new LanguageEntry("gui.signalcontroller.greencrosstime", "Green Time (Cross): ");
    public static final LanguageEntry GUI_SIGNALCONTROLLER_YELLOWMAINTIME = new LanguageEntry("gui.signalcontroller.yellowmaintime", "Yellow Time (Main): ");
    public static final LanguageEntry GUI_SIGNALCONTROLLER_YELLOWCROSSTIME = new LanguageEntry("gui.signalcontroller.yellowcrosstime", "Yellow Time (Cross): ");
    public static final LanguageEntry GUI_SIGNALCONTROLLER_ALLREDTIME = new LanguageEntry("gui.signalcontroller.allredtime", "All Red Time: ");
    public static final LanguageEntry GUI_SIGNALCONTROLLER_LANESETTINGS = new LanguageEntry("gui.signalcontroller.lanesettings", "LANE SETTINGS");
    public static final LanguageEntry GUI_SIGNALCONTROLLER_SIGNALSETTINGS = new LanguageEntry("gui.signalcontroller.lanesettings", "SIGNAL SETTINGS");
    public static final LanguageEntry GUI_SIGNALCONTROLLER_LEFTLANES = new LanguageEntry("gui.signalcontroller.leftlanes", "# Left Lanes: ");
    public static final LanguageEntry GUI_SIGNALCONTROLLER_CENTERLANES = new LanguageEntry("gui.signalcontroller.centerlanes", "# Center Lanes: ");
    public static final LanguageEntry GUI_SIGNALCONTROLLER_RIGHTLANES = new LanguageEntry("gui.signalcontroller.rightlanes", "# Right Lanes: ");
    public static final LanguageEntry GUI_SIGNALCONTROLLER_ROADWIDTH = new LanguageEntry("gui.signalcontroller.roadwidth", "Road Width: ");
    public static final LanguageEntry GUI_SIGNALCONTROLLER_CENTERDIST = new LanguageEntry("gui.signalcontroller.centerdist", "Dist Center->Road: ");
    public static final LanguageEntry GUI_SIGNALCONTROLLER_MEDIANDIST = new LanguageEntry("gui.signalcontroller.mediandist", "Dist Road->Median: ");

    public static final LanguageEntry GUI_PART_BENCH_WEIGHT = new LanguageEntry("gui.part_bench.weight", "Weight (kg): ");
    public static final LanguageEntry GUI_PART_BENCH_FUEL = new LanguageEntry("gui.part_bench.fuel", "Fuel Cap. (mb): ");
    public static final LanguageEntry GUI_PART_BENCH_CONTROLLERS = new LanguageEntry("gui.part_bench.controllers", "Drivers/Pilots: ");
    public static final LanguageEntry GUI_PART_BENCH_PASSENGERS = new LanguageEntry("gui.part_bench.passengers", "Passengers: ");
    public static final LanguageEntry GUI_PART_BENCH_CARGO = new LanguageEntry("gui.part_bench.cargo", "Cargo: ");
    public static final LanguageEntry GUI_PART_BENCH_MIXED = new LanguageEntry("gui.part_bench.mixed", "Mixed: ");
    public static final LanguageEntry GUI_PART_BENCH_ENGINE = new LanguageEntry("gui.part_bench.engine", "Engine Size: ");
    public static final LanguageEntry GUI_PART_BENCH_WHEEL = new LanguageEntry("gui.part_bench.wheel", "Wheel Size: ");
    public static final LanguageEntry GUI_PART_BENCH_COLOR = new LanguageEntry("gui.part_bench.color", "Color: ");

    public static final LanguageEntry GUI_PANEL_START = new LanguageEntry("gui.panel.start", "START");
    public static final LanguageEntry GUI_PANEL_MAGNETO = new LanguageEntry("gui.panel.magneto", "MAG");
    public static final LanguageEntry GUI_PANEL_ENGINE = new LanguageEntry("gui.panel.engine", "ENGINE");
    public static final LanguageEntry GUI_PANEL_TRIM_ROLL = new LanguageEntry("gui.panel.trim_roll", "ROLL TRIM");
    public static final LanguageEntry GUI_PANEL_TRIM_PITCH = new LanguageEntry("gui.panel.trim_pitch", "PITCH TRIM");
    public static final LanguageEntry GUI_PANEL_TRIM_YAW = new LanguageEntry("gui.panel.trim_yaw", "YAW TRIM");
    public static final LanguageEntry GUI_PANEL_REVERSE = new LanguageEntry("gui.panel.reverse", "REV");
    public static final LanguageEntry GUI_PANEL_GEAR = new LanguageEntry("gui.panel.gear", "GEAR");
    public static final LanguageEntry GUI_PANEL_BEACON = new LanguageEntry("gui.panel.beacon", "BEACON");
    public static final LanguageEntry GUI_PANEL_AUTOPILOT = new LanguageEntry("gui.panel.autopilot", "AUTO");
    public static final LanguageEntry GUI_PANEL_CRUISECONTROL = new LanguageEntry("gui.panel.cruisecontrol", "CRUISE");

    public static final LanguageEntry GUI_INSTRUMENTS_MAIN = new LanguageEntry("gui.instruments.main", "Main Instruments");
    public static final LanguageEntry GUI_INSTRUMENTS_PANEL = new LanguageEntry("gui.instruments.panel", "Control Panel");
    public static final LanguageEntry GUI_INSTRUMENTS_CLEAR = new LanguageEntry("gui.instruments.clear", "CLEAR");
    public static final LanguageEntry GUI_INSTRUMENTS_IDLE = new LanguageEntry("gui.instruments.idle", "Select an instrument.");
    public static final LanguageEntry GUI_INSTRUMENTS_DECIDE = new LanguageEntry("gui.instruments.decide", "Click an instrument to add.");

    public static final LanguageEntry GUI_CONFIG_HEADER_RENDERING = new LanguageEntry("gui.config.header.rendering", "RENDERING");
    public static final LanguageEntry GUI_CONFIG_HEADER_CONFIG = new LanguageEntry("gui.config.header.config", "CONFIG");
    public static final LanguageEntry GUI_CONFIG_HEADER_CONTROLS = new LanguageEntry("gui.config.header.controls", "CONTROLS");

    public static final LanguageEntry GUI_CONFIG_CONTROLS_TITLE = new LanguageEntry("gui.config.controls.title", "Choose a configuration to change: ");
    public static final LanguageEntry GUI_CONFIG_CONTROLS_VOLUME = new LanguageEntry("gui.config.controls.volume", "Volume: ");
    public static final LanguageEntry GUI_CONFIG_CONTROLS_AIRCRAFT_KEYBOARD = new LanguageEntry("gui.config.controls.aircraft.keyboard", "AIRCRAFT KEYBOARD");
    public static final LanguageEntry GUI_CONFIG_CONTROLS_AIRCRAFT_JOYSTICK = new LanguageEntry("gui.config.controls.aircraft.joystick", "AIRCRAFT JOYSTICK");
    public static final LanguageEntry GUI_CONFIG_CONTROLS_CAR_KEYBOARD = new LanguageEntry("gui.config.controls.car.keyboard", "CAR/BOAT KEYBOARD");
    public static final LanguageEntry GUI_CONFIG_CONTROLS_CAR_JOYSTICK = new LanguageEntry("gui.config.controls.car.joystick", "CAR/BOAT JOYSTICK");
    public static final LanguageEntry GUI_CONFIG_JOYSTICK_ERROR = new LanguageEntry("gui.config.joystick.error", "ERROR: MTS could not initialize the control system!  This is likely due to a device mis-representing itself as a joystick to Java.  Mice have been known to have this issue, especially Corsair mice, so try a different mouse.  You may also try un-plugging your mouse, and then opening this GUI to re-poll the joysticks.  This won't make controls work on boot, but can be used if you only have one mouse.");
    public static final LanguageEntry GUI_CONFIG_JOYSTICK_DISABLED = new LanguageEntry("gui.config.joystick.disabled", "ERROR: The control system locked up when MTS tried to initialize it.  This is likely a driver issue, and happens for some reason on some machines.  The root cause is not known, but if you find out how to make this erorr go away, let us know!  For now, joysick support will be disabled.");
    public static final LanguageEntry GUI_CONFIG_JOYSTICK_SELECT = new LanguageEntry("gui.config.joystick.select", "Choose a joystick: ");
    public static final LanguageEntry GUI_CONFIG_JOYSTICK_NAME = new LanguageEntry("gui.config.joystick.name", "Name: ");
    public static final LanguageEntry GUI_CONFIG_JOYSTICK_MAPPING = new LanguageEntry("gui.config.joystick.mapping", "Mapping");
    public static final LanguageEntry GUI_CONFIG_JOYSTICK_DEADZONE = new LanguageEntry("gui.config.joystick.deadzone", "Dead-zone: ");
    public static final LanguageEntry GUI_CONFIG_JOYSTICK_STATE = new LanguageEntry("gui.config.joystick.state", "State: ");
    public static final LanguageEntry GUI_CONFIG_JOYSTICK_ASSIGNMENT = new LanguageEntry("gui.config.joystick.assignment", "Assigned to: ");
    public static final LanguageEntry GUI_CONFIG_JOYSTICK_CANCEL = new LanguageEntry("gui.config.joystick.cancel", "Cancel");
    public static final LanguageEntry GUI_CONFIG_JOYSTICK_CLEAR = new LanguageEntry("gui.config.joystick.clear", "Clear Assignment");
    public static final LanguageEntry GUI_CONFIG_JOYSTICK_CHOOSEMAP = new LanguageEntry("gui.config.joystick.choosemap", "Choose what gets mapped to this button.");
    public static final LanguageEntry GUI_CONFIG_JOYSTICK_CALIBRATE1 = new LanguageEntry("gui.config.joystick.calibrate1", "Move axis until numbers stop changing.");
    public static final LanguageEntry GUI_CONFIG_JOYSTICK_CALIBRATE2 = new LanguageEntry("gui.config.joystick.calibrate2", "Then hit confirm to save settings.");
    public static final LanguageEntry GUI_CONFIG_JOYSTICK_AXISMODE = new LanguageEntry("gui.config.joystick.axismode", "Axis mode: ");
    public static final LanguageEntry GUI_CONFIG_JOYSTICK_NORMAL = new LanguageEntry("gui.config.joystick.normal", "Normal");
    public static final LanguageEntry GUI_CONFIG_JOYSTICK_INVERT = new LanguageEntry("gui.config.joystick.invert", "Inverted");

    public static final LanguageEntry GUI_PACKMISSING_TITLE = new LanguageEntry("gui.packmissing.title", "!ERROR!");
    public static final LanguageEntry GUI_PACKMISSING_TEXT = new LanguageEntry("gui.packmissing.text", "MTS has detected that it has been started without a content pack.  This will result in NO vehicles being present!  If you do NOT have a content pack yet installed, please see the Curse page for this mod for a link to one.  If you DID download a pack ensure that it is the same place as all your other mods.  If the pack is in that location, and you are still seeing this message, ensure you are running the correct pack version.");

    public static final LanguageEntry INTERACT_KEY_LOCK = new LanguageEntry("interact.key.lock", "Vehicle locked!");
    public static final LanguageEntry INTERACT_KEY_UNLOCK = new LanguageEntry("interact.key.unlock", "Vehicle unlocked!");
    public static final LanguageEntry INTERACT_KEY_BIND = new LanguageEntry("interact.key.bind", "Key bound to vehicle.  Ready for locking/unlocking use.");
    public static final LanguageEntry INTERACT_KEY_NOTOWNER = new LanguageEntry("interact.key.notowner", "This key is not for any vehicle, and only the owner of this vehicle may make new keys!");
    public static final LanguageEntry INTERACT_KEY_WRONGKEY = new LanguageEntry("interact.key.wrongkey", "This key does not go to this vehicle!");

    public static final LanguageEntry INTERACT_FUELPUMP_TOOFAR = new LanguageEntry("interact.fuelpump.toofar", "The pump is too far away from the vehicle.  The pump should be no more than 16 blocks away.");
    public static final LanguageEntry INTERACT_FUELPUMP_WRONGTYPE = new LanguageEntry("interact.fuelpump.wrongtype", "This pump does not contain the same fuel type as what is in the vehicle.");
    public static final LanguageEntry INTERACT_FUELPUMP_WRONGENGINES = new LanguageEntry("interact.fuelpump.wrongengines", "The contents in this pump is not a valid fuel for the engines in the vehicle.  Check the manual for details.");
    public static final LanguageEntry INTERACT_FUELPUMP_CONNECT = new LanguageEntry("interact.fuelpump.connect", "Connected and fueling.");
    public static final LanguageEntry INTERACT_FUELPUMP_COMPLETE = new LanguageEntry("interact.fuelpump.complete", "Vehicle is full, disconnecting.");
    public static final LanguageEntry INTERACT_FUELPUMP_EMPTY = new LanguageEntry("interact.fuelpump.empty", "Pump is empty, disconnecting.");
    public static final LanguageEntry INTERACT_FUELPUMP_DISCONNECT = new LanguageEntry("interact.fuelpump.disconnect", "Disconnecting and stopping pumping.");

    public static final LanguageEntry INTERACT_ROAD_SET = new LanguageEntry("interact.road.set", "Setting first dynamic road point.");
    public static final LanguageEntry INTERACT_ROAD_ALREADYCONNECTED = new LanguageEntry("interact.road.alreadyconnected", "This road segment is already connected to another road.");
    public static final LanguageEntry INTERACT_ROAD_LANEMISMATCHFIRST = new LanguageEntry("interact.road.lanemismatchfirst", "The number of lanes at the road segment clicked first does not match the number of lanes for this road.");
    public static final LanguageEntry INTERACT_ROAD_LANEMISMATCHSECOND = new LanguageEntry("interact.road.lanemismatchsecond", "The number of lanes at the road segment just clicked does not match the number of lanes for this road.");
    public static final LanguageEntry INTERACT_ROAD_BLOCKED = new LanguageEntry("interact.road.blocked", "No space found to place the master block for this road.  Try a less crowded area?");
    public static final LanguageEntry INTERACT_ROAD_SAME = new LanguageEntry("interact.road.same", "Both the first point and the second point clicked are the same block.  Reset and try again.");
    public static final LanguageEntry INTERACT_ROAD_TOOFAR = new LanguageEntry("interact.road.toofar", "The two points clicked are too far apart.  Either find closer points, or increase the distance in the config file.");
    public static final LanguageEntry INTERACT_ROAD_BLOCKINGBLOCKS = new LanguageEntry("interact.road.blockingblocks", "There are blocks blocking the placement of this road.  Break the highlighted red ones, and then click the blue master road block to continue.");

    public static final LanguageEntry INTERACT_JERRYCAN_EMPTY = new LanguageEntry("interact.jerrycan.empty", "This jerrycan is empty and cannot fuel this vehicle.");
    public static final LanguageEntry INTERACT_JERRYCAN_WRONGTYPE = new LanguageEntry("interact.jerrycan.wrongtype", "This jerrycan does not contain the same liquid as what is in the vehicle.");
    public static final LanguageEntry INTERACT_JERRYCAN_WRONGENGINES = new LanguageEntry("interact.jerrycan.wrongengines", "The contents in this jerrycan is not a valid fuel for the engines in the vehicle.  Check the manual for details.");
    public static final LanguageEntry INTERACT_JERRYCAN_TOOFULL = new LanguageEntry("interact.jerrycan.toofull", "This vehicle is already full of fuel.  You cannot add any more.");
    public static final LanguageEntry INTERACT_JERRYCAN_SUCCESS = new LanguageEntry("interact.jerrycan.success", "Added 1000mb of fluid to this vehicle.");

    public static final LanguageEntry INTERACT_VEHICLE_SEATTAKEN = new LanguageEntry("interact.vehicle.seattaken", "This seat is taken!");
    public static final LanguageEntry INTERACT_VEHICLE_LOCKED = new LanguageEntry("interact.vehicle.locked", "This vehicle is locked!");
    public static final LanguageEntry INTERACT_VEHICLE_OWNED = new LanguageEntry("interact.vehicle.owned", "You are not the owner of this vehicle and therefore cannot change it or its parts!");
    public static final LanguageEntry INTERACT_VEHICLE_NEEDKEY = new LanguageEntry("interact.vehicle.needkey", "You must have the key of this vehicle to start it!");
    public static final LanguageEntry INTERACT_VEHICLE_NOSPACE = new LanguageEntry("interact.vehicle.nospace", "There is not enough space to place down this vehicle here.");
    public static final LanguageEntry INTERACT_VEHICLE_CANTREMOVEINVENTORY = new LanguageEntry("interact.vehicle.cantremoveinventory", "You cannot remove this part because it is not empty!");
    public static final LanguageEntry INTERACT_VEHICLE_JUMPERPACK = new LanguageEntry("interact.vehicle.jumperpack", "Charged vehicle battery to maximum.");

    public static final LanguageEntry INTERACT_JUMPERCABLE_FIRSTLINK = new LanguageEntry("interact.jumpercable.firstlink", "Linking first engine to jumper cable.");
    public static final LanguageEntry INTERACT_JUMPERCABLE_SECONDLINK = new LanguageEntry("interact.jumpercable.secondlink", "Engines linked.  Transmitting power.");
    public static final LanguageEntry INTERACT_JUMPERCABLE_LINKDROPPED = new LanguageEntry("interact.jumpercable.linkdropped", "The two engines have moved too far apart from one another.  Breaking link.");
    public static final LanguageEntry INTERACT_JUMPERCABLE_POWEREQUAL = new LanguageEntry("interact.jumpercable.powerequal", "The two engines now have the same battery level.  Breaking link.");
    public static final LanguageEntry INTERACT_JUMPERCABLE_ALREADYLINKED = new LanguageEntry("interact.jumpercable.alreadylinked", "This engine is already linked to another engine and cannot be linked.");
    public static final LanguageEntry INTERACT_JUMPERCABLE_TOOFAR = new LanguageEntry("interact.jumpercable.toofar", "The two engines are too far away.  They should be no further than 16 blocks apart.");
    public static final LanguageEntry INTERACT_JUMPERCABLE_SAMEVEHICLE = new LanguageEntry("interact.jumpercable.samevehicle", "The two engines are on the same vehicle.  That's not going to work well..");

    public static final LanguageEntry INTERACT_FUELHOSE_FIRSTLINK = new LanguageEntry("interact.fuelhose.firstlink", "Source tank linked to fuel hose.");
    public static final LanguageEntry INTERACT_FUELHOSE_SECONDLINK = new LanguageEntry("interact.fuelhose.secondlink", "Linked to tank/vehicle.  Transfering fluid.");
    public static final LanguageEntry INTERACT_FUELHOSE_LINKDROPPED = new LanguageEntry("interact.fuelhose.linkdropped", "The linked tanks have moved too far apart from one another.  Breaking link.");
    public static final LanguageEntry INTERACT_FUELHOSE_TANKEMPTY = new LanguageEntry("interact.fuelhose.tankempty", "Source tank is empty, disconnecting.");
    public static final LanguageEntry INTERACT_FUELHOSE_TANKFULL = new LanguageEntry("interact.fuelhose.tankfull", "Destination tank is full, disconnecting.");
    public static final LanguageEntry INTERACT_FUELHOSE_ALREADYLINKED = new LanguageEntry("interact.fuelhose.alreadylinked", "This tank is already linked to another tank and cannot be linked.");
    public static final LanguageEntry INTERACT_FUELHOSE_TOOFAR = new LanguageEntry("interact.fuelhose.toofar", "The two tanks are too far away.  They should be no further than 16 blocks apart.");
    public static final LanguageEntry INTERACT_FUELHOSE_DIFFERENTFLUIDS = new LanguageEntry("interact.fuelhose.differentfluids", "The source and destination tanks do not contain the same fluid and cannot be linked.");

    public static final LanguageEntry INTERACT_TRAILER_CONNECTED = new LanguageEntry("interact.trailer.connected", "Trailer connected.");
    public static final LanguageEntry INTERACT_TRAILER_DISCONNECTED = new LanguageEntry("interact.trailer.disconnected", "Trailer disconnected.");
    public static final LanguageEntry INTERACT_TRAILER_NOTFOUND = new LanguageEntry("interact.trailer.notfound", "No trailers were found anywhere near this vehicle.");
    public static final LanguageEntry INTERACT_TRAILER_TOOFAR = new LanguageEntry("interact.trailer.toofar", "The trailer is too far from the vehicle.");
    public static final LanguageEntry INTERACT_TRAILER_WRONGHITCH = new LanguageEntry("interact.trailer.wronghitch", "This vehicle does not have the correct hitch for this trailer.");
    public static final LanguageEntry INTERACT_TRAILER_MISMATCH = new LanguageEntry("interact.trailer.mismatch", "A hitch was found that is close enough to connect, but was not the correct type.  Also, a matching hitch was found, but it was too far to connect.  Are you trying to use the wrong hitch?");
    public static final LanguageEntry INTERACT_TRAILER_ALREADYTOWED = new LanguageEntry("interact.trailer.alreadytowed", "The only nearby vehicle is already being towed.");
    public static final LanguageEntry INTERACT_TRAILER_FEEDBACKLOOP = new LanguageEntry("interact.trailer.feedbackloop", "The only nearby vehicles are being towed by this vehicle, towing would result in an infinite towing loop.");

    public static final LanguageEntry ITEMINFO_ENGINE_AUTOMATIC = new LanguageEntry("iteminfo.engine.automatic", "Transmission: Automatic");
    public static final LanguageEntry ITEMINFO_ENGINE_MANUAL = new LanguageEntry("iteminfo.engine.manual", "Transmission: Manual");
    public static final LanguageEntry ITEMINFO_ENGINE_GEARRATIOS = new LanguageEntry("iteminfo.engine.gearratios", "Gear Ratios: ");
    public static final LanguageEntry ITEMINFO_ENGINE_JETPOWERFACTOR = new LanguageEntry("iteminfo.engine.jetpowerfactor", "Jet Power: ");
    public static final LanguageEntry ITEMINFO_ENGINE_BYPASSRATIO = new LanguageEntry("iteminfo.engine.bypassratio", "Bypass Ratio: ");
    public static final LanguageEntry ITEMINFO_ENGINE_MAXRPM = new LanguageEntry("iteminfo.engine.maxrpm", "Max Possible RPM: ");
    public static final LanguageEntry ITEMINFO_ENGINE_MAXSAFERPM = new LanguageEntry("iteminfo.engine.maxsaferpm", "Max safe RPM: ");
    public static final LanguageEntry ITEMINFO_ENGINE_FUELCONSUMPTION = new LanguageEntry("iteminfo.engine.fuelconsumption", "Fuel use (mb/t): ");
    public static final LanguageEntry ITEMINFO_ENGINE_SUPERCHARGERFUELCONSUMPTION = new LanguageEntry("iteminfo.engine.superchargerfuelconsumption", "Supercharger fuel use (mb/t): ");
    public static final LanguageEntry ITEMINFO_ENGINE_SUPERCHARGEREFFICIENCY = new LanguageEntry("iteminfo.engine.superchargerefficiency", "Supercharger efficiency: ");
    public static final LanguageEntry ITEMINFO_ENGINE_FUELTYPE = new LanguageEntry("iteminfo.engine.fueltype", "Fuel type: ");
    public static final LanguageEntry ITEMINFO_ENGINE_FLUIDS = new LanguageEntry("iteminfo.engine.fluids", "Takes Fluids: ");
    public static final LanguageEntry ITEMINFO_ENGINE_HOURS = new LanguageEntry("iteminfo.engine.hours", "Runtime hours: ");

    public static final LanguageEntry ITEMINFO_GROUND_DEVICE_DIAMETER = new LanguageEntry("iteminfo.ground_device.diameter", "Diameter: ");
    public static final LanguageEntry ITEMINFO_GROUND_DEVICE_MOTIVEFRICTION = new LanguageEntry("iteminfo.ground_device.motivefrictionmotivefriction", "Power friction: ");
    public static final LanguageEntry ITEMINFO_GROUND_DEVICE_LATERALFRICTION = new LanguageEntry("iteminfo.ground_device.lateralfriction", "Turning friction: ");
    public static final LanguageEntry ITEMINFO_GROUND_DEVICE_FRICTIONMODIFIERS = new LanguageEntry("iteminfo.ground_device.frictionmodifiers", "Friction Modifiers: ");
    public static final LanguageEntry ITEMINFO_GROUND_DEVICE_ROTATESONSHAFT_TRUE = new LanguageEntry("iteminfo.ground_device.rotatesonshaft_true", "Is a wheel");
    public static final LanguageEntry ITEMINFO_GROUND_DEVICE_ROTATESONSHAFT_FALSE = new LanguageEntry("iteminfo.ground_device.rotatesonshaft_false", "Is NOT a wheel");
    public static final LanguageEntry ITEMINFO_GROUND_DEVICE_CANFLOAT_TRUE = new LanguageEntry("iteminfo.ground_device.canfloat_true", "Floats");
    public static final LanguageEntry ITEMINFO_GROUND_DEVICE_CANFLOAT_FALSE = new LanguageEntry("iteminfo.ground_device.canfloat_false", "Does not float");

    public static final LanguageEntry ITEMINFO_PROPELLER_STATICPITCH = new LanguageEntry("iteminfo.propeller.staticPitch", "Static Pitch");
    public static final LanguageEntry ITEMINFO_PROPELLER_DYNAMICPITCH = new LanguageEntry("iteminfo.propeller.dynamicPitch", "Dynamic Pitch");
    public static final LanguageEntry ITEMINFO_PROPELLER_PITCH = new LanguageEntry("iteminfo.propeller.pitch", "Pitch: ");
    public static final LanguageEntry ITEMINFO_PROPELLER_DIAMETER = new LanguageEntry("iteminfo.propeller.diameter", "Diameter: ");

    public static final LanguageEntry ITEMINFO_GUN_DIAMETER = new LanguageEntry("iteminfo.gun.diameter", "Barrel Diameter (mm): ");
    public static final LanguageEntry ITEMINFO_GUN_CASERANGE = new LanguageEntry("iteminfo.gun.caseRange", "Case Range (mm): ");
    public static final LanguageEntry ITEMINFO_GUN_FIREDELAY = new LanguageEntry("iteminfo.gun.fireDelay", "Fire Delay (ticks): ");
    public static final LanguageEntry ITEMINFO_GUN_MUZZLEVELOCITY = new LanguageEntry("iteminfo.gun.muzzleVelocity", "Velocity (blk/s): ");
    public static final LanguageEntry ITEMINFO_GUN_CAPACITY = new LanguageEntry("iteminfo.gun.capacity", "Bullet Capacity: ");
    public static final LanguageEntry ITEMINFO_GUN_AUTORELOAD = new LanguageEntry("iteminfo.gun.autoReload", "Auto-Reloads");
    public static final LanguageEntry ITEMINFO_GUN_YAWRANGE = new LanguageEntry("iteminfo.gun.yawRange", "Yaw Range: ");
    public static final LanguageEntry ITEMINFO_GUN_PITCHRANGE = new LanguageEntry("iteminfo.gun.pitchRange", "Pitch Range: ");

    public static final LanguageEntry ITEMINFO_BULLET_TYPE_EXPLOSIVE = new LanguageEntry("iteminfo.bullet.type.explosive", "Explosive Bullet");
    public static final LanguageEntry ITEMINFO_BULLET_TYPE_INCENDIARY = new LanguageEntry("iteminfo.bullet.type.incendiary", "Incendiary Bullet");
    public static final LanguageEntry ITEMINFO_BULLET_TYPE_ARMOR_PIERCING = new LanguageEntry("iteminfo.bullet.type.armor_piercing", "Armor Piercing Bullet");
    public static final LanguageEntry ITEMINFO_BULLET_TYPE_WATER = new LanguageEntry("iteminfo.bullet.type.water", "Water Bullet");
    public static final LanguageEntry ITEMINFO_BULLET_PELLETS = new LanguageEntry("iteminfo.bullet.pellets", "Pellets: ");
    public static final LanguageEntry ITEMINFO_BULLET_DIAMETER = new LanguageEntry("iteminfo.bullet.diameter", "Diameter (mm): ");
    public static final LanguageEntry ITEMINFO_BULLET_CASELENGTH = new LanguageEntry("iteminfo.bullet.caseLength", "Case Length (mm): ");
    public static final LanguageEntry ITEMINFO_BULLET_PENETRATION = new LanguageEntry("iteminfo.bullet.penetration", "Penetration (mm): ");
    public static final LanguageEntry ITEMINFO_BULLET_QUANTITY = new LanguageEntry("iteminfo.bullet.quantity", "Quantity: ");

    public static final LanguageEntry ITEMINFO_INTERACTABLE_CAPACITY = new LanguageEntry("iteminfo.interactable.capacity", "Capacity: ");

    public static final LanguageEntry ITEMINFO_JERRYCAN_FILL = new LanguageEntry("iteminfo.jerrycan.fill", "Fill this can by right-clicking a fuel pump.");
    public static final LanguageEntry ITEMINFO_JERRYCAN_DRAIN = new LanguageEntry("iteminfo.jerrycan.drain", "Fill a vehicle by right-clicking with a full can.");
    public static final LanguageEntry ITEMINFO_JERRYCAN_EMPTY = new LanguageEntry("iteminfo.jerrycan.empty", "This can is currently empty.");
    public static final LanguageEntry ITEMINFO_JERRYCAN_CONTAINS = new LanguageEntry("iteminfo.jerrycan.contains", "This can contains: ");

    public static final LanguageEntry INPUT_MOD = new LanguageEntry("input.mod", "Mod");
    public static final LanguageEntry INPUT_CAMLOCK = new LanguageEntry("input.camlock", "CamLock");
    public static final LanguageEntry INPUT_YAW = new LanguageEntry("input.yaw", "Yaw");
    public static final LanguageEntry INPUT_YAW_R = new LanguageEntry("input.yaw_r", "YawRight");
    public static final LanguageEntry INPUT_YAW_L = new LanguageEntry("input.yaw_l", "YawLeft");
    public static final LanguageEntry INPUT_PITCH = new LanguageEntry("input.pitch", "Pitch");
    public static final LanguageEntry INPUT_PITCH_U = new LanguageEntry("input.pitch_u", "PitchUp");
    public static final LanguageEntry INPUT_PITCH_D = new LanguageEntry("input.pitch_d", "PitchDown");
    public static final LanguageEntry INPUT_ROLL = new LanguageEntry("input.roll", "Roll");
    public static final LanguageEntry INPUT_ROLL_R = new LanguageEntry("input.roll_r", "RollRight");
    public static final LanguageEntry INPUT_ROLL_L = new LanguageEntry("input.roll_l", "RollLeft");
    public static final LanguageEntry INPUT_THROTTLE = new LanguageEntry("input.throttle", "Throttle");
    public static final LanguageEntry INPUT_THROTTLE_U = new LanguageEntry("input.throttle_u", "ThrottleUp");
    public static final LanguageEntry INPUT_THROTTLE_D = new LanguageEntry("input.throttle_d", "ThrottleDown");
    public static final LanguageEntry INPUT_FLAPS_U = new LanguageEntry("input.flaps_u", "FlapsUp");
    public static final LanguageEntry INPUT_FLAPS_D = new LanguageEntry("input.flaps_d", "FlapsDown");
    public static final LanguageEntry INPUT_BRAKE = new LanguageEntry("input.brake", "Brake");
    public static final LanguageEntry INPUT_GEAR = new LanguageEntry("input.gear", "Gear");
    public static final LanguageEntry INPUT_PANEL = new LanguageEntry("input.panel", "Panel");
    public static final LanguageEntry INPUT_PARK = new LanguageEntry("input.park", "ParkingBrake");
    public static final LanguageEntry INPUT_GUN_FIRE = new LanguageEntry("input.gun_fire", "Gun");
    public static final LanguageEntry INPUT_GUN_SWITCH = new LanguageEntry("input.gun_switch", "Switch Gun");
    public static final LanguageEntry INPUT_RADIO = new LanguageEntry("input.radio", "Radio");
    public static final LanguageEntry INPUT_ZOOM_I = new LanguageEntry("input.zoom_i", "ZoomIn");
    public static final LanguageEntry INPUT_ZOOM_O = new LanguageEntry("input.zoom_o", "ZoomOut");
    public static final LanguageEntry INPUT_CHANGEVIEW = new LanguageEntry("input.changeview", "ChangeView");
    public static final LanguageEntry INPUT_LOOK_L = new LanguageEntry("input.look_l", "LookLeft");
    public static final LanguageEntry INPUT_LOOK_R = new LanguageEntry("input.look_r", "LookRight");
    public static final LanguageEntry INPUT_LOOK_U = new LanguageEntry("input.look_u", "LookUp");
    public static final LanguageEntry INPUT_LOOK_D = new LanguageEntry("input.look_d", "LookDown");
    public static final LanguageEntry INPUT_LOOK_A = new LanguageEntry("input.look_a", "LookDirectional");
    public static final LanguageEntry INPUT_TRIM_YAW_R = new LanguageEntry("input.trim_yaw_r", "TrimYawRight");
    public static final LanguageEntry INPUT_TRIM_YAW_L = new LanguageEntry("input.trim_yaw_l", "TrimYawLeft");
    public static final LanguageEntry INPUT_TRIM_PITCH_U = new LanguageEntry("input.trim_pitch_u", "TrimPitchUp");
    public static final LanguageEntry INPUT_TRIM_PITCH_D = new LanguageEntry("input.trim_pitch_d", "TrimPitchDown");
    public static final LanguageEntry INPUT_TRIM_ROLL_R = new LanguageEntry("input.trim_roll_r", "TrimRollRight");
    public static final LanguageEntry INPUT_TRIM_ROLL_L = new LanguageEntry("input.trim_roll_l", "TrimRollLeft");
    public static final LanguageEntry INPUT_REVERSE = new LanguageEntry("input.reverse", "Reverse");
    public static final LanguageEntry INPUT_JS_INHIBIT = new LanguageEntry("input.js_inhibit", "DisableJoystick");

    public static final LanguageEntry INPUT_TURN = new LanguageEntry("input.turn", "Turn");
    public static final LanguageEntry INPUT_TURN_R = new LanguageEntry("input.turn_r", "TurnRight");
    public static final LanguageEntry INPUT_TURN_L = new LanguageEntry("input.turn_l", "TurnLeft");
    public static final LanguageEntry INPUT_GAS = new LanguageEntry("input.gas", "Gas");
    public static final LanguageEntry INPUT_SHIFT_U = new LanguageEntry("input.shift_u", "ShiftUp");
    public static final LanguageEntry INPUT_SHIFT_D = new LanguageEntry("input.shift_d", "ShiftDown");
    public static final LanguageEntry INPUT_SHIFT_N = new LanguageEntry("input.shift_n", "ShiftNeutral");
    public static final LanguageEntry INPUT_HORN = new LanguageEntry("input.horn", "Horn");
    public static final LanguageEntry INPUT_SLOW = new LanguageEntry("input.slow", "Slow");
    public static final LanguageEntry INPUT_LIGHTS = new LanguageEntry("input.lights", "Lights");
    public static final LanguageEntry INPUT_TURNSIGNAL_R = new LanguageEntry("input.turnsignal_r", "RightSignal");
    public static final LanguageEntry INPUT_TURNSIGNAL_L = new LanguageEntry("input.turnsignal_l", "LeftSignal");

    public static final LanguageEntry SYSTEM_SOUNDSLOT = new LanguageEntry("sytstem.soundslot", "IMMERSIVE VEHICLES ERROR: Tried to play a sound, but was told no sound slots were available. Some mod is taking up all the slots. If you have Immersive Railroading, set override sound channels to false in that mod's config. If running GregTech, set maxNumSounds to a lower value in that mod's config. Dynamic Surrondings and Optifine also may cause issues. Apply fixes, or complain to those mod's authors. Sounds will not play.");
    public static final LanguageEntry SYSTEM_DEBUG = new LanguageEntry("sytstem.debug", "%s");
}
