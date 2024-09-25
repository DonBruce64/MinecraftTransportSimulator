package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.NavWaypoint;
import minecrafttransportsimulator.baseclasses.NavWaypointUpdater;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketBase;
/**
 * Update waypoint from client to server
 */
public class PacketWaypointUpdate extends APacketBase {


    public final String index;
    public final String name;
    public final String targetSpeed;
    public final String bearing;
    public final String positionX;
    public final String positionY;
    public final String positionZ;
    public final String isDeleted;

    public PacketWaypointUpdate(String waypointIndex,String waypointName, String targetSpeedStr, String bearingStr, String xStr, String yStr, String zStr, String Deleted) {
        super(null);
        this.index = waypointIndex;
        this.name = waypointName;
        this.targetSpeed = targetSpeedStr;
        this.bearing = bearingStr;
        this.positionX = xStr;
        this.positionY = yStr;
        this.positionZ = zStr;
        this.isDeleted = Deleted;
    }

    public PacketWaypointUpdate(ByteBuf buf) {
        super(buf);
        this.index = readStringFromBuffer(buf);
        this.name = readStringFromBuffer(buf);
        this.targetSpeed = readStringFromBuffer(buf);
        this.bearing = readStringFromBuffer(buf);
        this.positionX = readStringFromBuffer(buf);
        this.positionY = readStringFromBuffer(buf);
        this.positionZ = readStringFromBuffer(buf);
        this.isDeleted = readStringFromBuffer(buf);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeStringToBuffer(index, buf);
        writeStringToBuffer(name, buf);
        writeStringToBuffer(targetSpeed, buf);
        writeStringToBuffer(bearing, buf);
        writeStringToBuffer(positionX, buf);
        writeStringToBuffer(positionY, buf);
        writeStringToBuffer(positionZ, buf);
        writeStringToBuffer(isDeleted, buf);
    }

    @Override
    public void handle(AWrapperWorld world) {
        NavWaypointUpdater waypointUpdater = new NavWaypointUpdater(NavWaypoint.getByIndexFromWorld(world,index));
        waypointUpdater.updateState(world,index,name,targetSpeed,bearing,positionX,positionY,positionZ,isDeleted);
    }
}
