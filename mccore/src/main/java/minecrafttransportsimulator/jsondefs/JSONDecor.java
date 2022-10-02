package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

@JSONDescription("Sometimes, you just want to have something shiny to place in the world that can't be driven away by a pesky thief.  In this case, decor is the go-to choice.  Decors are essentially block-based OBJ models that can be placed down.  These come with full animation support, and can even have some additional functions built-in to justify the cost of crafting them.")
public class JSONDecor extends AJSONMultiModelProvider {

    @JSONDescription("Decor-specific properties.")
    public JSONDecorGeneric decor;

    public static class JSONDecorGeneric {
        @JSONDescription("An optional type for this decor to give it functionality.")
        public DecorComponentType type;

        @JSONRequired
        @JSONDescription("How wide a decor is.  1 is a full block width.  Numbers over 1 will result in unpredictable operations, so don't use them.")
        public float width;

        @JSONRequired
        @JSONDescription("How high a decor is.  1 is a full block height.  Numbers over 1 will result in unpredictable operations, so don't use them.")
        public float height;

        @JSONRequired
        @JSONDescription("How deep a decor is.  1 is a full block depth.  Numbers over 1 will result in unpredictable operations, so don't use them.")
        public float depth;

        @JSONDescription("How much light this decor gives off.  A value from 0-1, with 1 being full possible light.")
        public float lightLevel;

        @JSONDescription("How much of an offset in the Y direction to make the player sit on this decor.  Only valid for seats.")
        public float sittingOffset;

        @JSONDescription("A optional crafting definition for this decor.  If this is included, the decor will open a GUI for crafting pack components when clicked.")
        public JSONCraftingBench crafting;

        @JSONDescription("An optional number of inventory units for this decor.  If set, it will make this decor act like a chest and hold items.  This is how many rows (of 9 slots) the inventory has.  Also used in item loaders/unloaders to configure their internal buffers.")
        public float inventoryUnits;

        @JSONDescription("The texture for the GUI if the decor has an inventory.  Only used if this decor has an inventory.  If not set, the default is used.")
        public String inventoryTexture;

        @JSONDescription("The fuel capacity of the decor. Units are in milli-buckets. Only use if this decor is a fuel pump/fluid loader. If not set, a value of 15000 is used.")
        public int fuelCapacity;

        @JSONDescription("The quantity of fuel or energy to pump per tick. Units are in milli-buckets/electric. Only use if this decor is a fuel pump/fluid loader/charger. If not set, a value of 10 is used.")
        public float pumpRate;

        @Deprecated
        public List<String> itemTypes;
        @Deprecated
        public List<String> partTypes;
        @Deprecated
        public List<String> items;
    }

    public enum DecorComponentType {
        @JSONDescription("Will make the decor have no functionality.")
        GENERIC,
        @JSONDescription("Will make the decor have chest functionality.")
        CHEST,
        @JSONDescription("Will make the decor have beacon functionality.")
        BEACON,
        @JSONDescription("Will make the decor have signal controller functionality.")
        SIGNAL_CONTROLLER,
        @JSONDescription("Will make the decor have fuel pump functionality.")
        FUEL_PUMP,
        @JSONDescription("Will make the decor have electric vehicle charger functionality.")
        CHARGER,
        @JSONDescription("Will make the decor have item loader functionality.")
        ITEM_LOADER,
        @JSONDescription("Will make the decor have item unloader functionality.")
        ITEM_UNLOADER,
        @JSONDescription("Will make the decor have fluid loader functionality.")
        FLUID_LOADER,
        @JSONDescription("Will make the decor have fluid unloader functionality.")
        FLUID_UNLOADER,
        @JSONDescription("Will make the decor have radio functionality.  Exact same system as vehicles.  It even syncs up with them!")
        RADIO,
        @JSONDescription("Will make the decor have seat functionality.")
        SEAT
    }
}