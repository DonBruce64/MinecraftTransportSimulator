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
                //Un-flattned item, convert via domain:name:meta string.
                String newName = flattenedNames.get(domain + ":" + name + ":" + itemParameters[2]);
                if (newName != null) {
                    //Returned format is domain:name.
                    name = newName.split(":")[1];
                }
                qty = Integer.parseInt(itemParameters[3]);
            } else {
                if (isOreDict) {
                    //May not have this converted, but we might.  Try to convert just in case.
                    //Need domain:name, no meta.
                    String newName = flattenedNames.get(domain + ":" + name);
                    if (newName != null) {
                        //Returned format is domain:name.
                        name = newName.split(":")[1];
                    }
                }
                qty = Integer.parseInt(itemParameters[2]);
            }
            //Meta is always 0 for flattened systems.
            meta = 0;
        } else {
            if (isOreDict) {
                //May not have this converted, but we might.  Try to convert just in case.
                //Need domain:name, no meta.
                String newName = unflattenedNames.get(domain + ":" + name);
                if (newName != null) {
                    //Returned format is domain:name.
                    name = newName.split(":")[1];
                }
                qty = Integer.parseInt(itemParameters[2]);
                meta = 0;
            } else if (itemParameters.length != 4) {
                //Flattened item, un-convert.  Need domain:name.
                String newName = unflattenedNames.get(domain + ":" + name);
                if (newName != null) {
                    String[] newNameComponents = unflattenedNames.get(name).split(":");
                    //Returned format is domain:name:meta.
                    name = newNameComponents[1];
                    meta = Integer.parseInt(newNameComponents[2]);
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
        //Standard Vanilla
        map.put("minecraft:air:0", "minecraft:air");
        map.put("minecraft:stone:0", "minecraft:stone");
        map.put("minecraft:stone:1", "minecraft:granite");
        map.put("minecraft:stone:2", "minecraft:polished_granite");
        map.put("minecraft:stone:3", "minecraft:diorite");
        map.put("minecraft:stone:4", "minecraft:polished_diorite");
        map.put("minecraft:stone:5", "minecraft:andesite");
        map.put("minecraft:stone:6", "minecraft:polished_andesite");
        map.put("minecraft:grass:0", "minecraft:grass_block");
        map.put("minecraft:dirt:0", "minecraft:dirt");
        map.put("minecraft:dirt:1", "minecraft:coarse_dirt");
        map.put("minecraft:dirt:2", "minecraft:podzol");
        map.put("minecraft:planks:0", "minecraft:oak_planks");
        map.put("minecraft:planks:1", "minecraft:spruce_planks");
        map.put("minecraft:planks:2", "minecraft:birch_planks");
        map.put("minecraft:planks:3", "minecraft:jungle_planks");
        map.put("minecraft:planks:4", "minecraft:acacia_planks");
        map.put("minecraft:planks:5", "minecraft:dark_oak_planks");
        map.put("minecraft:sapling:0", "minecraft:oak_sapling");
        map.put("minecraft:sapling:1", "minecraft:spruce_sapling");
        map.put("minecraft:sapling:2", "minecraft:birch_sapling");
        map.put("minecraft:sapling:3", "minecraft:jungle_sapling");
        map.put("minecraft:sapling:4", "minecraft:acacia_sapling");
        map.put("minecraft:sapling:5", "minecraft:dark_oak_sapling");
        map.put("minecraft:sand:0", "minecraft:sand");
        map.put("minecraft:sand:1", "minecraft:red_sand");
        map.put("minecraft:log:0", "minecraft:oak_log");
        map.put("minecraft:log:1", "minecraft:spruce_log");
        map.put("minecraft:log:2", "minecraft:birch_log");
        map.put("minecraft:log:3", "minecraft:jungle_log");
        map.put("minecraft:log:4", "minecraft:oak_wood");
        map.put("minecraft:log:5", "minecraft:spruce_wood");
        map.put("minecraft:log:6", "minecraft:birch_wood");
        map.put("minecraft:log:7", "minecraft:jungle_wood");
        map.put("minecraft:log2:0", "minecraft:acacia_log");
        map.put("minecraft:log2:1", "minecraft:dark_oak_log");
        map.put("minecraft:log2:2", "minecraft:acacia_wood");
        map.put("minecraft:log2:3", "minecraft:dark_oak_wood");
        map.put("minecraft:leaves:0", "minecraft:oak_leaves");
        map.put("minecraft:leaves:1", "minecraft:spruce_leaves");
        map.put("minecraft:leaves:2", "minecraft:birch_leaves");
        map.put("minecraft:leaves:3", "minecraft:jungle_leaves");
        map.put("minecraft:leaves2:0", "minecraft:acacia_leaves");
        map.put("minecraft:leaves2:1", "minecraft:dark_oak_leaves");
        map.put("minecraft:sponge:0", "minecraft:sponge");
        map.put("minecraft:sponge:1", "minecraft:wet_sponge");
        map.put("minecraft:sandstone:0", "minecraft:sandstone");
        map.put("minecraft:sandstone:1", "minecraft:chiseled_sandstone");
        map.put("minecraft:sandstone:2", "minecraft:cut_sandstone");
        map.put("minecraft:noteblock:0", "minecraft:note_block");
        map.put("minecraft:golden_rail:0", "minecraft:powered_rail");
        map.put("minecraft:web:0", "minecraft:cobweb");
        map.put("minecraft:tallgrass:0", "minecraft:dead_bush");
        map.put("minecraft:tallgrass:1", "minecraft:grass");
        map.put("minecraft:tallgrass:2", "minecraft:fern");
        map.put("minecraft:deadbush:0", "minecraft:dead_bush");
        map.put("minecraft:piston_extension:0", "minecraft:moving_piston");
        map.put("minecraft:wool:0", "minecraft:white_wool");
        map.put("minecraft:wool:1", "minecraft:orange_wool");
        map.put("minecraft:wool:2", "minecraft:magenta_wool");
        map.put("minecraft:wool:3", "minecraft:light_blue_wool");
        map.put("minecraft:wool:4", "minecraft:yellow_wool");
        map.put("minecraft:wool:5", "minecraft:lime_wool");
        map.put("minecraft:wool:6", "minecraft:pink_wool");
        map.put("minecraft:wool:7", "minecraft:gray_wool");
        map.put("minecraft:wool:8", "minecraft:light_gray_wool");
        map.put("minecraft:wool:9", "minecraft:cyan_wool");
        map.put("minecraft:wool:10", "minecraft:purple_wool");
        map.put("minecraft:wool:11", "minecraft:blue_wool");
        map.put("minecraft:wool:12", "minecraft:brown_wool");
        map.put("minecraft:wool:13", "minecraft:green_wool");
        map.put("minecraft:wool:14", "minecraft:red_wool");
        map.put("minecraft:wool:15", "minecraft:black_wool");
        map.put("minecraft:yellow_flower:0", "minecraft:dandelion");
        map.put("minecraft:red_flower:0", "minecraft:poppy");
        map.put("minecraft:red_flower:1", "minecraft:blue_orchid");
        map.put("minecraft:red_flower:2", "minecraft:allium");
        map.put("minecraft:red_flower:3", "minecraft:azure_bluet");
        map.put("minecraft:red_flower:4", "minecraft:red_tulip");
        map.put("minecraft:red_flower:5", "minecraft:orange_tulip");
        map.put("minecraft:red_flower:6", "minecraft:white_tulip");
        map.put("minecraft:red_flower:7", "minecraft:pink_tulip");
        map.put("minecraft:red_flower:8", "minecraft:oxeye_daisy");
        map.put("minecraft:double_wooden_slab:0", "minecraft:oak_slab");
        map.put("minecraft:double_wooden_slab:1", "minecraft:spruce_slab");
        map.put("minecraft:double_wooden_slab:2", "minecraft:birch_slab");
        map.put("minecraft:wooden_slab:0", "minecraft:jungle_slab");
        map.put("minecraft:wooden_slab:1", "minecraft:acacia_slab");
        map.put("minecraft:wooden_slab:2", "minecraft:dark_oak_slab");
        map.put("minecraft:double_stone_slab:0", "minecraft:stone_slab");
        map.put("minecraft:double_stone_slab:1", "minecraft:sandstone_slab");
        map.put("minecraft:double_stone_slab:2", "minecraft:petrified_oak_slab");
        map.put("minecraft:double_stone_slab:3", "minecraft:cobblestone_slab");
        map.put("minecraft:double_stone_slab:4", "minecraft:brick_slab");
        map.put("minecraft:stone_slab:0", "minecraft:stone_brick_slab");
        map.put("minecraft:stone_slab:1", "minecraft:nether_brick_slab");
        map.put("minecraft:stone_slab:2", "minecraft:quartz_slab");
        map.put("minecraft:stone_slab:3", "minecraft:smooth_stone");
        map.put("minecraft:stone_slab:4", "minecraft:smooth_sandstone");
        map.put("minecraft:stone_slab:5", "minecraft:smooth_quartz");
        map.put("minecraft:double_stone_slab2:0", "minecraft:red_sandstone_slab");
        map.put("minecraft:stone_slab2:0", "minecraft:smooth_red_sandstone");
        map.put("minecraft:double_purpur_slab:0", "minecraft:purpur_slab");
        map.put("minecraft:purpur_slab:0", "minecraft:purpur_slab");
        map.put("minecraft:brick_block:0", "minecraft:bricks");
        map.put("minecraft:mob_spawner:0", "minecraft:spawner");
        map.put("minecraft:portal:0", "minecraft:nether_portal");
        map.put("minecraft:torch:0", "minecraft:wall_torch");
        map.put("minecraft:torch:1", "minecraft:torch");
        map.put("minecraft:furnace:0", "minecraft:furnace");
        map.put("minecraft:lit_furnace:0", "minecraft:furnace");
        map.put("minecraft:stone_stairs:0", "minecraft:cobblestone_stairs");
        map.put("minecraft:wooden_pressure_plate:0", "minecraft:oak_pressure_plate");
        map.put("minecraft:redstone_ore:0", "minecraft:redstone_ore");
        map.put("minecraft:lit_redstone_ore:0", "minecraft:redstone_ore");
        map.put("minecraft:unlit_redstone_torch:0", "minecraft:redstone_wall_torch");
        map.put("minecraft:redstone_torch:0", "minecraft:redstone_torch");
        map.put("minecraft:snow_layer:0", "minecraft:snow");
        map.put("minecraft:snow:0", "minecraft:snow_block");
        map.put("minecraft:fence:0", "minecraft:oak_fence");
        map.put("minecraft:pumpkin:0", "minecraft:carved_pumpkin");
        map.put("minecraft:lit_pumpkin:0", "minecraft:jack_o_lantern");
        map.put("minecraft:trapdoor:0", "minecraft:oak_trapdoor");
        map.put("minecraft:monster_egg:0", "minecraft:infested_stone");
        map.put("minecraft:monster_egg:1", "minecraft:infested_cobblestone");
        map.put("minecraft:monster_egg:2", "minecraft:infested_stone_bricks");
        map.put("minecraft:monster_egg:3", "minecraft:infested_mossy_stone_bricks");
        map.put("minecraft:monster_egg:4", "minecraft:infested_cracked_stone_bricks");
        map.put("minecraft:monster_egg:5", "minecraft:infested_chiseled_stone_bricks");
        map.put("minecraft:stonebrick:0", "minecraft:stone_bricks");
        map.put("minecraft:stonebrick:1", "minecraft:mossy_stone_bricks");
        map.put("minecraft:stonebrick:2", "minecraft:cracked_stone_bricks");
        map.put("minecraft:stonebrick:3", "minecraft:chiseled_stone_bricks");
        map.put("minecraft:brown_mushroom_block:0", "minecraft:brown_mushroom_block");
        map.put("minecraft:brown_mushroom_block:1", "minecraft:mushroom_stem");
        map.put("minecraft:red_mushroom_block:0", "minecraft:mushroom_stem");
        map.put("minecraft:red_mushroom_block:1", "minecraft:red_mushroom_block");
        map.put("minecraft:melon_block:0", "minecraft:melon");
        map.put("minecraft:fence_gate:0", "minecraft:oak_fence_gate");
        map.put("minecraft:waterlily:0", "minecraft:lily_pad");
        map.put("minecraft:nether_brick:0", "minecraft:nether_bricks");
        map.put("minecraft:end_bricks:0", "minecraft:end_stone_bricks");
        map.put("minecraft:redstone_lamp:0", "minecraft:redstone_lamp");
        map.put("minecraft:lit_redstone_lamp:0", "minecraft:redstone_lamp");
        map.put("minecraft:cobblestone_wall:0", "minecraft:cobblestone_wall");
        map.put("minecraft:cobblestone_wall:1", "minecraft:mossy_cobblestone_wall");
        map.put("minecraft:wooden_button:0", "minecraft:oak_button");
        map.put("minecraft:anvil:0", "minecraft:anvil");
        map.put("minecraft:anvil:1", "minecraft:chipped_anvil");
        map.put("minecraft:anvil:2", "minecraft:damaged_anvil");
        map.put("minecraft:daylight_detector:0", "minecraft:daylight_detector");
        map.put("minecraft:daylight_detector_inverted:0", "minecraft:daylight_detector");
        map.put("minecraft:quartz_ore:0", "minecraft:nether_quartz_ore");
        map.put("minecraft:quartz_block:0", "minecraft:quartz_block");
        map.put("minecraft:quartz_block:1", "minecraft:chiseled_quartz_block");
        map.put("minecraft:quartz_block:2", "minecraft:quartz_pillar");
        map.put("minecraft:stained_hardened_clay:0", "minecraft:white_terracotta");
        map.put("minecraft:stained_hardened_clay:1", "minecraft:orange_terracotta");
        map.put("minecraft:stained_hardened_clay:2", "minecraft:magenta_terracotta");
        map.put("minecraft:stained_hardened_clay:3", "minecraft:light_blue_terracotta");
        map.put("minecraft:stained_hardened_clay:4", "minecraft:yellow_terracotta");
        map.put("minecraft:stained_hardened_clay:5", "minecraft:lime_terracotta");
        map.put("minecraft:stained_hardened_clay:6", "minecraft:pink_terracotta");
        map.put("minecraft:stained_hardened_clay:7", "minecraft:gray_terracotta");
        map.put("minecraft:stained_hardened_clay:8", "minecraft:light_gray_terracotta");
        map.put("minecraft:stained_hardened_clay:9", "minecraft:cyan_terracotta");
        map.put("minecraft:stained_hardened_clay:10", "minecraft:purple_terracotta");
        map.put("minecraft:stained_hardened_clay:11", "minecraft:blue_terracotta");
        map.put("minecraft:stained_hardened_clay:12", "minecraft:brown_terracotta");
        map.put("minecraft:stained_hardened_clay:13", "minecraft:green_terracotta");
        map.put("minecraft:stained_hardened_clay:14", "minecraft:red_terracotta");
        map.put("minecraft:stained_hardened_clay:15", "minecraft:black_terracotta");
        map.put("minecraft:carpet:0", "minecraft:white_carpet");
        map.put("minecraft:carpet:1", "minecraft:orange_carpet");
        map.put("minecraft:carpet:2", "minecraft:magenta_carpet");
        map.put("minecraft:carpet:3", "minecraft:light_blue_carpet");
        map.put("minecraft:carpet:4", "minecraft:yellow_carpet");
        map.put("minecraft:carpet:5", "minecraft:lime_carpet");
        map.put("minecraft:carpet:6", "minecraft:pink_carpet");
        map.put("minecraft:carpet:7", "minecraft:gray_carpet");
        map.put("minecraft:carpet:8", "minecraft:light_gray_carpet");
        map.put("minecraft:carpet:9", "minecraft:cyan_carpet");
        map.put("minecraft:carpet:10", "minecraft:purple_carpet");
        map.put("minecraft:carpet:11", "minecraft:blue_carpet");
        map.put("minecraft:carpet:12", "minecraft:brown_carpet");
        map.put("minecraft:carpet:13", "minecraft:green_carpet");
        map.put("minecraft:carpet:14", "minecraft:red_carpet");
        map.put("minecraft:carpet:15", "minecraft:black_carpet");
        map.put("minecraft:hardened_clay:0", "minecraft:terracotta");
        map.put("minecraft:slime:0", "minecraft:slime_block");
        map.put("minecraft:double_plant:0", "minecraft:sunflower");
        map.put("minecraft:double_plant:1", "minecraft:lilac");
        map.put("minecraft:double_plant:2", "minecraft:tall_grass");
        map.put("minecraft:double_plant:3", "minecraft:large_fern");
        map.put("minecraft:double_plant:4", "minecraft:rose_bush");
        map.put("minecraft:double_plant:5", "minecraft:peony");
        map.put("minecraft:stained_glass:0", "minecraft:white_stained_glass");
        map.put("minecraft:stained_glass:1", "minecraft:orange_stained_glass");
        map.put("minecraft:stained_glass:2", "minecraft:magenta_stained_glass");
        map.put("minecraft:stained_glass:3", "minecraft:light_blue_stained_glass");
        map.put("minecraft:stained_glass:4", "minecraft:yellow_stained_glass");
        map.put("minecraft:stained_glass:5", "minecraft:lime_stained_glass");
        map.put("minecraft:stained_glass:6", "minecraft:pink_stained_glass");
        map.put("minecraft:stained_glass:7", "minecraft:gray_stained_glass");
        map.put("minecraft:stained_glass:8", "minecraft:light_gray_stained_glass");
        map.put("minecraft:stained_glass:9", "minecraft:cyan_stained_glass");
        map.put("minecraft:stained_glass:10", "minecraft:purple_stained_glass");
        map.put("minecraft:stained_glass:11", "minecraft:blue_stained_glass");
        map.put("minecraft:stained_glass:12", "minecraft:brown_stained_glass");
        map.put("minecraft:stained_glass:13", "minecraft:green_stained_glass");
        map.put("minecraft:stained_glass:14", "minecraft:red_stained_glass");
        map.put("minecraft:stained_glass:15", "minecraft:black_stained_glass");
        map.put("minecraft:stained_glass_pane:0", "minecraft:white_stained_glass_pane");
        map.put("minecraft:stained_glass_pane:1", "minecraft:orange_stained_glass_pane");
        map.put("minecraft:stained_glass_pane:2", "minecraft:magenta_stained_glass_pane");
        map.put("minecraft:stained_glass_pane:3", "minecraft:light_blue_stained_glass_pane");
        map.put("minecraft:stained_glass_pane:4", "minecraft:yellow_stained_glass_pane");
        map.put("minecraft:stained_glass_pane:5", "minecraft:lime_stained_glass_pane");
        map.put("minecraft:stained_glass_pane:6", "minecraft:pink_stained_glass_pane");
        map.put("minecraft:stained_glass_pane:7", "minecraft:gray_stained_glass_pane");
        map.put("minecraft:stained_glass_pane:8", "minecraft:light_gray_stained_glass_pane");
        map.put("minecraft:stained_glass_pane:9", "minecraft:cyan_stained_glass_pane");
        map.put("minecraft:stained_glass_pane:10", "minecraft:purple_stained_glass_pane");
        map.put("minecraft:stained_glass_pane:11", "minecraft:blue_stained_glass_pane");
        map.put("minecraft:stained_glass_pane:12", "minecraft:brown_stained_glass_pane");
        map.put("minecraft:stained_glass_pane:13", "minecraft:green_stained_glass_pane");
        map.put("minecraft:stained_glass_pane:14", "minecraft:red_stained_glass_pane");
        map.put("minecraft:stained_glass_pane:15", "minecraft:black_stained_glass_pane");
        map.put("minecraft:prismarine:0", "minecraft:prismarine");
        map.put("minecraft:prismarine:1", "minecraft:prismarine_bricks");
        map.put("minecraft:prismarine:2", "minecraft:dark_prismarine");
        map.put("minecraft:red_sandstone:0", "minecraft:red_sandstone");
        map.put("minecraft:red_sandstone:1", "minecraft:chiseled_red_sandstone");
        map.put("minecraft:red_sandstone:2", "minecraft:cut_red_sandstone");
        map.put("minecraft:magma:0", "minecraft:magma_block");
        map.put("minecraft:red_nether_brick:0", "minecraft:red_nether_bricks");
        map.put("minecraft:silver_shulker_box:0", "minecraft:light_gray_shulker_box");
        map.put("minecraft:silver_glazed_terracotta:0", "minecraft:light_gray_glazed_terracotta");
        map.put("minecraft:concrete:0", "minecraft:white_concrete");
        map.put("minecraft:concrete:1", "minecraft:orange_concrete");
        map.put("minecraft:concrete:2", "minecraft:magenta_concrete");
        map.put("minecraft:concrete:3", "minecraft:light_blue_concrete");
        map.put("minecraft:concrete:4", "minecraft:yellow_concrete");
        map.put("minecraft:concrete:5", "minecraft:lime_concrete");
        map.put("minecraft:concrete:6", "minecraft:pink_concrete");
        map.put("minecraft:concrete:7", "minecraft:gray_concrete");
        map.put("minecraft:concrete:8", "minecraft:light_gray_concrete");
        map.put("minecraft:concrete:9", "minecraft:cyan_concrete");
        map.put("minecraft:concrete:10", "minecraft:purple_concrete");
        map.put("minecraft:concrete:11", "minecraft:blue_concrete");
        map.put("minecraft:concrete:12", "minecraft:brown_concrete");
        map.put("minecraft:concrete:13", "minecraft:green_concrete");
        map.put("minecraft:concrete:14", "minecraft:red_concrete");
        map.put("minecraft:concrete:15", "minecraft:black_concrete");
        map.put("minecraft:concrete_powder:0", "minecraft:white_concrete_powder");
        map.put("minecraft:concrete_powder:1", "minecraft:orange_concrete_powder");
        map.put("minecraft:concrete_powder:2", "minecraft:magenta_concrete_powder");
        map.put("minecraft:concrete_powder:3", "minecraft:light_blue_concrete_powder");
        map.put("minecraft:concrete_powder:4", "minecraft:yellow_concrete_powder");
        map.put("minecraft:concrete_powder:5", "minecraft:lime_concrete_powder");
        map.put("minecraft:concrete_powder:6", "minecraft:pink_concrete_powder");
        map.put("minecraft:concrete_powder:7", "minecraft:gray_concrete_powder");
        map.put("minecraft:concrete_powder:8", "minecraft:light_gray_concrete_powder");
        map.put("minecraft:concrete_powder:9", "minecraft:cyan_concrete_powder");
        map.put("minecraft:concrete_powder:10", "minecraft:purple_concrete_powder");
        map.put("minecraft:concrete_powder:11", "minecraft:blue_concrete_powder");
        map.put("minecraft:concrete_powder:12", "minecraft:brown_concrete_powder");
        map.put("minecraft:concrete_powder:13", "minecraft:green_concrete_powder");
        map.put("minecraft:concrete_powder:14", "minecraft:red_concrete_powder");
        map.put("minecraft:concrete_powder:15", "minecraft:black_concrete_powder");
        map.put("minecraft:wooden_door:0", "minecraft:oak_door");
        map.put("minecraft:unpowered_repeater:0", "minecraft:repeater");
        map.put("minecraft:powered_repeater:0", "minecraft:repeater");
        map.put("minecraft:repeater:0", "minecraft:repeater");
        map.put("minecraft:unpowered_comparator:0", "minecraft:comparator");
        map.put("minecraft:powered_comparator:0", "minecraft:comparator");
        map.put("minecraft:comparator:0", "minecraft:comparator");
        map.put("minecraft:coal:0", "minecraft:coal");
        map.put("minecraft:coal:1", "minecraft:charcoal");
        map.put("minecraft:golden_apple:0", "minecraft:golden_apple");
        map.put("minecraft:golden_apple:1", "minecraft:enchanted_golden_apple");
        map.put("minecraft:standing_sign:0", "minecraft:sign");
        map.put("minecraft:sign:0", "minecraft:sign");
        map.put("minecraft:water:0", "minecraft:water");
        map.put("minecraft:flowing_water:0", "minecraft:water");
        map.put("minecraft:lava:0", "minecraft:lava");
        map.put("minecraft:flowing_lava:0", "minecraft:lava");
        map.put("minecraft:boat:0", "minecraft:oak_boat");
        map.put("minecraft:reeds:0", "minecraft:sugar_cane");
        map.put("minecraft:fish:0", "minecraft:cod");
        map.put("minecraft:fish:1", "minecraft:salmon");
        map.put("minecraft:fish:2", "minecraft:tropical_fish");
        map.put("minecraft:fish:3", "minecraft:pufferfish");
        map.put("minecraft:cooked_fish:0", "minecraft:cooked_cod");
        map.put("minecraft:cooked_fish:1", "minecraft:cooked_salmon");
        map.put("minecraft:dye:0", "minecraft:bone_meal");
        map.put("minecraft:dye:1", "minecraft:orange_dye");
        map.put("minecraft:dye:2", "minecraft:magenta_dye");
        map.put("minecraft:dye:3", "minecraft:light_blue_dye");
        map.put("minecraft:dye:4", "minecraft:dandelion_yellow");
        map.put("minecraft:dye:5", "minecraft:lime_dye");
        map.put("minecraft:dye:6", "minecraft:pink_dye");
        map.put("minecraft:dye:7", "minecraft:gray_dye");
        map.put("minecraft:dye:8", "minecraft:light_gray_dye");
        map.put("minecraft:dye:9", "minecraft:cyan_dye");
        map.put("minecraft:dye:10", "minecraft:purple_dye");
        map.put("minecraft:dye:11", "minecraft:lapis_lazuli");
        map.put("minecraft:dye:12", "minecraft:cocoa_beans");
        map.put("minecraft:dye:13", "minecraft:cactus_green");
        map.put("minecraft:dye:14", "minecraft:rose_red");
        map.put("minecraft:dye:15", "minecraft:ink_sac");
        map.put("minecraft:bed:0", "minecraft:white_bed");
        map.put("minecraft:bed:1", "minecraft:orange_bed");
        map.put("minecraft:bed:2", "minecraft:magenta_bed");
        map.put("minecraft:bed:3", "minecraft:light_blue_bed");
        map.put("minecraft:bed:4", "minecraft:yellow_bed");
        map.put("minecraft:bed:5", "minecraft:lime_bed");
        map.put("minecraft:bed:6", "minecraft:pink_bed");
        map.put("minecraft:bed:7", "minecraft:gray_bed");
        map.put("minecraft:bed:8", "minecraft:light_gray_bed");
        map.put("minecraft:bed:9", "minecraft:cyan_bed");
        map.put("minecraft:bed:10", "minecraft:purple_bed");
        map.put("minecraft:bed:11", "minecraft:blue_bed");
        map.put("minecraft:bed:12", "minecraft:brown_bed");
        map.put("minecraft:bed:13", "minecraft:green_bed");
        map.put("minecraft:bed:14", "minecraft:red_bed");
        map.put("minecraft:bed:15", "minecraft:black_bed");
        map.put("minecraft:melon:0", "minecraft:melon_slice");
        map.put("minecraft:pumpkin_stem:0", "minecraft:pumpkin_stem");
        map.put("minecraft:pumpkin_stem:1", "minecraft:attached_pumpkin_stem");
        map.put("minecraft:melon_stem:0", "minecraft:melon_stem");
        map.put("minecraft:melon_stem:1", "minecraft:attached_melon_stem");
        map.put("minecraft:speckled_melon:0", "minecraft:glistering_melon_slice");
        map.put("minecraft:spawn_egg:0", "minecraft:bat_spawn_egg");
        map.put("minecraft:spawn_egg:1", "minecraft:blaze_spawn_egg");
        map.put("minecraft:spawn_egg:2", "minecraft:cave_spider_spawn_egg");
        map.put("minecraft:spawn_egg:3", "minecraft:chicken_spawn_egg");
        map.put("minecraft:spawn_egg:4", "minecraft:cow_spawn_egg");
        map.put("minecraft:spawn_egg:5", "minecraft:creeper_spawn_egg");
        map.put("minecraft:spawn_egg:6", "minecraft:donkey_spawn_egg");
        map.put("minecraft:spawn_egg:7", "minecraft:elder_guardian_spawn_egg");
        map.put("minecraft:spawn_egg:8", "minecraft:enderman_spawn_egg");
        map.put("minecraft:spawn_egg:9", "minecraft:endermite_spawn_egg");
        map.put("minecraft:spawn_egg:10", "minecraft:evoker_spawn_egg");
        map.put("minecraft:spawn_egg:11", "minecraft:ghast_spawn_egg");
        map.put("minecraft:spawn_egg:12", "minecraft:guardian_spawn_egg");
        map.put("minecraft:spawn_egg:13", "minecraft:horse_spawn_egg");
        map.put("minecraft:spawn_egg:14", "minecraft:husk_spawn_egg");
        map.put("minecraft:spawn_egg:15", "minecraft:llama_spawn_egg");
        map.put("minecraft:spawn_egg:16", "minecraft:magma_cube_spawn_egg");
        map.put("minecraft:spawn_egg:17", "minecraft:mooshroom_spawn_egg");
        map.put("minecraft:spawn_egg:18", "minecraft:mule_spawn_egg");
        map.put("minecraft:spawn_egg:19", "minecraft:ocelot_spawn_egg");
        map.put("minecraft:spawn_egg:20", "minecraft:parrot_spawn_egg");
        map.put("minecraft:spawn_egg:21", "minecraft:pig_spawn_egg");
        map.put("minecraft:spawn_egg:22", "minecraft:polar_bear_spawn_egg");
        map.put("minecraft:spawn_egg:23", "minecraft:rabbit_spawn_egg");
        map.put("minecraft:spawn_egg:24", "minecraft:sheep_spawn_egg");
        map.put("minecraft:spawn_egg:25", "minecraft:shulker_spawn_egg");
        map.put("minecraft:spawn_egg:26", "minecraft:silverfish_spawn_egg");
        map.put("minecraft:spawn_egg:27", "minecraft:skeleton_spawn_egg");
        map.put("minecraft:spawn_egg:28", "minecraft:skeleton_horse_spawn_egg");
        map.put("minecraft:spawn_egg:29", "minecraft:slime_spawn_egg");
        map.put("minecraft:spawn_egg:30", "minecraft:spider_spawn_egg");
        map.put("minecraft:spawn_egg:31", "minecraft:squid_spawn_egg");
        map.put("minecraft:spawn_egg:32", "minecraft:stray_spawn_egg");
        map.put("minecraft:spawn_egg:33", "minecraft:vex_spawn_egg");
        map.put("minecraft:spawn_egg:34", "minecraft:villager_spawn_egg");
        map.put("minecraft:spawn_egg:35", "minecraft:vindicator_spawn_egg");
        map.put("minecraft:spawn_egg:36", "minecraft:witch_spawn_egg");
        map.put("minecraft:spawn_egg:37", "minecraft:wither_skeleton_spawn_egg");
        map.put("minecraft:spawn_egg:38", "minecraft:wolf_spawn_egg");
        map.put("minecraft:spawn_egg:39", "minecraft:zombie_spawn_egg");
        map.put("minecraft:spawn_egg:40", "minecraft:zombie_horse_spawn_egg");
        map.put("minecraft:spawn_egg:41", "minecraft:zombie_pigman_spawn_egg");
        map.put("minecraft:spawn_egg:42", "minecraft:zombie_villager_spawn_egg");
        map.put("minecraft:flower_pot:0", "minecraft:flower_pot");
        map.put("minecraft:flower_pot:1", "minecraft:potted_poppy");
        map.put("minecraft:flower_pot:2", "minecraft:potted_dandelion");
        map.put("minecraft:flower_pot:3", "minecraft:potted_oak_sapling");
        map.put("minecraft:flower_pot:4", "minecraft:potted_spruce_sapling");
        map.put("minecraft:flower_pot:5", "minecraft:potted_birch_sapling");
        map.put("minecraft:flower_pot:6", "minecraft:potted_jungle_sapling");
        map.put("minecraft:flower_pot:7", "minecraft:potted_red_mushroom");
        map.put("minecraft:flower_pot:8", "minecraft:potted_brown_mushroom");
        map.put("minecraft:flower_pot:9", "minecraft:potted_cactus");
        map.put("minecraft:flower_pot:10", "minecraft:potted_dead_bush");
        map.put("minecraft:flower_pot:11", "minecraft:potted_fern");
        map.put("minecraft:flower_pot:12", "minecraft:potted_acacia_sapling");
        map.put("minecraft:flower_pot:13", "minecraft:potted_dark_oak_sapling");
        map.put("minecraft:flower_pot:14", "minecraft:potted_blue_orchid");
        map.put("minecraft:flower_pot:15", "minecraft:potted_allium");
        map.put("minecraft:flower_pot:16", "minecraft:potted_azure_bluet");
        map.put("minecraft:flower_pot:17", "minecraft:potted_red_tulip");
        map.put("minecraft:flower_pot:18", "minecraft:potted_orange_tulip");
        map.put("minecraft:flower_pot:19", "minecraft:potted_white_tulip");
        map.put("minecraft:flower_pot:20", "minecraft:potted_pink_tulip");
        map.put("minecraft:flower_pot:21", "minecraft:potted_oxeye_daisy");
        map.put("minecraft:skull:0", "minecraft:skeleton_skull");
        map.put("minecraft:skull:1", "minecraft:skeleton_wall_skull");
        map.put("minecraft:skull:2", "minecraft:wither_skeleton_skull");
        map.put("minecraft:skull:3", "minecraft:wither_skeleton_wall_skull");
        map.put("minecraft:skull:4", "minecraft:zombie_head");
        map.put("minecraft:skull:5", "minecraft:zombie_wall_head");
        map.put("minecraft:skull:6", "minecraft:player_head");
        map.put("minecraft:skull:7", "minecraft:player_wall_head");
        map.put("minecraft:skull:8", "minecraft:creeper_head");
        map.put("minecraft:skull:9", "minecraft:creeper_wall_head");
        map.put("minecraft:skull:10", "minecraft:dragon_head");
        map.put("minecraft:skull:11", "minecraft:dragon_wall_head");
        map.put("minecraft:fireworks:0", "minecraft:firework_rocket");
        map.put("minecraft:firework_charge:0", "minecraft:firework_star");
        map.put("minecraft:netherbrick:0", "minecraft:nether_brick");
        map.put("minecraft:standing_banner:0", "minecraft:white_banner");
        map.put("minecraft:standing_banner:1", "minecraft:orange_banner");
        map.put("minecraft:standing_banner:2", "minecraft:magenta_banner");
        map.put("minecraft:standing_banner:3", "minecraft:light_blue_banner");
        map.put("minecraft:standing_banner:4", "minecraft:yellow_banner");
        map.put("minecraft:standing_banner:5", "minecraft:lime_banner");
        map.put("minecraft:standing_banner:6", "minecraft:pink_banner");
        map.put("minecraft:standing_banner:7", "minecraft:gray_banner");
        map.put("minecraft:banner:0", "minecraft:light_gray_banner");
        map.put("minecraft:banner:1", "minecraft:cyan_banner");
        map.put("minecraft:banner:2", "minecraft:purple_banner");
        map.put("minecraft:banner:3", "minecraft:blue_banner");
        map.put("minecraft:banner:4", "minecraft:brown_banner");
        map.put("minecraft:banner:5", "minecraft:green_banner");
        map.put("minecraft:banner:6", "minecraft:red_banner");
        map.put("minecraft:banner:7", "minecraft:black_banner");
        map.put("minecraft:wall_banner:0", "minecraft:white_wall_banner");
        map.put("minecraft:wall_banner:1", "minecraft:orange_wall_banner");
        map.put("minecraft:wall_banner:2", "minecraft:magenta_wall_banner");
        map.put("minecraft:wall_banner:3", "minecraft:light_blue_wall_banner");
        map.put("minecraft:wall_banner:4", "minecraft:yellow_wall_banner");
        map.put("minecraft:wall_banner:5", "minecraft:lime_wall_banner");
        map.put("minecraft:wall_banner:6", "minecraft:pink_wall_banner");
        map.put("minecraft:wall_banner:7", "minecraft:gray_wall_banner");
        map.put("minecraft:wall_banner:8", "minecraft:light_gray_wall_banner");
        map.put("minecraft:wall_banner:9", "minecraft:cyan_wall_banner");
        map.put("minecraft:wall_banner:10", "minecraft:purple_wall_banner");
        map.put("minecraft:wall_banner:11", "minecraft:blue_wall_banner");
        map.put("minecraft:wall_banner:12", "minecraft:brown_wall_banner");
        map.put("minecraft:wall_banner:13", "minecraft:green_wall_banner");
        map.put("minecraft:wall_banner:14", "minecraft:red_wall_banner");
        map.put("minecraft:wall_banner:15", "minecraft:black_wall_banner");
        map.put("minecraft:chorus_fruit_popped:0", "minecraft:popped_chorus_fruit");
        map.put("minecraft:record_13:0", "minecraft:music_disc_13");
        map.put("minecraft:record_cat:0", "minecraft:music_disc_cat");
        map.put("minecraft:record_blocks:0", "minecraft:music_disc_blocks");
        map.put("minecraft:record_chirp:0", "minecraft:music_disc_chirp");
        map.put("minecraft:record_far:0", "minecraft:music_disc_far");
        map.put("minecraft:record_mall:0", "minecraft:music_disc_mall");
        map.put("minecraft:record_mellohi:0", "minecraft:music_disc_mellohi");
        map.put("minecraft:record_stal:0", "minecraft:music_disc_stal");
        map.put("minecraft:record_strad:0", "minecraft:music_disc_strad");
        map.put("minecraft:record_ward:0", "minecraft:music_disc_ward");
        map.put("minecraft:record_11:0", "minecraft:music_disc_11");
        map.put("minecraft:record_wait:0", "minecraft:music_disc_wait");

        //Immersive Engineering stuff.
        map.put("immersiveengineering:ore:0", "immersiveengineering:ore_copper");
        map.put("immersiveengineering:ore:1", "immersiveengineering:ore_aluminum");
        map.put("immersiveengineering:ore:2", "immersiveengineering:ore_lead");
        map.put("immersiveengineering:ore:3", "immersiveengineering:ore_silver");
        map.put("immersiveengineering:ore:4", "immersiveengineering:ore_nickel");
        map.put("immersiveengineering:ore:5", "immersiveengineering:ore_uranium");
        map.put("immersiveengineering:storage:0", "immersiveengineering:storage_copper");
        map.put("immersiveengineering:storage:1", "immersiveengineering:storage_aluminum");
        map.put("immersiveengineering:storage:2", "immersiveengineering:storage_lead");
        map.put("immersiveengineering:storage:3", "immersiveengineering:storage_silver");
        map.put("immersiveengineering:storage:4", "immersiveengineering:storage_nickel");
        map.put("immersiveengineering:storage:5", "immersiveengineering:storage_uranium");
        map.put("immersiveengineering:storage:6", "immersiveengineering:storage_constantan");
        map.put("immersiveengineering:storage:7", "immersiveengineering:storage_electrum");
        map.put("immersiveengineering:storage:8", "immersiveengineering:storage_steel");
        map.put("immersiveengineering:storage_slab:0", "immersiveengineering:slab_storage_copper");
        map.put("immersiveengineering:storage_slab:1", "immersiveengineering:slab_storage_aluminum");
        map.put("immersiveengineering:storage_slab:2", "immersiveengineering:slab_storage_lead");
        map.put("immersiveengineering:storage_slab:3", "immersiveengineering:slab_storage_silver");
        map.put("immersiveengineering:storage_slab:4", "immersiveengineering:slab_storage_nickel");
        map.put("immersiveengineering:storage_slab:5", "immersiveengineering:slab_storage_uranium");
        map.put("immersiveengineering:storage_slab:6", "immersiveengineering:slab_storage_constantan");
        map.put("immersiveengineering:storage_slab:7", "immersiveengineering:slab_storage_electrum");
        map.put("immersiveengineering:storage_slab:8", "immersiveengineering:slab_storage_steel");
        map.put("immersiveengineering:stone_decoration:0", "immersiveengineering:cokebrick");
        map.put("immersiveengineering:stone_decoration:1", "immersiveengineering:blastbrick");
        map.put("immersiveengineering:stone_decoration:2", "immersiveengineering:blastbrick_reinforced");
        map.put("immersiveengineering:stone_decoration:3", "immersiveengineering:coke");
        map.put("immersiveengineering:stone_decoration:4", "immersiveengineering:hempcrete");
        map.put("immersiveengineering:stone_decoration:5", "immersiveengineering:concrete");
        map.put("immersiveengineering:stone_decoration:6", "immersiveengineering:concrete_tile");
        map.put("immersiveengineering:stone_decoration:7", "immersiveengineering:concrete_leaded");
        map.put("immersiveengineering:stone_decoration:8", "immersiveengineering:insulating_glass");
        map.put("immersiveengineering:stone_decoration:9", "immersiveengineering:concrete_sprayed");
        map.put("immersiveengineering:stone_decoration:10", "immersiveengineering:alloybrick");
        map.put("immersiveengineering:stone_decoration_slab:0", "immersiveengineering:slab_cokebrick");
        map.put("immersiveengineering:stone_decoration_slab:1", "immersiveengineering:slab_blastbrick");
        map.put("immersiveengineering:stone_decoration_slab:2", "immersiveengineering:slab_blastbrick_reinforced");
        map.put("immersiveengineering:stone_decoration_slab:3", "immersiveengineering:slab_coke");
        map.put("immersiveengineering:stone_decoration_slab:4", "immersiveengineering:slab_hempcrete");
        map.put("immersiveengineering:stone_decoration_slab:5", "immersiveengineering:slab_concrete");
        map.put("immersiveengineering:stone_decoration_slab:6", "immersiveengineering:slab_concrete_tile");
        map.put("immersiveengineering:stone_decoration_slab:7", "immersiveengineering:slab_concrete_leaded");
        map.put("immersiveengineering:stone_decoration_slab:8", "immersiveengineering:slab_insulating_glass");
        map.put("immersiveengineering:stone_decoration_slab:9", "immersiveengineering:slab_alloybrick");
        map.put("immersiveengineering:stone_decoration_stairs_hempcrete:0", "immersiveengineering:stairs_hempcrete");
        map.put("immersiveengineering:stone_decoration_stairs_concrete:0", "immersiveengineering:stairs_concrete");
        map.put("immersiveengineering:stone_decoration_stairs_concrete_tile:0", "immersiveengineering:stairs_concrete_tile");
        map.put("immersiveengineering:stone_decoration_stairs_concrete_leaded:0", "immersiveengineering:stairs_concrete_leaded");
        map.put("immersiveengineering:stone_device:0", "immersiveengineering:coke_oven");
        map.put("immersiveengineering:stone_device:1", "immersiveengineering:blast_furnace");
        map.put("immersiveengineering:stone_device:2", "immersiveengineering:advanced_blast_furnace");
        map.put("immersiveengineering:stone_device:3", "immersiveengineering:concrete_sheet");
        map.put("immersiveengineering:stone_device:4", "immersiveengineering:concrete_quarter");
        map.put("immersiveengineering:stone_device:5", "immersiveengineering:concrete_three_quarter");
        map.put("immersiveengineering:stone_device:6", "immersiveengineering:coresample");
        map.put("immersiveengineering:stone_device:7", "immersiveengineering:alloy_smelter");
        map.put("immersiveengineering:treated_wood:0", "immersiveengineering:treated_wood_horizontal");
        map.put("immersiveengineering:treated_wood:1", "immersiveengineering:treated_wood_horizontal");
        map.put("immersiveengineering:treated_wood:2", "immersiveengineering:treated_wood_horizontal");
        map.put("immersiveengineering:treated_wood_slab:0", "immersiveengineering:slab_treated_wood_horizontal");
        map.put("immersiveengineering:treated_wood_slab:1", "immersiveengineering:slab_treated_wood_horizontal");
        map.put("immersiveengineering:treated_wood_slab:2", "immersiveengineering:slab_treated_wood_horizontal");
        map.put("immersiveengineering:treated_wood_stairs0:0", "immersiveengineering:stairs_treated_wood_horizontal");
        map.put("immersiveengineering:treated_wood_stairs1:0", "immersiveengineering:stairs_treated_wood_horizontal");
        map.put("immersiveengineering:treated_wood_stairs2:0", "immersiveengineering:stairs_treated_wood_horizontal");
        map.put("immersiveengineering:wooden_decoration:0", "immersiveengineering:treated_fence");
        map.put("immersiveengineering:wooden_decoration:1", "immersiveengineering:treated_scaffold");
        map.put("immersiveengineering:wooden_device0:0", "immersiveengineering:wooden_barrel");
        map.put("immersiveengineering:wooden_device0:1", "immersiveengineering:crate");
        map.put("immersiveengineering:wooden_device0:2", "immersiveengineering:workbench");
        map.put("immersiveengineering:wooden_device0:3", "immersiveengineering:sorter");
        map.put("immersiveengineering:wooden_device0:4", "immersiveengineering:gunpowder_barrel");
        map.put("immersiveengineering:wooden_device0:5", "immersiveengineering:reinforced_crate");
        map.put("immersiveengineering:wooden_device0:6", "immersiveengineering:turntable");
        map.put("immersiveengineering:wooden_device0:7", "immersiveengineering:fluid_sorter");
        map.put("immersiveengineering:wooden_device1:0", "immersiveengineering:watermill");
        map.put("immersiveengineering:wooden_device1:1", "immersiveengineering:windmill");
        map.put("immersiveengineering:wooden_device1:2", "immersiveengineering:windmill_advanced");
        map.put("immersiveengineering:wooden_device1:3", "immersiveengineering:treated_post");
        map.put("immersiveengineering:wooden_device1:4", "immersiveengineering:treated_wallmount");
        map.put("immersiveengineering:sheetmetal:0", "immersiveengineering:sheetmetal_copper");
        map.put("immersiveengineering:sheetmetal:1", "immersiveengineering:sheetmetal_aluminum");
        map.put("immersiveengineering:sheetmetal:2", "immersiveengineering:sheetmetal_lead");
        map.put("immersiveengineering:sheetmetal:3", "immersiveengineering:sheetmetal_silver");
        map.put("immersiveengineering:sheetmetal:4", "immersiveengineering:sheetmetal_nickel");
        map.put("immersiveengineering:sheetmetal:5", "immersiveengineering:sheetmetal_uranium");
        map.put("immersiveengineering:sheetmetal:6", "immersiveengineering:sheetmetal_constantan");
        map.put("immersiveengineering:sheetmetal:7", "immersiveengineering:sheetmetal_electrum");
        map.put("immersiveengineering:sheetmetal:8", "immersiveengineering:sheetmetal_steel");
        map.put("immersiveengineering:sheetmetal:9", "immersiveengineering:sheetmetal_iron");
        map.put("immersiveengineering:sheetmetal:10", "immersiveengineering:sheetmetal_gold");
        map.put("immersiveengineering:sheetmetal_slab:0", "immersiveengineering:slab_sheetmetal_copper");
        map.put("immersiveengineering:sheetmetal_slab:1", "immersiveengineering:slab_sheetmetal_aluminum");
        map.put("immersiveengineering:sheetmetal_slab:2", "immersiveengineering:slab_sheetmetal_lead");
        map.put("immersiveengineering:sheetmetal_slab:3", "immersiveengineering:slab_sheetmetal_silver");
        map.put("immersiveengineering:sheetmetal_slab:4", "immersiveengineering:slab_sheetmetal_nickel");
        map.put("immersiveengineering:sheetmetal_slab:5", "immersiveengineering:slab_sheetmetal_uranium");
        map.put("immersiveengineering:sheetmetal_slab:6", "immersiveengineering:slab_sheetmetal_constantan");
        map.put("immersiveengineering:sheetmetal_slab:7", "immersiveengineering:slab_sheetmetal_electrum");
        map.put("immersiveengineering:sheetmetal_slab:8", "immersiveengineering:slab_sheetmetal_steel");
        map.put("immersiveengineering:sheetmetal_slab:9", "immersiveengineering:slab_sheetmetal_iron");
        map.put("immersiveengineering:sheetmetal_slab:10", "immersiveengineering:slab_sheetmetal_gold");
        map.put("immersiveengineering:metal_decoration0:0", "immersiveengineering:coil_lv");
        map.put("immersiveengineering:metal_decoration0:1", "immersiveengineering:coil_mv");
        map.put("immersiveengineering:metal_decoration0:2", "immersiveengineering:coil_hv");
        map.put("immersiveengineering:metal_decoration0:3", "immersiveengineering:rs_engineering");
        map.put("immersiveengineering:metal_decoration0:4", "immersiveengineering:light_engineering");
        map.put("immersiveengineering:metal_decoration0:5", "immersiveengineering:heavy_engineering");
        map.put("immersiveengineering:metal_decoration0:6", "immersiveengineering:generator");
        map.put("immersiveengineering:metal_decoration0:7", "immersiveengineering:radiator");
        map.put("immersiveengineering:metal_decoration1:0", "immersiveengineering:steel_fence");
        map.put("immersiveengineering:metal_decoration1:1", "immersiveengineering:steel_scaffolding_standard");
        map.put("immersiveengineering:metal_decoration1:2", "immersiveengineering:steel_scaffolding_standard");
        map.put("immersiveengineering:metal_decoration1:3", "immersiveengineering:steel_scaffolding_standard");
        map.put("immersiveengineering:metal_decoration1:4", "immersiveengineering:alu_fence");
        map.put("immersiveengineering:metal_decoration1:5", "immersiveengineering:alu_scaffolding_standard");
        map.put("immersiveengineering:metal_decoration1:6", "immersiveengineering:alu_scaffolding_standard");
        map.put("immersiveengineering:metal_decoration1:7", "immersiveengineering:alu_scaffolding_standard");
        map.put("immersiveengineering:metal_decoration2:0", "immersiveengineering:steel_post");
        map.put("immersiveengineering:metal_decoration2:1", "immersiveengineering:steel_wallmount");
        map.put("immersiveengineering:metal_decoration2:2", "immersiveengineering:alu_post");
        map.put("immersiveengineering:metal_decoration2:3", "immersiveengineering:alu_wallmount");
        map.put("immersiveengineering:metal_decoration2:4", "immersiveengineering:lantern");
        map.put("immersiveengineering:metal_decoration2:5", "immersiveengineering:razor_wire");
        map.put("immersiveengineering:metal_decoration2:6", "immersiveengineering:toolbox");
        map.put("immersiveengineering:metal_decoration2:7", "immersiveengineering:steel_slope");
        map.put("immersiveengineering:metal_decoration2:8", "immersiveengineering:alu_slope");
        map.put("immersiveengineering:metal_decoration1_slab:0", "immersiveengineering:slab_steel_scaffolding_standard");
        map.put("immersiveengineering:metal_decoration1_slab:1", "immersiveengineering:slab_steel_scaffolding_standard");
        map.put("immersiveengineering:metal_decoration1_slab:2", "immersiveengineering:slab_steel_scaffolding_standard");
        map.put("immersiveengineering:metal_decoration1_slab:3", "immersiveengineering:slab_alu_scaffolding_standard");
        map.put("immersiveengineering:metal_decoration1_slab:4", "immersiveengineering:slab_alu_scaffolding_standard");
        map.put("immersiveengineering:metal_decoration1_slab:5", "immersiveengineering:slab_alu_scaffolding_standard");
        map.put("immersiveengineering:steel_scaffolding_stairs0:0", "immersiveengineering:stairs_steel_scaffolding_standard");
        map.put("immersiveengineering:steel_scaffolding_stairs1:0", "immersiveengineering:stairs_steel_scaffolding_standard");
        map.put("immersiveengineering:steel_scaffolding_stairs2:0", "immersiveengineering:stairs_steel_scaffolding_standard");
        map.put("immersiveengineering:aluminum_scaffolding_stairs0:0", "immersiveengineering:stairs_alu_scaffolding_standard");
        map.put("immersiveengineering:aluminum_scaffolding_stairs1:0", "immersiveengineering:stairs_alu_scaffolding_standard");
        map.put("immersiveengineering:aluminum_scaffolding_stairs2:0", "immersiveengineering:stairs_alu_scaffolding_standard");
        map.put("immersiveengineering:metal_ladder:0", "immersiveengineering:metal_ladder_none");
        map.put("immersiveengineering:metal_ladder:1", "immersiveengineering:metal_ladder_steel");
        map.put("immersiveengineering:metal_ladder:2", "immersiveengineering:metal_ladder_steel");
        map.put("immersiveengineering:connector:0", "immersiveengineering:connector_lv");
        map.put("immersiveengineering:connector:1", "immersiveengineering:connector_lv_relay");
        map.put("immersiveengineering:connector:2", "immersiveengineering:connector_mv");
        map.put("immersiveengineering:connector:3", "immersiveengineering:connector_mv_relay");
        map.put("immersiveengineering:connector:4", "immersiveengineering:connector_hv");
        map.put("immersiveengineering:connector:5", "immersiveengineering:connector_hv_relay");
        map.put("immersiveengineering:connector:6", "immersiveengineering:connector_structural");
        map.put("immersiveengineering:connector:7", "immersiveengineering:transformer");
        map.put("immersiveengineering:connector:8", "immersiveengineering:transformer_hv");
        map.put("immersiveengineering:connector:9", "immersiveengineering:breaker_switch");
        map.put("immersiveengineering:connector:10", "immersiveengineering:redstone_breaker");
        map.put("immersiveengineering:connector:11", "immersiveengineering:current_transformer");
        map.put("immersiveengineering:connector:12", "immersiveengineering:connector_redstone");
        map.put("immersiveengineering:connector:13", "immersiveengineering:connector_probe");
        map.put("immersiveengineering:connector:14", "immersiveengineering:feedthrough");
        map.put("immersiveengineering:metal_device0:0", "immersiveengineering:capacitor_lv");
        map.put("immersiveengineering:metal_device0:1", "immersiveengineering:capacitor_mv");
        map.put("immersiveengineering:metal_device0:2", "immersiveengineering:capacitor_hv");
        map.put("immersiveengineering:metal_device0:3", "immersiveengineering:capacitor_creative");
        map.put("immersiveengineering:metal_device0:4", "immersiveengineering:metal_barrel");
        map.put("immersiveengineering:metal_device0:5", "immersiveengineering:fluid_pump");
        map.put("immersiveengineering:metal_device0:6", "immersiveengineering:fluid_placer");
        map.put("immersiveengineering:metal_device1:0", "immersiveengineering:blastfurnace_preheater");
        map.put("immersiveengineering:metal_device1:1", "immersiveengineering:furnace_heater");
        map.put("immersiveengineering:metal_device1:2", "immersiveengineering:dynamo");
        map.put("immersiveengineering:metal_device1:3", "immersiveengineering:thermoelectric_generator");
        map.put("immersiveengineering:metal_device1:4", "immersiveengineering:electric_lantern");
        map.put("immersiveengineering:metal_device1:5", "immersiveengineering:charging_station");
        map.put("immersiveengineering:metal_device1:6", "immersiveengineering:fluid_pipe");
        map.put("immersiveengineering:metal_device1:7", "immersiveengineering:sample_drill");
        map.put("immersiveengineering:metal_device1:8", "immersiveengineering:tesla_coil");
        map.put("immersiveengineering:metal_device1:9", "immersiveengineering:floodlight");
        map.put("immersiveengineering:metal_device1:10", "immersiveengineering:turret_chem");
        map.put("immersiveengineering:metal_device1:11", "immersiveengineering:turret_gun");
        map.put("immersiveengineering:metal_device1:12", "immersiveengineering:cloche");
        map.put("immersiveengineering:conveyor:0", "immersiveengineering:conveyor_basic");
        map.put("immersiveengineering:conveyor:1", "immersiveengineering:conveyor_basic");
        map.put("immersiveengineering:conveyor:2", "immersiveengineering:conveyor_dropper");
        map.put("immersiveengineering:conveyor:3", "immersiveengineering:conveyor_droppercovered");
        map.put("immersiveengineering:conveyor:4", "immersiveengineering:conveyor_vertical");
        map.put("immersiveengineering:conveyor:5", "immersiveengineering:conveyor_splitter");
        map.put("immersiveengineering:conveyor:6", "immersiveengineering:conveyor_covered");
        map.put("immersiveengineering:conveyor:7", "immersiveengineering:conveyor_verticalcovered");
        map.put("immersiveengineering:conveyor:8", "immersiveengineering:conveyor_extract");
        map.put("immersiveengineering:conveyor:9", "immersiveengineering:conveyor_extractcovered");
        map.put("immersiveengineering:conveyor:10", "immersiveengineering:chute_iron");
        map.put("immersiveengineering:conveyor:11", "immersiveengineering:chute_iron");
        map.put("immersiveengineering:conveyor:12", "immersiveengineering:chute_iron");
        map.put("immersiveengineering:conveyor:13", "immersiveengineering:chute_iron");
        map.put("immersiveengineering:metal_multiblock:0", "immersiveengineering:metal_press");
        map.put("immersiveengineering:metal_multiblock:1", "immersiveengineering:crusher");
        map.put("immersiveengineering:metal_multiblock:2", "immersiveengineering:tank");
        map.put("immersiveengineering:metal_multiblock:3", "immersiveengineering:silo");
        map.put("immersiveengineering:metal_multiblock:4", "immersiveengineering:assembler");
        map.put("immersiveengineering:metal_multiblock:5", "immersiveengineering:auto_workbench");
        map.put("immersiveengineering:metal_multiblock:6", "immersiveengineering:bottling_machine");
        map.put("immersiveengineering:metal_multiblock:7", "immersiveengineering:squeezer");
        map.put("immersiveengineering:metal_multiblock:8", "immersiveengineering:fermenter");
        map.put("immersiveengineering:metal_multiblock:9", "immersiveengineering:refinery");
        map.put("immersiveengineering:metal_multiblock:10", "immersiveengineering:diesel_generator");
        map.put("immersiveengineering:metal_multiblock:11", "immersiveengineering:excavator");
        map.put("immersiveengineering:metal_multiblock:12", "immersiveengineering:bucket_wheel");
        map.put("immersiveengineering:metal_multiblock:13", "immersiveengineering:arc_furnace");
        map.put("immersiveengineering:metal_multiblock:14", "immersiveengineering:lightning_rod");
        map.put("immersiveengineering:metal_multiblock:15", "immersiveengineering:mixer");
        map.put("immersiveengineering:metal_decoration:0", "immersiveengineering:structural_arm");
        map.put("immersiveengineering:hemp:0", "immersiveengineering:hemp");
        map.put("immersiveengineering:cloth_device:0", "immersiveengineering:cushion");
        map.put("immersiveengineering:cloth_device:1", "immersiveengineering:balloon");
        map.put("immersiveengineering:cloth_device:2", "immersiveengineering:strip_curtain");
        map.put("immersiveengineering:molten_uranium:0", "immersiveengineering:molten_uranium");
        map.put("immersiveengineering:molten_constantan:0", "immersiveengineering:molten_constantan");
        map.put("immersiveengineering:arb_solid:0", "immersiveengineering:arb_solid");
        map.put("immersiveengineering:harvesterTrolley:ieCrop:0", "immersiveengineering:harvesterTrolley:ieCrop");
        map.put("immersiveengineering:fluidCreosote:0", "immersiveengineering:fluidCreosote");
        map.put("immersiveengineering:fluidPlantoil:0", "immersiveengineering:fluidPlantoil");
        map.put("immersiveengineering:fluidEthanol:0", "immersiveengineering:fluidEthanol");
        map.put("immersiveengineering:fluidBiodiesel:0", "immersiveengineering:fluidBiodiesel");
        map.put("immersiveengineering:fluidConcrete:0", "immersiveengineering:fluidConcrete");
        map.put("immersiveengineering:metal:0", "immersiveengineering:ingot_copper");
        map.put("immersiveengineering:metal:1", "immersiveengineering:ingot_aluminum");
        map.put("immersiveengineering:metal:2", "immersiveengineering:ingot_lead");
        map.put("immersiveengineering:metal:3", "immersiveengineering:ingot_silver");
        map.put("immersiveengineering:metal:4", "immersiveengineering:ingot_nickel");
        map.put("immersiveengineering:metal:5", "immersiveengineering:ingot_uranium");
        map.put("immersiveengineering:metal:6", "immersiveengineering:ingot_constantan");
        map.put("immersiveengineering:metal:7", "immersiveengineering:ingot_electrum");
        map.put("immersiveengineering:metal:8", "immersiveengineering:ingot_steel");
        map.put("immersiveengineering:metal:9", "immersiveengineering:dust_copper");
        map.put("immersiveengineering:metal:10", "immersiveengineering:dust_aluminum");
        map.put("immersiveengineering:metal:11", "immersiveengineering:dust_lead");
        map.put("immersiveengineering:metal:12", "immersiveengineering:dust_silver");
        map.put("immersiveengineering:metal:13", "immersiveengineering:dust_nickel");
        map.put("immersiveengineering:metal:14", "immersiveengineering:dust_uranium");
        map.put("immersiveengineering:metal:15", "immersiveengineering:dust_constantan");
        map.put("immersiveengineering:metal:16", "immersiveengineering:dust_electrum");
        map.put("immersiveengineering:metal:17", "immersiveengineering:dust_steel");
        map.put("immersiveengineering:metal:18", "immersiveengineering:dust_iron");
        map.put("immersiveengineering:metal:19", "immersiveengineering:dust_gold");
        map.put("immersiveengineering:metal:20", "immersiveengineering:nugget_copper");
        map.put("immersiveengineering:metal:21", "immersiveengineering:nugget_aluminum");
        map.put("immersiveengineering:metal:22", "immersiveengineering:nugget_lead");
        map.put("immersiveengineering:metal:23", "immersiveengineering:nugget_silver");
        map.put("immersiveengineering:metal:24", "immersiveengineering:nugget_nickel");
        map.put("immersiveengineering:metal:25", "immersiveengineering:nugget_uranium");
        map.put("immersiveengineering:metal:26", "immersiveengineering:nugget_constantan");
        map.put("immersiveengineering:metal:27", "immersiveengineering:nugget_electrum");
        map.put("immersiveengineering:metal:28", "immersiveengineering:nugget_steel");
        map.put("immersiveengineering:metal:29", "immersiveengineering:nugget_iron");
        map.put("immersiveengineering:metal:30", "immersiveengineering:plate_copper");
        map.put("immersiveengineering:metal:31", "immersiveengineering:plate_aluminum");
        map.put("immersiveengineering:metal:32", "immersiveengineering:plate_lead");
        map.put("immersiveengineering:metal:33", "immersiveengineering:plate_silver");
        map.put("immersiveengineering:metal:34", "immersiveengineering:plate_nickel");
        map.put("immersiveengineering:metal:35", "immersiveengineering:plate_uranium");
        map.put("immersiveengineering:metal:36", "immersiveengineering:plate_constantan");
        map.put("immersiveengineering:metal:37", "immersiveengineering:plate_electrum");
        map.put("immersiveengineering:metal:38", "immersiveengineering:plate_steel");
        map.put("immersiveengineering:metal:39", "immersiveengineering:plate_iron");
        map.put("immersiveengineering:metal:40", "immersiveengineering:plate_gold");
        map.put("immersiveengineering:material:0", "immersiveengineering:stick_treated");
        map.put("immersiveengineering:material:1", "immersiveengineering:stick_iron");
        map.put("immersiveengineering:material:2", "immersiveengineering:stick_steel");
        map.put("immersiveengineering:material:3", "immersiveengineering:stick_aluminum");
        map.put("immersiveengineering:material:4", "immersiveengineering:hemp_fiber");
        map.put("immersiveengineering:material:5", "immersiveengineering:hemp_fabric");
        map.put("immersiveengineering:material:6", "immersiveengineering:coal_coke");
        map.put("immersiveengineering:material:7", "immersiveengineering:slag");
        map.put("immersiveengineering:material:8", "immersiveengineering:component_iron");
        map.put("immersiveengineering:material:9", "immersiveengineering:component_steel");
        map.put("immersiveengineering:material:10", "immersiveengineering:waterwheel_segment");
        map.put("immersiveengineering:material:11", "immersiveengineering:windmill_blade");
        map.put("immersiveengineering:material:12", "immersiveengineering:windmill_sail");
        map.put("immersiveengineering:material:13", "immersiveengineering:wooden_grip");
        map.put("immersiveengineering:material:14", "immersiveengineering:gunpart_barrel");
        map.put("immersiveengineering:material:15", "immersiveengineering:gunpart_drum");
        map.put("immersiveengineering:material:16", "immersiveengineering:gunpart_hammer");
        map.put("immersiveengineering:material:17", "immersiveengineering:dust_coke");
        map.put("immersiveengineering:material:18", "immersiveengineering:dust_hop_graphite");
        map.put("immersiveengineering:material:19", "immersiveengineering:ingot_hop_graphite");
        map.put("immersiveengineering:material:20", "immersiveengineering:wire_copper");
        map.put("immersiveengineering:material:21", "immersiveengineering:wire_electrum");
        map.put("immersiveengineering:material:22", "immersiveengineering:wire_aluminum");
        map.put("immersiveengineering:material:23", "immersiveengineering:wire_steel");
        map.put("immersiveengineering:material:24", "immersiveengineering:dust_saltpeter");
        map.put("immersiveengineering:material:25", "immersiveengineering:dust_sulfur");
        map.put("immersiveengineering:material:26", "immersiveengineering:electron_tube");
        map.put("immersiveengineering:material:27", "immersiveengineering:circuit_board");
        map.put("immersiveengineering:seed:0", "immersiveengineering:seed");
        map.put("immersiveengineering:fluidContainers:1", "immersiveengineering:creosote_bucket");
        map.put("immersiveengineering:fluidContainers:3", "immersiveengineering:plantoil_bucket");
        map.put("immersiveengineering:fluidContainers:5", "immersiveengineering:ethanol_bucket");
        map.put("immersiveengineering:fluidContainers:7", "immersiveengineering:biodiesel_bucket");
        map.put("immersiveengineering:wirecoil:0", "immersiveengineering:wirecoil_copper");
        map.put("immersiveengineering:wirecoil:1", "immersiveengineering:wirecoil_copper_ins");
        map.put("immersiveengineering:wirecoil:2", "immersiveengineering:wirecoil_electrum");
        map.put("immersiveengineering:wirecoil:3", "immersiveengineering:wirecoil_electrum_ins");
        map.put("immersiveengineering:wirecoil:4", "immersiveengineering:wirecoil_steel");
        map.put("immersiveengineering:wirecoil:5", "immersiveengineering:wirecoil_structure_rope");
        map.put("immersiveengineering:wirecoil:6", "immersiveengineering:wirecoil_structure_steel");
        map.put("immersiveengineering:wirecoil:7", "immersiveengineering:wirecoil_redstone");
        map.put("immersiveengineering:tool:0", "immersiveengineering:hammer");
        map.put("immersiveengineering:tool:1", "immersiveengineering:wirecutter");
        map.put("immersiveengineering:tool:2", "immersiveengineering:voltmeter");
        map.put("immersiveengineering:tool:3", "immersiveengineering:manual");
        map.put("immersiveengineering:toolbox:0", "immersiveengineering:toolbox");
        map.put("immersiveengineering:pickaxe_steel:0", "immersiveengineering:pickaxe_steel");
        map.put("immersiveengineering:shovel_steel:0", "immersiveengineering:shovel_steel");
        map.put("immersiveengineering:axe_steel:0", "immersiveengineering:axe_steel");
        map.put("immersiveengineering:hoe_steel:0", "immersiveengineering:hoe_steel");
        map.put("immersiveengineering:sword_steel:0", "immersiveengineering:sword_steel");
        map.put("immersiveengineering:revolver:0", "immersiveengineering:revolver");
        map.put("immersiveengineering:revolver:0", "immersiveengineering:revolver.dev");
        map.put("immersiveengineering:revolver:1", "immersiveengineering:revolver.infinity");
        map.put("immersiveengineering:revolver:2", "immersiveengineering:revolver.tesla");
        map.put("immersiveengineering:revolver:3", "immersiveengineering:revolver.sns");
        map.put("immersiveengineering:revolver:4", "immersiveengineering:revolver.nerf");
        map.put("immersiveengineering:revolver:5", "immersiveengineering:revolver.oblivion");
        map.put("immersiveengineering:revolver:6", "immersiveengineering:revolver.oathkeeper");
        map.put("immersiveengineering:revolver:7", "immersiveengineering:revolver.bee");
        map.put("immersiveengineering:revolver:8", "immersiveengineering:revolver.fenrir");
        map.put("immersiveengineering:revolver:9", "immersiveengineering:revolver.earthshaker");
        map.put("immersiveengineering:revolver:10", "immersiveengineering:revolver.warlord");
        map.put("immersiveengineering:revolver:11", "immersiveengineering:revolver.superchief");
        map.put("immersiveengineering:revolver:12", "immersiveengineering:revolver.rose");
        map.put("immersiveengineering:revolver:13", "immersiveengineering:revolver.noName");
        map.put("immersiveengineering:revolver:14", "immersiveengineering:revolver.patreonBlu");
        map.put("immersiveengineering:revolver:15", "immersiveengineering:revolver.patreonHazard");
        map.put("immersiveengineering:speedloader:0", "immersiveengineering:speedloader");
        map.put("immersiveengineering:bullet:0", "immersiveengineering:empty_casing");
        map.put("immersiveengineering:bullet:1", "immersiveengineering:empty_shell");
        map.put("immersiveengineering:bullet:2", "immersiveengineering:bullet.casull");
        map.put("immersiveengineering:bullet:3", "immersiveengineering:bullet.armor_piercing");
        map.put("immersiveengineering:bullet:4", "immersiveengineering:bullet.buckshot");
        map.put("immersiveengineering:bullet:5", "immersiveengineering:bullet.he");
        map.put("immersiveengineering:bullet:6", "immersiveengineering:bullet.dragons_breath");
        map.put("immersiveengineering:bullet:7", "immersiveengineering:bullet.wolfpack");
        map.put("immersiveengineering:bullet:8", "immersiveengineering:bullet.silver");
        map.put("immersiveengineering:bullet:9", "immersiveengineering:bullet.potion");
        map.put("immersiveengineering:bullet:10", "immersiveengineering:bullet.potion.splash");
        map.put("immersiveengineering:bullet:11", "immersiveengineering:bullet.potion.linger");
        map.put("immersiveengineering:bullet:12", "immersiveengineering:bullet.flare");
        map.put("immersiveengineering:drill:0", "immersiveengineering:drill");
        map.put("immersiveengineering:drillhead:0", "immersiveengineering:drillhead_iron");
        map.put("immersiveengineering:drillhead:1", "immersiveengineering:drillhead_steel");
        map.put("immersiveengineering:skyhook:0", "immersiveengineering:skyhook");
        map.put("immersiveengineering:toolupgrade:0", "immersiveengineering:toolupgrade_drill_waterproof");
        map.put("immersiveengineering:toolupgrade:1", "immersiveengineering:toolupgrade_drill_lube");
        map.put("immersiveengineering:toolupgrade:2", "immersiveengineering:toolupgrade_drill_damage");
        map.put("immersiveengineering:toolupgrade:3", "immersiveengineering:toolupgrade_drill_capacity");
        map.put("immersiveengineering:toolupgrade:4", "immersiveengineering:toolupgrade_revolver_bayonet");
        map.put("immersiveengineering:toolupgrade:5", "immersiveengineering:toolupgrade_revolver_magazine");
        map.put("immersiveengineering:toolupgrade:6", "immersiveengineering:toolupgrade_revolver_electro");
        map.put("immersiveengineering:toolupgrade:7", "immersiveengineering:toolupgrade_chemthrower_focus");
        map.put("immersiveengineering:toolupgrade:8", "immersiveengineering:toolupgrade_railgun_scope");
        map.put("immersiveengineering:toolupgrade:9", "immersiveengineering:toolupgrade_railgun_capacitors");
        map.put("immersiveengineering:toolupgrade:10", "immersiveengineering:toolupgrade_shield_flash");
        map.put("immersiveengineering:toolupgrade:11", "immersiveengineering:toolupgrade_shield_shock");
        map.put("immersiveengineering:toolupgrade:12", "immersiveengineering:toolupgrade_shield_magnet");
        map.put("immersiveengineering:toolupgrade:13", "immersiveengineering:toolupgrade_chemthrower_multitank");
        map.put("immersiveengineering:blueprint:0", "immersiveengineering:blueprint");
        map.put("immersiveengineering:mold:0", "immersiveengineering:mold_plate");
        map.put("immersiveengineering:mold:1", "immersiveengineering:mold_gear");
        map.put("immersiveengineering:mold:2", "immersiveengineering:mold_rod");
        map.put("immersiveengineering:mold:3", "immersiveengineering:mold_bullet_casing");
        map.put("immersiveengineering:mold:4", "immersiveengineering:mold_wire");
        map.put("immersiveengineering:mold:5", "immersiveengineering:mold_packing_4");
        map.put("immersiveengineering:mold:6", "immersiveengineering:mold_packing_9");
        map.put("immersiveengineering:mold:7", "immersiveengineering:mold_unpacking");
        map.put("immersiveengineering:graphite_electrode:0", "immersiveengineering:graphite_electrode");
        map.put("immersiveengineering:shader:0", "immersiveengineering:shader");
        map.put("immersiveengineering:shader_bag:0", "immersiveengineering:shader_bag");
        map.put("immersiveengineering:jerrycan:0", "immersiveengineering:jerrycan");
        map.put("immersiveengineering:chemthrower:0", "immersiveengineering:chemthrower");
        map.put("immersiveengineering:railgun:0", "immersiveengineering:railgun");
        map.put("immersiveengineering:earmuffs:0", "immersiveengineering:earmuffs");
        map.put("immersiveengineering:coresample:0", "immersiveengineering:coresample");
        map.put("immersiveengineering:faraday_suit_head:0", "immersiveengineering:armor_faraday_head");
        map.put("immersiveengineering:faraday_suit_chest:0", "immersiveengineering:armor_faraday_chest");
        map.put("immersiveengineering:faraday_suit_legs:0", "immersiveengineering:armor_faraday_legs");
        map.put("immersiveengineering:faraday_suit_feet:0", "immersiveengineering:armor_faraday_feet");
        map.put("immersiveengineering:fluorescent_tube:0", "immersiveengineering:fluorescent_tube");
        map.put("immersiveengineering:powerpack:0", "immersiveengineering:powerpack");
        map.put("immersiveengineering:shield:0", "immersiveengineering:shield");
        map.put("immersiveengineering:maintenance_kit:0", "immersiveengineering:maintenance_kit");
        map.put("immersiveengineering:steel_armor_head:0", "immersiveengineering:armor_steel_head");
        map.put("immersiveengineering:steel_armor_chest:0", "immersiveengineering:armor_steel_chest");
        map.put("immersiveengineering:steel_armor_legs:0", "immersiveengineering:armor_steel_legs");
        map.put("immersiveengineering:steel_armor_feet:0", "immersiveengineering:armor_steel_feet");
        map.put("immersiveengineering:map_orevein:0", "immersiveengineering:map_orevein");
        
        //Oredict entries
        map.put("oredict:ingotIron", "tags:ingots/iron");
        map.put("oredict:ingotGold", "tags:ingots/gold");
        map.put("oredict:ingotCopper", "tags:ingots/copper");

        map.put("oredict:nuggetGold", "tags:nuggets/gold");
        map.put("oredict:nuggetIron", "tags:nuggets/iron");

        map.put("oredict:dyeBlack", "tags:dyes/black");
        map.put("oredict:dyeBlue", "tags:dyes/blue");
        map.put("oredict:dyeBrown", "tags:dyes/brown");
        map.put("oredict:dyeCyan", "tags:dyes/cyan");
        map.put("oredict:dyeGray", "tags:dyes/gray");
        map.put("oredict:dyeGreen", "tags:dyes/green");
        map.put("oredict:dyeLightBlue", "tags:dyes/light_blue");
        map.put("oredict:dyeLightGray", "tags:dyes/light_gray");
        map.put("oredict:dyeLime", "tags:dyes/lime");
        map.put("oredict:dyeMagenta", "tags:dyes/magenta");
        map.put("oredict:dyeOrange", "tags:dyes/orange");
        map.put("oredict:dyePink", "tags:dyes/pink");
        map.put("oredict:dyePurple", "tags:dyes/purple");
        map.put("oredict:dyeRed", "tags:dyes/red");
        map.put("oredict:dyeWhite", "tags:dyes/white");
        map.put("oredict:dyeYellow", "tags:dyes/yellow");

        map.put("oredict:dustRedstone", "tags:dusts/redstone");
        map.put("oredict:dustGlowstone", "tags:dusts/glowstone");

        map.put("oredict:plankWood", "tags:planks");
        map.put("oredict:logWood", "tags:logs");
        return map;
    }
    
    private static Map<String, String> generateUnflattnedMappings(){
        Map<String, String> map = new HashMap<>();
        flattenedNames.forEach((key, value) -> map.put(value, key));
        return map;
    }
}
