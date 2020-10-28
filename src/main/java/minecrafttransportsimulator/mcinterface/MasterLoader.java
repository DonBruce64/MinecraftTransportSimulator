package minecrafttransportsimulator.mcinterface;

import java.io.File;
import java.io.InputStreamReader;

import mcinterface1122.MasterInterface;
import minecrafttransportsimulator.items.instances.ItemFuelHose;
import minecrafttransportsimulator.items.instances.ItemJerrycan;
import minecrafttransportsimulator.items.instances.ItemJumperCable;
import minecrafttransportsimulator.items.instances.ItemKey;
import minecrafttransportsimulator.items.instances.ItemPaintGun;
import minecrafttransportsimulator.items.instances.ItemTicket;
import minecrafttransportsimulator.items.instances.ItemWrench;
import minecrafttransportsimulator.jsondefs.JSONPack;
import minecrafttransportsimulator.packets.instances.PacketBulletHit;
import minecrafttransportsimulator.packets.instances.PacketEntityRiderChange;
import minecrafttransportsimulator.packets.instances.PacketFluidTankChange;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketPlayerCraftItem;
import minecrafttransportsimulator.packets.instances.PacketRadioStateChange;
import minecrafttransportsimulator.packets.instances.PacketTileEntityFluidLoaderConnection;
import minecrafttransportsimulator.packets.instances.PacketTileEntityFuelPumpConnection;
import minecrafttransportsimulator.packets.instances.PacketTileEntityPoleChange;
import minecrafttransportsimulator.packets.instances.PacketTileEntityRoadChange;
import minecrafttransportsimulator.packets.instances.PacketTileEntitySignalControllerChange;
import minecrafttransportsimulator.packets.instances.PacketVehicleColorChange;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlAnalog;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlDigital;
import minecrafttransportsimulator.packets.instances.PacketVehicleInstruments;
import minecrafttransportsimulator.packets.instances.PacketVehicleInteract;
import minecrafttransportsimulator.packets.instances.PacketVehicleLightToggle;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartChange;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartEngine;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartGroundDevice;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartGun;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartInteractable;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartSeat;
import minecrafttransportsimulator.packets.instances.PacketVehicleServerMovement;
import minecrafttransportsimulator.packets.instances.PacketVehicleTextChange;
import minecrafttransportsimulator.packets.instances.PacketVehicleTrailerChange;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.PackParserSystem;

/**Master loader for the mod.  This class is not an interface, unlike everything else.
 * Instead, it keeps references to all interfaces, which are passed-in during construction.
 * It also handles initialization calls when the game is first booted.  There will only
 * be ONE loader per running instance of Minecraft.
 *
 * @author don_bruce
 */
public class MasterLoader{
	public static final String MODID = "mts";
	public static final String MODNAME = "Minecraft Transport Simulator";
	public static final String MODVER = "19.8.0-BETA4";
	
	public static String resourceDomain;
	public static IInterfaceAudio audioInterface;
	public static IInterfaceCore coreInterface;
	public static IInterfaceGame gameInterface;
	public static IInterfaceGUI guiInterface;
	public static IInterfaceInput inputInterface;
	public static IInterfaceNetwork networkInterface;
	public static IInterfaceOGGDecoder oggDecoderInterface;
	public static IInterfaceRender renderInterface;
	public static String gameDirectory;
	
