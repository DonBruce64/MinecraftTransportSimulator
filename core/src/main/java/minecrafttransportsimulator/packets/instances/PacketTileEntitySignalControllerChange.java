package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet sent to signal controllers when the confirm button is clicked in the controller GUI to update their state.
 * This packet arrives on the server and sets the states of the controller as if it was loaded from NBT.  These states
 * are then loaded and sent to all clients to keep them in-sync.  While the NBT has some overhead due to saving non-essential
 * variables, it's far less work than the alternative of specifying the entire parameter list.
 *
 * @author don_bruce
 */
public class PacketTileEntitySignalControllerChange extends APacketEntity<TileEntitySignalController> {
    private final IWrapperNBT controllerData;

    public PacketTileEntitySignalControllerChange(TileEntitySignalController controller) {
        super(controller);
        this.controllerData = InterfaceManager.coreInterface.getNewNBTWrapper();
        controller.save(controllerData);
    }

    public PacketTileEntitySignalControllerChange(ByteBuf buf) {
        super(buf);
        this.controllerData = readDataFromBuffer(buf);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeDataToBuffer(controllerData, buf);
    }

    @Override
    protected boolean handle(AWrapperWorld world, TileEntitySignalController controller) {
        controller.initializeController(controllerData);
        return true;
    }
}
