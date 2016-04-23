package minecraftflightsimulator.containers;

import minecraftflightsimulator.items.ItemWheelSmall;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class SlotWheelSmall extends Slot{
	public SlotWheelSmall(IInventory inventory, int xDisplayPosition, int yDisplayPosition, int slotIndex){
		super(inventory, slotIndex, xDisplayPosition, yDisplayPosition);
	}
	
    public boolean isItemValid(ItemStack item){
    	return item.getItem() instanceof ItemWheelSmall;
    }
}
