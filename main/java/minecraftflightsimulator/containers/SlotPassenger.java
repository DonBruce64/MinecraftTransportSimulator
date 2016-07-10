package minecraftflightsimulator.containers;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.entities.core.EntityParent;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;

public class SlotPassenger extends SlotItem{
	private EntityParent parent;

	public SlotPassenger(EntityParent parent, int xDisplayPosition, int yDisplayPosition) {
		super(parent, xDisplayPosition, yDisplayPosition, parent.passengerSeatSlot, MFS.proxy.seat, Item.getItemFromBlock(Blocks.chest));
		this.parent=parent;
	}
    
    public int getSlotStackLimit(){
        return parent.getNumberPassengerSeats();
    }
}
