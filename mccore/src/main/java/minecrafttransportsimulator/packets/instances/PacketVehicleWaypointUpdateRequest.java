package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.NavWaypoint;
import minecrafttransportsimulator.baseclasses.NavWaypointUpdater;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Request waypoint selection update from client to server for vehicle
 */
public class PacketVehicleWaypointUpdateRequest extends APacketEntity<EntityVehicleF_Physics> {
    private final String operation;
    private final String opIndex;
    private final String index;
    private final String name;
    private final String targetSpeed;
    private final String bearing;
    private final String StrX;
    private final String StrY;
    private final String StrZ;

    public PacketVehicleWaypointUpdateRequest(EntityVehicleF_Physics vehicle, String operation,String opIndex, String index, String name, String targetSpeed, String bearing, String StrX, String StrY, String StrZ) {
        super(vehicle);
        this.operation = operation;
        this.opIndex = opIndex;
        this.index = index;
        this.name = name;
        this.targetSpeed = targetSpeed;
        this.bearing = bearing;
        this.StrX = StrX;
        this.StrY = StrY;
        this.StrZ = StrZ;
    }

    public PacketVehicleWaypointUpdateRequest(ByteBuf buf) {
        super(buf);
        this.operation = readStringFromBuffer(buf);
        this.opIndex = readStringFromBuffer(buf);
        this.index = readStringFromBuffer(buf);
        this.name = readStringFromBuffer(buf);
        this.targetSpeed = readStringFromBuffer(buf);
        this.bearing = readStringFromBuffer(buf);
        this.StrX = readStringFromBuffer(buf);
        this.StrY = readStringFromBuffer(buf);
        this.StrZ = readStringFromBuffer(buf);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeStringToBuffer(operation, buf);
        writeStringToBuffer(opIndex, buf);
        writeStringToBuffer(index, buf);
        writeStringToBuffer(name, buf);
        writeStringToBuffer(targetSpeed, buf);
        writeStringToBuffer(bearing, buf);
        writeStringToBuffer(StrX, buf);
        writeStringToBuffer(StrY, buf);
        writeStringToBuffer(StrZ, buf);
    }

    @Override
    public boolean handle(AWrapperWorld world, EntityVehicleF_Physics vehicle) {

        if(vehicle.selectedWaypointList != null && vehicle.selectedWaypointList.size()>Integer.parseInt(opIndex)){
            InterfaceManager.packetInterface.sendToAllClients(new PacketVehicleWaypointUpdate(vehicle,operation,opIndex,index,name,targetSpeed,bearing,StrX,StrY,StrZ));
        }else{
            InterfaceManager.packetInterface.sendToAllClients(new PacketVehicleWaypointUpdate(vehicle,"REMOVE",opIndex,index,"null","0.0","0.0","0.0","0.0","0.0"));
        }

        return true;
    }
}
