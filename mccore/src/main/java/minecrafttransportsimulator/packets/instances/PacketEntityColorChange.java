package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperInventory;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.components.APacketEntityInteract;
import minecrafttransportsimulator.packloading.PackMaterialComponent;

/**
 * Packet sent to entities to update their their subName (color).  This gets sent from
 * a client when they change the color in the paint gun GUI.  Change is applied to the
 * entity and all parts (if applicable).
 *
 * @author don_bruce
 */
public class PacketEntityColorChange extends APacketEntityInteract<AEntityD_Definable<?>, IWrapperPlayer> {
    private final AItemSubTyped<?> newItem;
    private final int recipeIndex;

    public PacketEntityColorChange(AEntityD_Definable<?> entity, IWrapperPlayer player, AItemSubTyped<?> newItem, int recipeIndex) {
        super(entity, player);
        this.newItem = newItem;
        this.recipeIndex = recipeIndex;
    }

    public PacketEntityColorChange(ByteBuf buf) {
        super(buf);
        this.newItem = readItemFromBuffer(buf);
        this.recipeIndex = buf.readInt();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeItemToBuffer(newItem, buf);
        buf.writeInt(recipeIndex);
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityD_Definable<?> entity, IWrapperPlayer player) {
        IWrapperInventory inventory = player.getInventory();
        if (player.isCreative() || inventory.hasMaterials(PackMaterialComponent.parseFromJSON(newItem, recipeIndex, false, true, false))) {
            //Remove livery materials (if required) and set new subName.
            if (!player.isCreative()) {
                inventory.removeMaterials(newItem, recipeIndex, false, true, false);
            }
            entity.updateSubDefinition(newItem.subDefinition.subName);

            //If we have parts, and have a second tone, change parts to match if possible.
            if (entity instanceof AEntityF_Multipart) {
                ((AEntityF_Multipart<?>) entity).parts.forEach(part -> part.updateTone(true));
            }
            return true;
        }
        return false;
    }
}
