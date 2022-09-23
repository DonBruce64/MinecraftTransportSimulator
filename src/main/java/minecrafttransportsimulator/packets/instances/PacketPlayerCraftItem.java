package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperInventory;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.components.APacketPlayer;
import minecrafttransportsimulator.packloading.PackMaterialComponent;

/**
 * Packet used to craft items from crafting benches.  This goes to the server which verifies the
 * player has the appropriate materials.  If so, the item is crafted on the server and materials
 * are deducted if required.  Packet is not sent back to the client as MC will auto-add the item
 * into the player's inventory and will do updates for us.
 *
 * @author don_bruce
 */
public class PacketPlayerCraftItem extends APacketPlayer {
    private final AItemPack<?> itemToCraft;
    private final int recipeIndex;
    private final boolean forRepair;

    public PacketPlayerCraftItem(IWrapperPlayer player, AItemPack<?> itemToCraft, int recipeIndex, boolean forRepair) {
        super(player);
        this.itemToCraft = itemToCraft;
        this.recipeIndex = recipeIndex;
        this.forRepair = forRepair;
    }

    public PacketPlayerCraftItem(ByteBuf buf) {
        super(buf);
        this.itemToCraft = readItemFromBuffer(buf);
        this.recipeIndex = buf.readInt();
        this.forRepair = buf.readBoolean();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeItemToBuffer(itemToCraft, buf);
        buf.writeInt(recipeIndex);
        buf.writeBoolean(forRepair);
    }

    @Override
    public void handle(AWrapperWorld world, IWrapperPlayer player) {
        IWrapperInventory inventory = player.getInventory();
        if (player.isCreative() || inventory.hasMaterials(PackMaterialComponent.parseFromJSON(itemToCraft, recipeIndex, true, true, forRepair))) {
            //If this is for repair, we don't make a new stack, we just use the old stack and a method call.
            if (forRepair) {
                //Find the repair item and repair it.
                int repairIndex = inventory.getRepairIndex(itemToCraft, recipeIndex);
                IWrapperItemStack stack = inventory.getStack(repairIndex);
                AItemPack<?> item = (AItemPack<?>) stack.getItem();
                IWrapperNBT stackData = stack.getData();
                item.repair(stackData);
                stack.setData(stackData);
                if (!player.isCreative()) {
                    inventory.removeMaterials(itemToCraft, recipeIndex, true, true, forRepair);
                }
                //Need to set stack after item removal, as removal code removes this item.
                inventory.setStack(stack, repairIndex);
            } else {
                //Check we can add the stack before removing materials.
                if (inventory.addStack(itemToCraft.getNewStack(null))) {
                    if (!player.isCreative()) {
                        inventory.removeMaterials(itemToCraft, recipeIndex, true, true, forRepair);
                    }
                }
            }
        }
    }
}
