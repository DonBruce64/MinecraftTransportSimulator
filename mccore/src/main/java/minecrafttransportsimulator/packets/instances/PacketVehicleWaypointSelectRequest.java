package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.components.APacketEntity;

public class PacketVehicleWaypointSelectRequest extends APacketEntity<EntityVehicleF_Physics> {
    private final String waypointIndex;

    public PacketVehicleWaypointSelectRequest(EntityVehicleF_Physics entity, String waypointIndex) {
        super(entity);
        this.waypointIndex = waypointIndex;
    }

    public PacketVehicleWaypointSelectRequest(ByteBuf buf) {
        super(buf);
        this.waypointIndex = readStringFromBuffer(buf);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeStringToBuffer(waypointIndex, buf);
    }

    @Override
    protected boolean handle(AWrapperWorld world, EntityVehicleF_Physics entity) {
        if (!world.isClient()) {
            entity.selectedWaypointIndex = waypointIndex;
            InterfaceManager.packetInterface.sendToAllClients(new PacketVehicleWaypointSelect(entity, waypointIndex));
        }
        return true;
    }
}
