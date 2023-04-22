package minecrafttransportsimulator.packloading;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.InterfaceManager;

/**
 * Class that contains information about one set of materials for a pack item.
 * This is the number of materials, as well as the various items that can qualify for
 * these materials.  Used for crafting pack items.
 *
 * @author don_bruce
 */
public class PackMaterialComponent {
    public final int qty;
    public final int meta;
    public final List<IWrapperItemStack> possibleItems;
    public static String lastErrorMessage;
    public static final Map<String, String> flattenedNames = generateFlattnedMappings();
    public static final Map<String, String> unflattenedNames = generateUnflattnedMappings();

    private PackMaterialComponent(String itemText) {
        possibleItems = new ArrayList<>();
        String[] itemParameters = itemText.split(":");
        String domain = itemParameters[0];
        String name = itemParameters[1];
        boolean isOreDict = domain.equals("oredict") || domain.equals("tags");
        if (InterfaceManager.coreInterface.isGameFlattened()) {
            if (itemParameters.length == 4) {
                //Un-flattned item, convert via name:meta string.
                String newName = flattenedNames.get(name + ":" + itemParameters[2]);
                if (newName != null) {
                    name = newName;
                }
                qty = Integer.parseInt(itemParameters[3]);
            } else {
                if (isOreDict) {
                    //May not have this converted, but we might.  Try to convert just in case.
                    String newName = flattenedNames.get(name);
                    if (newName != null) {
                        name = newName;
                    }
                }
                qty = Integer.parseInt(itemParameters[2]);
            }
            //Meta is always 0 for flattened systems.
            meta = 0;
        } else {
            if (isOreDict) {
                //May not have this converted, but we might.  Try to convert just in case.
                String newName = unflattenedNames.get(name);
                if (newName != null) {
                    name = newName;
                }
                qty = Integer.parseInt(itemParameters[2]);
                meta = 0;
            } else if (itemParameters.length != 4) {
                //Flattened item, un-convert.
                String newName = unflattenedNames.get(name);
                if (newName != null) {
                    String[] newNameComponents = unflattenedNames.get(name).split(":");
                    name = newNameComponents[0];
                    meta = Integer.parseInt(newNameComponents[1]);
                } else {
                    meta = 0;
                }
                qty = Integer.parseInt(itemParameters[2]);
            } else {
                //Un-flattened item in proper environment.
                meta = Integer.parseInt(itemParameters[2]);
                qty = Integer.parseInt(itemParameters[3]);
            }
        }

        if (isOreDict) {
            possibleItems.addAll(InterfaceManager.coreInterface.getOredictMaterials(name, qty));
        } else {
            IWrapperItemStack stack = InterfaceManager.coreInterface.getStackForProperties(domain + ":" + name, meta, qty);
            if (!stack.isEmpty()) {
                possibleItems.add(stack);
            }
        }
    }

