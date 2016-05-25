package minecraftflightsimulator.containers;

import minecraftflightsimulator.items.ItemPontoon;
import minecraftflightsimulator.items.ItemWheelLarge;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class SlotWheelLarge extends Slot{
	public SlotWheelLarge(IInventory inventory, int xDisplayPosition, int yDisplayPosition, int slotIndex){
		super(inventory, slotIndex, xDisplayPosition, yDisplayPosition);
	}
	
    public boolean isItemValid(ItemStack item){
    	return item.getItem() instanceof ItemWheelLarge || item.getItem() instanceof ItemPontoon;
    }
}
