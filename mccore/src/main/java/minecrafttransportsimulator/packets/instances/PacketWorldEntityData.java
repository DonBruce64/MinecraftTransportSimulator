package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.components.APacketBase;

/**
 * Packet sent to client worlds with data to create entities in the world.
 * Only sent for tracked entities like vehicles and inventories.  Not sent
 * for untracked entities like particles.
 *
 * @author don_bruce
 */
public class PacketWorldEntityData extends APacketBase {
    private final IWrapperNBT data;

    public PacketWorldEntityData(AEntityD_Definable<?> entity) {
        super(null);
        this.data = entity.save(InterfaceManager.coreInterface.getNewNBTWrapper());
    }

    public PacketWorldEntityData(ByteBuf buf) {
        super(buf);
        this.data = readDataFromBuffer(buf);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeDataToBuffer(data, buf);
    }

    @Override
    public void handle(AWrapperWorld world) {
        world.addEntityByData(data);
    }

    @Override
    public boolean runOnMainThread() {
        //We can run this on networking thread since entity lists are thread-safe.
        return false;
    }
}
