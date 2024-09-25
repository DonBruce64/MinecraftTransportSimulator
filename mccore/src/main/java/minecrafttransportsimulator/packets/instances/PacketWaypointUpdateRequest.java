package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.NavWaypoint;
import minecrafttransportsimulator.baseclasses.NavWaypointUpdater;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.components.APacketPlayer;

/**
 * Request waypoint update from client to server
 *
 */
public class PacketWaypointUpdateRequest extends APacketPlayer {

    public final String index;

    public PacketWaypointUpdateRequest(IWrapperPlayer player,String waypointName) {
        super(player);
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
    protected void handle(AWrapperWorld world, IWrapperPlayer player) {
        if(NavWaypoint.getByIndexFromWorld(world, index)!=null){
            NavWaypointUpdater waypointUpdater = new NavWaypointUpdater(NavWaypoint.getByIndexFromWorld(world, index));
            NavWaypoint currentWaypoint = waypointUpdater.currentWaypoint;
            player.sendPacket(new PacketWaypointUpdate(
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
            player.sendPacket(new PacketWaypointUpdate(index,"null","0.0","0.0","0.0","0.0","0.0","true"));
        }
    }
}
