package minecrafttransportsimulator.mcinterface;

import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import mcinterface1122.MasterInterface;
import minecrafttransportsimulator.items.instances.ItemFuelHose;
import minecrafttransportsimulator.items.instances.ItemJerrycan;
import minecrafttransportsimulator.items.instances.ItemJumperCable;
import minecrafttransportsimulator.items.instances.ItemKey;
import minecrafttransportsimulator.items.instances.ItemPaintGun;
import minecrafttransportsimulator.items.instances.ItemPartScanner;
import minecrafttransportsimulator.items.instances.ItemTicket;
import minecrafttransportsimulator.items.instances.ItemWrench;
import minecrafttransportsimulator.items.instances.ItemY2KButton;
import minecrafttransportsimulator.jsondefs.JSONPack;
import minecrafttransportsimulator.packets.components.NetworkSystem;
import minecrafttransportsimulator.packets.instances.PacketBeaconListingChange;
import minecrafttransportsimulator.packets.instances.PacketBulletHit;
import minecrafttransportsimulator.packets.instances.PacketEntityRiderChange;
import minecrafttransportsimulator.packets.instances.PacketFluidTankChange;
import minecrafttransportsimulator.packets.instances.PacketGunChange;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketPlayerCraftItem;
import minecrafttransportsimulator.packets.instances.PacketPlayerGunChange;
import minecrafttransportsimulator.packets.instances.PacketRadioStateChange;
import minecrafttransportsimulator.packets.instances.PacketTileEntityDecorColorChange;
import minecrafttransportsimulator.packets.instances.PacketTileEntityDecorTextChange;
import minecrafttransportsimulator.packets.instances.PacketTileEntityFluidLoaderConnection;
import minecrafttransportsimulator.packets.instances.PacketTileEntityFuelPumpConnection;
import minecrafttransportsimulator.packets.instances.PacketTileEntityPoleChange;
import minecrafttransportsimulator.packets.instances.PacketTileEntityRoadChange;
import minecrafttransportsimulator.packets.instances.PacketTileEntitySignalControllerChange;
import minecrafttransportsimulator.packets.instances.PacketVehicleBeaconChange;
import minecrafttransportsimulator.packets.instances.PacketVehicleColorChange;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlAnalog;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlDigital;
import minecrafttransportsimulator.packets.instances.PacketVehicleInstruments;
import minecrafttransportsimulator.packets.instances.PacketVehicleInteract;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartChange;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartEngine;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartGroundDevice;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartInteractable;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartSeat;
import minecrafttransportsimulator.packets.instances.PacketVehicleServerMovement;
import minecrafttransportsimulator.packets.instances.PacketVehicleTextChange;
import minecrafttransportsimulator.packets.instances.PacketVehicleTrailerChange;
import minecrafttransportsimulator.packets.instances.PacketVehicleVariableToggle;
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
	public static final String MODVER = "19.13.0-BETA7";
	
	public static String resourceDomain;
	public static IInterfaceCore coreInterface;
	public static IInterfaceClient clientInterface;
	public static IInterfaceGUI guiInterface;
	public static IInterfaceInput inputInterface;
	public static IInterfaceRender renderInterface;
	public static String gameDirectory;
	
	/**
	 *  Called to set interfaces.  This needs to be done before ANY logic is executed.
	 */
	public static void setInterfaces(String resourceDomain, IInterfaceCore coreInterface, IInterfaceClient clientInterface, IInterfaceGUI guiInterface, IInterfaceInput inputInterface, IInterfaceRender renderInterface){
		//Set interfaces.
		MasterLoader.resourceDomain = resourceDomain;
		MasterLoader.coreInterface = coreInterface;
		MasterLoader.clientInterface = clientInterface;
		MasterLoader.guiInterface = guiInterface;
		MasterLoader.inputInterface = inputInterface;
		MasterLoader.renderInterface = renderInterface;
		
		//Create main items.
		MasterInterface.createItem(new ItemWrench());
		MasterInterface.createItem(new ItemPartScanner());
		MasterInterface.createItem(new ItemKey());
		MasterInterface.createItem(new ItemFuelHose());
		MasterInterface.createItem(new ItemJumperCable());
		MasterInterface.createItem(new ItemJerrycan());
		MasterInterface.createItem(new ItemPaintGun());
		MasterInterface.createItem(new ItemTicket());
		MasterInterface.createItem(new ItemY2KButton());
		
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
			
			PackParserSystem.addItemDefinition(new InputStreamReader(MasterLoader.class.getResourceAsStream("/assets/" + resourceDomain + "/jsondefs/items/handbook_en.json"), "UTF-8"), "handbook_en", resourceDomain);
			PackParserSystem.addItemDefinition(new InputStreamReader(MasterLoader.class.getResourceAsStream("/assets/" + resourceDomain + "/jsondefs/items/handbook_ru.json"), "UTF-8"), "handbook_ru", resourceDomain);
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
		List<File> packDirectories = new ArrayList<File>();
		File modDirectory = new File(directory, "mods");
		if(modDirectory.exists()){
			packDirectories.add(modDirectory);
			
			//Also add version-specific directory.
			File versionedModDirectory = new File(modDirectory, coreInterface.getGameVersion());
			if(versionedModDirectory.exists()){
				packDirectories.add(versionedModDirectory);
			}
			
			//Parse the packs.
			PackParserSystem.parsePacks(packDirectories);
		}else{
			coreInterface.logError("ERROR: Could not find mods directory!  Game directory is confirmed to: " + gameDirectory);
		}
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
		NetworkSystem.registerPacket(packetIndex++, PacketBeaconListingChange.class);
		NetworkSystem.registerPacket(packetIndex++, PacketBulletHit.class);
		NetworkSystem.registerPacket(packetIndex++, PacketEntityRiderChange.class);
		NetworkSystem.registerPacket(packetIndex++, PacketFluidTankChange.class);
		NetworkSystem.registerPacket(packetIndex++, PacketGunChange.class);
		NetworkSystem.registerPacket(packetIndex++, PacketPlayerChatMessage.class);
		NetworkSystem.registerPacket(packetIndex++, PacketPlayerCraftItem.class);
		NetworkSystem.registerPacket(packetIndex++, PacketPlayerGunChange.class);
		NetworkSystem.registerPacket(packetIndex++, PacketRadioStateChange.class);
		NetworkSystem.registerPacket(packetIndex++, PacketTileEntityDecorColorChange.class);
		NetworkSystem.registerPacket(packetIndex++, PacketTileEntityDecorTextChange.class);
		NetworkSystem.registerPacket(packetIndex++, PacketTileEntityFluidLoaderConnection.class);
		NetworkSystem.registerPacket(packetIndex++, PacketTileEntityFuelPumpConnection.class);
		NetworkSystem.registerPacket(packetIndex++, PacketTileEntityPoleChange.class);
		NetworkSystem.registerPacket(packetIndex++, PacketTileEntityRoadChange.class);
		NetworkSystem.registerPacket(packetIndex++, PacketTileEntitySignalControllerChange.class);
		NetworkSystem.registerPacket(packetIndex++, PacketVehicleBeaconChange.class);
		NetworkSystem.registerPacket(packetIndex++, PacketVehicleColorChange.class);
		NetworkSystem.registerPacket(packetIndex++, PacketVehicleControlAnalog.class);
		NetworkSystem.registerPacket(packetIndex++, PacketVehicleControlDigital.class);
		NetworkSystem.registerPacket(packetIndex++, PacketVehicleInstruments.class);
		NetworkSystem.registerPacket(packetIndex++, PacketVehicleInteract.class);
		NetworkSystem.registerPacket(packetIndex++, PacketVehiclePartChange.class);
		NetworkSystem.registerPacket(packetIndex++, PacketVehiclePartEngine.class);
		NetworkSystem.registerPacket(packetIndex++, PacketVehiclePartGroundDevice.class);
		NetworkSystem.registerPacket(packetIndex++, PacketVehiclePartInteractable.class);
		NetworkSystem.registerPacket(packetIndex++, PacketVehiclePartSeat.class);
		NetworkSystem.registerPacket(packetIndex++, PacketVehicleServerMovement.class);
		NetworkSystem.registerPacket(packetIndex++, PacketVehicleTextChange.class);
		NetworkSystem.registerPacket(packetIndex++, PacketVehicleTrailerChange.class);
		NetworkSystem.registerPacket(packetIndex++, PacketVehicleVariableToggle.class);
	}
}
