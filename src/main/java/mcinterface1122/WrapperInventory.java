package mcinterface1122;

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
        return inventory.getSizeInventory();
    }

    @Override
    public IWrapperItemStack getStack(int slot) {
        return new WrapperItemStack(inventory.getStackInSlot(slot));
    }

    @Override
    public void setStack(IWrapperItemStack stackToSet, int index) {
        inventory.setInventorySlotContents(index, ((WrapperItemStack) stackToSet).stack);
        inventory.markDirty();
    }
}