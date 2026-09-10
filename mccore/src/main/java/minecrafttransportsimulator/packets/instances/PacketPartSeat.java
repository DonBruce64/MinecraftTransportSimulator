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
    private final int gunGroupIndex;

    public PacketPartSeat(PartSeat seat, SeatAction packetType) {
        super(seat);
        this.packetType = packetType;
        this.gunGroupIndex = -1;
    }

    /**
     * Variant used to select a specific gun group by index (0-based).  The target index
     * corresponds to the insertion order of {@link PartSeat#gunGroups}.
     */
    public PacketPartSeat(PartSeat seat, int gunGroupIndex) {
        super(seat);
        this.packetType = SeatAction.SET_ACTIVE_GUN;
        this.gunGroupIndex = gunGroupIndex;
    }

    public PacketPartSeat(ByteBuf buf) {
        super(buf);
        this.packetType = SeatAction.values()[buf.readByte()];
        this.gunGroupIndex = packetType == SeatAction.SET_ACTIVE_GUN ? buf.readInt() : -1;
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeByte(packetType.ordinal());
        if (packetType == SeatAction.SET_ACTIVE_GUN) {
            buf.writeInt(gunGroupIndex);
        }
    }

    @Override
    public boolean handle(AWrapperWorld world, PartSeat seat) {
        switch (packetType) {
            case CHANGE_GUN: {
                seat.setNextActiveGun();
                return true;
            }
            case SET_ACTIVE_GUN: {
                seat.setActiveGunByIndex(gunGroupIndex);
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
        SET_ACTIVE_GUN,
        ZOOM_IN,
        ZOOM_OUT;
    }
}
