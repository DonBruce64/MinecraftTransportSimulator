package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.instances.EntityInventoryContainer;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityGUIRequest;

/**
 * Chest tile entity.
 *
 * @author don_bruce
 */
public class TileEntityChest extends TileEntityDecor {

    public final EntityInventoryContainer inventory;

    public TileEntityChest(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, IWrapperNBT data) {
        super(world, position, placingPlayer, data);
        this.inventory = new EntityInventoryContainer(world, data.getDataOrNew("inventory"), (int) (definition.decor.inventoryUnits * 9F), definition.decor.inventoryStackSize > 0 ? definition.decor.inventoryStackSize : 64);
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
                world.spawnItemStack(stack, position);
            }
        }
        //Now forward destruction call.  If we didn't, we'd have removed our inventory already.
        super.destroy(box);
    }

    @Override
    public boolean interact(IWrapperPlayer player) {
        player.sendPacket(new PacketEntityGUIRequest(this, player, PacketEntityGUIRequest.EntityGUIType.INVENTORY_CHEST));
        return true;
    }

    @Override
    public double getRawVariableValue(String variable, float partialTicks) {
        switch (variable) {
            case ("inventory_count"): {
                if (inventory != null) {
                    return inventory.getCount();
                } else {
                    return 0;
                }
            }
            case ("inventory_percent"): {
                if (inventory != null) {
                    return inventory.getCount() / (double) inventory.getSize();
                } else {
                    return 0;
                }
            }
            case ("inventory_capacity"): {
                if (inventory != null) {
                    return inventory.getSize();
                } else {
                    return 0;
                }
            }
        }

        return super.getRawVariableValue(variable, partialTicks);
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