    /**
     * Returns the Material Components require to craft the passed-in item with the passed-in recipe.
     * Each component corresponds to a single ingredient input, with each list in the component corresponding to the
     * possible items that are valid for that ingredient.  The idea being that OreDict allows for
     * multiple items to be used.  If this component is not for crafting checks, set forCraftingCheck to false.
     * This prevents the returned stacks from having the wildcard value in their metadata and not being actual items.
     * If an error occured during parsing, then null is returned and {@link #lastErrorMessage} is set to the error.
     */
    public static List<PackMaterialComponent> parseFromJSON(AItemPack<?> item, int recipeIndex, boolean includeMain, boolean includeSub, boolean forRepair) {
        //Get all the strings that represent the items.
        List<String> itemTexts = new ArrayList<String>();
        String currentSubName = "";
        if (forRepair) {
            if (item.definition.general.repairMaterialLists != null) {
                //Get repair materials. Make sure to add actual item into the list too.
                if (InterfaceManager.coreInterface.isGameFlattened()) {
                    itemTexts.add(InterfaceManager.coreModID + ":" + item.getRegistrationName() + "1");
                } else {
                    itemTexts.add(InterfaceManager.coreModID + ":" + item.getRegistrationName() + ":0:1");
                }
                itemTexts.addAll(item.definition.general.repairMaterialLists.get(recipeIndex));
            }
        } else {
            //Get main materials.
            if (includeMain) {
                itemTexts.addAll(item.definition.general.materialLists.get(recipeIndex));
            }

            //Get subType materials, if required.
            if (includeSub && item instanceof AItemSubTyped) {
                AItemSubTyped<?> subItem = (AItemSubTyped<?>) item;
                currentSubName = subItem.subDefinition.subName;
                if (subItem.subDefinition.extraMaterialLists.size() != item.definition.general.materialLists.size()) {
                    lastErrorMessage = "This item has a mis-matched number of normal materialLists (" + item.definition.general.materialLists.size() + ") and extraMaterialLists (" + subItem.subDefinition.extraMaterialLists.size() + ") for " + item.definition.packID + ":" + item.definition.systemName + currentSubName + ".  Crafting will be disabled in survival mode.  Report this to the pack author!";
                    return null;
                }
                itemTexts.addAll(subItem.subDefinition.extraMaterialLists.get(recipeIndex));
            }
        }

        //Parse the items.
        List<PackMaterialComponent> components = new ArrayList<PackMaterialComponent>();
        for (String itemText : itemTexts) {
            try {
                PackMaterialComponent component = new PackMaterialComponent(itemText);
                if (!component.possibleItems.isEmpty()) {
                    components.add(component);
                } else {
                    lastErrorMessage = "No valid items found for " + (forRepair ? "repair" : "normal") + " crafting ingredient entry " + itemText + " for " + item.definition.packID + ":" + item.definition.systemName + currentSubName + ".  Crafting will be disabled in survival mode.  Report this to the pack author!";
                    return null;
                }
            } catch (Exception e) {
                lastErrorMessage = "A fault was encountered when trying to parse " + (forRepair ? "repair" : "normal") + " crafting ingredient entry " + itemText + " for " + item.definition.packID + ":" + item.definition.systemName + currentSubName + ".  Crafting will be disabled in survival mode.  Report this to the pack author!";
                return null;
            }
        }

        //Return all materials.
        return components;
    }
    
