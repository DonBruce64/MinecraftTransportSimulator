package mcinterface1201;

import minecrafttransportsimulator.mcinterface.IWrapperInventory;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import net.minecraft.world.entity.player.Inventory;

class WrapperInventory implements IWrapperInventory {
    private final Inventory inventory;

    public WrapperInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public int getSize() {
        return inventory.getContainerSize();
    }

    @Override
    public IWrapperItemStack getStack(int slot) {
        return new WrapperItemStack(inventory.getItem(slot));
    }

    @Override
    public void setStack(IWrapperItemStack stackToSet, int index) {
        inventory.setItem(index, ((WrapperItemStack) stackToSet).stack);
        inventory.setChanged();
    }
}