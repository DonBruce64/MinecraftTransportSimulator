package minecrafttransportsimulator;

import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;

import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.jsondefs.JSONPack;
import minecrafttransportsimulator.mcinterface.BuilderItem;
import minecrafttransportsimulator.mcinterface.InterfaceChunkloader;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.packets.components.InterfacePacket;
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
	public static final String MODVER = "19.15.5-BETA2";
	
	public static Logger logger;
	public static String resourceDomain;
	public static String gameDirectory;
	
	@Instance(MODID)
	public static MasterLoader INSTANCE;
	
	static{
		//Enable universal bucket so we can use buckets on fuel pumps.
		FluidRegistry.enableUniversalBucket();
		
		//Set main resource domain location..
		resourceDomain = MODID;
		
		//Manually create the internal core mod pack items.
		//These need to be created before we do checks for block registration.
		//If we don't, then we risk not creating and registering the blocks.
		try{
			JSONPack packDef = new JSONPack();
			packDef.internallyGenerated = true;
			packDef.packID = resourceDomain;
			packDef.fileStructure = 0;
			packDef.packName = InterfaceCore.getModName(MODID);
			packDef.packItem = "wrench";
			PackParserSystem.packMap.put(resourceDomain, packDef);
			
			PackParserSystem.addItemDefinition(new InputStreamReader(MasterLoader.class.getResourceAsStream("/assets/" + resourceDomain + "/jsondefs/items/fuelhose.json"), "UTF-8"), "fuelhose", resourceDomain);
			PackParserSystem.addItemDefinition(new InputStreamReader(MasterLoader.class.getResourceAsStream("/assets/" + resourceDomain + "/jsondefs/items/handbook_en.json"), "UTF-8"), "handbook_en", resourceDomain);
			PackParserSystem.addItemDefinition(new InputStreamReader(MasterLoader.class.getResourceAsStream("/assets/" + resourceDomain + "/jsondefs/items/handbook_ru.json"), "UTF-8"), "handbook_ru", resourceDomain);
			PackParserSystem.addItemDefinition(new InputStreamReader(MasterLoader.class.getResourceAsStream("/assets/" + resourceDomain + "/jsondefs/items/jumpercable.json"), "UTF-8"), "jumpercable", resourceDomain);
			PackParserSystem.addItemDefinition(new InputStreamReader(MasterLoader.class.getResourceAsStream("/assets/" + resourceDomain + "/jsondefs/items/jumperpack.json"), "UTF-8"), "jumperpack", resourceDomain);
			PackParserSystem.addItemDefinition(new InputStreamReader(MasterLoader.class.getResourceAsStream("/assets/" + resourceDomain + "/jsondefs/items/key.json"), "UTF-8"), "key", resourceDomain);
			PackParserSystem.addItemDefinition(new InputStreamReader(MasterLoader.class.getResourceAsStream("/assets/" + resourceDomain + "/jsondefs/items/paintgun.json"), "UTF-8"), "paintgun", resourceDomain);
			PackParserSystem.addItemDefinition(new InputStreamReader(MasterLoader.class.getResourceAsStream("/assets/" + resourceDomain + "/jsondefs/items/partscanner.json"), "UTF-8"), "partscanner", resourceDomain);
			PackParserSystem.addItemDefinition(new InputStreamReader(MasterLoader.class.getResourceAsStream("/assets/" + resourceDomain + "/jsondefs/items/ticket.json"), "UTF-8"), "ticket", resourceDomain);
			PackParserSystem.addItemDefinition(new InputStreamReader(MasterLoader.class.getResourceAsStream("/assets/" + resourceDomain + "/jsondefs/items/wrench.json"), "UTF-8"), "wrench", resourceDomain);
			PackParserSystem.addItemDefinition(new InputStreamReader(MasterLoader.class.getResourceAsStream("/assets/" + resourceDomain + "/jsondefs/items/y2kbutton.json"), "UTF-8"), "y2kbutton", resourceDomain);
			PackParserSystem.addPartDefinition(new InputStreamReader(MasterLoader.class.getResourceAsStream("/assets/" + resourceDomain + "/jsondefs/parts/jerrycan.json"), "UTF-8"), "jerrycan", resourceDomain);
			PackParserSystem.addDecorDefinition(new InputStreamReader(MasterLoader.class.getResourceAsStream("/assets/" + resourceDomain + "/jsondefs/decors/fuelpump.json"), "UTF-8"), "fuelpump", resourceDomain);
			PackParserSystem.addDecorDefinition(new InputStreamReader(MasterLoader.class.getResourceAsStream("/assets/" + resourceDomain + "/jsondefs/decors/vehiclebench.json"), "UTF-8"), "vehiclebench", resourceDomain);
			PackParserSystem.addDecorDefinition(new InputStreamReader(MasterLoader.class.getResourceAsStream("/assets/" + resourceDomain + "/jsondefs/decors/enginebench.json"), "UTF-8"), "enginebench", resourceDomain);
			PackParserSystem.addDecorDefinition(new InputStreamReader(MasterLoader.class.getResourceAsStream("/assets/" + resourceDomain + "/jsondefs/decors/propellerbench.json"), "UTF-8"), "propellerbench", resourceDomain);
			PackParserSystem.addDecorDefinition(new InputStreamReader(MasterLoader.class.getResourceAsStream("/assets/" + resourceDomain + "/jsondefs/decors/wheelbench.json"), "UTF-8"), "wheelbench", resourceDomain);
			PackParserSystem.addDecorDefinition(new InputStreamReader(MasterLoader.class.getResourceAsStream("/assets/" + resourceDomain + "/jsondefs/decors/seatbench.json"), "UTF-8"), "seatbench", resourceDomain);
			PackParserSystem.addDecorDefinition(new InputStreamReader(MasterLoader.class.getResourceAsStream("/assets/" + resourceDomain + "/jsondefs/decors/gunbench.json"), "UTF-8"), "gunbench", resourceDomain);
			PackParserSystem.addDecorDefinition(new InputStreamReader(MasterLoader.class.getResourceAsStream("/assets/" + resourceDomain + "/jsondefs/decors/custombench.json"), "UTF-8"), "custombench", resourceDomain);
			PackParserSystem.addDecorDefinition(new InputStreamReader(MasterLoader.class.getResourceAsStream("/assets/" + resourceDomain + "/jsondefs/decors/instrumentbench.json"), "UTF-8"), "instrumentbench", resourceDomain);
			PackParserSystem.addDecorDefinition(new InputStreamReader(MasterLoader.class.getResourceAsStream("/assets/" + resourceDomain + "/jsondefs/decors/decorbench.json"), "UTF-8"), "decorbench", resourceDomain);
			PackParserSystem.addDecorDefinition(new InputStreamReader(MasterLoader.class.getResourceAsStream("/assets/" + resourceDomain + "/jsondefs/decors/itembench.json"), "UTF-8"), "itembench", resourceDomain);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	@EventHandler
	public void preInit(FMLPreInitializationEvent event){
		//Set logger and add log items from pre-boot operations.
		logger = event.getModLog();
		InterfaceCore.flushLogQueue();
		
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
			PackParserSystem.parsePacks(packDirectories);
		}else{
			InterfaceCore.logError("Could not find mods directory!  Game directory is confirmed to: " + gameDirectory);
		}
	}
	
	@EventHandler
	public void init(FMLInitializationEvent event){
		//Init networking interface.  This will register packets as well.
		InterfacePacket.init();
		
		//Init chunkloader system.
		ForgeChunkManager.setForcedChunkLoadingCallback(INSTANCE, InterfaceChunkloader.INSTANCE);
	}
	
	/**
	 *  Creates a wrapper for the the passed-in Item, saving the wrapper to be registered later.
	 *  This wrapper instance will interact with all MC code via passthrough of the item's methods.
	 */
	public static void createItem(AItemBase item){
		BuilderItem builder = new BuilderItem(item);
		//TODO remove when packs don't register their own items.  Instead, auto-register items from pack creative tabs.
		if(item instanceof AItemPack){
			String packID = ((AItemPack<?>) item).definition.packID;
			if(PackParserSystem.getPackConfiguration(packID) == null){
				builder.setTranslationKey(packID + "." + item.getRegistrationName());
			}
		}
	}
}
