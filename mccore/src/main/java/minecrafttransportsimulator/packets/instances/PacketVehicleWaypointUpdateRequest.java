package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Request waypoint selection update from client to server for vehicle
 */
public class PacketVehicleWaypointUpdateRequest extends APacketEntity<EntityVehicleF_Physics> {
    public PacketVehicleWaypointUpdateRequest(EntityVehicleF_Physics vehicle) {
        super(vehicle);
    }

    public PacketVehicleWaypointUpdateRequest(ByteBuf buf) {
        super(buf);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
    }

    @Override
    public boolean handle(AWrapperWorld world, EntityVehicleF_Physics vehicle) {

//        InterfaceManager.packetInterface.sendToPlayer(new PacketVehicleWaypointUpdate(operation,index,name,targetSpeed,bearing,StrX,StrY,StrZ));
        return true;
    }
}
