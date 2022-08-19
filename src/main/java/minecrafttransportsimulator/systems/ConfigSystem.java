package minecrafttransportsimulator.systems;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.jsondefs.JSONConfigClient;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONConfigSettings;
import minecrafttransportsimulator.jsondefs.JSONCraftingOverrides;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packloading.JSONParser;
import minecrafttransportsimulator.packloading.PackParser;

/**Class that handles all configuration settings. This file is responsible for saving and loading
 * the config, and representing that config as an instance object of type {@link JSONConfigSettings} for access in the code.
 * This class is NOT responsible for detecting config changes.  It is up to the code that calls this class to ensure the 
 * changes made are valid and can be saved to the disk.  This also cuts down on saves in some instances where configs
 * cam be saved/modified in a batch rather than as single values.
 * 
 * @author don_bruce
 */
public final class ConfigSystem {
    private static File settingsFile;
    private static File languageFile;
    private static File clientFile;
    private static File craftingFile;
    public static JSONConfigSettings settings;
    public static JSONConfigLanguage language;
    public static JSONConfigClient client;

    /**Called to load this class from the files in the passed-in folder.
     * If a require file is not present, one will be created at the end of the loading phase.
     */
    public static void loadFromDisk(File configDirectory, boolean onClient) {
        //If we have a settings file already, parse it into Java.
        //Otherwise, make a new one.
        //After parsing the settings file, save it.  This allows new entries to be populated.
        settingsFile = new File(configDirectory, "mtsconfig.json");
        if (settingsFile.exists()) {
            try {
                settings = JSONParser.parseStream(new FileInputStream(settingsFile), JSONConfigSettings.class, null, null);
            } catch (Exception e) {
                InterfaceManager.coreInterface.logError("ConfigSystem failed to parse settings file JSON.  Reverting to defauts.");
                InterfaceManager.coreInterface.logError(e.getMessage());
            }
        }
        if (settings == null) {
            settings = new JSONConfigSettings();
        }

        //Do the same for the client and language file, normally only displayed on clients, but names may be used on servers for debug messages.
        languageFile = new File(configDirectory, "mtslanguage_" + (onClient ? InterfaceManager.clientInterface.getLanguageName() : "en_us") + ".json");
        if (languageFile.exists()) {
            try {
                language = JSONParser.parseStream(new FileInputStream(languageFile), JSONConfigLanguage.class, null, null);
            } catch (Exception e) {
                InterfaceManager.coreInterface.logError("ConfigSystem failed to parse language file JSON.  Reverting to defauts.");
                InterfaceManager.coreInterface.logError(e.getMessage());
            }
        }
        if (language == null) {
            language = new JSONConfigLanguage();
        }

        //Now parse the client config file for clients only.
        if (onClient) {
            clientFile = new File(configDirectory, "mtsconfigclient.json");
            if (clientFile.exists()) {
                try {
                    client = JSONParser.parseStream(new FileInputStream(clientFile), JSONConfigClient.class, null, null);
                } catch (Exception e) {
                    InterfaceManager.coreInterface.logError("ConfigSystem failed to parse client file JSON.  Reverting to defauts.");
                    InterfaceManager.coreInterface.logError(e.getMessage());
                }
            }
            if (client == null) {
                client = new JSONConfigClient();
            }
        }

        //Get crafting overrides file location.  This is used later when packs are parsed.
        craftingFile = new File(settingsFile.getParentFile(), "mtscraftingoverrides.json");

        //If we have the old config file, delete it.
        File oldConfigFile = new File(configDirectory, "mts.cfg");
        if (oldConfigFile.exists()) {
            oldConfigFile.delete();
        }
    }

    /**Called to do crafting overrides.  Must be called after all packs are loaded.
     */
    public static void initCraftingOverrides() {
        if (settings.general.dumpCraftingConfig.value) {
            //Make the default override file and save it.
            try {
                JSONCraftingOverrides craftingOverridesObject = new JSONCraftingOverrides();
                craftingOverridesObject.overrides = new LinkedHashMap<String, Map<String, List<String>>>();
                for (AItemPack<?> packItem : PackParser.getAllPackItems()) {
                    if (!craftingOverridesObject.overrides.containsKey(packItem.definition.packID)) {
                        craftingOverridesObject.overrides.put(packItem.definition.packID, new LinkedHashMap<String, List<String>>());
                    }
                    if (packItem instanceof AItemSubTyped) {
                        List<String> materials = new ArrayList<String>();
                        materials.addAll(packItem.definition.general.materials);
                        materials.addAll(((AItemSubTyped<?>) packItem).getExtraMaterials());
                        craftingOverridesObject.overrides.get(packItem.definition.packID).put(packItem.definition.systemName + ((AItemSubTyped<?>) packItem).subName, materials);
                    } else {
                        craftingOverridesObject.overrides.get(packItem.definition.packID).put(packItem.definition.systemName, packItem.definition.general.materials);
                    }
                    if (packItem.definition.general.repairMaterials != null) {
                        craftingOverridesObject.overrides.get(packItem.definition.packID).put(packItem.definition.systemName + "_repair", packItem.definition.general.repairMaterials);
                    }
                }
                JSONParser.exportStream(craftingOverridesObject, new FileOutputStream(craftingFile));
            } catch (Exception e) {
                InterfaceManager.coreInterface.logError("ConfigSystem failed to create fresh crafting overrides file.  Report to the mod author!");
            }
        } else if (craftingFile.exists()) {
            try {
                JSONCraftingOverrides craftingOverridesObject = JSONParser.parseStream(new FileInputStream(craftingFile), JSONCraftingOverrides.class, null, null);
                for (String craftingOverridePackID : craftingOverridesObject.overrides.keySet()) {
                    for (String craftingOverrideSystemName : craftingOverridesObject.overrides.get(craftingOverridePackID).keySet()) {
                        AItemPack<? extends AJSONItem> item = PackParser.getItem(craftingOverridePackID, craftingOverrideSystemName);
                        if (item instanceof AItemSubTyped) {
                            List<String> extraMaterials = ((AItemSubTyped<?>) item).getExtraMaterials();
                            extraMaterials.clear();
                            extraMaterials.addAll(craftingOverridesObject.overrides.get(craftingOverridePackID).get(craftingOverrideSystemName));
                            item.definition.general.materials.clear();
                        } else if (item != null) {
                            item.definition.general.materials = craftingOverridesObject.overrides.get(craftingOverridePackID).get(craftingOverrideSystemName);
                            item.definition.general.repairMaterials = craftingOverridesObject.overrides.get(craftingOverridePackID).get(craftingOverrideSystemName + "_repair");
                        }
                    }
                }
            } catch (Exception e) {
                InterfaceManager.coreInterface.logError("ConfigSystem failed to parse crafting override file JSON.  Crafting overrides will not be applied.");
                InterfaceManager.coreInterface.logError(e.getMessage());
            }
        }
    }

    /**Called to save changes to the various configs to disk. Call this whenever
     * configs are edited to ensure they are saved, as the system does not do this automatically.
     */
    public static void saveToDisk() {
        try {
            JSONParser.exportStream(settings, new FileOutputStream(settingsFile));
            JSONParser.exportStream(language, new FileOutputStream(languageFile));
            JSONParser.exportStream(client, new FileOutputStream(clientFile));
        } catch (Exception e) {
            InterfaceManager.coreInterface.logError("ConfigSystem failed to save modified config files.  Report to the mod author!");
        }
    }
}
