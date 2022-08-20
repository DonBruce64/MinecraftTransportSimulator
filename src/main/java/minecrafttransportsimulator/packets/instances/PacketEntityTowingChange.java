package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.TowingConnection;
import minecrafttransportsimulator.entities.components.AEntityG_Towable;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet used to send signals to entities to connect/disconnect towing entities.  Sent from the server to all clients when
 * towing linking changes.
 *
 * @author don_bruce
 */
public class PacketEntityTowingChange extends APacketEntity<AEntityG_Towable<?>> {
    private final int connectionIndex;
    private final IWrapperNBT connectionData;

    public PacketEntityTowingChange(AEntityG_Towable<?> hitchEntity, TowingConnection connection) {
        super(hitchEntity);
        this.connectionIndex = -1;
        this.connectionData = connection.save(InterfaceManager.coreInterface.getNewNBTWrapper());
    }

    public PacketEntityTowingChange(AEntityG_Towable<?> hitchEntity, int connectionIndex) {
        super(hitchEntity);
        this.connectionIndex = connectionIndex;
        this.connectionData = null;
    }

    public PacketEntityTowingChange(ByteBuf buf) {
        super(buf);
        this.connectionIndex = buf.readInt();
        if (connectionIndex == -1) {
            this.connectionData = readDataFromBuffer(buf);
        } else {
            this.connectionData = null;
        }
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeInt(connectionIndex);
        if (connectionData != null) {
            writeDataToBuffer(connectionData, buf);
        }
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityG_Towable<?> hitchEntity) {
        if (connectionIndex == -1) {
            TowingConnection connection = new TowingConnection(connectionData);
            if (connection.initConnection(world)) {
                hitchEntity.connectTrailer(connection);
            } else {
                return false;
            }
        } else {
            hitchEntity.disconnectTrailer(connectionIndex);
        }
        return true;
    }
}
