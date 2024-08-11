package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.blocks.tileentities.instances.ATileEntityFuelPump;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet sent to pumps on clients to change what vehicle they are connected to.
 * Send just a normal packet to disconnect, and one with a vehicle to connect.
 *
 * @author don_bruce
 */
public class PacketTileEntityFuelPumpConnection extends APacketEntity<ATileEntityFuelPump> {
    private final UUID linkedID;

    public PacketTileEntityFuelPumpConnection(ATileEntityFuelPump pump) {
        super(pump);
        this.linkedID = null;
    }

    public PacketTileEntityFuelPumpConnection(ATileEntityFuelPump pump, EntityVehicleF_Physics connectedVehicle) {
        super(pump);
        this.linkedID = connectedVehicle.uniqueUUID;
    }

    public PacketTileEntityFuelPumpConnection(ByteBuf buf) {
        super(buf);
        this.linkedID = buf.readBoolean() ? readUUIDFromBuffer(buf) : null;
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        if (linkedID != null) {
            buf.writeBoolean(true);
            writeUUIDToBuffer(linkedID, buf);
        } else {
            buf.writeBoolean(false);
        }
    }

    @Override
    protected boolean handle(AWrapperWorld world, ATileEntityFuelPump pump) {
        if (linkedID != null) {
            EntityVehicleF_Physics vehicle = world.getEntity(linkedID);
            if (vehicle != null) {
                pump.setConnection(vehicle);
            }
        } else {
            pump.setConnection(null);
        }
        return true;
    }
}
