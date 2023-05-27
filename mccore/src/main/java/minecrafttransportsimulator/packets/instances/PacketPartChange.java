package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet used to add/remove parts from an entity.  This packet only appears on clients after the
 * server has added or removed a part from the entity.
 *
 * @author don_bruce
 */
public class PacketPartChange extends APacketEntity<AEntityF_Multipart<?>> {
    private final int partSlot;
    private final IWrapperItemStack partStack;
    private final UUID partUUID;

    public PacketPartChange(APart partRemoved) {
        super(partRemoved.entityOn);
        this.partSlot = 0;
        this.partStack = null;
        this.partUUID = partRemoved.uniqueUUID;
    }

    public PacketPartChange(AEntityF_Multipart<?> entity, APart partAdded) {
        super(entity);
        this.partSlot = partAdded.placementSlot;
        this.partStack = partAdded.getStack();
        this.partUUID = null;
    }

    public PacketPartChange(ByteBuf buf) {
        super(buf);
        if (buf.readBoolean()) {
            this.partSlot = buf.readInt();
            this.partStack = readItemFromBuffer(buf).getNewStack(readDataFromBuffer(buf));
            this.partUUID = null;
        } else {
            this.partSlot = -1;
            this.partStack = null;
            this.partUUID = readUUIDFromBuffer(buf);
        }
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        if (partStack != null) {
            buf.writeBoolean(true);
            buf.writeInt(partSlot);
            writeItemToBuffer((AItemPack<?>) partStack.getItem(), buf);
            writeDataToBuffer(partStack.getData(), buf);
        } else {
            buf.writeBoolean(false);
            writeUUIDToBuffer(partUUID, buf);
        }
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityF_Multipart<?> entity) {
        if (partStack != null) {
            APart part = ((AItemPart) partStack.getItem()).createPart(entity, null, entity.definition.parts.get(partSlot), partStack.getData());
            entity.addPart(part, false);
            part.addPartsPostAddition(null, partStack.getData());
        } else {
            APart part = world.getEntity(partUUID);
            if (part != null) {
                part.entityOn.removePart(part, null);
            }
        }
        return true;
    }
}
