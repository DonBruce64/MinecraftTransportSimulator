package mcinterface1165;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.instances.BlockCollision;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityEnergyCharger;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityFluidTankProvider;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityInventoryProvider;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.IItemBlock;
import minecrafttransportsimulator.items.components.IItemEntityProvider;
import minecrafttransportsimulator.items.components.IItemFood;
import minecrafttransportsimulator.items.instances.ItemItem;
import minecrafttransportsimulator.jsondefs.AJSONMultiModelProvider;
import minecrafttransportsimulator.jsondefs.JSONPack;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.mcinterface.IInterfaceCore;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Food;
import net.minecraft.item.Item;
import net.minecraft.tags.ItemTags;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Loader interface for the mod.  This class is not actually an interface, unlike everything else.
 * Instead, it keeps references to all interfaces, which are passed-in during construction.
 * It also handles initialization calls when the game is first booted.  There will only
 * be ONE loader per running instance of Minecraft.
 *
 * @author don_bruce
 */
@Mod(InterfaceLoader.MODID)
public class InterfaceLoader {
    public static final String MODID = "mts";
    public static final String MODNAME = "Immersive Vehicles (MTS)";
    public static final String MODVER = "22.8.0-BETA";

    public static final Logger LOGGER = LogManager.getLogger(InterfaceManager.coreModID);
    private final String gameDirectory;


