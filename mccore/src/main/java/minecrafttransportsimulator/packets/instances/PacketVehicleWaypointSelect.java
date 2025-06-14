package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

public class PacketVehicleWaypointSelect extends APacketEntity<EntityVehicleF_Physics> {
    private final String waypointIndex;

    public PacketVehicleWaypointSelect(EntityVehicleF_Physics entity, String waypointIndex) {
        super(entity);
        this.waypointIndex = waypointIndex;
    }

    public PacketVehicleWaypointSelect(ByteBuf buf) {
        super(buf);
        this.waypointIndex = readStringFromBuffer(buf);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeStringToBuffer(waypointIndex, buf);
    }

    @Override
    public boolean handle(AWrapperWorld world, EntityVehicleF_Physics vehicle) {
        vehicle.selectedWaypointIndex = waypointIndex;
        return true;
    }
}
