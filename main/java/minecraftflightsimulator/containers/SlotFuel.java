package minecraftflightsimulator.containers;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.entities.EntityParent;
import minecraftflightsimulator.packets.general.FuelPacket;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidContainerRegistry;
import cpw.mods.fml.common.IFuelHandler;

public class SlotFuel extends Slot{
	private EntityParent parent;
	
	public SlotFuel(EntityParent parent, int xDisplayPosition, int yDisplayPosition){
		super(parent, parent.fuelBucketSlot, xDisplayPosition, yDisplayPosition);
		this.parent = parent;
	}
	
    public boolean isItemValid(ItemStack item){
    	if(item.getItem().equals(Items.lava_bucket)){
    		return true;
    	}else if(FluidContainerRegistry.isBucket(item)){
    		if(item.getItem() instanceof IFuelHandler){
    			if(((IFuelHandler) item.getItem()).getBurnTime(item) > 0){
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
	    		if(item.getItem().equals(Items.lava_bucket)){
	    			parent.fuel = Math.min(parent.fuel + 1000, parent.maxFuel);
	    		}else{
	    			parent.fuel = Math.min(parent.fuel + ((IFuelHandler) item.getItem()).getBurnTime(item), parent.maxFuel);
	    		}
	    		parent.setInventorySlotContents(parent.emptyBucketSlot, new ItemStack(Items.bucket, buckets != null ? buckets.stackSize+1 : 1));
	    	}
    	}
    }
}
