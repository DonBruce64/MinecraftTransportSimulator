package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet used to add parts to an entity.  This packet only appears on clients.
 *
 * @author don_bruce
 */
public class PacketPartChange_Add extends APacketEntity<AEntityF_Multipart<?>> {
    private final int partSlot;
    private final IWrapperItemStack partStack;

    public PacketPartChange_Add(AEntityF_Multipart<?> entity, APart partAdded) {
        super(entity);
        this.partSlot = partAdded.placementSlot;
        this.partStack = partAdded.getStack();
    }

    public PacketPartChange_Add(ByteBuf buf) {
        super(buf);
        this.partSlot = buf.readInt();
        this.partStack = readItemFromBuffer(buf).getNewStack(readDataFromBuffer(buf));
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeInt(partSlot);
        writeItemToBuffer((AItemPack<?>) partStack.getItem(), buf);
        writeDataToBuffer(partStack.getData(), buf);
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityF_Multipart<?> entity) {
        APart part = ((AItemPart) partStack.getItem()).createPart(entity, null, entity.definition.parts.get(partSlot), partStack.getData());
        entity.addPart(part, false);
        part.addPartsPostAddition(null, partStack.getData());
        return false;
    }
}
