package minecraftflightsimulator.containers;

import minecraftflightsimulator.MFS;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class SlotLandingGear extends SlotPart{
	
	public SlotLandingGear(IInventory inventory, int xDisplayPosition, int yDisplayPosition, int slotIndex, Item slotItem){
		super(inventory, xDisplayPosition, yDisplayPosition, slotIndex, slotItem);
	}
	
    public boolean isItemValid(ItemStack item){
    	return super.isItemValid(item) || item.getItem().equals(MFS.proxy.pontoon);
    }
}
