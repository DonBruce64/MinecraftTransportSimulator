package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet used to send signals to seats.  Currently, this is only used to change the currently-selected
 * gun that the seat is set to fire.  Sent to servers when a player presses the gun key.  Sent back to all
 * clients for updating.  May also be sent from the server to all clients when a seat is first entered.
 *
 * @author don_bruce
 */
public class PacketPartSeat extends APacketEntity<PartSeat> {

    public PacketPartSeat(PartSeat seat) {
        super(seat);
    }

    public PacketPartSeat(ByteBuf buf) {
        super(buf);
    }

    @Override
    public boolean handle(AWrapperWorld world, PartSeat seat) {
        seat.setNextActiveGun();
        return true;
    }
}
