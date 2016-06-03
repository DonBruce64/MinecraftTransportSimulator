package minecraftflightsimulator.containers;

import minecraftflightsimulator.MFS;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class SlotPart extends Slot{
	private final Item slotItem;
	
	public SlotPart(IInventory inventory, int xDisplayPosition, int yDisplayPosition, int slotIndex, Item slotItem){
		super(inventory, slotIndex, xDisplayPosition, yDisplayPosition);
		this.slotItem = slotItem;
	}
	
    public boolean isItemValid(ItemStack item){
    	return item.getItem().equals(this.slotItem);
    }
}
