package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet used to add/remove parts from an entity.  This packet only appears on clients after the
 * server has added or removed a part from the entity.
 *
 * @author don_bruce
 */
public class PacketPartChange extends APacketEntity<AEntityF_Multipart<?>> {
    private final int partSlot;
    private final AItemPart partItem;
    private final IWrapperNBT partData;
    private final UUID partUUID;

    public PacketPartChange(APart partRemoved) {
        super(partRemoved.entityOn);
        this.partSlot = 0;
        this.partItem = null;
        this.partData = null;
        this.partUUID = partRemoved.uniqueUUID;
    }

    public PacketPartChange(AEntityF_Multipart<?> entity, APart partAdded) {
        super(entity);
        this.partSlot = partAdded.placementSlot;
        this.partItem = partAdded.getItem();
        this.partData = InterfaceManager.coreInterface.getNewNBTWrapper();
        partAdded.save(partData);
        this.partUUID = null;
    }

    public PacketPartChange(ByteBuf buf) {
        super(buf);
        if (buf.readBoolean()) {
            this.partSlot = buf.readInt();
            this.partItem = readItemFromBuffer(buf);
            this.partData = readDataFromBuffer(buf);
            this.partUUID = null;
        } else {
            this.partSlot = -1;
            this.partItem = null;
            this.partData = null;
            this.partUUID = readUUIDFromBuffer(buf);
        }
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        if (partItem != null) {
            buf.writeBoolean(true);
            buf.writeInt(partSlot);
            writeItemToBuffer(partItem, buf);
            writeDataToBuffer(partData, buf);
        } else {
            buf.writeBoolean(false);
            writeUUIDToBuffer(partUUID, buf);
        }
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityF_Multipart<?> entity) {
        if (partItem != null) {
            APart part = partItem.createPart(entity, null, entity.definition.parts.get(partSlot), partData);
            entity.addPart(part, false);
            part.addPartsPostAddition(null, partData);
        } else {
            APart part = world.getEntity(partUUID);
            if (part != null) {
                part.entityOn.removePart(part, null);
            }
        }
        return true;
    }
}
