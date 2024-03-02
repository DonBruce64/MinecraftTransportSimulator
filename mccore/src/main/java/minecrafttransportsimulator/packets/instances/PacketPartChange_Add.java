package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet used to add parts to an entity.  This packet only appears on clients.
 *
 * @author don_bruce
 */
public class PacketPartChange_Add extends APacketEntity<AEntityF_Multipart<?>> {
    private final int partSlot;
    private final AItemPart item;
    private final IWrapperNBT data;

    public PacketPartChange_Add(AEntityF_Multipart<?> entity, APart partAdded) {
        super(entity);
        this.partSlot = partAdded.placementSlot;
        this.item = (AItemPart) partAdded.cachedItem;
        this.data = partAdded.save(InterfaceManager.coreInterface.getNewNBTWrapper());
    }

    public PacketPartChange_Add(ByteBuf buf) {
        super(buf);
        this.partSlot = buf.readInt();
        this.item = readItemFromBuffer(buf);
        this.data = readDataFromBuffer(buf);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeInt(partSlot);
        writeItemToBuffer(item, buf);
        writeDataToBuffer(data, buf);
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityF_Multipart<?> entity) {
        APart part = item.createPart(entity, null, entity.definition.parts.get(partSlot), data);
        entity.addPart(part, false);
        part.addPartsPostAddition(null, data);
        return false;
    }
}
