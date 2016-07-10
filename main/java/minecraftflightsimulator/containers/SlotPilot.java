package minecraftflightsimulator.containers;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.entities.core.EntityParent;
import minecraftflightsimulator.items.ItemSeat;
import net.minecraft.item.ItemStack;

public class SlotPilot extends SlotItem{
	private EntityParent parent;

	public SlotPilot(EntityParent parent, int xDisplayPosition, int yDisplayPosition) {
		super(parent, xDisplayPosition, yDisplayPosition, parent.pilotSeatSlot, MFS.proxy.seat);
		this.parent = parent;
	}
	
    public boolean isItemValid(ItemStack item){
    	return item.getItem() instanceof ItemSeat;
    }
    
    public int getSlotStackLimit(){
        return parent.getNumberPilotSeats();
    }
}
