package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**Packet used to add/remove parts from an entity.  This packet only appears on clients after the
 * server has added or removed a part from the entity.
 * 
 * @author don_bruce
 */
public class PacketPartChange extends APacketEntity<AEntityF_Multipart<?>> {
    private final int partSlot;
    private final AItemPart partItem;
    private final IWrapperNBT partData;

    public PacketPartChange(AEntityF_Multipart<?> entity, int partSlot) {
        super(entity);
        this.partSlot = partSlot;
        this.partItem = null;
        this.partData = null;
    }

    public PacketPartChange(AEntityF_Multipart<?> entity, APart partAdded) {
        super(entity);
        this.partSlot = partAdded.placementSlot;
        this.partItem = partAdded.getItem();
        this.partData = InterfaceManager.coreInterface.getNewNBTWrapper();
        partAdded.save(partData);
    }

    public PacketPartChange(ByteBuf buf) {
        super(buf);
        this.partSlot = buf.readInt();
        if (buf.readBoolean()) {
            this.partItem = readItemFromBuffer(buf);
            this.partData = readDataFromBuffer(buf);
        } else {
            this.partItem = null;
            this.partData = null;
        }
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeInt(partSlot);
        if (partItem != null) {
            buf.writeBoolean(true);
            writeItemToBuffer(partItem, buf);
            writeDataToBuffer(partData, buf);
        } else {
            buf.writeBoolean(false);
        }
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityF_Multipart<?> entity) {
        if (partItem == null) {
            APart part = entity.partsInSlots.get(partSlot);
            if (part != null) {
                entity.removePart(part, null);
            }
        } else {
            APart part = partItem.createPart(entity, null, entity.definition.parts.get(partSlot), partData);
            entity.addPart(part, false);
            part.addPartsPostAddition(null, partData);
        }
        return true;
    }
}
