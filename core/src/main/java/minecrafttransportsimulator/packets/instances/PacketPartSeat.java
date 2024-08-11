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
    private final SeatAction packetType;

    public PacketPartSeat(PartSeat seat, SeatAction packetType) {
        super(seat);
        this.packetType = packetType;
    }

    public PacketPartSeat(ByteBuf buf) {
        super(buf);
        this.packetType = SeatAction.values()[buf.readByte()];
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeByte(packetType.ordinal());
    }

    @Override
    public boolean handle(AWrapperWorld world, PartSeat seat) {
        switch (packetType) {
            case CHANGE_GUN: {
                seat.setNextActiveGun();
                return true;
            }
            case ZOOM_IN: {
                if (seat.zoomLevel > 0) {
                    --seat.zoomLevel;
                    return true;
                } else {
                    return false;
                }
            }
            case ZOOM_OUT: {
                ++seat.zoomLevel;
                return true;
            }
        }
        return false;
    }

    public enum SeatAction {
        CHANGE_GUN,
        ZOOM_IN,
        ZOOM_OUT;
    }
}
