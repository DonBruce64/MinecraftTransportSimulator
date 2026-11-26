package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.TowingConnection;
import minecrafttransportsimulator.entities.components.AEntityG_Towable;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
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
    private final MountedDisconnectState mountedDisconnect;

    public PacketEntityTowingChange(AEntityG_Towable<?> hitchEntity, TowingConnection connection) {
        super(hitchEntity);
        this.connectionIndex = -1;
        this.connectionData = connection.save(InterfaceManager.coreInterface.getNewNBTWrapper());
        this.mountedDisconnect = null;
    }

    public PacketEntityTowingChange(AEntityG_Towable<?> hitchEntity, int connectionIndex, TowingConnection connection) {
        super(hitchEntity);
        this.connectionIndex = connectionIndex;
        this.connectionData = null;
        this.mountedDisconnect = connection.hitchConnection.mounted ? new MountedDisconnectState(connection.towedVehicle) : null;
    }

    public PacketEntityTowingChange(ByteBuf buf) {
        super(buf);
        this.connectionIndex = buf.readInt();
        this.connectionData = connectionIndex == -1 ? readDataFromBuffer(buf) : null;
        this.mountedDisconnect = connectionIndex >= 0 && buf.isReadable() ? new MountedDisconnectState(buf) : null;
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeInt(connectionIndex);
        if (connectionData != null) {
            writeDataToBuffer(connectionData, buf);
        } else if (mountedDisconnect != null) {
            mountedDisconnect.writeToBuffer(buf);
        }
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityG_Towable<?> hitchEntity) {
        if (!world.isClient()) {
            return false;
        }
        if (connectionIndex == -1) {
            TowingConnection connection = new TowingConnection(connectionData);
            if (!connection.initConnection(world, false)) {
                return false;
            }
            hitchEntity.connectTrailer(connection, false);
        } else if (mountedDisconnect != null) {
            //Use the released vehicle's ID in case the connections loaded in a different order.
            for (int i = 0; i < hitchEntity.towingConnections.size(); ++i) {
                TowingConnection connection = hitchEntity.towingConnections.get(i);
                if (connection.hitchConnection.mounted && mountedDisconnect.vehicleID.equals(connection.towedVehicle.uniqueUUID)) {
                    connection.towedVehicle.applyMountedDisconnectState(mountedDisconnect.position, mountedDisconnect.angles, mountedDisconnect.motion);
                    hitchEntity.disconnectTrailer(i);
                    return true;
                }
            }
            return false;
        } else if (connectionIndex >= 0 && connectionIndex < hitchEntity.towingConnections.size()) {
            hitchEntity.disconnectTrailer(connectionIndex);
        } else {
            return false;
        }
        return true;
    }

    private static final class MountedDisconnectState {
        private final UUID vehicleID;
        private final Point3D position;
        private final Point3D angles;
        private final Point3D motion;

        private MountedDisconnectState(EntityVehicleF_Physics vehicle) {
            this.vehicleID = vehicle.uniqueUUID;
            this.position = vehicle.position.copy();
            this.angles = vehicle.orientation.angles.copy();
            this.motion = vehicle.motion.copy();
        }

        private MountedDisconnectState(ByteBuf buf) {
            this.vehicleID = readUUIDFromBuffer(buf);
            this.position = readPoint3dFromBuffer(buf);
            this.angles = readPoint3dFromBuffer(buf);
            this.motion = readPoint3dFromBuffer(buf);
        }

        private void writeToBuffer(ByteBuf buf) {
            writeUUIDToBuffer(vehicleID, buf);
            writePoint3dToBuffer(position, buf);
            writePoint3dToBuffer(angles, buf);
            writePoint3dToBuffer(motion, buf);
        }
    }
}
