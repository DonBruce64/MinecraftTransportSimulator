package minecraftflightsimulator.containers;

import minecraftflightsimulator.items.ItemWheel;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class SlotWheel extends Slot{
	int wheelType;

	public SlotWheel(IInventory inventory, int xDisplayPosition, int yDisplayPosition, int slotIndex, int wheelType){
		super(inventory, slotIndex, xDisplayPosition, yDisplayPosition);
		this.wheelType=wheelType;
	}
	
    public boolean isItemValid(ItemStack item){
    	return item.getItem() instanceof ItemWheel && item.getItemDamage() == wheelType;
    }
}