	/**
	 *  Called to set interfaces.  This needs to be done before ANY logic is executed.
	 */
	public static void setInterfaces(String resourceDomain, IInterfaceAudio audioInterface, IInterfaceCore coreInterface, IInterfaceGame gameInterface, IInterfaceGUI guiInterface, IInterfaceInput inputInterface, IInterfaceNetwork networkInterface, IInterfaceOGGDecoder oggDecoderInterface, IInterfaceRender renderInterface){
		//Set interfaces.
		MasterLoader.resourceDomain = resourceDomain;
		MasterLoader.audioInterface = audioInterface;
		MasterLoader.coreInterface = coreInterface;
		MasterLoader.gameInterface = gameInterface;
		MasterLoader.guiInterface = guiInterface;
		MasterLoader.inputInterface = inputInterface;
		MasterLoader.networkInterface = networkInterface;
		MasterLoader.oggDecoderInterface = oggDecoderInterface;
		MasterLoader.renderInterface = renderInterface;
		
		//Create main items.
		MasterInterface.createItem(new ItemWrench());
		MasterInterface.createItem(new ItemKey());
		MasterInterface.createItem(new ItemFuelHose());
		MasterInterface.createItem(new ItemJumperCable());
		MasterInterface.createItem(new ItemJerrycan());
		MasterInterface.createItem(new ItemPaintGun());
		MasterInterface.createItem(new ItemTicket());
		
		//Manually create the internal core mod pack items.
		//These need to be created before we do checks for block registration.
		//If we don't, then we risk not creating and registering the blocks.
		try{
			JSONPack packDef = new JSONPack();
			packDef.internallyGenerated = true;
			packDef.packID = resourceDomain;
			packDef.fileStructure = 0;
			packDef.packName = MasterLoader.coreInterface.getModName(resourceDomain);
			PackParserSystem.packMap.put(resourceDomain, packDef);
			
			PackParserSystem.addBookletDefinition(new InputStreamReader(MasterLoader.class.getResourceAsStream("/assets/" + resourceDomain + "/jsondefs/booklets/handbook_en.json"), "UTF-8"), "handbook_en", resourceDomain);
			PackParserSystem.addBookletDefinition(new InputStreamReader(MasterLoader.class.getResourceAsStream("/assets/" + resourceDomain + "/jsondefs/booklets/handbook_ru.json"), "UTF-8"), "handbook_ru", resourceDomain);
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
	
	/**
	 *  Called when directories are ready.  This sets the main game directory
	 *  and loads any packs by checking for valid pack JSON files.
	 */
	public static void setMainDirectory(String directory){
		gameDirectory = directory;
		ConfigSystem.loadFromDisk(new File(directory, "config"));
		File modDirectory = new File(directory, "mods");
		if(modDirectory.exists()){
			//Check main directory for packs.
			for(File file : modDirectory.listFiles()){
				if(file.getName().endsWith(".jar")){
					PackParserSystem.checkJarForPacks(file);
				}
			}
			
			//Check version-specific directory for packs.
			modDirectory = new File(modDirectory, coreInterface.getGameVersion());
			if(modDirectory.exists()){
				for(File file : modDirectory.listFiles()){
					if(file.getName().endsWith(".jar")){
						PackParserSystem.checkJarForPacks(file);
					}
				}
			}
		}else{
			coreInterface.logError("ERROR: Could not find mods directory!  Game directory is confirmed to: " + gameDirectory);
		}
		PackParserSystem.parseAllPacks();
	}
	
	/**
	 *  Called to register all packets with the network interface.
	 *  Packets should not register themselves before this point, as the
	 *  interface may not be ready at that time.  Passed-in index will be
	 *  offset to account for internal packets, so use it for the first registration.
	 */
	public static void registerPackets(byte packetIndex){
		//Register all classes in the minecrafttransportsimulator.packets.instances package.
		//Ideally this could be done via reflection, but it doesn't work too well so we don't do that.
		networkInterface.registerPacket(packetIndex++, PacketBulletHit.class);
		networkInterface.registerPacket(packetIndex++, PacketEntityRiderChange.class);
		networkInterface.registerPacket(packetIndex++, PacketFluidTankChange.class);
		networkInterface.registerPacket(packetIndex++, PacketPlayerChatMessage.class);
		networkInterface.registerPacket(packetIndex++, PacketPlayerCraftItem.class);
		networkInterface.registerPacket(packetIndex++, PacketRadioStateChange.class);
		networkInterface.registerPacket(packetIndex++, PacketTileEntityFluidLoaderConnection.class);
		networkInterface.registerPacket(packetIndex++, PacketTileEntityFuelPumpConnection.class);
		networkInterface.registerPacket(packetIndex++, PacketTileEntityPoleChange.class);
		networkInterface.registerPacket(packetIndex++, PacketTileEntityRoadChange.class);
		networkInterface.registerPacket(packetIndex++, PacketTileEntitySignalControllerChange.class);
		networkInterface.registerPacket(packetIndex++, PacketVehicleColorChange.class);
		networkInterface.registerPacket(packetIndex++, PacketVehicleControlAnalog.class);
		networkInterface.registerPacket(packetIndex++, PacketVehicleControlDigital.class);
		networkInterface.registerPacket(packetIndex++, PacketVehicleInstruments.class);
		networkInterface.registerPacket(packetIndex++, PacketVehicleInteract.class);
		networkInterface.registerPacket(packetIndex++, PacketVehicleLightToggle.class);
		networkInterface.registerPacket(packetIndex++, PacketVehiclePartChange.class);
		networkInterface.registerPacket(packetIndex++, PacketVehiclePartEngine.class);
		networkInterface.registerPacket(packetIndex++, PacketVehiclePartGroundDevice.class);
		networkInterface.registerPacket(packetIndex++, PacketVehiclePartGun.class);
		networkInterface.registerPacket(packetIndex++, PacketVehiclePartInteractable.class);
		networkInterface.registerPacket(packetIndex++, PacketVehiclePartSeat.class);
		networkInterface.registerPacket(packetIndex++, PacketVehicleServerMovement.class);
		networkInterface.registerPacket(packetIndex++, PacketVehicleTextChange.class);
		networkInterface.registerPacket(packetIndex++, PacketVehicleTrailerChange.class);
	}
}
