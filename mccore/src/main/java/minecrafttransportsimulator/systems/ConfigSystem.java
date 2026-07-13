package minecrafttransportsimulator.systems;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.items.instances.ItemPartEffector;
import minecrafttransportsimulator.items.instances.ItemVehicle;
import minecrafttransportsimulator.jsondefs.JSONConfigClient;
import minecrafttransportsimulator.jsondefs.JSONConfigCraftingOverrides;
import minecrafttransportsimulator.jsondefs.JSONConfigCraftingOverrides.JSONCraftingOverride;
import minecrafttransportsimulator.jsondefs.JSONConfigExternalDamageOverrides;
import minecrafttransportsimulator.jsondefs.JSONConfigSettings;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
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
    private static final List<String> pendingConfigWarnings = new ArrayList<>();
    private static final Set<String> blockedConfigSavePaths = new HashSet<>();

    /**
     * Called to load the config objects from the files in the passed-in folder.
     * If a required file is not present, one will be created at the end of the loading phase.
     */
    public static void loadFromDisk(boolean onClient) {
        ConfigSystem.configDirectory = new File(InterfaceManager.gameDirectory, "config");
        configDirectory.mkdirs();

        //If we have a settings file already, parse it into Java.
        //Otherwise, make a new one.
        //After parsing the settings file, save it.  This allows new entries to be populated.
        settingsFile = new File(configDirectory, "mtsconfig.json");
        settings = loadConfig(settingsFile, JSONConfigSettings.class, new JSONConfigSettings(), true);

        //Now parse the client config file for clients only.
        if (onClient) {
            clientFile = new File(configDirectory, "mtsconfigclient.json");
            client = loadConfig(clientFile, JSONConfigClient.class, new JSONConfigClient(), true);
            //Check to make sure we have the right keyset.  If not, reset the binds.
            if (client.controls == null || client.controls.keysetID != InterfaceManager.inputInterface.getKeysetID()) {
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
     * Called to do overrides.  Must be called after all packs are loaded.
     */
    public static void initOverrides() {
        if (settings.general.generateOverrideConfigs.value) {
            //Make the default override file and save it.
            try {
                exportConfig(createDefaultCraftingOverrides(), craftingFile);
            } catch (Exception e) {
                InterfaceManager.coreInterface.logError("ConfigSystem failed to create fresh crafting overrides file.  Report to the mod author!");
                InterfaceManager.coreInterface.logError(getErrorMessage(e));
            }
            LanguageSystem.dumpToFolder(new File(configDirectory, "IVLanguageDumps"));
            try {
                createDefaultExternalDamageOverrides();
                exportConfig(externalDamageOverrides, externalDamageFile);
            } catch (Exception e) {
                InterfaceManager.coreInterface.logError("ConfigSystem failed to create fresh external damage overrides file.  Report to the mod author!");
                InterfaceManager.coreInterface.logError(getErrorMessage(e));
            }
        } else {
            try {
                JSONConfigCraftingOverrides overridesObject = loadConfig(craftingFile, JSONConfigCraftingOverrides.class, createEmptyCraftingOverrides(), true);
                if (overridesObject.overrides == null) {
                    overridesObject.overrides = new LinkedHashMap<>();
                }
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
                                if (override.extraRepairMaterialLists != null) {
                                    subDefinition.extraRepairMaterialLists = override.extraMaterialLists.get(subDefinition.subName);
                                }
                                if (override.extraReturnedMaterialLists != null) {
                                    subDefinition.extraReturnedMaterialLists = override.extraReturnedMaterialLists.get(subDefinition.subName);
                                }
                            }
                            if (override.repairMaterialLists != null) {
                                packItem.definition.general.repairMaterialLists = override.repairMaterialLists;
                            }

                            if (packItem instanceof ItemPartEffector) {
                                ItemPartEffector effectorItem = (ItemPartEffector) packItem;
                                if (effectorItem.definition.effector.crafterInputs != null) {
                                    effectorItem.definition.effector.crafterInputs = override.autocrafterInputs;
                                }
                                if (effectorItem.definition.effector.crafterOutputs != null) {
                                    effectorItem.definition.effector.crafterOutputs = override.autocrafterOutputs;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                InterfaceManager.coreInterface.logError("ConfigSystem failed to parse crafting override file JSON.  Crafting overrides will not be applied.");
                InterfaceManager.coreInterface.logError(getErrorMessage(e));
            }
            try {
                externalDamageOverrides = loadConfig(externalDamageFile, JSONConfigExternalDamageOverrides.class, createDefaultExternalDamageOverridesObject(), true);
                if (externalDamageOverrides.overrides == null) {
                    externalDamageOverrides.overrides = new LinkedHashMap<>();
                }
                boolean updatedOverrides = ensureDefaultExternalDamageOverrides(externalDamageOverrides);
                if (updatedOverrides) {
                    safelySaveConfig(externalDamageOverrides, externalDamageFile, "external damage overrides");
                }
            } catch (Exception e) {
                InterfaceManager.coreInterface.logError("ConfigSystem failed to parse external damage override file.  Overrides will not be applied.");
                InterfaceManager.coreInterface.logError(getErrorMessage(e));
                createDefaultExternalDamageOverrides();
            }
        }
    }

    private static void createDefaultExternalDamageOverrides() {
        externalDamageOverrides = createDefaultExternalDamageOverridesObject();
    }

    /**
     * Called to save changes to the various configs to disk. Call this whenever
     * configs are edited to ensure they are saved, as the system does not do this automatically.
     */
    public static void saveToDisk() {
        if (settingsFile != null) {
            safelySaveConfig(settings, settingsFile, "settings config");
        }
        if (clientFile != null) {
            safelySaveConfig(client, clientFile, "client config");
        }
    }

    public static void displayPendingConfigWarnings(IWrapperPlayer player) {
        if (player != null && !pendingConfigWarnings.isEmpty()) {
            for (String warning : pendingConfigWarnings) {
                player.displayChatMessage(LanguageSystem.SYSTEM_DEBUG, warning);
            }
            pendingConfigWarnings.clear();
        }
    }

    private static <ConfigClass> ConfigClass loadConfig(File configFile, Class<ConfigClass> configClass, ConfigClass defaultObject, boolean createIfMissing) {
        if (configFile.exists()) {
            try {
                ConfigClass parsedConfig = JSONParser.parseStream(Files.newInputStream(configFile.toPath()), configClass, null, null);
                if (parsedConfig != null) {
                    return parsedConfig;
                }
                recoverMalformedConfig(configFile, defaultObject, new IOException("Parsed config was null."));
            } catch (Exception e) {
                recoverMalformedConfig(configFile, defaultObject, e);
            }
        } else if (createIfMissing) {
            InterfaceManager.coreInterface.logError("ConfigSystem could not find " + configFile.getName() + ".  Creating a fresh default config.");
            queueConfigWarning("MTS config warning: " + configFile.getName() + " was missing. A new default file was created.");
            safelySaveConfig(defaultObject, configFile, configFile.getName());
        }
        return defaultObject;
    }

    private static <ConfigClass> void recoverMalformedConfig(File configFile, ConfigClass defaultObject, Exception exception) {
        File backupFile = backupConfigFile(configFile);
        if (backupFile != null) {
            InterfaceManager.coreInterface.logError("ConfigSystem failed to parse " + configFile.getName() + ".  Backed it up to " + backupFile.getName() + " and created a fresh default file.");
            InterfaceManager.coreInterface.logError(getErrorMessage(exception));
            queueConfigWarning("MTS config warning: " + configFile.getName() + " was malformed. It was moved to " + backupFile.getName() + " and a new default file was created. Check commas and JSON formatting.");
            safelySaveConfig(defaultObject, configFile, configFile.getName());
        } else {
            blockedConfigSavePaths.add(configFile.getAbsolutePath());
            InterfaceManager.coreInterface.logError("ConfigSystem failed to parse " + configFile.getName() + ", and a backup copy could not be created.  The original file will not be overwritten this session.");
            InterfaceManager.coreInterface.logError(getErrorMessage(exception));
            queueConfigWarning("MTS config warning: " + configFile.getName() + " was malformed, but MTS could not create a safe backup copy. The original file was left untouched. Check commas and JSON formatting.");
        }
    }

    private static void safelySaveConfig(Object configObject, File configFile, String configName) {
        if (configObject == null || configFile == null) {
            return;
        }
        if (blockedConfigSavePaths.contains(configFile.getAbsolutePath())) {
            InterfaceManager.coreInterface.logError("ConfigSystem skipped saving " + configName + " because the original file could not be safely backed up after a parse failure.");
            return;
        }
        try {
            exportConfig(configObject, configFile);
        } catch (Exception e) {
            InterfaceManager.coreInterface.logError("ConfigSystem failed to save " + configName + ".  Report to the mod author!");
            InterfaceManager.coreInterface.logError(getErrorMessage(e));
        }
    }

    private static void exportConfig(Object configObject, File configFile) throws IOException {
        File parentDirectory = configFile.getParentFile();
        if (parentDirectory != null) {
            parentDirectory.mkdirs();
        }
        File tempFile = new File(parentDirectory, configFile.getName() + ".tmp");
        JSONParser.exportStream(configObject, Files.newOutputStream(tempFile.toPath()));
        try {
            Files.move(tempFile.toPath(), configFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tempFile.toPath(), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            tempFile.delete();
            throw e;
        }
    }

    private static File backupConfigFile(File configFile) {
        try {
            File backupFile = getBackupFile(configFile);
            try {
                Files.move(configFile.toPath(), backupFile.toPath());
            } catch (IOException moveException) {
                Files.copy(configFile.toPath(), backupFile.toPath());
            }
            return backupFile;
        } catch (Exception backupException) {
            InterfaceManager.coreInterface.logError("ConfigSystem failed to back up malformed config file " + configFile.getName() + ".");
            InterfaceManager.coreInterface.logError(getErrorMessage(backupException));
            return null;
        }
    }

    private static File getBackupFile(File configFile) {
        String fileName = configFile.getName();
        int extensionIndex = fileName.lastIndexOf('.');
        String filePrefix = extensionIndex >= 0 ? fileName.substring(0, extensionIndex) : fileName;
        String fileSuffix = extensionIndex >= 0 ? fileName.substring(extensionIndex) : "";
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File backupFile = new File(configFile.getParentFile(), filePrefix + "_error_" + timestamp + fileSuffix);
        int counter = 1;
        while (backupFile.exists()) {
            backupFile = new File(configFile.getParentFile(), filePrefix + "_error_" + timestamp + "_" + counter++ + fileSuffix);
        }
        return backupFile;
    }

    private static String getErrorMessage(Exception exception) {
        return exception.getClass().getSimpleName() + (exception.getMessage() != null ? ": " + exception.getMessage() : "");
    }

    private static void queueConfigWarning(String warning) {
        pendingConfigWarnings.add(warning);
    }

    private static JSONConfigCraftingOverrides createEmptyCraftingOverrides() {
        JSONConfigCraftingOverrides overridesObject = new JSONConfigCraftingOverrides();
        overridesObject.overrides = new LinkedHashMap<>();
        return overridesObject;
    }

    private static JSONConfigCraftingOverrides createDefaultCraftingOverrides() {
        JSONConfigCraftingOverrides overridesObject = createEmptyCraftingOverrides();
        for (AItemPack<?> packItem : PackParser.getAllPackItems()) {
            Map<String, JSONCraftingOverride> packOverrides = overridesObject.overrides.computeIfAbsent(packItem.definition.packID, k -> new LinkedHashMap<>());
            JSONCraftingOverride override = packOverrides.computeIfAbsent(packItem.definition.systemName, k -> new JSONCraftingOverride());
            override.commonMaterialLists = packItem.definition.general.materialLists;
            if (packItem instanceof AItemSubTyped) {
                JSONSubDefinition subDefinition = ((AItemSubTyped<?>) packItem).subDefinition;
                if (override.extraMaterialLists == null) {
                    override.extraMaterialLists = new HashMap<>();
                }
                override.extraMaterialLists.put(subDefinition.subName, subDefinition.extraMaterialLists);
                if (subDefinition.extraRepairMaterialLists != null) {
                    if (override.extraRepairMaterialLists == null) {
                        override.extraRepairMaterialLists = new HashMap<>();
                    }
                    override.extraRepairMaterialLists.put(subDefinition.subName, subDefinition.extraMaterialLists);
                }
                if (subDefinition.extraReturnedMaterialLists != null) {
                    if (override.extraReturnedMaterialLists == null) {
                        override.extraReturnedMaterialLists = new HashMap<>();
                    }
                    override.extraReturnedMaterialLists.put(subDefinition.subName, subDefinition.extraReturnedMaterialLists);
                }
            }
            override.repairMaterialLists = packItem.definition.general.repairMaterialLists;
            override.returnedMaterialLists = packItem.definition.general.returnedMaterialLists;

            if (packItem instanceof ItemPartEffector) {
                ItemPartEffector effectorItem = (ItemPartEffector) packItem;
                if (effectorItem.definition.effector.crafterInputs != null) {
                    override.autocrafterInputs = effectorItem.definition.effector.crafterInputs;
                    override.autocrafterOutputs = effectorItem.definition.effector.crafterOutputs;
                }
            }
        }
        return overridesObject;
    }

    private static JSONConfigExternalDamageOverrides createDefaultExternalDamageOverridesObject() {
        JSONConfigExternalDamageOverrides overridesObject = new JSONConfigExternalDamageOverrides();
        overridesObject.overrides = new LinkedHashMap<>();
        ensureDefaultExternalDamageOverrides(overridesObject);
        return overridesObject;
    }

    private static boolean ensureDefaultExternalDamageOverrides(JSONConfigExternalDamageOverrides overridesObject) {
        boolean modified = false;
        for (AItemPack<?> packItem : PackParser.getAllPackItems()) {
            if (packItem instanceof ItemVehicle) {
                Map<String, Double> packOverrides = overridesObject.overrides.get(packItem.definition.packID);
                if (packOverrides == null) {
                    packOverrides = new LinkedHashMap<>();
                    overridesObject.overrides.put(packItem.definition.packID, packOverrides);
                    modified = true;
                }
                if (!packOverrides.containsKey(packItem.definition.systemName)) {
                    packOverrides.put(packItem.definition.systemName, 1.0D);
                    modified = true;
                }
            }
        }
        return modified;
    }
}
