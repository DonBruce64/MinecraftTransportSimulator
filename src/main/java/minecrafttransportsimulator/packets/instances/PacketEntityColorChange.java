package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperInventory;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.components.APacketEntityInteract;

/**
 * Packet sent to entities to update their their subName (color).  This gets sent from
 * a client when they change the color in the paint gun GUI.  Change is applied to the
 * entity and all parts (if applicable).
 *
 * @author don_bruce
 */
public class PacketEntityColorChange extends APacketEntityInteract<AEntityD_Definable<?>, IWrapperPlayer> {
    private final AItemSubTyped<?> newItem;

    public PacketEntityColorChange(AEntityD_Definable<?> entity, IWrapperPlayer player, AItemSubTyped<?> newItem) {
        super(entity, player);
        this.newItem = newItem;
    }

    public PacketEntityColorChange(ByteBuf buf) {
        super(buf);
        this.newItem = readItemFromBuffer(buf);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeItemToBuffer(newItem, buf);
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityD_Definable<?> entity, IWrapperPlayer player) {
        IWrapperInventory inventory = player.getInventory();
        if (player.isCreative() || inventory.hasMaterials(newItem, false, true, false)) {
            //Remove livery materials (if required) and set new subName.
            if (!player.isCreative()) {
                inventory.removeMaterials(newItem, false, true, false);
            }
            entity.subName = newItem.subName;

            //If we have parts, and have a second tone, change parts to match if possible.
            if (entity instanceof AEntityF_Multipart) {
                ((AEntityF_Multipart<?>) entity).parts.forEach(part -> part.updateTone(true));
            }
            return true;
        }
        return false;
    }
}
