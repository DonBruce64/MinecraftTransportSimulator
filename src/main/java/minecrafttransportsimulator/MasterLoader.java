package minecrafttransportsimulator;

import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.Logger;

import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.instances.ItemPartEffector;
import minecrafttransportsimulator.items.instances.ItemPartEngine;
import minecrafttransportsimulator.items.instances.ItemPartGeneric;
import minecrafttransportsimulator.items.instances.ItemPartGroundDevice;
import minecrafttransportsimulator.items.instances.ItemPartGun;
import minecrafttransportsimulator.items.instances.ItemPartInteractable;
import minecrafttransportsimulator.items.instances.ItemPartPropeller;
import minecrafttransportsimulator.items.instances.ItemPartSeat;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.jsondefs.JSONPack;
import minecrafttransportsimulator.mcinterface.BuilderItem;
import minecrafttransportsimulator.mcinterface.InterfaceChunkloader;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packloading.JSONParser;
import minecrafttransportsimulator.packloading.PackResourceLoader.ItemClassification;
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
	public static final String MODVER = "20.0.0-BETA37";
	
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
		
    	//Add part constructors to the part map.
		//TODO move to pack parser main init call when old loadrs are gone.
    	PackParserSystem.addItemPartCreator(ItemPartEffector.CREATOR);
    	PackParserSystem.addItemPartCreator(ItemPartEngine.CREATOR);
    	PackParserSystem.addItemPartCreator(ItemPartGeneric.CREATOR);
    	PackParserSystem.addItemPartCreator(ItemPartGroundDevice.CREATOR);
    	PackParserSystem.addItemPartCreator(ItemPartGun.CREATOR);
    	PackParserSystem.addItemPartCreator(ItemPartInteractable.CREATOR);
    	PackParserSystem.addItemPartCreator(ItemPartPropeller.CREATOR);
    	PackParserSystem.addItemPartCreator(ItemPartSeat.CREATOR);
		
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
			
			Map<String, ItemClassification> defaultItems = new HashMap<String, ItemClassification>();
			defaultItems.put("fuelhose", ItemClassification.ITEM);
			defaultItems.put("handbook_en", ItemClassification.ITEM);
			defaultItems.put("handbook_ru", ItemClassification.ITEM);
			defaultItems.put("jumpercable", ItemClassification.ITEM);
			defaultItems.put("jumperpack", ItemClassification.ITEM);
			defaultItems.put("key", ItemClassification.ITEM);
			defaultItems.put("jumperpack", ItemClassification.ITEM);
			defaultItems.put("paintgun", ItemClassification.ITEM);
			defaultItems.put("partscanner", ItemClassification.ITEM);
			defaultItems.put("ticket", ItemClassification.ITEM);
			defaultItems.put("wrench", ItemClassification.ITEM);
			defaultItems.put("y2kbutton", ItemClassification.ITEM);
			defaultItems.put("jerrycan", ItemClassification.PART);
			defaultItems.put("fuelpump", ItemClassification.DECOR);
			defaultItems.put("vehiclebench", ItemClassification.DECOR);
			defaultItems.put("enginebench", ItemClassification.DECOR);
			defaultItems.put("propellerbench", ItemClassification.DECOR);
			defaultItems.put("wheelbench", ItemClassification.DECOR);
			defaultItems.put("seatbench", ItemClassification.DECOR);
			defaultItems.put("gunbench", ItemClassification.DECOR);
			defaultItems.put("custombench", ItemClassification.DECOR);
			defaultItems.put("instrumentbench", ItemClassification.DECOR);
			defaultItems.put("decorbench", ItemClassification.DECOR);
			defaultItems.put("itembench", ItemClassification.DECOR);
			
			String prefixFolders = "/assets/" + resourceDomain + "/jsondefs/";
			for(Entry<String, ItemClassification> defaultItem : defaultItems.entrySet()){
				String systemName = defaultItem.getKey();
				ItemClassification classification = defaultItem.getValue();
				InputStreamReader reader = new InputStreamReader(MasterLoader.class.getResourceAsStream(prefixFolders + classification.toDirectory() + systemName + ".json"), "UTF-8");
				AJSONItem itemDef = JSONParser.parseStream(reader, classification.representingClass, packDef.packID, systemName);
				itemDef.packID = packDef.packID;
				itemDef.systemName = systemName;
				itemDef.classification = classification;
				itemDef.prefixFolders = prefixFolders;
				PackParserSystem.registerItem(itemDef);
			}
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
		
		//Put all liquids into the config file for use by modpack makers.
		ConfigSystem.configObject.fuel.lastLoadedFluids = InterfaceCore.getAllFluids();
		ConfigSystem.saveToDisk();
		
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
