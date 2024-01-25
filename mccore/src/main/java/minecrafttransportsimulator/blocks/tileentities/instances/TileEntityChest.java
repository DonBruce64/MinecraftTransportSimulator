package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.instances.EntityInventoryContainer;
import minecrafttransportsimulator.items.instances.ItemDecor;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityGUIRequest;
import minecrafttransportsimulator.packets.instances.PacketEntityInteractGUI;

/**
 * Chest tile entity.
 *
 * @author don_bruce
 */
public class TileEntityChest extends TileEntityDecor {

    public final EntityInventoryContainer inventory;

    public TileEntityChest(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, ItemDecor item, IWrapperNBT data) {
        super(world, position, placingPlayer, item, data);
        this.inventory = new EntityInventoryContainer(world, data != null ? data.getData("inventory") : null, (int) (definition.decor.inventoryUnits * 9F), definition.decor.inventoryStackSize > 0 ? definition.decor.inventoryStackSize : 64);
        world.addEntity(inventory);
    }

    @Override
    public void remove() {
        super.remove();
        inventory.remove();
    }

    @Override
    public void destroy(BoundingBox box) {
        //Drop all inventory items when destroyed.
        for (int i = 0; i < inventory.getSize(); ++i) {
            IWrapperItemStack stack = inventory.getStack(i);
            if (stack != null) {
                world.spawnItemStack(stack, position, null);
            }
        }
        //Now forward destruction call.  If we didn't, we'd have removed our inventory already.
        super.destroy(box);
    }

    @Override
    public boolean interact(IWrapperPlayer player) {
        player.sendPacket(new PacketEntityGUIRequest(this, player, PacketEntityGUIRequest.EntityGUIType.INVENTORY_CHEST));
        playersInteracting.add(player);
        InterfaceManager.packetInterface.sendToAllClients(new PacketEntityInteractGUI(this, player, true));
        return true;
    }

    @Override
    public ComputedVariable createComputedVariable(String variable) {
        switch (variable) {
            case ("inventory_count"):
                return new ComputedVariable(this, variable, partialTicks -> inventory != null ? inventory.getCount() : 0, false);
            case ("inventory_percent"):
                return new ComputedVariable(this, variable, partialTicks -> inventory != null ? inventory.getCount() / (double) inventory.getSize() : 0, false);
            case ("inventory_capacity"):
                return new ComputedVariable(this, variable, partialTicks -> inventory != null ? inventory.getSize() : 0, false);
            default:
                return super.createComputedVariable(variable);
        }
    }

    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        if (inventory != null) {
            data.setData("inventory", inventory.save(InterfaceManager.coreInterface.getNewNBTWrapper()));
        }
        return data;
    }
}