    public InterfaceLoader() {
        gameDirectory = FMLPaths.GAMEDIR.get().toFile().getAbsolutePath();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::init);
    }

    /**Need to defer init until post-mod construction, as in this version
     * {@link IInterfaceCore#getModName(String)} requires a constructor pack-mod
     * instance to query the classloader for a resource, and we need that for pack
     * init in the boot calls.
     * 
     */
    public void init(FMLConstructModEvent event) {
        //Add registries.
        BuilderItem.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        BuilderBlock.BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
        BuilderTileEntity.TILE_ENTITIES.register(FMLJavaModLoadingContext.get().getModEventBus());
        ABuilderEntityBase.ENTITIES.register(FMLJavaModLoadingContext.get().getModEventBus());

        //Need to do pack parsing first, since that generates items which have to be registered prior to any other events.
        boolean isClient = FMLEnvironment.dist.isClient();

        //Init interfaces and send to the main game system.
        if (isClient) {
            new InterfaceManager(MODID, gameDirectory, new InterfaceCore(), new InterfacePacket(), new InterfaceClient(), new InterfaceInput(), new InterfaceSound(), new InterfaceRender());
            FMLJavaModLoadingContext.get().getModEventBus().addListener(InterfaceRender::registerRenderer);
        } else {
            new InterfaceManager(MODID, gameDirectory, new InterfaceCore(), new InterfacePacket(), null, null, null, null);
        }

        InterfaceManager.coreInterface.logError("Welcome to MTS VERSION: " + MODVER);

        //Parse packs
        ConfigSystem.loadFromDisk(new File(gameDirectory, "config"), isClient);
        List<File> packDirectories = new ArrayList<>();
        File modDirectory = new File(gameDirectory, "mods");
        if (modDirectory.exists()) {
            packDirectories.add(modDirectory);

            //Parse the packs.
            PackParser.addDefaultItems();
            PackParser.parsePacks(packDirectories);
            
            //For each item in each pack's crafting def, convert it to 1.16.5 MC format if it isn't already.
            for(AItemPack<?> item : PackParser.getAllPackItems()) {
                for (List<String> materialList : item.definition.general.materialLists) {
                    for (int i = 0; i < materialList.size(); ++i) {
                        String[] components = materialList.get(i).split(":");
                        //Format must be domain, item, meta, qty, convert.
                        if (components.length == 4) {
                            materialList.set(i, getNewItemName(components));
                        } else if (components[0].equals("oredict")) {
                            materialList.set(i, getNewOredictName(components));
                        }
                    }
                }
                if (item.definition instanceof AJSONMultiModelProvider) {
                    AJSONMultiModelProvider provider = (AJSONMultiModelProvider) item.definition;
                    for (JSONSubDefinition definition : provider.definitions) {
                        for (List<String> materialList : definition.extraMaterialLists) {
                            for (int i = 0; i < materialList.size(); ++i) {
                                String[] components = materialList.get(i).split(":");
                                //Format must be domain, item, meta, qty, convert.
                                if (components.length == 4) {
                                    materialList.set(i, getNewItemName(components));
                                } else if (components[0].equals("oredict")) {
                                    materialList.set(i, getNewOredictName(components));
                                }
                            }
                        }
                    }
                }
            }
        } else {
            InterfaceManager.coreInterface.logError("Could not find mods directory!  Game directory is confirmed to: " + gameDirectory);
        }

        //Create all pack items.  We need to do this before anything else.
        //block registration comes first, and we use the items registered to determine
        //which blocks we need to register.
        for (String packID : PackParser.getAllPackIDs()) {
            for (AItemPack<?> item : PackParser.getAllItemsForPack(packID, true)) {
                if (item.autoGenerate()) {
                    Item.Properties itemProperties = new Item.Properties();

                    //Check if the creative tab is set/created.
                    //The only except is for "invisible" parts of the core mod, these are internal.
                    if (!item.definition.packID.equals(InterfaceManager.coreModID) || !item.definition.systemName.contains("invisible")) {
                        String tabID = item.getCreativeTabID();
                        if (!BuilderCreativeTab.createdTabs.containsKey(tabID)) {
                            JSONPack packConfiguration = PackParser.getPackConfiguration(tabID);
                            AItemPack<?> tabItem = packConfiguration.packItem != null ? PackParser.getItem(packConfiguration.packID, packConfiguration.packItem) : null;
                            BuilderCreativeTab.createdTabs.put(tabID, new BuilderCreativeTab(packConfiguration.packName, tabItem));
                        }
                        itemProperties.tab(BuilderCreativeTab.createdTabs.get(tabID));
                    }
                    itemProperties.stacksTo(item.getStackSize());
                    if (item instanceof ItemItem && ((ItemItem) item).definition.food != null) {
                        IItemFood food = (IItemFood) item;
                        itemProperties.food(new Food.Builder().nutrition(food.getHungerAmount()).saturationMod(food.getSaturationAmount()).build());
                    }
                    new BuilderItem(itemProperties, item);
                }
            }
        }

        //Register all items in our wrapper map.
        for (Entry<AItemBase, BuilderItem> entry : BuilderItem.itemMap.entrySet()) {
            AItemPack<?> item = (AItemPack<?>) entry.getKey();
            BuilderItem mcItem = entry.getValue();

            //Register the item.
            BuilderItem.ITEMS.register(item.getRegistrationName(), () -> mcItem);

            //If the item is for OreDict, add it...as a tag!  Cause this is the new standard.
            if (item.definition.general.oreDict != null) {
                ItemTags.createOptional(new ResourceLocation(MODID, item.definition.general.oreDict));
            }
        }

        //Register the IItemBlock blocks.  We cheat here and
        //iterate over all items and get the blocks they spawn.
        //Not only does this prevent us from having to manually set the blocks
        //we also pre-generate the block classes here.
        List<ABlockBase> blocksRegistred = new ArrayList<>();
        for (AItemBase item : BuilderItem.itemMap.keySet()) {
            if (item instanceof IItemBlock) {
                ABlockBase itemBlockBlock = ((IItemBlock) item).getBlock();
                if (!blocksRegistred.contains(itemBlockBlock)) {
                    //New block class detected.  Register it and its instance.
                    BuilderBlock wrapper = new BuilderBlock(itemBlockBlock);
                    String name = itemBlockBlock.getClass().getSimpleName().substring("Block".length()).toLowerCase();
                    BuilderBlock.BLOCKS.register(name, () -> wrapper);
                    BuilderBlock.blockMap.put(itemBlockBlock, wrapper);
                    blocksRegistred.add(itemBlockBlock);
                }
            }
        }

        //Register the collision blocks.
        for (int i = 0; i < BlockCollision.blockInstances.size(); ++i) {
            BlockCollision collisionBlock = BlockCollision.blockInstances.get(i);
            BuilderBlock wrapper = new BuilderBlock(collisionBlock);
            String name = collisionBlock.getClass().getSimpleName().substring("Block".length()).toLowerCase() + i;
            BuilderBlock.BLOCKS.register(name, () -> wrapper);
            BuilderBlock.blockMap.put(collisionBlock, wrapper);
        }

        //If we are on the client, create models.
        if (isClient) {
            InterfaceEventsModelLoader.init();
        }

        //Register the TEs.  Has to be done last to ensure block maps are populated.
        List<BuilderBlock> normalBlocks = new ArrayList<>();
        List<BuilderBlock> fluidBlocks = new ArrayList<>();
        List<BuilderBlock> inventoryBlocks = new ArrayList<>();
        List<BuilderBlock> chargerBlocks = new ArrayList<>();

        BuilderBlock.blockMap.values().forEach(builder -> {
            if (builder.block instanceof ABlockBaseTileEntity) {
                if (ITileEntityFluidTankProvider.class.isAssignableFrom(((ABlockBaseTileEntity) builder.block).getTileEntityClass())) {
                    fluidBlocks.add(builder);
                } else if (ITileEntityInventoryProvider.class.isAssignableFrom(((ABlockBaseTileEntity) builder.block).getTileEntityClass())) {
                    inventoryBlocks.add(builder);
                } else if (ITileEntityEnergyCharger.class.isAssignableFrom(((ABlockBaseTileEntity) builder.block).getTileEntityClass())) {
                    chargerBlocks.add(builder);
                } else {
                    normalBlocks.add(builder);
                }
            }
        });

        BuilderTileEntity.TE_TYPE = BuilderTileEntity.TILE_ENTITIES.register("builder_base", () -> TileEntityType.Builder.of(BuilderTileEntity::new, normalBlocks.toArray(new BuilderBlock[0])).build(null));
        BuilderTileEntityFluidTank.TE_TYPE2 = BuilderTileEntity.TILE_ENTITIES.register("builder_fluidtank", () -> TileEntityType.Builder.of(BuilderTileEntityFluidTank::new, fluidBlocks.toArray(new BuilderBlock[0])).build(null));
        BuilderTileEntityInventoryContainer.TE_TYPE2 = BuilderTileEntity.TILE_ENTITIES.register("builder_inventory", () -> TileEntityType.Builder.of(BuilderTileEntityInventoryContainer::new, inventoryBlocks.toArray(new BuilderBlock[0])).build(null));
        BuilderTileEntityEnergyCharger.TE_TYPE2 = BuilderTileEntity.TILE_ENTITIES.register("builder_charger", () -> TileEntityType.Builder.of(BuilderTileEntityEnergyCharger::new, chargerBlocks.toArray(new BuilderBlock[0])).build(null));

        //Init entities.
        BuilderEntityExisting.E_TYPE2 = ABuilderEntityBase.ENTITIES.register("builder_existing", () -> EntityType.Builder.<BuilderEntityExisting>of(BuilderEntityExisting::new, EntityClassification.MISC).sized(0.05F, 0.05F).clientTrackingRange(32 * 16).updateInterval(5).build("builder_existing"));
        BuilderEntityLinkedSeat.E_TYPE3 = ABuilderEntityBase.ENTITIES.register("builder_seat", () -> EntityType.Builder.<BuilderEntityLinkedSeat>of(BuilderEntityLinkedSeat::new, EntityClassification.MISC).sized(0.05F, 0.05F).clientTrackingRange(32 * 16).updateInterval(5).build("builder_seat"));
        BuilderEntityRenderForwarder.E_TYPE4 = ABuilderEntityBase.ENTITIES.register("builder_rendering", () -> EntityType.Builder.<BuilderEntityRenderForwarder>of(BuilderEntityRenderForwarder::new, EntityClassification.MISC).sized(0.05F, 0.05F).clientTrackingRange(32 * 16).updateInterval(5).build("builder_rendering"));

        //Iterate over all pack items and find those that spawn entities.
        for (AItemPack<?> packItem : PackParser.getAllPackItems()) {
            if (packItem instanceof IItemEntityProvider) {
                ((IItemEntityProvider) packItem).registerEntities(BuilderEntityExisting.entityMap);
            }
        }

        //Init networking interface.  This will register packets as well.
        InterfacePacket.init();

        if (isClient) {
            //Init keybinds if we're on the client.
            InterfaceManager.inputInterface.initConfigKey();

            //Also put all liquids into the config file for use by modpack makers.
            ConfigSystem.settings.fuel.lastLoadedFluids = InterfaceManager.clientInterface.getAllFluidNames();

            //Also disable playerTweaks if some known-problematic mods are present.
            if (InterfaceManager.coreInterface.isModPresent("tails") || InterfaceManager.coreInterface.isModPresent("obfuscate") || InterfaceManager.coreInterface.isModPresent("mobends")) {
                ConfigSystem.client.renderingSettings.playerTweaks.value = false;
            }

            //Save modified config.
            ConfigSystem.saveToDisk();
        }
    }
    
    private static String getNewItemName(String[] components) {
        switch(components[1] + ":" + components[2]) {
            case ("stone:1"):
                return components[0] + ":granite:" + components[3];
            case ("stone:2"):
                return components[0] + ":polished_granite:" + components[3];
            case ("stone:3"):
                return components[0] + ":diorite:" + components[3];
            case ("stone:4"):
                return components[0] + ":polished_diorite:" + components[3];
            case ("stone:5"):
                return components[0] + ":andesite:" + components[3];
            case ("stone:6"):
                return components[0] + ":polished_andesite:" + components[3];
            case ("grass:0"):
                return components[0] + ":grass_block:" + components[3];
            case ("dirt:1"):
                return components[0] + ":coarse_dirt:" + components[3];
            case ("dirt:2"):
                return components[0] + ":podzol:" + components[3];
            case ("planks:0"):
                return components[0] + ":oak_planks:" + components[3];
            case ("planks:1"):
                return components[0] + ":spruce_planks:" + components[3];
            case ("planks:2"):
                return components[0] + ":birch_planks:" + components[3];
            case ("planks:3"):
                return components[0] + ":jungle_planks:" + components[3];
            case ("planks:4"):
                return components[0] + ":acacia_planks:" + components[3];
            case ("planks:5"):
                return components[0] + ":dark_oak_planks:" + components[3];
            case ("sapling:0"):
                return components[0] + ":oak_sapling:" + components[3];
            case ("sapling:1"):
                return components[0] + ":spruce_sapling:" + components[3];
            case ("sapling:2"):
                return components[0] + ":birch_sapling:" + components[3];
            case ("sapling:3"):
                return components[0] + ":jungle_sapling:" + components[3];
            case ("sapling:4"):
                return components[0] + ":acacia_sapling:" + components[3];
            case ("sapling:5"):
                return components[0] + ":dark_oak_sapling:" + components[3];
            case ("sand:1"):
                return components[0] + ":red_sand:" + components[3];
            case ("log:0"):
                return components[0] + ":oak_log:" + components[3];
            case ("log:1"):
                return components[0] + ":spruce_log:" + components[3];
            case ("log:2"):
                return components[0] + ":birch_log:" + components[3];
            case ("log:3"):
                return components[0] + ":jungle_log:" + components[3];
            case ("log:4"):
                return components[0] + ":acacia_log:" + components[3];
            case ("log:5"):
                return components[0] + ":dark_oak_log:" + components[3];
            case ("leaves:0"):
                return components[0] + ":oak_leaves:" + components[3];
            case ("leaves:1"):
                return components[0] + ":spruce_leaves:" + components[3];
            case ("leaves:2"):
                return components[0] + ":birch_leaves:" + components[3];
            case ("leaves:3"):
                return components[0] + ":jungle_leaves:" + components[3];
            case ("leaves:4"):
                return components[0] + ":acacia_leaves:" + components[3];
            case ("leaves:5"):
                return components[0] + ":dark_oak_leaves:" + components[3];
            case ("sponge:1"):
                return components[0] + ":wet_sponge:" + components[3];
            case ("sandstone:1"):
                return components[0] + ":chiseled_sandstone:" + components[3];
            case ("sandstone:2"):
                return components[0] + ":cut_sandstone:" + components[3];
            case ("golden_rail:0"):
                return components[0] + ":powered_rail:" + components[3];
            case ("tallgrass:2"):
                return components[0] + ":fern:" + components[3];
            case ("deadbush:0"):
                return components[0] + ":dead_bush:" + components[3];
            case ("wool:0"):
                return components[0] + ":white_wool:" + components[3];
            case ("wool:1"):
                return components[0] + ":orange_wool:" + components[3];
            case ("wool:2"):
                return components[0] + ":magenta_wool:" + components[3];
            case ("wool:3"):
                return components[0] + ":light_blue_wool:" + components[3];
            case ("wool:4"):
                return components[0] + ":yellow_wool:" + components[3];
            case ("wool:5"):
                return components[0] + ":lime_wool:" + components[3];
            case ("wool:6"):
                return components[0] + ":pink_wool:" + components[3];
            case ("wool:7"):
                return components[0] + ":gray_wool:" + components[3];
            case ("wool:8"):
                return components[0] + ":light_gray_wool:" + components[3];
            case ("wool:9"):
                return components[0] + ":cyan_wool:" + components[3];
            case ("wool:10"):
                return components[0] + ":purple_wool:" + components[3];
            case ("wool:11"):
                return components[0] + ":blue_wool:" + components[3];
            case ("wool:12"):
                return components[0] + ":brown_wool:" + components[3];
            case ("wool:13"):
                return components[0] + ":green_wool:" + components[3];
            case ("wool:14"):
                return components[0] + ":red_wool:" + components[3];
            case ("wool:15"):
                return components[0] + ":black_wool:" + components[3];
            case ("yellow_flower:0"):
                return components[0] + ":dandelion:" + components[3];
            case ("red_flower:0"):
                return components[0] + ":poppy:" + components[3];
            case ("red_flower:1"):
                return components[0] + ":blue_orchid:" + components[3];
            case ("red_flower:2"):
                return components[0] + ":allium:" + components[3];
            case ("red_flower:3"):
                return components[0] + ":azure_bluet:" + components[3];
            case ("red_flower:4"):
                return components[0] + ":red_tulip:" + components[3];
            case ("red_flower:5"):
                return components[0] + ":orange_tulip:" + components[3];
            case ("red_flower:6"):
                return components[0] + ":white_tulip:" + components[3];
            case ("red_flower:7"):
                return components[0] + ":pink_tulip:" + components[3];
            case ("red_flower:8"):
                return components[0] + ":oxeye_daisy:" + components[3];
            case ("wooden_slab:0"):
                return components[0] + ":oak_slab:" + components[3];
            case ("wooden_slab:1"):
                return components[0] + ":spruce_slab:" + components[3];
            case ("wooden_slab:2"):
                return components[0] + ":birch_slab:" + components[3];
            case ("wooden_slab:3"):
                return components[0] + ":jungle_slab:" + components[3];
            case ("wooden_slab:4"):
                return components[0] + ":acacia_slab:" + components[3];
            case ("wooden_slab:5"):
                return components[0] + ":dark_oak_slab:" + components[3];
            case ("stone_slab:1"):
                return components[0] + ":sandstone_slab:" + components[3];
            case ("stone_slab:3"):
                return components[0] + ":cobblestone_slab:" + components[3];
            case ("stone_slab:4"):
                return components[0] + ":brick_slab:" + components[3];
            case ("stone_slab:5"):
                return components[0] + ":stone_brick_slab:" + components[3];
            case ("stone_slab:6"):
                return components[0] + ":nether_brick_slab:" + components[3];
            case ("stone_slab:7"):
                return components[0] + ":quartz_slab:" + components[3];
            case ("double_stone_slab:7"):
                return components[0] + ":smooth_quartz:" + components[3];
            case ("double_stone_slab:8"):
                return components[0] + ":smooth_stone:" + components[3];
            case ("brick_block:8"):
                return components[0] + ":bricks:" + components[3];
            case ("wooden_pressure_plate:0"):
                return components[0] + ":oak_pressure_plate:" + components[3];
            case ("snow_layer:0"):
                return components[0] + ":snow:" + components[3];
            case ("snow:0"):
                return components[0] + ":snow_block:" + components[3];
            case ("fence:0"):
                return components[0] + ":oak_fence:" + components[3];
            case ("lit_pumpkin:0"):
                return components[0] + ":jack_o_lantern:" + components[3];
            case ("trapdoor:0"):
                return components[0] + ":oak_trapdoor:" + components[3];
        }
        return components[0] + ":" + components[1] + ":" + components[3];
    }

    private static String getNewOredictName(String[] components) {
        //Old oredict uses camelCase, if we have it, we need to convert.
        if (!components[1].toLowerCase().equals(components[1])) {
            if (components[1].startsWith("ingot")) {
                return components[0] + ":ingots/" + components[1].substring("ingot".length()).toLowerCase() + ":" + components[2];
            } else if (components[1].startsWith("dye")) {
                return components[0] + ":dyes/" + components[1].substring("dye".length()).toLowerCase().replace("light", "light_") + ":" + components[2];
            } else if (components[1].startsWith("dust")) {
                return components[0] + ":dusts/" + components[1].substring("dust".length()).toLowerCase() + ":" + components[2];
            } else if (components[1].equals("plankWood")) {
                return components[0] + ":planks" + ":" + components[2];
            }
        }
        return components[0] + ":" + components[1] + ":" + components[2];
    }
}
