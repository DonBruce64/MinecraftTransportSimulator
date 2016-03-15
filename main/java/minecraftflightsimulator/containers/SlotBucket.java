package minecraftflightsimulator.containers;

import minecraftflightsimulator.entities.EntityParent;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class SlotBucket extends Slot{
	private EntityParent parent;

	public SlotBucket(EntityParent parent, int xDisplayPosition, int yDisplayPosition){
		super(parent, parent.emptyBucketSlot, xDisplayPosition, yDisplayPosition);
		this.parent = parent;
	}
	
    public boolean isItemValid(ItemStack item){    	
    	return false;
    }
    
    @Override
    public void onSlotChanged(){
    	super.onSlotChanged();
    	parent.emptyBuckets = (byte) (this.getStack()==null ? 0 : this.getStack().stackSize);
    }
}
