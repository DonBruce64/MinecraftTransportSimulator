package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.NavWaypoint;
import minecrafttransportsimulator.baseclasses.NavWaypointUpdater;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.components.APacketBase;
import minecrafttransportsimulator.packets.components.APacketPlayer;

/**
 * Request waypoint update from client to server
 *
 */
public class PacketWaypointUpdateRequest extends APacketBase {

    public final String index;

    public PacketWaypointUpdateRequest(String waypointName) {
        super(null);
        this.index = waypointName;
    }

    public PacketWaypointUpdateRequest(ByteBuf buf) {
        super(buf);
        this.index = readStringFromBuffer(buf);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeStringToBuffer(index, buf);
    }

    @Override
    public void handle(AWrapperWorld world) {
        if(NavWaypoint.getByIndexFromWorld(world, index)!=null){
            NavWaypointUpdater waypointUpdater = new NavWaypointUpdater(NavWaypoint.getByIndexFromWorld(world, index));
            NavWaypoint currentWaypoint = waypointUpdater.currentWaypoint;
            InterfaceManager.packetInterface.sendToAllClients(new PacketWaypointUpdate(
                            currentWaypoint.index,
                            currentWaypoint.name,
                            Double.toString(currentWaypoint.targetSpeed),
                            Double.toString(currentWaypoint.bearing),
                            Double.toString(currentWaypoint.position.x),
                            Double.toString(currentWaypoint.position.y),
                            Double.toString(currentWaypoint.position.z),
                            "false"
                    )
            );
        }else{
            InterfaceManager.packetInterface.sendToAllClients(new PacketWaypointUpdate(index,"null","0.0","0.0","0.0","0.0","0.0","true"));
        }
    }
}
