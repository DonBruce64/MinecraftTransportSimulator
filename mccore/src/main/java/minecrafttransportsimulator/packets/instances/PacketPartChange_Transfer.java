package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet used to transfer parts from one entity to another.  This packet only appears on clients.
 *
 * @author don_bruce
 */
public class PacketPartChange_Transfer extends APacketEntity<APart> {
    private final UUID entityUUID;
    private final int slotIndex;

    public PacketPartChange_Transfer(APart partToTransfer, AEntityF_Multipart<?> entityToTransferTo, JSONPartDefinition partDef) {
        super(partToTransfer);
        this.entityUUID = entityToTransferTo.uniqueUUID;
        this.slotIndex = entityToTransferTo.definition.parts.indexOf(partDef);
    }

    public PacketPartChange_Transfer(ByteBuf buf) {
        super(buf);
        this.entityUUID = readUUIDFromBuffer(buf);
        this.slotIndex = buf.readByte();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeUUIDToBuffer(entityUUID, buf);
        buf.writeByte(slotIndex);
    }

    @Override
    public boolean handle(AWrapperWorld world, APart part) {
        AEntityF_Multipart<?> entity = world.getEntity(entityUUID);
        if (entity != null) {
            JSONPartDefinition partDef = entity.definition.parts.get(slotIndex);
            part.linkToEntity(entity, partDef);
            entity.addPart(part, false);
        }
        return false;
    }
}
