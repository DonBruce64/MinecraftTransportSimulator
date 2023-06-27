package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet used to remove parts from an entity.  This packet only appears on clients.
 *
 * @author don_bruce
 */
public class PacketPartChange_Remove extends APacketEntity<AEntityF_Multipart<?>> {
    private final boolean removeFromWorld;
    private final UUID partUUID;

    public PacketPartChange_Remove(APart partRemoved, boolean removeFromWorld) {
        super(partRemoved.entityOn);
        this.removeFromWorld = removeFromWorld;
        this.partUUID = partRemoved.uniqueUUID;
    }

    public PacketPartChange_Remove(ByteBuf buf) {
        super(buf);
        this.removeFromWorld = buf.readBoolean();
        this.partUUID = readUUIDFromBuffer(buf);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeBoolean(removeFromWorld);
        writeUUIDToBuffer(partUUID, buf);
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityF_Multipart<?> entity) {
        APart part = world.getEntity(partUUID);
        if (part != null) {
            part.entityOn.removePart(part, removeFromWorld, null);
        }
        return false;
    }
}
