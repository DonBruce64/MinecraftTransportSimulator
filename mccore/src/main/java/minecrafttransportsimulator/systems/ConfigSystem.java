package minecrafttransportsimulator.systems;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.items.instances.ItemVehicle;
import minecrafttransportsimulator.jsondefs.JSONConfigClient;
import minecrafttransportsimulator.jsondefs.JSONConfigCraftingOverrides;
import minecrafttransportsimulator.jsondefs.JSONConfigCraftingOverrides.JSONCraftingOverride;
import minecrafttransportsimulator.jsondefs.JSONConfigExternalDamageOverrides;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONConfigSettings;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packloading.JSONParser;
import minecrafttransportsimulator.packloading.PackParser;

/**
 * Class that handles all configuration settings. This file is responsible for saving and loading
 * the config, and representing that config as an instance object of type {@link JSONConfigSettings} for access in the code.
 * This class is NOT responsible for detecting config changes.  It is up to the code that calls this class to ensure the
 * changes made are valid and can be saved to the disk.  This also cuts down on saves in some instances where configs
 * cam be saved/modified in a batch rather than as single values.
 *
 * @author don_bruce
 */
public final class ConfigSystem {
    private static File settingsFile;
    private static File clientFile;
    private static File craftingFile;
    private static File externalDamageFile;
    public static JSONConfigSettings settings;
    public static JSONConfigClient client;
    public static JSONConfigExternalDamageOverrides externalDamageOverrides;
    private static File configDirectory;
    private static boolean onClient;
    private static Map<String, JSONConfigLanguage> languageFiles = new HashMap<>();

    /**
     * Called to load the config objects from the files in the passed-in folder.
     * If a required file is not present, one will be created at the end of the loading phase.
     */
    public static void loadFromDisk(File configDirectory, boolean onClient) {
        ConfigSystem.configDirectory = configDirectory;
        ConfigSystem.onClient = onClient;

        //If we have a settings file already, parse it into Java.
        //Otherwise, make a new one.
        //After parsing the settings file, save it.  This allows new entries to be populated.
        settingsFile = new File(configDirectory, "mtsconfig.json");
        if (settingsFile.exists()) {
            try {
                settings = JSONParser.parseStream(Files.newInputStream(settingsFile.toPath()), JSONConfigSettings.class, null, null);
            } catch (Exception e) {
                InterfaceManager.coreInterface.logError("ConfigSystem failed to parse settings file JSON.  Reverting to defaults.");
                InterfaceManager.coreInterface.logError(e.getMessage());
            }
        }
        if (settings == null) {
            settings = new JSONConfigSettings();
        }

        //Now parse the client config file for clients only.
        if (onClient) {
            clientFile = new File(configDirectory, "mtsconfigclient.json");
            if (clientFile.exists()) {
                try {
                    client = JSONParser.parseStream(Files.newInputStream(clientFile.toPath()), JSONConfigClient.class, null, null);
                } catch (Exception e) {
                    InterfaceManager.coreInterface.logError("ConfigSystem failed to parse client file JSON.  Reverting to defaults.");
                    InterfaceManager.coreInterface.logError(e.getMessage());
                }
            }
            if (client == null) {
                client = new JSONConfigClient();
            }
            //Check to make sure we have the right keyset.  If not, reset the binds.
            if (client.controls.keysetID != InterfaceManager.inputInterface.getKeysetID()) {
                client.controls = new JSONConfigClient.JSONControls();
                client.controls.keysetID = InterfaceManager.inputInterface.getKeysetID();
            }
        }

        //Get overrides file location.  This is used later when packs are parsed.
        craftingFile = new File(settingsFile.getParentFile(), "mtscraftingoverrides.json");
        externalDamageFile = new File(settingsFile.getParentFile(), "mtsexternaldamageoverrides.json");

        //If we have the old config file, delete it.
        File oldConfigFile = new File(configDirectory, "mts.cfg");
        if (oldConfigFile.exists()) {
            oldConfigFile.delete();
        }
    }

    /**
     * Gets the language file for the specified language, if it exists, or the default, if it doesn't.
     * This method requires that {@link #loadFromDisk(File, boolean)} has been called prior.
     */
    public static JSONConfigLanguage getLanguage() {
        String currentLanguageKey = onClient ? InterfaceManager.clientInterface.getLanguageName() : "en_us";
        JSONConfigLanguage language = languageFiles.get(currentLanguageKey);
        if (language == null) {
            File languageFile = new File(configDirectory, "mtslanguage_" + currentLanguageKey + ".json");
            if (languageFile.exists()) {
                try {
                    language = JSONParser.parseStream(Files.newInputStream(languageFile.toPath()), JSONConfigLanguage.class, null, null);
                } catch (Exception e) {
                    InterfaceManager.coreInterface.logError("ConfigSystem failed to parse language file JSON.  Reverting to defaults.");
                    InterfaceManager.coreInterface.logError(e.getMessage());
                }
            }

            if (language == null) {
                language = new JSONConfigLanguage();
            }

            //Check to make sure we populated the current language file.  If we are missing entries for packs, add them.
            language.populateEntries(onClient);

            languageFiles.put(currentLanguageKey, language);
        }
        return language;
    }

