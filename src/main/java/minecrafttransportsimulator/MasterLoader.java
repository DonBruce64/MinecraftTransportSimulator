package minecrafttransportsimulator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;

import minecrafttransportsimulator.items.instances.ItemPartEffector;
import minecrafttransportsimulator.items.instances.ItemPartEngine;
import minecrafttransportsimulator.items.instances.ItemPartGeneric;
import minecrafttransportsimulator.items.instances.ItemPartGroundDevice;
import minecrafttransportsimulator.items.instances.ItemPartGun;
import minecrafttransportsimulator.items.instances.ItemPartInteractable;
import minecrafttransportsimulator.items.instances.ItemPartPropeller;
import minecrafttransportsimulator.items.instances.ItemPartSeat;
import minecrafttransportsimulator.mcinterface.InterfaceChunkloader;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.InterfaceInput;
import minecrafttransportsimulator.mcinterface.InterfacePacket;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**Master loader for the mod.  This class is not an interface, unlike everything else.
 * Instead, it keeps references to all interfaces, which are passed-in during construction.
 * It also handles initialization calls when the game is first booted.  There will only
 * be ONE loader per running instance of Minecraft.
 *
 * @author don_bruce
 */
@Mod(modid = MasterLoader.MODID, name = MasterLoader.MODNAME, version = MasterLoader.MODVER)
public class MasterLoader{
	public static final String MODID = "mts";
	public static final String MODNAME = "Minecraft Transport Simulator";
	public static final String MODVER = "21.0.0-BETA60";
	public static Logger logger;
	public static String resourceDomain;
	public static String gameDirectory;
	
	@Instance(MODID)
	public static MasterLoader INSTANCE;
	
	static{
		//Enable universal bucket so we can use buckets on fuel pumps.
		FluidRegistry.enableUniversalBucket();
		
		//Set main resource domain location.
		resourceDomain = MODID;
		
    	//Add part constructors to the part map.
    	PackParserSystem.addItemPartCreator(ItemPartEffector.CREATOR);
    	PackParserSystem.addItemPartCreator(ItemPartEngine.CREATOR);
    	PackParserSystem.addItemPartCreator(ItemPartGeneric.CREATOR);
    	PackParserSystem.addItemPartCreator(ItemPartGroundDevice.CREATOR);
    	PackParserSystem.addItemPartCreator(ItemPartGun.CREATOR);
    	PackParserSystem.addItemPartCreator(ItemPartInteractable.CREATOR);
    	PackParserSystem.addItemPartCreator(ItemPartPropeller.CREATOR);
    	PackParserSystem.addItemPartCreator(ItemPartSeat.CREATOR);
	}
	
	@EventHandler
	public void preInit(FMLPreInitializationEvent event){
		//Set logger and add log items from pre-boot operations.
		logger = event.getModLog();
		InterfaceCore.flushLogQueue();
		InterfaceCore.logError("Welcome to MTS VERSION:" + MODVER);
		
		//Set main mod directory and parse packs.
		gameDirectory = event.getModConfigurationDirectory().getParent();
		ConfigSystem.loadFromDisk(new File(gameDirectory, "config"));
		List<File> packDirectories = new ArrayList<File>();
		File modDirectory = new File(gameDirectory, "mods");
		if(modDirectory.exists()){
			packDirectories.add(modDirectory);
			
			//Also add version-specific directory.
			File versionedModDirectory = new File(modDirectory, InterfaceCore.getGameVersion());
			if(versionedModDirectory.exists()){
				packDirectories.add(versionedModDirectory);
			}
			
			//Parse the packs.
			PackParserSystem.addDefaultItems();
			PackParserSystem.parsePacks(packDirectories);
		}else{
			InterfaceCore.logError("Could not find mods directory!  Game directory is confirmed to: " + gameDirectory);
		}
	}
	
	@EventHandler
	public void init(FMLInitializationEvent event){
		//Init networking interface.  This will register packets as well.
		InterfacePacket.init();
		
		//Put all liquids into the config file for use by modpack makers.
		ConfigSystem.configObject.fuel.lastLoadedFluids = InterfaceCore.getAllFluids();
		ConfigSystem.saveToDisk();
		
		//Init chunkloader system.
		ForgeChunkManager.setForcedChunkLoadingCallback(INSTANCE, InterfaceChunkloader.INSTANCE);
		
		//Init keybinds if we're on the client.
		if(event.getSide().isClient()){
			InterfaceInput.initConfigKey();
		}
	}
}