    private static Map<String, String> generateFlattnedMappings(){
        Map<String, String> map = new HashMap<>();
        map.put("stone:1", "granite");
        map.put("stone:2", "polished_granite");
        map.put("stone:3", "diorite");
        map.put("stone:4", "polished_diorite");
        map.put("stone:5", "andesite");
        map.put("stone:6", "polished_andesite");
        map.put("grass:0", "grass_block");
        map.put("dirt:1", "coarse_dirt");
        map.put("dirt:2", "podzol");
        map.put("planks:0", "oak_planks");
        map.put("planks:1", "spruce_planks");
        map.put("planks:2", "birch_planks");
        map.put("planks:3", "jungle_planks");
        map.put("planks:4", "acacia_planks");
        map.put("planks:5", "dark_oak_planks");
        map.put("sapling:0", "oak_sapling");
        map.put("sapling:1", "spruce_sapling");
        map.put("sapling:2", "birch_sapling");
        map.put("sapling:3", "jungle_sapling");
        map.put("sapling:4", "acacia_sapling");
        map.put("sapling:5", "dark_oak_sapling");
        map.put("sand:1", "red_sand");
        map.put("log:0", "oak_log");
        map.put("log:1", "spruce_log");
        map.put("log:2", "birch_log");
        map.put("log:3", "jungle_log");
        map.put("log:4", "acacia_log");
        map.put("log:5", "dark_oak_log");
        map.put("leaves:0", "oak_leaves");
        map.put("leaves:1", "spruce_leaves");
        map.put("leaves:2", "birch_leaves");
        map.put("leaves:3", "jungle_leaves");
        map.put("leaves:4", "acacia_leaves");
        map.put("leaves:5", "dark_oak_leaves");
        map.put("sponge:1", "wet_sponge");
        map.put("sandstone:1", "chiseled_sandstone");
        map.put("sandstone:2", "cut_sandstone");
        map.put("golden_rail:0", "powered_rail");
        map.put("tallgrass:2", "fern");
        map.put("deadbush:0", "dead_bush");
        map.put("wool:0", "white_wool");
        map.put("wool:1", "orange_wool");
        map.put("wool:2", "magenta_wool");
        map.put("wool:3", "light_blue_wool");
        map.put("wool:4", "yellow_wool");
        map.put("wool:5", "lime_wool");
        map.put("wool:6", "pink_wool");
        map.put("wool:7", "gray_wool");
        map.put("wool:8", "light_gray_wool");
        map.put("wool:9", "cyan_wool");
        map.put("wool:10", "purple_wool");
        map.put("wool:11", "blue_wool");
        map.put("wool:12", "brown_wool");
        map.put("wool:13", "green_wool");
        map.put("wool:14", "red_wool");
        map.put("wool:15", "black_wool");
        map.put("yellow_flower:0", "dandelion");
        map.put("red_flower:0", "poppy");
        map.put("red_flower:1", "blue_orchid");
        map.put("red_flower:2", "allium");
        map.put("red_flower:3", "azure_bluet");
        map.put("red_flower:4", "red_tulip");
        map.put("red_flower:5", "orange_tulip");
        map.put("red_flower:6", "white_tulip");
        map.put("red_flower:7", "pink_tulip");
        map.put("red_flower:8", "oxeye_daisy");
        map.put("wooden_slab:0", "oak_slab");
        map.put("wooden_slab:1", "spruce_slab");
        map.put("wooden_slab:2", "birch_slab");
        map.put("wooden_slab:3", "jungle_slab");
        map.put("wooden_slab:4", "acacia_slab");
        map.put("wooden_slab:5", "dark_oak_slab");
        map.put("stone_slab:1", "sandstone_slab");
        map.put("stone_slab:3", "cobblestone_slab");
        map.put("stone_slab:4", "brick_slab");
        map.put("stone_slab:5", "stone_brick_slab");
        map.put("stone_slab:6", "nether_brick_slab");
        map.put("stone_slab:7", "quartz_slab");
        map.put("double_stone_slab:7", "smooth_quartz");
        map.put("double_stone_slab:8", "smooth_stone");
        map.put("brick_block:8", "bricks");
        map.put("wooden_pressure_plate:0", "oak_pressure_plate");
        map.put("snow_layer:0", "snow");
        map.put("snow:0", "snow_block");
        map.put("fence:0", "oak_fence");
        map.put("lit_pumpkin:0", "jack_o_lantern");
        map.put("trapdoor:0", "oak_trapdoor");
        map.put("stained_hardened_clay:0", "white_terracotta");
        map.put("stained_hardened_clay:1", "orange_terracotta");
        map.put("stained_hardened_clay:2", "magenta_terracotta");
        map.put("stained_hardened_clay:3", "light_blue_terracotta");
        map.put("stained_hardened_clay:4", "yellow_terracotta");
        map.put("stained_hardened_clay:5", "lime_terracotta");
        map.put("stained_hardened_clay:6", "pink_terracotta");
        map.put("stained_hardened_clay:7", "gray_terracotta");
        map.put("stained_hardened_clay:8", "light_gray_terracotta");
        map.put("stained_hardened_clay:9", "cyan_terracotta");
        map.put("stained_hardened_clay:10", "purple_terracotta");
        map.put("stained_hardened_clay:11", "blue_terracotta");
        map.put("stained_hardened_clay:12", "brown_terracotta");
        map.put("stained_hardened_clay:13", "green_terracotta");
        map.put("stained_hardened_clay:14", "red_terracotta");
        map.put("stained_hardened_clay:15", "black_terracotta");

        //Oredict entries
        map.put("ingotIron", "ingots/iron");
        map.put("ingotGold", "ingots/gold");
        map.put("ingotCopper", "ingots/copper");

        map.put("nuggetGold", "nuggets/gold");
        map.put("nuggetIron", "nuggets/iron");

        map.put("dyeBlack", "dyes/black");
        map.put("dyeBlue", "dyes/blue");
        map.put("dyeBrown", "dyes/brown");
        map.put("dyeCyan", "dyes/cyan");
        map.put("dyeGray", "dyes/gray");
        map.put("dyeGreen", "dyes/green");
        map.put("dyeLightBlue", "dyes/light_blue");
        map.put("dyeLightGray", "dyes/light_gray");
        map.put("dyeLime", "dyes/lime");
        map.put("dyeMagenta", "dyes/magenta");
        map.put("dyeOrange", "dyes/orange");
        map.put("dyePink", "dyes/pink");
        map.put("dyePurple", "dyes/purple");
        map.put("dyeRed", "dyes/red");
        map.put("dyeWhite", "dyes/white");
        map.put("dyeYellow", "dyes/yellow");

        map.put("dustRedstone", "dusts/redstone");
        map.put("dustGlowstone", "dusts/glowstone");

        map.put("plankWood", "planks");
        map.put("logWood", "logs");

        //Immersive Engineering stuff.
        map.put("metal:0", "ingot_copper");
        map.put("metal:1", "ingot_aluminum");
        map.put("metal:2", "ingot_lead");
        map.put("metal:3", "ingot_silver");
        map.put("metal:4", "ingot_nickel");
        map.put("metal:5", "ingot_uranium");
        map.put("metal:6", "ingot_constantan");
        map.put("metal:7", "ingot_electrum");
        map.put("metal:8", "ingot_steel");

        map.put("metal:20", "nugget_copper");
        map.put("metal:21", "nugget_aluminum");
        map.put("metal:22", "nugget_lead");
        map.put("metal:23", "nugget_silver");
        map.put("metal:24", "nugget_nickel");
        map.put("metal:25", "nugget_uranium");
        map.put("metal:26", "nugget_constantan");
        map.put("metal:27", "nugget_electrum");
        map.put("metal:28", "nugget_steel");

        map.put("metal:30", "plate_copper");
        map.put("metal:31", "plate_aluminum");
        map.put("metal:32", "plate_lead");
        map.put("metal:33", "plate_silver");
        map.put("metal:34", "plate_nickel");
        map.put("metal:35", "plate_uranium");
        map.put("metal:36", "plate_constantan");
        map.put("metal:37", "plate_electrum");
        map.put("metal:38", "plate_steel");
        map.put("metal:39", "plate_iron");
        map.put("metal:40", "plate_gold");

        map.put("material:1", "stick_iron");
        map.put("material:2", "stick_steel");
        map.put("material:3", "stick_aluminum");

        map.put("material:8", "component_iron");
        map.put("material:9", "component_steel");
        map.put("material:26", "electron_tube");
        map.put("material:27", "logic_circuit");

        map.put("material:20", "wire_copper");
        map.put("material:21", "wire_electrum");
        map.put("material:22", "wire_aluminum");
        map.put("material:23", "wire_steel");

        map.put("storage:0", "storage_copper");
        map.put("storage:1", "storage_aluminum");
        map.put("storage:2", "storage_lead");
        map.put("storage:3", "storage_silver");
        map.put("storage:4", "storage_nickel");
        map.put("storage:5", "storage_uranium");
        map.put("storage:6", "storage_constantan");
        map.put("storage:7", "storage_electrum");
        map.put("storage:8", "storage_steel");
        return map;
    }
    
    private static Map<String, String> generateUnflattnedMappings(){
        Map<String, String> map = new HashMap<>();
        flattenedNames.forEach((key, value) -> map.put(value, key));
        return map;
    }
}
