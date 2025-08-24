package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet used to send signals to engines.  This can be a state change or damage from an attack.
 * Constructors are present for each of these situations, though the side this packet is present
 * on differ between packet types.  For example engine signal data is sent both from clients to
 * the server, and from the server to clients, while damage information is only sent from
 * servers to clients.
 *
 * @author don_bruce
 */
public class PacketPartEngine extends APacketEntity<PartEngine> {
    private final Signal packetType;
    private final double hours;
    private final UUID linkedID;
    private final int linkedIndex;

    public PacketPartEngine(PartEngine engine, Signal packetType) {
        super(engine);
        this.packetType = packetType;
        this.hours = 0;
        this.linkedID = null;
        this.linkedIndex = 0;
    }

    public PacketPartEngine(PartEngine engine, double hours) {
        super(engine);
        this.packetType = Signal.DAMAGE;
        this.hours = hours;
        this.linkedID = null;
        this.linkedIndex = 0;
    }

    public PacketPartEngine(PartEngine engine, PartEngine linkedEngine) {
        super(engine);
        this.packetType = Signal.LINK;
        this.hours = 0;
        this.linkedID = linkedEngine.entityOn.uniqueUUID;
        this.linkedIndex = linkedEngine.placementSlot;
    }

    public PacketPartEngine(ByteBuf buf) {
        super(buf);
        this.packetType = Signal.values()[buf.readByte()];
        if (packetType.equals(Signal.DAMAGE)) {
            this.hours = buf.readDouble();
        } else {
            this.hours = 0;
        }
        if (packetType.equals(Signal.LINK)) {
            this.linkedID = readUUIDFromBuffer(buf);
            this.linkedIndex = buf.readInt();
        } else {
            this.linkedID = null;
            this.linkedIndex = 0;
        }
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeByte(packetType.ordinal());
        if (packetType.equals(Signal.DAMAGE)) {
            buf.writeDouble(hours);
        } else if (packetType.equals(Signal.LINK)) {
            writeUUIDToBuffer(linkedID, buf);
            buf.writeInt(linkedIndex);
        }
    }

    @Override
    public boolean handle(AWrapperWorld world, PartEngine engine) {
        switch (packetType) {
            case HS_ON:
                engine.handStartEngine();
                break;
            case AS_ON:
                engine.autoStartEngine();
                break;
            case BACKFIRE:
                engine.backfireEngine();
                break;
            case SHIFT_UP:
                engine.shiftUp();
                break;
            case SHIFT_DOWN:
                engine.shiftDown();
                break;
            case SHIFT_NEUTRAL:
                engine.shiftNeutral();
                break;
            case BAD_SHIFT:
                engine.badShiftEngine();
                break;
            case START:
                engine.startEngine();
                break;
            case INACTIVE:
            case FUEL_OUT:
            case TOO_SLOW:
            case OUT_OF_HEALTH:
            case INVALID_DIMENSION:
            case DROWN:
                engine.stallEngine(packetType);
                break;
            case DAMAGE: {
                engine.hours += hours;
                break;
            }
            case LINK: {
                AEntityF_Multipart<?> otherEntity = world.getEntity(linkedID);
                if (otherEntity != null) {
                    engine.linkedEngine = (PartEngine) otherEntity.partsInSlots.get(linkedIndex);
                    return false;
                }
            }
        }
        return true;
    }

    public enum Signal {
        HS_ON,
        AS_ON,
        BACKFIRE,
        SHIFT_UP,
        SHIFT_DOWN,
        SHIFT_NEUTRAL,
        BAD_SHIFT,
        START,
        INACTIVE,
        FUEL_OUT,
        OUT_OF_HEALTH,
        INVALID_DIMENSION,
        TOO_SLOW,
        DROWN,
        DAMAGE,
        LINK
    }
}
