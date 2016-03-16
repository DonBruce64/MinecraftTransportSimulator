package minecraftflightsimulator.containers;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.entities.EntityParent;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;

public class SlotFuel extends Slot{
	private EntityParent parent;
	
	public SlotFuel(EntityParent parent, int xDisplayPosition, int yDisplayPosition){
		super(parent, parent.fuelBucketSlot, xDisplayPosition, yDisplayPosition);
		this.parent = parent;
	}
	
    public boolean isItemValid(ItemStack item){
    	FluidStack stack = FluidContainerRegistry.getFluidForFilledItem(item);
    	if(stack != null){
    		if(MFS.fluidValues.containsKey(stack.getFluid().getName())){
    			if(MFS.fluidValues.get(stack.getFluid().getName()) > 0){
    				return true;
    			}
    		}
    	}
    	return false;
    }
    
    @Override
    public void putStack(ItemStack item){
    	super.putStack(item);
    	if(item != null){
    		ItemStack buckets = parent.getStackInSlot(parent.emptyBucketSlot);
    		if(buckets != null){
    			if(buckets.stackSize == Items.bucket.getItemStackLimit(buckets)){
    				return;
    			}
    		}
	    	if(parent.fuel < parent.maxFuel - 100){
	    		this.putStack(null);
	    		int fuelValue = MFS.fluidValues.get(FluidContainerRegistry.getFluidForFilledItem(item).getFluid().getName());
	    		parent.fuel = Math.min(parent.fuel + fuelValue, parent.maxFuel);
	    		parent.setInventorySlotContents(parent.emptyBucketSlot, new ItemStack(Items.bucket, buckets != null ? buckets.stackSize+1 : 1));
	    	}
    	}
    }
}
