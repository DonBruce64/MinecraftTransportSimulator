package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.NavWaypoint;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * 
 */
public class PacketVehicleWaypointChange extends APacketEntity<EntityVehicleF_Physics> {
    private final String WaypointName;

    public PacketVehicleWaypointChange(EntityVehicleF_Physics vehicle, String waypointName) {
        super(vehicle);
        this.WaypointName = waypointName;
    }

    public PacketVehicleWaypointChange(ByteBuf buf) {
        super(buf);
        this.WaypointName = readStringFromBuffer(buf);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeStringToBuffer(WaypointName, buf);
    }

    @Override
    public boolean handle(AWrapperWorld world, EntityVehicleF_Physics vehicle) {
        vehicle.selectedWaypointName = WaypointName;
        vehicle.selectedWaypoint = NavWaypoint.getByNameFromWorld(world, WaypointName);
        return true;
    }
}
