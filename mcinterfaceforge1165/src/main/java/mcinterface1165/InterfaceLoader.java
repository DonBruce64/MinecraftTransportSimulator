package mcinterface1165;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.instances.BlockCollision;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.IItemBlock;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Loader interface for the mod.  This class is not actually an interface, unlike everything else.
 * Instead, it keeps references to all interfaces, which are passed-in during construction.
 * It also handles initialization calls when the game is first booted.  There will only
 * be ONE loader per running instance of Minecraft.
 *
 * @author don_bruce
 */
@Mod(InterfaceLoader.MODID)
public final class InterfaceLoader {
    public static final String MODID = "mts";
    public static final String MODNAME = "Immersive Vehicles (MTS)";
    public static final String MODVER = "22.1.0";

    static {
        //Enable universal bucket so that we can use buckets on fuel pumps.
        FluidRegistry.enableUniversalBucket();

        //Add registries.
        BuilderBlock.BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
        BuilderTileEntity.TILE_ENTITIES.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

	public static final Logger LOGGER = LogManager.getLogger(InterfaceManager.coreModID);

    @SuppressWarnings("InstantiationOfUtilityClass")
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        //Get game directory.
        String gameDirectory = event.getModConfigurationDirectory().getParent();

        //Init interfaces and send to the main game system.
        if (event.getSide().isClient()) {
            new InterfaceManager(MODID, gameDirectory, new InterfaceCore(), new InterfacePacket(), new InterfaceClient(), new InterfaceInput(), new InterfaceSound(), new InterfaceRender());
        } else {
            new InterfaceManager(MODID, gameDirectory, new InterfaceCore(), new InterfacePacket(), null, null, null, null);
        }

        InterfaceManager.coreInterface.logError("Welcome to MTS VERSION:" + MODVER);

        //Parse packs
        ConfigSystem.loadFromDisk(new File(gameDirectory, "config"), event.getSide().isClient());
        List<File> packDirectories = new ArrayList<>();
        File modDirectory = new File(gameDirectory, "mods");
        if (modDirectory.exists()) {
            packDirectories.add(modDirectory);

            //Also add version-specific directory.
            File versionedModDirectory = new File(modDirectory, InterfaceManager.coreInterface.getGameVersion());
            if (versionedModDirectory.exists()) {
                packDirectories.add(versionedModDirectory);
            }

            //Parse the packs.
            PackParser.addDefaultItems();
            PackParser.parsePacks(packDirectories, event.getSide().isClient());
        } else {
            InterfaceManager.coreInterface.logError("Could not find mods directory!  Game directory is confirmed to: " + gameDirectory);
        }

        //Create all pack items.  We need to do this before anything else.
        //block registration comes first, and we use the items registered to determine
        //which blocks we need to register.
        for (String packID : PackParser.getAllPackIDs()) {
            for (AItemPack<?> packItem : PackParser.getAllItemsForPack(packID, true)) {
                if (packItem.autoGenerate()) {
                    new BuilderItem(packItem);
                }
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
                    String name = itemBlockBlock.getClass().getSimpleName().substring("Block".length());
                    wrapper.setRegistryName(InterfaceManager.coreModID + ":" + name);
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
            String name = collisionBlock.getClass().getSimpleName().substring("Block".length()) + i;
            wrapper.setRegistryName(InterfaceManager.coreModID + ":" + name);
            BuilderBlock.BLOCKS.register(name, () -> wrapper);
            BuilderBlock.blockMap.put(collisionBlock, wrapper);
        }

        //Register the TEs.  Has to be done last to ensure block maps are populated.
        BuilderBlock[] blockArray = BuilderBlock.blockMap.values().toArray(new BuilderBlock[0]);
        BuilderTileEntity.TE_TYPE = BuilderTileEntity.TILE_ENTITIES.register("builder_base", () -> TileEntityType.Builder.of(BuilderTileEntity::new, blockArray).build(null)).get();
        BuilderTileEntityInventoryContainer.TE_TYPE2 = BuilderTileEntity.TILE_ENTITIES.register("builder_inventory", () -> TileEntityType.Builder.of(BuilderTileEntityInventoryContainer::new, blockArray).build(null)).get();

        BuilderTileEntityInventoryContainer.TE_TYPE2.register("builder_inventory", () -> TileEntityType.Builder.of(BuilderTileEntityInventoryContainer::new, blockArray).build(null));
        BuilderTileEntityFluidTank.TE_TYPE2.register("builder_fluid", () -> TileEntityType.Builder.of(BuilderTileEntityFluidTank::new, blockArray).build(null));
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        //Init networking interface.  This will register packets as well.
        InterfacePacket.init();

        if (event.getSide().isClient()) {
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
}
