package minecrafttransportsimulator.packets.instances;

import java.util.List;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperInventory;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.components.APacketEntityInteract;
import minecrafttransportsimulator.packloading.PackMaterialComponent;

/**
 * Packet used to craft items from crafting benches.  This goes to the server which verifies the
 * player has the appropriate materials.  If so, the item is crafted on the server and materials
 * are deducted if required.  Packet is not sent back to the client as MC will auto-add the item
 * into the player's inventory and will do updates for us.
 *
 * @author don_bruce
 */
public class PacketPlayerCraftItem extends APacketEntityInteract<AEntityD_Definable<?>, IWrapperPlayer> {
    private final AItemPack<?> itemToCraft;
    private final int recipeIndex;
    private final boolean forRepair;

    public PacketPlayerCraftItem(AEntityD_Definable<?> entity, IWrapperPlayer player, AItemPack<?> itemToCraft, int recipeIndex, boolean forRepair) {
        super(entity, player);
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
    public boolean handle(AWrapperWorld world, AEntityD_Definable<?> entity, IWrapperPlayer player) {
        if (!world.isClient()) {
            IWrapperInventory inventory = player.getInventory();
            List<PackMaterialComponent> materials = PackMaterialComponent.parseFromJSON(itemToCraft, recipeIndex, true, true, forRepair, false);
            if (player.isCreative() || inventory.hasMaterials(materials)) {
                //If this is for repair, we don't make a new stack, we just use the old stack and a method call.
                if (forRepair) {
                    //Find the repair item and fix it.
                    int repairIndex = inventory.getRepairIndex(itemToCraft);
                    if (repairIndex != -1) {
                        IWrapperItemStack stack = inventory.getStack(repairIndex);
                        AItemPack<?> item = (AItemPack<?>) stack.getItem();
                        IWrapperNBT stackData = stack.getData();
                        item.repair(stackData);
                        stack.setData(stackData);

                        if (!player.isCreative()) {
                            inventory.removeMaterials(materials);
                        }
                        inventory.setStack(stack, repairIndex);
                        return true;
                    }
                } else {
                    //Check we can add the stack before removing materials.
                    if (inventory.addStack(itemToCraft.getNewStack(null))) {
                        if (!player.isCreative()) {
                            inventory.removeMaterials(materials);
                        }
                        if (itemToCraft.definition.general.returnedMaterialLists != null) {
                            itemToCraft.definition.general.returnedMaterialLists.get(recipeIndex).forEach(itemName -> world.spawnItemStack(new PackMaterialComponent(itemName).possibleItems.get(0), player.getPosition(), null));
                        }
                        if (itemToCraft instanceof AItemSubTyped) {
                            if (((AItemSubTyped<?>) itemToCraft).subDefinition.extraReturnedMaterialLists != null) {
                                ((AItemSubTyped<?>) itemToCraft).subDefinition.extraReturnedMaterialLists.get(recipeIndex).forEach(itemName -> world.spawnItemStack(new PackMaterialComponent(itemName).possibleItems.get(0), player.getPosition(), null));
                            }
                        }
                        return true;
                    }
                }
            }
        }
        entity.playerCraftedItem = true;
        return false;
    }
}
