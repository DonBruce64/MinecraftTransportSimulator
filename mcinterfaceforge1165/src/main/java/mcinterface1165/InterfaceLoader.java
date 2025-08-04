package mcinterface1165;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.items.components.IItemBlock;
import minecrafttransportsimulator.items.components.IItemEntityProvider;
import minecrafttransportsimulator.items.components.IItemFood;
import minecrafttransportsimulator.items.instances.ItemItem;
import minecrafttransportsimulator.jsondefs.JSONPack;
import minecrafttransportsimulator.mcinterface.IInterfaceCore;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.LanguageSystem;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Food;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
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
    public static final String MODVER = "22.18.0";

    public static final Logger LOGGER = LogManager.getLogger(InterfaceManager.coreModID);
    private final String gameDirectory;


    public InterfaceLoader() {
        gameDirectory = FMLPaths.GAMEDIR.get().toFile().getAbsolutePath();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::init);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onPostConstruction);
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
                    boolean hideOnCreativeTab = item.definition.general.hideOnCreativeTab || (item instanceof AItemSubTyped && ((AItemSubTyped<?>) item).subDefinition.hideOnCreativeTab);
                    if (!hideOnCreativeTab && (!item.definition.packID.equals(InterfaceManager.coreModID) || !item.definition.systemName.contains("invisible"))) {
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

        //Init the language system for the created items.
        LanguageSystem.init(isClient);

        //Register all items in our wrapper map.
        for (Entry<AItemBase, BuilderItem> entry : BuilderItem.itemMap.entrySet()) {
            AItemPack<?> item = (AItemPack<?>) entry.getKey();
            BuilderItem mcItem = entry.getValue();

            //Register the item.
            BuilderItem.ITEMS.register(item.getRegistrationName(), () -> mcItem);

            //If the item is for OreDict, make it a fake tag, since we are forced to use JSON otherwise.
            //Stupid JSON everything without code hooks.
            if (item.definition.general.oreDict != null) {
                String lowerCaseOre = item.definition.general.oreDict.toLowerCase(Locale.ROOT);
                List<BuilderItem> items = InterfaceCore.taggedItems.get(lowerCaseOre);
                if (items == null) {
                    items = new ArrayList<>();
                    InterfaceCore.taggedItems.put(lowerCaseOre, items);
                }
                items.add(mcItem);
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
                    String name = itemBlockBlock.getClass().getSimpleName().substring("Block".length()).toLowerCase(Locale.ROOT);
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
            String name = collisionBlock.getClass().getSimpleName().substring("Block".length()).toLowerCase(Locale.ROOT) + i;
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

            //Save modified config.
            ConfigSystem.saveToDisk();
        }
    }

    public void onPostConstruction(FMLLoadCompleteEvent event) {
        if (FMLEnvironment.dist.isClient()) {
            //Put all liquids into the config file for use by modpack makers.
            ConfigSystem.settings.fuel.lastLoadedFluids = InterfaceManager.clientInterface.getAllFluidNames();

            //Save modified config.
            ConfigSystem.saveToDisk();
        }
    }
}
