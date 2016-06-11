package minecraftflightsimulator.containers;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class SlotItem extends Slot{
	private final Item[] validItems;
	
	public SlotItem(IInventory inventory, int xDisplayPosition, int yDisplayPosition, int slotIndex, Item... validItems){
		super(inventory, slotIndex, xDisplayPosition, yDisplayPosition);
		this.validItems = validItems;
	}
	
    public boolean isItemValid(ItemStack itemStack){
    	for(Item validItem : validItems){
    		if(itemStack.getItem().equals(validItem)){
    			return true;
    		}
    	}
    	return false;
    }
}
