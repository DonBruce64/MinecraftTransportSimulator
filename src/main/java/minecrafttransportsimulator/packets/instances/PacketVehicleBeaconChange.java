package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.NavBeacon;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet sent to vehicles to update their selected beacons.  This is sent from clients when they
 * change the beacon parameters in the panel.  A callback packet is then sent from the server to
 * all clients to update their states.
 *
 * @author don_bruce
 */
public class PacketVehicleBeaconChange extends APacketEntity<EntityVehicleF_Physics> {
    private final String beaconName;

    public PacketVehicleBeaconChange(EntityVehicleF_Physics vehicle, String beaconName) {
        super(vehicle);
        this.beaconName = beaconName;
    }

    public PacketVehicleBeaconChange(ByteBuf buf) {
        super(buf);
        this.beaconName = readStringFromBuffer(buf);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeStringToBuffer(beaconName, buf);
    }

    @Override
    public boolean handle(AWrapperWorld world, EntityVehicleF_Physics vehicle) {
        vehicle.selectedBeaconName = beaconName;
        vehicle.selectedBeacon = NavBeacon.getByNameFromWorld(world, beaconName);
        return true;
    }
}
