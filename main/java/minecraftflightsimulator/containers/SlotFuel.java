package minecraftflightsimulator.containers;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.entities.core.EntityVehicle;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

public class SlotFuel extends SlotItem{
	private EntityVehicle vehicle;
	
	public SlotFuel(EntityVehicle vehicle, int xDisplayPosition, int yDisplayPosition){
		super(vehicle, xDisplayPosition, yDisplayPosition, vehicle.fuelBucketSlot);
		this.vehicle = vehicle;
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
    			if(vehicle.fuel < vehicle.maxFuel && vehicle.getStackInSlot(vehicle.emptyBucketSlot) == null){
	    			FluidStack fluidStack = ((IFluidContainerItem) stack.getItem()).getFluid(stack);
	    			if(fluidStack != null){
	    				if(fluidStack.getFluid() != null){
	    					if(MFS.fluidValues.containsKey(fluidStack.getFluid().getName())){
	    						double fuelValue = MFS.fluidValues.get(fluidStack.getFluid().getName());
	    						FluidStack drainedFluid = ((IFluidContainerItem) stack.getItem()).drain(stack, (int) ((vehicle.maxFuel - vehicle.fuel)*fuelValue), true);
	    						vehicle.fuel += drainedFluid.amount;
	    						vehicle.setInventorySlotContents(vehicle.emptyBucketSlot, stack);
	    						this.putStack(null);
    						}
    					}
    				}
    			}
    		}else if(FluidContainerRegistry.isFilledContainer(stack)){
    			FluidStack fluidStack = FluidContainerRegistry.getFluidForFilledItem(stack);
    			if(vehicle.getStackInSlot(vehicle.emptyBucketSlot) != null){
    				ItemStack emptyBucketStack = vehicle.getStackInSlot(vehicle.emptyBucketSlot);
    				ItemStack emptyContainerStack = FluidContainerRegistry.drainFluidContainer(stack.copy());
    				if(!emptyBucketStack.getItem().equals(emptyContainerStack.getItem()) || emptyBucketStack.stackSize == emptyBucketStack.getMaxStackSize()){
    					return;
    				}
    			}
    			if(MFS.fluidValues.containsKey(fluidStack.getFluid().getName())){
    				double fuelValue = MFS.fluidValues.get(fluidStack.getFluid().getName());  
    				if((vehicle.fuel + fluidStack.amount*fuelValue) - 100 < vehicle.maxFuel){
    					vehicle.fuel = Math.min(vehicle.fuel + fluidStack.amount*fuelValue, vehicle.maxFuel);
    					if(vehicle.getStackInSlot(vehicle.emptyBucketSlot) != null){
    						++vehicle.getStackInSlot(vehicle.emptyBucketSlot).stackSize; 
    					}else{
    						vehicle.setInventorySlotContents(vehicle.emptyBucketSlot, FluidContainerRegistry.drainFluidContainer(stack));
    					}
    					this.putStack(null);
    				}
    			}
    		}
    	}
    }
}
