package minecrafttransportsimulator.mcinterface;

import minecrafttransportsimulator.baseclasses.IInventoryProvider;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class WrapperInventory implements IInventoryProvider{
	private final IInventory inventory;
	
	public WrapperInventory(IInventory inventory){
		this.inventory = inventory;
	}
	
	@Override
	public int getSize(){
		return inventory.getSizeInventory();
	}
	
	@Override
	public ItemStack getStack(int slot){
		return inventory.getStackInSlot(slot);
	}
	
	@Override
	public void setStack(ItemStack stackToSet, int index){
		inventory.setInventorySlotContents(index, stackToSet);
		inventory.markDirty();
	}
}