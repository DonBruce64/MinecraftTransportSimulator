package mcinterface1182;

import minecrafttransportsimulator.mcinterface.IWrapperInventory;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import net.minecraft.world.Container;

class WrapperInventory implements IWrapperInventory {
    private final Container inventory;

    public WrapperInventory(Container inventory) {
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