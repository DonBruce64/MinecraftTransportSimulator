package minecrafttransportsimulator.packloading;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.jsondefs.JSONBullet;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.jsondefs.JSONInstrument;
import minecrafttransportsimulator.jsondefs.JSONItem;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent;
import minecrafttransportsimulator.jsondefs.JSONSkin;
import minecrafttransportsimulator.jsondefs.JSONVehicle;

/**
 * Class responsible for loading pack resource files from pack archives.  This happens both during load
 * and during gameplay as different references are requested.
 *
 * @author don_bruce
 */
public class PackResourceLoader {
    /**
     * Returns the requested resource for the passed-in definition.  Name in this case is used
     * for the file-name, and may be different than the definition system name.
     */
    public static String getPackResource(AJSONItem definition, ResourceType type, String name) {
        switch (PackStructure.values()[PackParser.getPackConfiguration(definition.packID).fileStructure]) {
            case DEFAULT: {
                return "/assets/" + definition.packID + "/" + type.prefixFolder + definition.classification.toDirectory() + name + type.normalSuffix;
            }
            case LAYERED: {
                return "/assets/" + definition.packID + "/" + type.prefixFolder + definition.classification.toDirectory() + definition.prefixFolders + name + type.normalSuffix;
            }
            case MODULAR: {
                return "/assets/" + definition.packID + "/" + definition.classification.toDirectory() + definition.prefixFolders + name + type.modularSuffix;
            }
        }
        return null;
    }

    public enum PackStructure {
        DEFAULT,
        LAYERED,
        MODULAR
    }

    public enum ResourceType {
        OBJ_MODEL("objmodels/", ".obj", ".obj"),
        LT_MODEL("littletilesmodels/", ".txt", ".txt"),
        PNG("textures/", ".png", ".png"),
        ITEM_PNG("textures/items/", ".png", "_item.png"),
        ITEM_JSON("models/item/", ".json", "_item.json");

        public final String prefixFolder;
        public final String normalSuffix;
        public final String modularSuffix;

        ResourceType(String prefixFolder, String normalSuffix, String modularSuffix) {
            this.prefixFolder = prefixFolder;
            this.normalSuffix = normalSuffix;
            this.modularSuffix = modularSuffix;
        }
    }

    public enum ItemClassification {
        VEHICLE(JSONVehicle.class),
        PART(JSONPart.class),
        INSTRUMENT(JSONInstrument.class),
        POLE(JSONPoleComponent.class),
        ROAD(JSONRoadComponent.class),
        DECOR(JSONDecor.class),
        BULLET(JSONBullet.class),
        ITEM(JSONItem.class),
        SKIN(JSONSkin.class);

        public final Class<? extends AJSONItem> representingClass;

        ItemClassification(Class<? extends AJSONItem> representingClass) {
            this.representingClass = representingClass;
        }

        public static List<String> getAllTypesAsStrings() {
            List<String> assetTypes = new ArrayList<>();
            for (ItemClassification classification : ItemClassification.values()) {
                assetTypes.add(classification.name().toLowerCase());
            }
            return assetTypes;
        }

        public String toDirectory() {
            return this.name().toLowerCase() + "s/";
        }

        public static ItemClassification fromDirectory(String directory) {
            try {
                return ItemClassification.valueOf(directory.substring(0, directory.length() - "s/".length()).toUpperCase());
            } catch (Exception e) {
                throw new IllegalArgumentException("Was told to get classification for directory: " + directory + " but none exists.  Contact the mod author!");
            }
        }
    }
}
