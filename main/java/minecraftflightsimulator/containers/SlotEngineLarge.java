package minecraftflightsimulator.containers;

import minecraftflightsimulator.items.ItemEngineLarge;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class SlotEngineLarge extends Slot{

	public SlotEngineLarge(IInventory inventory, int xDisplayPosition, int yDisplayPosition, int slotIndex){
		super(inventory, slotIndex, xDisplayPosition, yDisplayPosition);
	}
	
    public boolean isItemValid(ItemStack item){
    	return item.getItem() instanceof ItemEngineLarge;
    }
}
