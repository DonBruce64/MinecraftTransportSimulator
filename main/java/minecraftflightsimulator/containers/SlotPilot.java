package minecraftflightsimulator.containers;

import minecraftflightsimulator.entities.core.EntityParent;
import minecraftflightsimulator.items.ItemSeat;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class SlotPilot extends Slot{
	private EntityParent parent;

	public SlotPilot(EntityParent parent, int xDisplayPosition, int yDisplayPosition) {
		super(parent, parent.pilotSeatSlot, xDisplayPosition, yDisplayPosition);
		this.parent=parent;
	}
	
    public boolean isItemValid(ItemStack item){
    	return item.getItem() instanceof ItemSeat;
    }
    
    public int getSlotStackLimit(){
        return parent.getNumberPilotSeats();
    }
}
