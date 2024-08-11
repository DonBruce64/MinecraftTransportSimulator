package mcinterface1165;

import minecrafttransportsimulator.mcinterface.IWrapperInventory;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import net.minecraft.inventory.Inventory;

class WrapperInventory implements IWrapperInventory {
    private final Inventory inventory;

    public WrapperInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public int getSize() {
        return inventory.size();
    }

    @Override
    public IWrapperItemStack getStack(int slot) {
        return new WrapperItemStack(inventory.getStack(slot));
    }

    @Override
    public void setStack(IWrapperItemStack stackToSet, int index) {
        inventory.setStack(index, ((WrapperItemStack) stackToSet).stack);
        inventory.markDirty();
    }
}