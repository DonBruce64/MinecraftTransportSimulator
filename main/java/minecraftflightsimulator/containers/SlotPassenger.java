package minecraftflightsimulator.containers;

import minecraftflightsimulator.entities.core.EntityParent;
import minecraftflightsimulator.items.ItemSeat;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class SlotPassenger extends Slot{
	private EntityParent parent;

	public SlotPassenger(EntityParent parent, int xDisplayPosition, int yDisplayPosition) {
		super(parent, parent.passengerSeatSlot, xDisplayPosition, yDisplayPosition);
		this.parent=parent;
	}
	
    public boolean isItemValid(ItemStack item){
    	return item.getItem() instanceof ItemSeat || item.getItem().equals(Item.getItemFromBlock(Blocks.chest));
    }
    
    public int getSlotStackLimit(){
        return parent.getNumberPassengerSeats();
    }
}