    /**
     * Called to do overrides.  Must be called after all packs are loaded.
     */
    public static void initOverrides() {
        if (settings.general.generateOverrideConfigs.value) {
            //Make the default override file and save it.
            try {
                JSONConfigCraftingOverrides overridesObject = new JSONConfigCraftingOverrides();
                overridesObject.overrides = new LinkedHashMap<>();
                for (AItemPack<?> packItem : PackParser.getAllPackItems()) {
                    Map<String, JSONCraftingOverride> packOverrides = overridesObject.overrides.get(packItem.definition.packID);
                    if (packOverrides == null) {
                        packOverrides = new LinkedHashMap<>();
                        overridesObject.overrides.put(packItem.definition.packID, packOverrides);
                    }

                    JSONCraftingOverride override = packOverrides.get(packItem.definition.systemName);
                    if (override == null) {
                        override = new JSONCraftingOverride();
                        packOverrides.put(packItem.definition.systemName, override);
                    }

                    override.commonMaterialLists = packItem.definition.general.materialLists;
                    if (packItem instanceof AItemSubTyped) {
                        if (override.extraMaterialLists == null) {
                            override.extraMaterialLists = new HashMap<>();
                        }
                        JSONSubDefinition subDefinition = ((AItemSubTyped<?>) packItem).subDefinition;
                        override.extraMaterialLists.put(subDefinition.subName, subDefinition.extraMaterialLists);
                    }
                    override.repairMaterialLists = packItem.definition.general.repairMaterialLists;
                }
                JSONParser.exportStream(overridesObject, Files.newOutputStream(craftingFile.toPath()));
            } catch (Exception e) {
                InterfaceManager.coreInterface.logError("ConfigSystem failed to create fresh crafting overrides file.  Report to the mod author!");
            }
            try {
                createDefaultExternalDamageOverrides();
                JSONParser.exportStream(externalDamageOverrides, Files.newOutputStream(externalDamageFile.toPath()));
            } catch (Exception e) {
                InterfaceManager.coreInterface.logError("ConfigSystem failed to create fresh external damage overrides file.  Report to the mod author!");
            }
        } else {
            if (craftingFile.exists()) {
                try {
                    JSONConfigCraftingOverrides overridesObject = JSONParser.parseStream(Files.newInputStream(craftingFile.toPath()), JSONConfigCraftingOverrides.class, null, null);
                    for (AItemPack<?> packItem : PackParser.getAllPackItems()) {
                        Map<String, JSONCraftingOverride> packOverrides = overridesObject.overrides.get(packItem.definition.packID);
                        if (packOverrides != null) {
                            JSONCraftingOverride override = packOverrides.get(packItem.definition.systemName);
                            if (override != null) {
                                if (override.commonMaterialLists != null) {
                                    packItem.definition.general.materialLists = override.commonMaterialLists;
                                }
                                if (override.extraMaterialLists != null) {
                                    JSONSubDefinition subDefinition = ((AItemSubTyped<?>) packItem).subDefinition;
                                    List<List<String>> extraMaterialLists = override.extraMaterialLists.get(subDefinition.subName);
                                    if (extraMaterialLists != null) {
                                        subDefinition.extraMaterialLists = extraMaterialLists;
                                    }
                                }
                                if (override.repairMaterialLists != null) {
                                    packItem.definition.general.repairMaterialLists = override.repairMaterialLists;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    InterfaceManager.coreInterface.logError("ConfigSystem failed to parse crafting override file JSON.  Crafting overrides will not be applied.");
                    InterfaceManager.coreInterface.logError(e.getMessage());
                }
            }
            if (externalDamageFile.exists()) {
                try {
                    externalDamageOverrides = JSONParser.parseStream(Files.newInputStream(externalDamageFile.toPath()), JSONConfigExternalDamageOverrides.class, null, null);
                } catch (Exception e) {
                    InterfaceManager.coreInterface.logError("ConfigSystem failed to parse external damage override file.  Overrides will not be applied.");
                    InterfaceManager.coreInterface.logError(e.getMessage());
                    createDefaultExternalDamageOverrides();
                }
            } else {
                createDefaultExternalDamageOverrides();
            }
        }
    }

    private static void createDefaultExternalDamageOverrides() {
        externalDamageOverrides = new JSONConfigExternalDamageOverrides();
        externalDamageOverrides.overrides = new LinkedHashMap<>();
        for (AItemPack<?> packItem : PackParser.getAllPackItems()) {
            if (packItem instanceof ItemVehicle) {
                if (!externalDamageOverrides.overrides.containsKey(packItem.definition.packID)) {
                    externalDamageOverrides.overrides.put(packItem.definition.packID, new LinkedHashMap<>());
                }
                externalDamageOverrides.overrides.get(packItem.definition.packID).put(packItem.definition.systemName, 1.0D);
            }
        }
    }

    /**
     * Called to save changes to the various configs to disk. Call this whenever
     * configs are edited to ensure they are saved, as the system does not do this automatically.
     */
    public static void saveToDisk() {
        try {
            JSONParser.exportStream(settings, Files.newOutputStream(settingsFile.toPath()));
            for (Entry<String, JSONConfigLanguage> languageEntry : languageFiles.entrySet()) {
                JSONParser.exportStream(languageEntry.getValue(), Files.newOutputStream(new File(configDirectory, "mtslanguage_" + languageEntry.getKey() + ".json").toPath()));
            }
            JSONParser.exportStream(client, Files.newOutputStream(clientFile.toPath()));
        } catch (Exception e) {
            InterfaceManager.coreInterface.logError("ConfigSystem failed to save modified config files.  Report to the mod author!");
        }
    }
}