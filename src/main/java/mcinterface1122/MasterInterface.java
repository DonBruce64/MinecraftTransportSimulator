package mcinterface1122;

import org.apache.logging.log4j.Logger;

import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = MasterInterface.MODID, name = MasterInterface.MODNAME, version = MasterInterface.MODVER)
public class MasterInterface{
	//Forge variables.
	static final String MODID = MasterLoader.MODID;
	static final String MODNAME = MasterLoader.MODNAME;
	static final String MODVER = MasterLoader.MODVER;
	
	//Variables common to interface systems
	static Logger logger;
	static final InterfaceAudio audioInterface;
	static final InterfaceCore coreInterface;
	static final InterfaceGame gameInterface;
	static final InterfaceGUI guiInterface;
	static final InterfaceInput inputInterface;
	static final InterfaceNetwork networkInterface;
	static final InterfaceOGGDecoder oggDecoderInterface;
	static final InterfaceRender renderInterface;
	
	static{
		//Enable universal bucket so we can use buckets on fuel pumps.
		FluidRegistry.enableUniversalBucket();
		
		//Create instances of the various interfaces and send them to the loader.
		//If we're on a server, don't create the client interfaces.
		if(FMLCommonHandler.instance().getSide().isClient()){
			audioInterface = new InterfaceAudio();
			coreInterface = new InterfaceCore();
			gameInterface = new InterfaceGame();
			guiInterface = new InterfaceGUI();
			inputInterface = new InterfaceInput();
			networkInterface = new InterfaceNetwork();
			oggDecoderInterface = new InterfaceOGGDecoder();
			renderInterface = new InterfaceRender();
		}else{
			audioInterface = null;
			coreInterface = new InterfaceCore();
			gameInterface = null;
			guiInterface = null;
			inputInterface = null;
			networkInterface = new InterfaceNetwork();
			oggDecoderInterface = null;
			renderInterface = null;
		}
		MasterLoader.setInterfaces(MODID, audioInterface, coreInterface, gameInterface, guiInterface, inputInterface, networkInterface, oggDecoderInterface, renderInterface);
	}

	@EventHandler
	public void preInit(FMLPreInitializationEvent event){
		//Set logger and add log items from pre-boot operations.
		logger = event.getModLog();
		coreInterface.flushLogQueue();
		
		//Load config file and set minecraft directory.
		MasterLoader.setMainDirectory(event.getModConfigurationDirectory().getParent());
	}
	
	@EventHandler
	public void init(FMLInitializationEvent event){
		//Init networking interface and fire off command to load packets.
		//Register our own packet for handshaking.
		networkInterface.init();
		byte packetIndex = 0;
		networkInterface.registerPacket(packetIndex++, PacketEntityCSHandshake.class);
		MasterLoader.registerPackets(packetIndex);
	}
	
	/**
	 *  Creates a wrapper for the the passed-in Item, saving the wrapper to be registered later.
	 *  This wrapper instance will interact with all MC code via passthrough of the item's methods.
	 */
	public static void createItem(AItemBase item){
		BuilderItem.itemWrapperMap.put(item, new BuilderItem(item));
		//TODO remove when packs don't register their own items.  Instead, auto-register items from pack creative tabs.
		if(item instanceof AItemPack){
			String packID = ((AItemPack<?>) item).definition.packID;
			String systemName = ((AItemPack<?>) item).definition.systemName;
			if(!packID.equals(MODID)){
				BuilderItem.itemWrapperMap.get(item).setUnlocalizedName(packID + "." + systemName);
			}
		}
	}
	
	/**
	 *  Gets the actual item for the passed-in wrapper from the lookup table.
	 *  Only present because packs still load their own items.
	 */
	public static BuilderItem getItem(AItemBase item){
		return BuilderItem.itemWrapperMap.get(item);
	}
}
