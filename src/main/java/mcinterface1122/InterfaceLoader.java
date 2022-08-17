package mcinterface1122;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;

import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**Loader interface for the mod.  This class is not actually an interface, unlike everything else.
 * Instead, it keeps references to all interfaces, which are passed-in during construction.
 * It also handles initialization calls when the game is first booted.  There will only
 * be ONE loader per running instance of Minecraft.
 *
 * @author don_bruce
 */
@Mod(modid = InterfaceLoader.MODID, name = InterfaceLoader.MODNAME, version = InterfaceLoader.MODVER)
public class InterfaceLoader{
	public static final String MODID = "mts";
	public static final String MODNAME = "Immersive Vehicles (MTS)";
	public static final String MODVER = "21.4L.0-BETA10";
	public static Logger logger;
	
	@Instance(MODID)
	public static InterfaceLoader INSTANCE;
	
	static{
		//Enable universal bucket so we can use buckets on fuel pumps.
		FluidRegistry.enableUniversalBucket();
	}
	
	@EventHandler
	public void preInit(FMLPreInitializationEvent event){
		//Get game directory.
		String gameDirectory = event.getModConfigurationDirectory().getParent();

		//Init interfaces and send to the main game system.
		if(event.getSide().isClient()){
			new InterfaceManager(MODID, gameDirectory, new InterfaceCore(), new InterfacePacket(), new InterfaceClient(), new InterfaceInput(), new InterfaceSound(), new InterfaceRender());
		}else {
			new InterfaceManager(MODID, gameDirectory, new InterfaceCore(), new InterfacePacket(), null, null, null, null);
		}

		//Set logger and add log items from pre-boot operations.
		logger = event.getModLog();
		InterfaceManager.coreInterface.flushLogQueue();
		InterfaceManager.coreInterface.logError("Welcome to MTS VERSION:" + MODVER);
		
		//Parse packs now that we have a logger.
		ConfigSystem.loadFromDisk(new File(gameDirectory, "config"), event.getSide().isClient());
		List<File> packDirectories = new ArrayList<File>();
		File modDirectory = new File(gameDirectory, "mods");
		if(modDirectory.exists()){
			packDirectories.add(modDirectory);
			
			//Also add version-specific directory.
			File versionedModDirectory = new File(modDirectory, InterfaceManager.coreInterface.getGameVersion());
			if(versionedModDirectory.exists()){
				packDirectories.add(versionedModDirectory);
			}
			
			//Parse the packs.
			PackParser.addDefaultItems();
			PackParser.parsePacks(packDirectories, event.getSide().isClient());
		}else{
			InterfaceManager.coreInterface.logError("Could not find mods directory!  Game directory is confirmed to: " + gameDirectory);
		}
	}
	
	@EventHandler
	public void init(FMLInitializationEvent event){
		//Init networking interface.  This will register packets as well.
		InterfacePacket.init();
		
		
		if(event.getSide().isClient()){
			//Init keybinds if we're on the client.
			InterfaceManager.inputInterface.initConfigKey();
			
			//Also put all liquids into the config file for use by modpack makers.
			ConfigSystem.settings.fuel.lastLoadedFluids = InterfaceManager.clientInterface.getAllFluidNames();
			
			//Also diable playerTweaks if some known-problematic mods are present.
			if(InterfaceManager.coreInterface.isModPresent("tails") || InterfaceManager.coreInterface.isModPresent("obfuscate") || InterfaceManager.coreInterface.isModPresent("mobends")) {
				ConfigSystem.client.renderingSettings.playerTweaks.value = false;
			}
			
			//Save modified config.
			ConfigSystem.saveToDisk();
		}
	}
}
