package minecrafttransportsimulator.mcinterface;

import minecrafttransportsimulator.items.instances.ItemPartEffector;
import minecrafttransportsimulator.items.instances.ItemPartEngine;
import minecrafttransportsimulator.items.instances.ItemPartGeneric;
import minecrafttransportsimulator.items.instances.ItemPartGroundDevice;
import minecrafttransportsimulator.items.instances.ItemPartGun;
import minecrafttransportsimulator.items.instances.ItemPartInteractable;
import minecrafttransportsimulator.items.instances.ItemPartPropeller;
import minecrafttransportsimulator.items.instances.ItemPartSeat;
import minecrafttransportsimulator.packloading.PackParser;

/**
 * Master interface system for the mod.  This class is not an interface, unlike everything else.
 * Instead, it keeps references to all interfaces, which are passed-in during construction.
 *
 * @author don_bruce
 */
public class InterfaceManager {
    public static String coreModID;
    public static String gameDirectory;
    public static IInterfaceCore coreInterface;
    public static IInterfacePacket packetInterface;
    public static IInterfaceClient clientInterface;
    public static IInterfaceInput inputInterface;
    public static IInterfaceSound soundInterface;
    public static IInterfaceRender renderingInterface;
    
    public InterfaceManager(String coreModID, String gameDirectory, IInterfaceCore coreInterface, IInterfacePacket packetInterface, IInterfaceClient clientInterface, IInterfaceInput inputInterface, IInterfaceSound soundInterface, IInterfaceRender renderingInterface) {
        InterfaceManager.coreModID = coreModID;
        InterfaceManager.gameDirectory = gameDirectory;
        InterfaceManager.coreInterface = coreInterface;
        InterfaceManager.packetInterface = packetInterface;
        InterfaceManager.clientInterface = clientInterface;
        InterfaceManager.inputInterface = inputInterface;
        InterfaceManager.soundInterface = soundInterface;
        InterfaceManager.renderingInterface = renderingInterface;
    }

    static {
        //Add part constructors to the part map.
        PackParser.addItemPartCreator(ItemPartEffector.CREATOR);
        PackParser.addItemPartCreator(ItemPartEngine.CREATOR);
        PackParser.addItemPartCreator(ItemPartGeneric.CREATOR);
        PackParser.addItemPartCreator(ItemPartGroundDevice.CREATOR);
        PackParser.addItemPartCreator(ItemPartGun.CREATOR);
        PackParser.addItemPartCreator(ItemPartInteractable.CREATOR);
        PackParser.addItemPartCreator(ItemPartPropeller.CREATOR);
        PackParser.addItemPartCreator(ItemPartSeat.CREATOR);
    }
}
