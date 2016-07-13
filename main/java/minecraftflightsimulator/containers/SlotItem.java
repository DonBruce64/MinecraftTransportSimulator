package minecraftflightsimulator.containers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class SlotItem extends Slot{
	public boolean enabled = true;
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
    
    public boolean canTakeStack(EntityPlayer player){
        return enabled;
    }
    
    public boolean func_111238_b(){
        return enabled;
    }
}
