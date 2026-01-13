package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet sent to entities to update their last opened item.  This is sent from the
 * client to the server on a GUI operation, and then back to all clients.
 *
 * @author don_bruce
 */
public class PacketEntityLastOpenedItemChange extends APacketEntity<AEntityD_Definable<?>> {
    private final AItemPack<?> lastOpenendItem;

    public PacketEntityLastOpenedItemChange(AEntityD_Definable<?> entity, AItemPack<?> lastOpenendItem) {
        super(entity);
        this.lastOpenendItem = lastOpenendItem;
    }

    public PacketEntityLastOpenedItemChange(ByteBuf buf) {
        super(buf);
        this.lastOpenendItem = readItemFromBuffer(buf);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeItemToBuffer(lastOpenendItem, buf);
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityD_Definable<?> entity) {
        entity.lastOpenedItem = lastOpenendItem;
        return true;
    }
}
