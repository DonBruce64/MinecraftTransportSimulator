package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.packloading.PackResourceLoader.ItemClassification;

/**
 * Base JSON class for all pack-loaded JSONs.  All pack-loaded JSONs MUST extend this class.
 * This class will be loaded by {@link PackParser} to create objects that contain the properties
 * of the pack items that the sub-classes of this class define.
 *
 * @author don_bruce
 */
public abstract class AJSONItem {
    /**
     * A String containing any prefix folders for this item.  Used by the packloading system to tell
     * where resources are relative to the item's JSON definition file path.  This contains all folders
     * "between" the item's classification directory and the item's actual file.
     */
    public transient String prefixFolders;
    /**
     * ID of the pack that this JSON was pulled from.  Set after JSON is parsed into an object and used
     * to tell MTS what jar to load assets from.
     */
    public transient String packID;
    /**
     * Unique name (per-pack) that is used to identify this item in the MTS system.  Set after JSON is
     * parsed into an object and used for lookup operations or for maps when we need to keep track of
     * a pack component by name rather than an object instance.  Used heavily in save/load operations.
     */
    public transient String systemName;
    /**
     * Classification for this item.  This is used to determine what folders this JSON definition,
     * and it's associated resources, are found in.
     */
    public transient ItemClassification classification;
    /**
     * Generic variable used to represent the general properties of this pack item.  Extend this and
     * reference it in the generic for all sub-classes.
     */
    @JSONRequired
    @JSONDescription("Common and core content to all JSONs.")
    public General general;

    public static class General {
        @JSONDescription("The name of this content.  Will be displayed in item form.  Note that if the content is a type that has a set of definitions, then this name is ignored and the appropriate definition name is used instead.")
        public String name;

        @JSONDescription("An optional description.  Will be rendered as an item tooltip.  Unlike the name parameter, descriptions on definitions are appended to this description, and do not overwrite it.  The idea behind this is that some variants need extra text to tell why they are different from one another, but shouldn't require re-writing the main description.")
        public String description;

        @JSONDescription("The optional stack size for this item.  Items with this set will stack to the size specified, up to the standard stack size of 64.  This of course won't work if the item has NBT on it, such as used engines.")
        public int stackSize;

        @JSONDescription("How much health the entity that this item spawns has.  When the damage reaches the health amount, the entity will execute its 'death' logic.  What exactly this entails depends on what the entity is.  Some entities do not have any logic for death, and items that don't spawn entities don't use this parameter at all.")
        public int health;

        @JSONRequired
        @JSONDescription("A set of lists of material lists that are required to create this component.  Normally, items have just one list, but can have multiple for multiple ways to craft the item.  This allows for incorperation of modded items, as well as different Minecraft versions.  The format for each material entry in each list is <GiveString:Metadata:Qty> on 1.12.2, and <GiveString:Qty> on higher versions.  Where GiveString is the name of the item that's found in the /give command, Metadata is the metadata of the item, and Qty is the quantity needed.  Should a component have no materials (and no extraMaterialLists, if it uses definitions) it will not be available for crafting in any benches.  If you wish to use OreDict, simply replace the GiveString with the OreDict name, and omit the Metadata parameter if on 1.12.2.")
        public List<List<String>> materialLists;

        @JSONDescription("Like materials, but these materials, in combination with an existing instance of this item, can be used to craft a fresh copy.")
        public List<List<String>> repairMaterialLists;

        @JSONDescription("An optional oreDict name for this item.  This will make the item be part of the specified oreDict.  Note that you may use custom oreDict names if you want multiple items to be the same ingredient.")
        public String oreDict;

        //Moved from multiple locations.
        //Vehicle was deprecated for vehicle type.
        //Part was for part type, and went to generic.
        //Decor was for decor type, and went to decor
        //Pole was for pole type, and went to pole.
        //Item was for item type, and went in item.
        @Deprecated
        public String type;

        //Moved from MultiModelProvider during common JSON overhauling.
        @Deprecated
        public String modelName;

        //Moved from Vehicle during common JSON overhauling.
        @Deprecated
        public boolean isAircraft;
        @Deprecated
        public boolean isBlimp;
        @Deprecated
        public boolean openTop;
        @Deprecated
        public int emptyMass;

        //Moved from Part during common JSON overhauling.
        @Deprecated
        public String customType;
        @Deprecated
        public boolean useVehicleTexture;

        //Moved from Decor during common JSON overhauling.
        @Deprecated
        public float width;
        @Deprecated
        public float height;
        @Deprecated
        public float depth;
        @Deprecated
        public List<String> itemTypes;
        @Deprecated
        public List<String> partTypes;
        @Deprecated
        public List<String> items;

        //Moved from Pole during common JSON overhauling.
        @Deprecated
        public float radius;

        //Moved from Skin during common JSON overhauling.
        @Deprecated
        public String packID;
        @Deprecated
        public String systemName;

        //These came from both Decor and Pole.
        @Deprecated
        public TextLine[] textLines;
        @Deprecated
        public List<JSONText> textObjects;

        @Deprecated
        public static class TextLine {
            public int characters;
            public float xPos;
            public float yPos;
            public float zPos;
            public float scale;
            public ColorRGB color;
        }

        @Deprecated
        public List<String> materials;

        @Deprecated
        public List<String> repairMaterials;
    }
}