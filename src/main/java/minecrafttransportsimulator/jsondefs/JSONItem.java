package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

@JSONDescription("While allowing users to craft vehicles is great, it can get a bit un-realistic to make a truck out of stacks of iron ingots and glass.  To help with this, MTS allows you to make custom items.  These items are loaded via JSON like all other vehicle bits, but rather than use an OBJ model they use the normal Minecraft item JSON format.  This allows you to take any JSON outputted from any modeling software (such as BlockBench) and plop it, along with a small file in the jsondefs section, into your pack for crafting use!  Items just have a general section per the default.  That's it.  If you want to add functionality to your items, you can do so by giving the appropriate type parameter.")
public class JSONItem extends AJSONItem {

    @JSONDescription("Properties for all items..")
    public JSONItemGeneric item;

    @JSONRequired(dependentField = "type", dependentValues = {"booklet"}, subField = "item")
    @JSONDescription("Booklet-specific item section.")
    public JSONBooklet booklet;

    @JSONRequired(dependentField = "type", dependentValues = {"food"}, subField = "item")
    @JSONDescription("Food-specific item section.")
    public JSONFood food;

    @JSONRequired(dependentField = "type", dependentValues = {"weapon"}, subField = "item")
    @JSONDescription("Weapon-specific item section.")
    public JSONWeapon weapon;

    public static class JSONItemGeneric {
        @JSONDescription("The functionality to give this item.")
        public ItemComponentType type;
    }

    public static class JSONBooklet {
        @JSONDescription("How wide of a texture, in px, this booklet uses.  Used for ALL pages")
        public int textureWidth;

        @JSONDescription("How high of a texture, in px, this booklet uses.  Used for ALL pages")
        public int textureHeight;

        @JSONDescription("If present and true, the Table of Contents will not be created.")
        public boolean disableTOC;

        @JSONDescription("The name of the texture for the cover.  Should be prefixed by your modID and MUST be a power of 2.")
        public String coverTexture;

        @JSONDescription("A list of Text objects that make up the text for the title.")
        public List<JSONText> titleText;

        @JSONDescription("A list of Page objects that make up the pages of this booklet.")
        public List<BookletPage> pages;

        public static class BookletPage {
            @JSONDescription("The name of the texture for this page.  Each page may use a different texture, if desired.\nHowever, all textures MUST be the same resolution as defined by textureWidth and textureHeight.")
            public String pageTexture;

            @JSONDescription("The title for this page that will be used to make the Table of Contents.  May be omitted if the Table of Contents is disabled.")
            public String title;

            @JSONDescription("An list of text objects that make up the text for the page. These follow the same format as textObjects.\nThe only difference between these and vehicles is the z-coord should always be 0, and the defaultText parameter is what will be rendered in the booklet.")
            public List<JSONText> pageText;
        }
    }

    public static class JSONFood {
        @JSONDescription("The animation to play while consuming this item.  If true, the drinking animation is played.  If false, the eating animation is played.")
        public boolean isDrink;

        @JSONDescription("How long, in ticks, it takes to eat this food item.")
        public int timeToEat;

        @JSONDescription("How much hunger this food item fills.  Must be a whole number.")
        public int hungerAmount;

        @JSONDescription("How much saturation this food has.  May be a decimal.")
        public float saturationAmount;

        @JSONDescription("A optional list of effects that this food item provides.")
        public List<JSONPotionEffect> effects;
    }

    public static class JSONWeapon {
        @JSONDescription("How much damage this weapon will inflict when it hits an entity.")
        public double attackDamage;

        @JSONDescription("How long, in ticks, between each strike of this weapon.")
        public double attackCooldown;
    }

    public enum ItemComponentType {
        @JSONDescription("Creates an item with no functionality.")
        NONE,
        @JSONDescription("Creates a booklet, which is a book-like item.")
        BOOKLET,
        @JSONDescription("Creates an item that can be eaten.")
        FOOD,
        @JSONDescription("Creates an item that can be used as a weapon.")
        WEAPON,
        @JSONDescription("Creates an item that works as a part scanner.")
        SCANNER,
        @JSONDescription("Creates an item that works as a wrench.")
        WRENCH,
        @JSONDescription("Creates an item that works as a paint gun.")
        PAINT_GUN,
        @JSONDescription("Creates an item that works as a key.")
        KEY,
        @JSONDescription("Creates an item that works as a ticket.")
        TICKET,
        @JSONDescription("Creates an item that works as a fuel hose.")
        FUEL_HOSE,
        @JSONDescription("Creates an item that works as jumper cables.")
        JUMPER_CABLES,
        @JSONDescription("Creates an item that works as a jumper pack.")
        JUMPER_PACK,
        @JSONDescription("Creates an item that works as a Y2K button.")
        Y2K_BUTTON
    }
}
