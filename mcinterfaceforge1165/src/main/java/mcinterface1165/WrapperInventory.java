package mcinterface1165;

import minecrafttransportsimulator.mcinterface.IWrapperInventory;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import net.minecraft.inventory.IInventory;

class WrapperInventory implements IWrapperInventory {
    private final IInventory inventory;

    public WrapperInventory(IInventory inventory) {
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