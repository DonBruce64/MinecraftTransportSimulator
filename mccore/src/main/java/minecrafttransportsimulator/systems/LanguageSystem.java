package minecrafttransportsimulator.systems;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.items.instances.ItemItem;
import minecrafttransportsimulator.jsondefs.JSONItem.JSONBooklet.BookletPage;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packloading.JSONParser;
import minecrafttransportsimulator.packloading.PackParser;

/**
 * System for handling language translation.  This stores all language files found during boot,
 * and references those files when requested.  This system also is responsible for exporting language
 * files to a folder if requested.
 *
 * @author don_bruce
 */
public class LanguageSystem {
    public static final Map<String, LanguageEntry> coreLanguageEntires = new LinkedHashMap<>();
    private static final Map<String, Map<String, LanguageEntry>> packLanguageEntries = new LinkedHashMap<>();
    private static boolean onClient;
    private static File dumpToFolder;

    /**
     * Called to init this system.  Must be called after all pack items are loaded, but
     * before any pack item name references are used.
     */
    public static void init(boolean onClient) {
        LanguageSystem.onClient = onClient;

        //Init all packs, getting their language entries first.
        for (String packID : PackParser.getAllPackIDs()) {
            //Get all pack items and init their language objects.
            Map<String, LanguageEntry> packMap = packLanguageEntries.computeIfAbsent(packID, k -> new LinkedHashMap<>());
            for (AItemPack<?> packItem : PackParser.getAllItemsForPack(packID, true)) {
                String itemID = packItem.getRegistrationName();
                String itemName = null;

                if (packItem instanceof AItemSubTyped) {
                    AItemSubTyped<?> subDefItem = (AItemSubTyped<?>) packItem;
                    itemName = subDefItem.subDefinition.name;
                    subDefItem.languageSubDescription = new LanguageEntry(subDefItem.subDefinition.description != null ? subDefItem.subDefinition.description : "");
                    packMap.put(itemID + ".subDescription", subDefItem.languageSubDescription);

                    //Description ignores subName, strip it and associate it if it already exists.
                    String masterItemID = itemID.substring(0, itemID.length() - subDefItem.subDefinition.subName.length()) + ".description";
                    LanguageEntry masterDescription = packMap.get(masterItemID);
                    if (masterDescription == null) {
                        masterDescription = new LanguageEntry(packItem.definition.general.description != null ? packItem.definition.general.description : "");
                        packMap.put(masterItemID, masterDescription);
                    }
                    packItem.languageDescription = masterDescription;
                } else {
                    packItem.languageDescription = new LanguageEntry(packItem.definition.general.description != null ? packItem.definition.general.description : "");
                    packMap.put(itemID + ".description", packItem.languageDescription);
                }
                if (itemName == null) {
                    itemName = packItem.definition.general.name != null ? packItem.definition.general.name : packItem.definition.systemName;
                }
                packItem.languageName = new LanguageEntry(itemName);
                packMap.put(itemID + ".name", packItem.languageName);

                //If this is a book item, we need to add a section for all the components.
                if (packItem instanceof ItemItem) {
                    ItemItem packItemItem = (ItemItem) packItem;
                    if (packItemItem.definition.booklet != null) {
                        itemID += ".booklet";
                        if (packItemItem.definition.booklet.titleText != null) {
                            packItemItem.languageTitle = new ArrayList<>();
                            for (int i = 0; i < packItemItem.definition.booklet.titleText.size(); ++i) {
                                LanguageEntry language = new LanguageEntry(packItemItem.definition.booklet.titleText.get(i).defaultText);
                                packItemItem.languageTitle.add(language);
                                packMap.put(itemID + ".title.textline" + (i + 1), language);
                            }
                        }

                        packItemItem.languagePageTitle = new ArrayList<>();
                        packItemItem.languagePageText = new ArrayList<>();
                        for (int i = 0; i < packItemItem.definition.booklet.pages.size(); ++i) {
                            BookletPage page = packItemItem.definition.booklet.pages.get(i);
                            if (page.title != null) {
                                LanguageEntry language = new LanguageEntry(page.title);
                                packItemItem.languagePageTitle.add(language);
                                packMap.put(itemID + ".page" + (i + 1) + ".title", language);
                            } else {
                                packItemItem.languagePageTitle.add(null);
                            }
                            if (page.pageText != null) {
                                List<LanguageEntry> languageList = new ArrayList<>();
                                for (int j = 0; j < page.pageText.size(); ++j) {
                                    LanguageEntry language = new LanguageEntry(page.pageText.get(j).defaultText);
                                    languageList.add(language);
                                    packMap.put(itemID + ".page" + (i + 1) + ".textline" + (j + i), language);
                                }
                                packItemItem.languagePageText.add(languageList);
                            } else {
                                packItemItem.languagePageText.add(null);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Called to populate the names.  This has to happen after {@link #init(boolean)},
     * but can be deffered until the game boots up and we can scan for language settings.
     * This method only populates runtime values, not default code ones (en_us).  Those will 
     * always be present, provided the init function has been called.  Only call this on clients
     * where the language setting is used: servers will crash if language population is attempted.
     */
    public static void populateNames() {
        //Populate pack language objects with text.
        //Core entries, being static, will be initied on class init, so they'll be ready here too.
        for (String packID : PackParser.getAllPackIDs()) {
            Map<String, LanguageEntry> packMap = packLanguageEntries.get(packID);
            for (String language : InterfaceManager.clientInterface.getAllLanguages()) {
                String filePath = "/assets/" + packID + "/language/" + language + ".json";
                InputStream languageStream = InterfaceManager.coreInterface.getPackResource(filePath);
                if (languageStream != null) {
                    JSONLanguageFile languageFile;
                    try {
                        languageFile = JSONParser.parseStream(languageStream, JSONLanguageFile.class, null, null);
                        languageFile.entries.forEach((key, value) -> {
                            LanguageEntry languageEntry = packMap.get(key);
                            if (languageEntry != null) {
                                languageEntry.values.put(language, value);
                            }
                            languageEntry = coreLanguageEntires.get(key);
                            if (languageEntry != null) {
                                languageEntry.values.put(language, value);
                            }
                        });
                    } catch (IOException e) {
                        InterfaceManager.coreInterface.logError("Could not load language file: " + filePath + "  Language for this language/pack will be disabled.  Report this to the pack author!");
                    }
                }
            }
        }

        if (dumpToFolder != null) {
            //We got a message from ConfigSystem to dump what we parsed, do so now.
            try {
                for (Entry<String, Map<String, LanguageEntry>> languagePacks : packLanguageEntries.entrySet()) {
                    String packID = languagePacks.getKey();
                    JSONLanguageFile jsonFileToWrite = new JSONLanguageFile();
                    jsonFileToWrite.entries = new LinkedHashMap<>();
                    languagePacks.getValue().forEach((key, languageEntry) -> jsonFileToWrite.entries.put(key, languageEntry.getDefaultValue()));
                    if (packID.equals(InterfaceManager.coreModID)) {
                        coreLanguageEntires.forEach((key, languageEntry) -> jsonFileToWrite.entries.put(key, languageEntry.getDefaultValue()));
                    }
                    dumpToFolder.mkdir();
                    File packFolder = new File(dumpToFolder, packID);
                    packFolder.mkdir();
                    JSONParser.exportStream(jsonFileToWrite, Files.newOutputStream(new File(packFolder, InterfaceManager.clientInterface.getLanguageName() + ".json").toPath()));
                }
            } catch (Exception e) {
                InterfaceManager.coreInterface.logError("ConfigSystem failed to create template language files.  Report to the mod author!  Or, are you trying to do language stuff on servers?  Cause that's a bad idea...");
            }
        }
    }

    public static void dumpToFolder(File folder) {
        //Set dump folder for later, ConfigSystem runs before language setup as it has to run before pack parsing.
        dumpToFolder = folder;
    }

    public static class LanguageEntry {
        private static final String DEFAULT_LANGUAGE_KEY = "en_us";
        public final Map<String, String> values = new HashMap<>();
        public final String key;

        public LanguageEntry(String defaultValue) {
            values.put(DEFAULT_LANGUAGE_KEY, defaultValue);
            this.key = null;
        }

        /**Used only for internal language entires.**/
        private LanguageEntry(String key, String defaultValue) {
            values.put(DEFAULT_LANGUAGE_KEY, defaultValue);
            this.key = key;
            coreLanguageEntires.put(key, this);
        }

        public String getCurrentValue() {
            if (onClient) {
                String value = values.get(InterfaceManager.clientInterface.getLanguageName());
                return value != null ? value : getDefaultValue();
            } else {
                return getDefaultValue();
            }
        }

        public String getDefaultValue() {
            return values.get(DEFAULT_LANGUAGE_KEY);
        }
    }

    public static class JSONLanguageFile {
        public Map<String, String> entries;
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
    public static final LanguageEntry GUI_IMPORT = new LanguageEntry("gui.import", "Import Packs (Developers Only)");

    public static final LanguageEntry GUI_SIGNALCONTROLLER_SCAN = new LanguageEntry("gui.signalcontroller.scan", "Scan For Signals & Components");
    public static final LanguageEntry GUI_SIGNALCONTROLLER_SCANDISTANCE = new LanguageEntry("gui.signalcontroller.scandistance", "Radius: ");
    public static final LanguageEntry GUI_SIGNALCONTROLLER_SCANCENTER = new LanguageEntry("gui.signalcontroller.scancenter", "Center (X/Z): ");
    public static final LanguageEntry GUI_SIGNALCONTROLLER_SCANFOUND = new LanguageEntry("gui.signalcontroller.scanfound", "Found: ");
    public static final LanguageEntry GUI_SIGNALCONTROLLER_RIGHTHANDDRIVE = new LanguageEntry("gui.signalcontroller.righthanddrive", "Right-Hand Drive");
    public static final LanguageEntry GUI_SIGNALCONTROLLER_LEFTHANDDRIVE = new LanguageEntry("gui.signalcontroller.lefthanddrive", "Left-Hand Drive");
    public static final LanguageEntry GUI_SIGNALCONTROLLER_STOPONLY = new LanguageEntry("gui.signalcontroller.stoponly", "Red Before Green");
    public static final LanguageEntry GUI_SIGNALCONTROLLER_STOPYELLOW = new LanguageEntry("gui.signalcontroller.stopyellow", "Ylw/Red Before Green");
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

    public static final LanguageEntry GUI_PANEL_TRIM_ROLL = new LanguageEntry("gui.panel.trim_roll", "ROLL TRIM");
    public static final LanguageEntry GUI_PANEL_TRIM_PITCH = new LanguageEntry("gui.panel.trim_pitch", "PITCH TRIM");
    public static final LanguageEntry GUI_PANEL_TRIM_YAW = new LanguageEntry("gui.panel.trim_yaw", "YAW TRIM");
    public static final LanguageEntry GUI_PANEL_REVERSE = new LanguageEntry("gui.panel.reverse", "REV");
    public static final LanguageEntry GUI_PANEL_GEAR = new LanguageEntry("gui.panel.gear", "GEAR");
    public static final LanguageEntry GUI_PANEL_BEACON = new LanguageEntry("gui.panel.beacon", "BEACON");
    public static final LanguageEntry GUI_PANEL_AUTOPILOT = new LanguageEntry("gui.panel.autopilot", "AUTO");

    public static final LanguageEntry GUI_INSTRUMENTS_MAIN = new LanguageEntry("gui.instruments.main", "Main Instruments");
    public static final LanguageEntry GUI_INSTRUMENTS_PANEL = new LanguageEntry("gui.instruments.panel", "Control Panel");
    public static final LanguageEntry GUI_INSTRUMENTS_CLEAR = new LanguageEntry("gui.instruments.clear", "CLEAR");
    public static final LanguageEntry GUI_INSTRUMENTS_IDLE = new LanguageEntry("gui.instruments.idle", "Select an instrument.");
    public static final LanguageEntry GUI_INSTRUMENTS_DECIDE = new LanguageEntry("gui.instruments.decide", "Click an instrument to add.");

    public static final LanguageEntry GUI_CONFIG_HEADER_RENDERING = new LanguageEntry("gui.config.header.rendering", "RENDERING");
    public static final LanguageEntry GUI_CONFIG_HEADER_CONFIG = new LanguageEntry("gui.config.header.config", "CONFIG");
    public static final LanguageEntry GUI_CONFIG_HEADER_CONTROLS = new LanguageEntry("gui.config.header.controls", "CONTROLS");

    public static final LanguageEntry GUI_CONFIG_CONTROLS_TITLE = new LanguageEntry("gui.config.controls.title", "Choose a configuration to change: ");
    public static final LanguageEntry GUI_CONFIG_CONTROLS_SOUNDVOLUME = new LanguageEntry("gui.config.controls.soundvolume", "Sound Volume: ");
    public static final LanguageEntry GUI_CONFIG_CONTROLS_RADIOVOLUME = new LanguageEntry("gui.config.controls.radiovolume", "Radio Volume: ");
    public static final LanguageEntry GUI_CONFIG_CONTROLS_GENERAL_KEYBOARD = new LanguageEntry("gui.config.controls.general.keyboard", "GENERAL KEYBOARD");
    public static final LanguageEntry GUI_CONFIG_CONTROLS_GENERAL_JOYSTICK = new LanguageEntry("gui.config.controls.general.joystick", "GENERAL JOYSTICK");
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
    public static final LanguageEntry GUI_CONFIG_RENDERING_LABEL = new LanguageEntry("gui.config.rendering.label", "Rendering mode. Slower modes have more compatibility with shaders and performance mods at the cost of FPS.");
    public static final LanguageEntry GUI_CONFIG_RENDERING_MODE0 = new LanguageEntry("gui.config.rendering.mode0", "Superfast");
    public static final LanguageEntry GUI_CONFIG_RENDERING_MODE1 = new LanguageEntry("gui.config.rendering.mode1", "Fast");
    public static final LanguageEntry GUI_CONFIG_RENDERING_MODE2 = new LanguageEntry("gui.config.rendering.mode2", "Slow");

    public static final LanguageEntry GUI_PACKMISSING_TITLE = new LanguageEntry("gui.packmissing.title", "!ERROR!");
    public static final LanguageEntry GUI_PACKMISSING_TEXT = new LanguageEntry("gui.packmissing.text", "MTS has detected that it has been started without a content pack.  This will result in NO vehicles being present!  If you do NOT have a content pack yet installed, please see the Curse page for this mod for a link to one.  If you DID download a pack ensure that it is the same place as all your other mods.  If the pack is in that location, and you are still seeing this message, ensure you are running the correct pack version.");

    public static final LanguageEntry INTERACT_KEY_LOCK = new LanguageEntry("interact.key.lock", "Vehicle locked!");
    public static final LanguageEntry INTERACT_KEY_UNLOCK = new LanguageEntry("interact.key.unlock", "Vehicle unlocked!");
    public static final LanguageEntry INTERACT_KEY_BIND = new LanguageEntry("interact.key.bind", "Key bound to vehicle.  Ready for locking/unlocking use.");
    public static final LanguageEntry INTERACT_KEY_HASKEY = new LanguageEntry("interact.key.haskey", "This vehicle already has a key for it.  Only OPs may make new keys for vehicles that already have them.");
    public static final LanguageEntry INTERACT_KEY_WRONGKEY = new LanguageEntry("interact.key.wrongkey", "This key does not go to this vehicle!");

    public static final LanguageEntry INTERACT_FUELPUMP_NOFUEL = new LanguageEntry("interact.fuelpump.nofuel", "The pump is empty.  Either insert fuel into the pump via buckets, or connect it to a fluid or electrical system from the bottom.");
    public static final LanguageEntry INTERACT_FUELPUMP_FULLITEMS = new LanguageEntry("interact.fuelpump.fullitems", "This pump is set up to require payment for fuel, but it cannot accept more items.  Contact the owner to empty it!");
    public static final LanguageEntry INTERACT_FUELPUMP_TOOFAR = new LanguageEntry("interact.fuelpump.toofar", "The pump is too far away from the vehicle.  The pump should be no more than 16 blocks away.");
    public static final LanguageEntry INTERACT_FUELPUMP_WRONGTYPE = new LanguageEntry("interact.fuelpump.wrongtype", "This pump does not contain the same fluid as what is in the vehicle.  %s is in the vehicle.");
    public static final LanguageEntry INTERACT_FUELPUMP_NOENGINE = new LanguageEntry("interact.fuelpump.noengine", "There is no engine in this vehicle.  Engines with different fuel types will reset fuel, so put an engine in first to avoid fuel loss!");
    public static final LanguageEntry INTERACT_FUELPUMP_WRONGENGINES = new LanguageEntry("interact.fuelpump.wrongengines", "The contents in this pump is not a valid fuel for the engines in the vehicle.  Check the manual for details.");
    public static final LanguageEntry INTERACT_FUELPUMP_CONNECT = new LanguageEntry("interact.fuelpump.connect", "Connected and fueling.");
    public static final LanguageEntry INTERACT_FUELPUMP_FULL = new LanguageEntry("interact.fuelpump.full", "Vehicle is full, disconnecting.");
    public static final LanguageEntry INTERACT_FUELPUMP_COMPLETE = new LanguageEntry("interact.fuelpump.complete", "Paid fuel has been dispensed, disconnecting.");
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
    public static final LanguageEntry INTERACT_JERRYCAN_NOENGINE = new LanguageEntry("interact.jerrycan.noengine", "There is no engine in this vehicle.  Removing engines from vehicles resets their fuel tank, so fueling this vehicle would do nothing!");
    public static final LanguageEntry INTERACT_JERRYCAN_TOOFULL = new LanguageEntry("interact.jerrycan.toofull", "This vehicle is already full of fuel.  You cannot add any more.");
    public static final LanguageEntry INTERACT_JERRYCAN_SUCCESS = new LanguageEntry("interact.jerrycan.success", "Added 1000mb of fluid to this vehicle.");

    public static final LanguageEntry INTERACT_BATTERY_CHARGERLOW = new LanguageEntry("interact.battery.chargerlow", "This charger does not yet have enough power to charge this battery.");
    public static final LanguageEntry INTERACT_BATTERY_CHARGED = new LanguageEntry("interact.battery.charged", "Battery charged.");
    public static final LanguageEntry INTERACT_BATTERY_EMPTY = new LanguageEntry("interact.battery.empty", "This battery is empty and cannot charge this vehicle.");
    public static final LanguageEntry INTERACT_BATTERY_WRONGENGINES = new LanguageEntry("interact.battery.wrongengines", "The engines in this vehicle are not electric and therefore the battery cannot charge them.");
    public static final LanguageEntry INTERACT_BATTERY_NOENGINE = new LanguageEntry("interact.battery.noengine", "There is no engine in this vehicle.  Removing engines from vehicles resets their charge, so charging this vehicle would do nothing!");
    public static final LanguageEntry INTERACT_BATTERY_TOOFULL = new LanguageEntry("interact.battery.toofull", "This vehicle is already fully charged.  You cannot charge it any more.");
    public static final LanguageEntry INTERACT_BATTERY_SUCCESS = new LanguageEntry("interact.battery.success", "Added 1000mj of charge to this vehicle.");

    public static final LanguageEntry INTERACT_VEHICLE_SEATTAKEN = new LanguageEntry("interact.vehicle.seattaken", "This seat is taken!");
    public static final LanguageEntry INTERACT_VEHICLE_LOCKED = new LanguageEntry("interact.vehicle.locked", "This vehicle is locked!");
    public static final LanguageEntry INTERACT_VEHICLE_NEEDKEY = new LanguageEntry("interact.vehicle.needkey", "You must have the key of this vehicle to start it!");
    public static final LanguageEntry INTERACT_VEHICLE_NOSPACE = new LanguageEntry("interact.vehicle.nospace", "There is not enough space to place down this vehicle here.");
    public static final LanguageEntry INTERACT_VEHICLE_JUMPERPACK = new LanguageEntry("interact.vehicle.jumperpack", "Charged vehicle battery to maximum.");
    
    public static final LanguageEntry INTERACT_PARTREMOVE_HASPARTS = new LanguageEntry("interact.partremove.hasparts", "You must first remove all parts from this part to remove it!");
    public static final LanguageEntry INTERACT_PARTREMOVE_WRENCH = new LanguageEntry("interact.partremove.wrench", "This part requires a wrench to remove!");
    public static final LanguageEntry INTERACT_PARTREMOVE_SCREWDRIVER = new LanguageEntry("interact.partremove.screwdriver", "This part requires a screwdriver to remove!");
    public static final LanguageEntry INTERACT_PARTREMOVE_INVENTORY = new LanguageEntry("interact.partremove.inventory", "You cannot remove this part because it is not empty!");
    public static final LanguageEntry INTERACT_PARTREMOVE_RIDER = new LanguageEntry("interact.partremove.rider", "You cannot remove this seat while it is being ridden!");

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

    public static final LanguageEntry INTERACT_REPAIR_PASS = new LanguageEntry("interact.repair.pass", "Repaired vehicle HP by %s.  HP is now at %s of %s.");
    public static final LanguageEntry INTERACT_REPAIR_NONEED = new LanguageEntry("interact.repair.noneed", "This vehicle is already at full health and doesn't need to be repaired.");
    public static final LanguageEntry INTERACT_REPAIR_TOTALED = new LanguageEntry("interact.repair.fail", "This vehicle is totaled and cannot be repaired with this repair pack.  Only special repair packs can repair totaled vehicles.");
    public static final LanguageEntry INTERACT_REPAIR_TOOSOON = new LanguageEntry("interact.repair.toosoon", "You cannot repair this vehicle now.  Wait 10 seconds between repairs.");

    public static final LanguageEntry INTERACT_TRAILER_CONNECTED = new LanguageEntry("interact.trailer.connected", "Trailer connected.");
    public static final LanguageEntry INTERACT_TRAILER_DISCONNECTED = new LanguageEntry("interact.trailer.disconnected", "Trailer disconnected.");
    public static final LanguageEntry INTERACT_TRAILER_NOTFOUND = new LanguageEntry("interact.trailer.notfound", "No trailers were found anywhere near this vehicle.");
    public static final LanguageEntry INTERACT_TRAILER_TOOFAR = new LanguageEntry("interact.trailer.toofar", "The trailer is too far from the vehicle.");
    public static final LanguageEntry INTERACT_TRAILER_WRONGHITCH = new LanguageEntry("interact.trailer.wronghitch", "This vehicle does not have the correct hitch for this trailer.");
    public static final LanguageEntry INTERACT_TRAILER_MISMATCH = new LanguageEntry("interact.trailer.mismatch", "A hitch was found that is close enough to connect, but was not the correct type.  Also, a matching hitch was found, but it was too far to connect.  Are you trying to use the wrong hitch?");
    public static final LanguageEntry INTERACT_TRAILER_ALREADYTOWED = new LanguageEntry("interact.trailer.alreadytowed", "The only nearby vehicle is already being towed.");
    public static final LanguageEntry INTERACT_TRAILER_FEEDBACKLOOP = new LanguageEntry("interact.trailer.feedbackloop", "The only nearby vehicles are being towed by this vehicle, towing would result in an infinite towing loop.");

    public static final LanguageEntry ITEMINFO_SCANNER = new LanguageEntry("iteminfo.scanner", "Hover over a part box to see what can be placed at that slot.\nSneak while using to cycle slower.");
    public static final LanguageEntry ITEMINFO_WRENCH = new LanguageEntry("iteminfo.wrench", "Attack a vehicle part to remove it.\nSneak-attack a vehicle to remove entire vehicle.\nUse on vehicle to change instruments.\nSneak-use on vehicle or blocks to change text.\nUse on pole components to remove them.\nUse on fuel pumps you placed to change prices.");
    public static final LanguageEntry ITEMINFO_SCREWDRIVER = new LanguageEntry("iteminfo.screwdriver", "Like a wrench, but used only to remove specific parts that wrenches cannot.  Does not interact with blocks or other things.");
    public static final LanguageEntry ITEMINFO_PAINTGUN = new LanguageEntry("iteminfo.paintgun", "Use on a vehicle, a part, decor, or sign to change its color.\nNote: Not all things you can click will have multiple colors.");
    public static final LanguageEntry ITEMINFO_KEY = new LanguageEntry("iteminfo.key", "Sneak-use on cars to take ownership, right-click to lock/unlock.");
    public static final LanguageEntry ITEMINFO_TICKET = new LanguageEntry("iteminfo.ticket", "Right-click on a vehicle to load nearby entities.\nRight-click on a seat to load just that seat.\nSneak-click on a vehicle to unload all entities.\nNote: Will not unload players.");
    public static final LanguageEntry ITEMINFO_FUELHOSE = new LanguageEntry("iteminfo.fuelhose", "Use on a vehicle tank to select.\nUse on second tank or vehicle to link.\nHose will transfer liquids from tank 1 to selected.\nHose will stop when tank 1 is empty, selected is full or if tank 1 and selected are too far away.");
    public static final LanguageEntry ITEMINFO_JUMPERCABLES = new LanguageEntry("iteminfo.jumpercables", "Use on an engine to select.\nUse on second engine to link.\nLinked engines will exchange power.\nEngines will un-link when power is even or if they get too far away.");
    public static final LanguageEntry ITEMINFO_JUMPERPACK = new LanguageEntry("iteminfo.jumperpack", "Use on a vehicle to charge battery to max level.");
    public static final LanguageEntry ITEMINFO_REPAIRPACK = new LanguageEntry("iteminfo.repairpack", "Use on a vehicle to repair it.  This pack heals the vehicle: ");
    public static final LanguageEntry ITEMINFO_REPAIRPACK_UNTOTAL = new LanguageEntry("iteminfo.repairpack.untotal", "This repair pack is able to repair totaled vehicles as well.");
    public static final LanguageEntry ITEMINFO_Y2KBUTTON = new LanguageEntry("iteminfo.y2kbutton", "Use in a world to turn off all vehicle engines, engage their parking brakes, and turn off their lights.\nOnly usable by admins.");

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
    public static final LanguageEntry ITEMINFO_ENGINE_MAGIC = new LanguageEntry("iteminfo.engine.fluids", "Uses no fuel!");
    public static final LanguageEntry ITEMINFO_ENGINE_HOURS = new LanguageEntry("iteminfo.engine.hours", "Runtime hours: ");

    public static final LanguageEntry ITEMINFO_GROUND_DEVICE_DIAMETER = new LanguageEntry("iteminfo.ground_device.diameter", "Diameter: ");
    public static final LanguageEntry ITEMINFO_GROUND_DEVICE_MOTIVEFRICTION = new LanguageEntry("iteminfo.ground_device.motivefrictionmotivefriction", "Power friction: ");
    public static final LanguageEntry ITEMINFO_GROUND_DEVICE_LATERALFRICTION = new LanguageEntry("iteminfo.ground_device.lateralfriction", "Turning friction: ");
    public static final LanguageEntry ITEMINFO_GROUND_DEVICE_WETFRICTION = new LanguageEntry("iteminfo.ground_device.wetfriction", "Wet penalty: ");
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
    public static final LanguageEntry ITEMINFO_INTERACTABLE_CONTENTS = new LanguageEntry("iteminfo.interactable.contents", "Contents: ");

    public static final LanguageEntry ITEMINFO_JERRYCAN_FILL = new LanguageEntry("iteminfo.jerrycan.fill", "Fill this can by right-clicking a fuel pump.");
    public static final LanguageEntry ITEMINFO_JERRYCAN_DRAIN = new LanguageEntry("iteminfo.jerrycan.drain", "Fill a vehicle by right-clicking with a full can.");
    public static final LanguageEntry ITEMINFO_JERRYCAN_EMPTY = new LanguageEntry("iteminfo.jerrycan.empty", "This can is currently empty.");
    public static final LanguageEntry ITEMINFO_JERRYCAN_CONTAINS = new LanguageEntry("iteminfo.jerrycan.contains", "This can contains: ");

    public static final LanguageEntry ITEMINFO_BATTERY_FILL = new LanguageEntry("iteminfo.battery.fill", "Chargethis battery by right-clicking a charger with power.");
    public static final LanguageEntry ITEMINFO_BATTERY_DRAIN = new LanguageEntry("iteminfo.battery.drain", "Charge a vehicle by right-clicking with a full charge.");
    public static final LanguageEntry ITEMINFO_BATTERY_EMPTY = new LanguageEntry("iteminfo.battery.empty", "This battery is currently empty.");
    public static final LanguageEntry ITEMINFO_BATTERY_FULL = new LanguageEntry("iteminfo.battery.full", "This battery is currently full.");

    public static final LanguageEntry INPUT_CUSTOM1 = new LanguageEntry("input.custom1", "Custom1");
    public static final LanguageEntry INPUT_CUSTOM2 = new LanguageEntry("input.custom2", "Custom2");
    public static final LanguageEntry INPUT_CUSTOM3 = new LanguageEntry("input.custom3", "Custom3");
    public static final LanguageEntry INPUT_CUSTOM4 = new LanguageEntry("input.custom4", "Custom4");

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
    public static final LanguageEntry INPUT_GUN_RELOAD = new LanguageEntry("input.gun.reload", "Reload Gun");
    public static final LanguageEntry INPUT_RADIO = new LanguageEntry("input.radio", "Radio");
    public static final LanguageEntry INPUT_ZOOM_I = new LanguageEntry("input.zoom_i", "ZoomIn");
    public static final LanguageEntry INPUT_ZOOM_O = new LanguageEntry("input.zoom_o", "ZoomOut");
    public static final LanguageEntry INPUT_CHANGEVIEW = new LanguageEntry("input.changeview", "ChangeView");
    public static final LanguageEntry INPUT_LOOK_UD = new LanguageEntry("input.look_ud", "LookUpDown");
    public static final LanguageEntry INPUT_LOOK_LR = new LanguageEntry("input.look_lr", "LookLeftRight");
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
    public static final LanguageEntry INPUT_SHIFT_1 = new LanguageEntry("input.shift_1", "Gear1");
    public static final LanguageEntry INPUT_SHIFT_2 = new LanguageEntry("input.shift_2", "Gear2");
    public static final LanguageEntry INPUT_SHIFT_3 = new LanguageEntry("input.shift_3", "Gear3");
    public static final LanguageEntry INPUT_SHIFT_4 = new LanguageEntry("input.shift_4", "Gear4");
    public static final LanguageEntry INPUT_SHIFT_5 = new LanguageEntry("input.shift_5", "Gear5");
    public static final LanguageEntry INPUT_SHIFT_6 = new LanguageEntry("input.shift_6", "Gear6");
    public static final LanguageEntry INPUT_SHIFT_7 = new LanguageEntry("input.shift_6", "Gear7");
    public static final LanguageEntry INPUT_SHIFT_8 = new LanguageEntry("input.shift_6", "Gear8");
    public static final LanguageEntry INPUT_SHIFT_9 = new LanguageEntry("input.shift_6", "Gear9");
    public static final LanguageEntry INPUT_SHIFT_R = new LanguageEntry("input.shift_r", "GearR");
    public static final LanguageEntry INPUT_HORN = new LanguageEntry("input.horn", "Horn");
    public static final LanguageEntry INPUT_SLOW = new LanguageEntry("input.slow", "Slow");
    public static final LanguageEntry INPUT_LIGHTS = new LanguageEntry("input.lights", "Lights");
    public static final LanguageEntry INPUT_TURNSIGNAL_R = new LanguageEntry("input.turnsignal_r", "RightSignal");
    public static final LanguageEntry INPUT_TURNSIGNAL_L = new LanguageEntry("input.turnsignal_l", "LeftSignal");

    public static final LanguageEntry SYSTEM_SOUNDSLOT = new LanguageEntry("sytstem.soundslot", "IMMERSIVE VEHICLES ERROR: Tried to play a sound, but was told no sound slots were available. Some mod is taking up all the slots. If you have Immersive Railroading, set override sound channels to false in that mod's config. If running GregTech, set maxNumSounds to a lower value in that mod's config. If you have Receiver Gun Mod, un-install it (there is no config for this incopatibility).  If you have fixRTM, set expandPlayableSoundSlotCount to false.  Dynamic Surrondings and Optifine also may cause issues. Apply fixes, or complain to those mod's authors.");
    public static final LanguageEntry SYSTEM_SOUNDSYSTEM = new LanguageEntry("sytstem.soundsystem", "IMMERSIVE VEHICLES ERROR: Tried to play a sound but couldn't due to an audio system fault.  Do you have bad audio drivers?");
    public static final LanguageEntry SYSTEM_DEBUG = new LanguageEntry("sytstem.debug", "%s");
}
