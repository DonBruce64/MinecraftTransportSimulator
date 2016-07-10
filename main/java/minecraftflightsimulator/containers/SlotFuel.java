package minecraftflightsimulator.containers;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.entities.core.EntityParent;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

public class SlotFuel extends SlotItem{
	private EntityParent parent;
	
	public SlotFuel(EntityParent parent, int xDisplayPosition, int yDisplayPosition){
		super(parent, xDisplayPosition, yDisplayPosition, parent.fuelBucketSlot);
		this.parent = parent;
	}
	
    public boolean isItemValid(ItemStack stack){
    	FluidStack fluidStack;
    	if(stack.getItem() instanceof IFluidContainerItem){
    		fluidStack = ((IFluidContainerItem) stack.getItem()).getFluid(stack);
    	}else{
    		fluidStack = FluidContainerRegistry.getFluidForFilledItem(stack);
    	}
    	if(fluidStack != null){
			if(MFS.fluidValues.containsKey(fluidStack.getFluid().getName())){
				return MFS.fluidValues.get(fluidStack.getFluid().getName()) > 0;
			}
		}
    	return false;
    }
    
    @Override
    public void putStack(ItemStack stack){
    	super.putStack(stack);
    	if(stack != null){
    		if(stack.getItem() instanceof IFluidContainerItem){
    			if(parent.fuel < parent.maxFuel && parent.getStackInSlot(parent.emptyBucketSlot) == null){
	    			FluidStack fluidStack = ((IFluidContainerItem) stack.getItem()).getFluid(stack);
	    			if(fluidStack != null){
	    				if(fluidStack.getFluid() != null){
	    					if(MFS.fluidValues.containsKey(fluidStack.getFluid().getName())){
	    						double fuelValue = MFS.fluidValues.get(fluidStack.getFluid().getName());
	    						FluidStack drainedFluid = ((IFluidContainerItem) stack.getItem()).drain(stack, (int) ((parent.maxFuel - parent.fuel)*fuelValue), true);
	    						parent.fuel += drainedFluid.amount;
	    						parent.setInventorySlotContents(parent.emptyBucketSlot, stack);
	    						this.putStack(null);
    						}
    					}
    				}
    			}
    		}else if(FluidContainerRegistry.isFilledContainer(stack)){
    			FluidStack fluidStack = FluidContainerRegistry.getFluidForFilledItem(stack);
    			if(parent.getStackInSlot(parent.emptyBucketSlot) != null){
    				ItemStack emptyBucketStack = parent.getStackInSlot(parent.emptyBucketSlot);
    				ItemStack emptyContainerStack = FluidContainerRegistry.drainFluidContainer(stack.copy());
    				if(!emptyBucketStack.getItem().equals(emptyContainerStack.getItem()) || emptyBucketStack.stackSize == emptyBucketStack.getMaxStackSize()){
    					return;
    				}
    			}
    			if(MFS.fluidValues.containsKey(fluidStack.getFluid().getName())){
    				double fuelValue = MFS.fluidValues.get(fluidStack.getFluid().getName());  
    				if((parent.fuel + fluidStack.amount*fuelValue) - 100 < parent.maxFuel){
    					parent.fuel = Math.min(parent.fuel + fluidStack.amount*fuelValue, parent.maxFuel);
    					if(parent.getStackInSlot(parent.emptyBucketSlot) != null){
    						++parent.getStackInSlot(parent.emptyBucketSlot).stackSize; 
    					}else{
    						parent.setInventorySlotContents(parent.emptyBucketSlot, FluidContainerRegistry.drainFluidContainer(stack));
    					}
    					this.putStack(null);
    				}
    			}
    		}
    	}
    }
}
