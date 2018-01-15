package minecrafttransportsimulator.blocks;

import javax.annotation.Nullable;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MTSTileEntity;
import minecrafttransportsimulator.packets.general.FuelPumpFillDrainPacket;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidEvent;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

public class TileEntityFuelPump extends MTSTileEntity implements IFluidTank, IFluidHandler, ITickable{
    private FluidTankInfo tankInfo;
    private FluidTankInfo emptyTankInfo = new FluidTankInfo(null, 15000);
	    
	public TileEntityFuelPump(){
		super();
		this.tankInfo = emptyTankInfo;
	}
	
	@Override
	public void update(){
		if(worldObj.getTotalWorldTime()%20 == 0)System.out.println(" " + (tankInfo.fluid != null ? tankInfo.fluid.amount : 0));
	}

	@Override
	public IFluidTankProperties[] getTankProperties(){
		return FluidTankProperties.convert(new FluidTankInfo[]{tankInfo});
	}

	@Override
	public FluidStack getFluid(){
		return tankInfo.fluid;
	}
	
	public void setFluid(Fluid fluid){
		tankInfo = new FluidTankInfo(new FluidStack(fluid, 0), emptyTankInfo.capacity);
	}

	@Override
	public int getFluidAmount(){
		return tankInfo.fluid.amount;
	}

	@Override
	public int getCapacity(){
		return tankInfo.capacity;
	}

	@Override
	public FluidTankInfo getInfo(){
		return tankInfo;
	}

	@Override
	public int fill(FluidStack stack, boolean doFill){
		if(stack.isFluidEqual(tankInfo.fluid) || tankInfo.fluid == null){
			double fuelFactor = ConfigSystem.getFuelValue(FluidRegistry.getFluidName(stack.getFluid()));
			int amountAbleToFill = Math.min(tankInfo.capacity - (tankInfo.fluid != null ? tankInfo.fluid.amount : 0), 1000);
			int amountToFill = (int) Math.min(amountAbleToFill, stack.amount*fuelFactor);
			if(doFill){
				if(tankInfo.fluid == null){
					this.setFluid(stack.getFluid());
				}
				tankInfo.fluid.amount += amountToFill;
				FluidEvent.fireEvent(new FluidEvent.FluidFillingEvent(tankInfo.fluid, worldObj, getPos(), this, amountToFill));
				MTS.MTSNet.sendToAll(new FuelPumpFillDrainPacket(this, new FluidStack(tankInfo.fluid, amountToFill)));
			}
			return amountToFill;
		}else{
			return 0;
		}
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain){
		if(maxDrain < tankInfo.fluid.amount){
			if(doDrain){
				tankInfo.fluid.amount -= maxDrain;
				FluidEvent.fireEvent(new FluidEvent.FluidDrainingEvent(tankInfo.fluid, worldObj, getPos(), this, maxDrain));
				MTS.MTSNet.sendToAll(new FuelPumpFillDrainPacket(this, new FluidStack(tankInfo.fluid, -maxDrain)));
			}
			return new FluidStack(tankInfo.fluid, maxDrain);
		}else{
			FluidStack returnedFluid = tankInfo.fluid;
			if(doDrain){
				FluidEvent.fireEvent(new FluidEvent.FluidDrainingEvent(returnedFluid, worldObj, getPos(), this, returnedFluid.amount));
				MTS.MTSNet.sendToAll(new FuelPumpFillDrainPacket(this, new FluidStack(returnedFluid, -returnedFluid.amount)));
				this.tankInfo = emptyTankInfo;
			}
			return returnedFluid;
		}
	}
	
	@Override
	public FluidStack drain(FluidStack stack, boolean doDrain){
		if(stack.isFluidEqual(this.tankInfo.fluid)){
			return this.drain(stack.amount, doDrain);
		}else{
			return null;
		}
	}
	
    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing){
    	//Only let fluid be interacted with on the bottom face.
    	if(facing != null && facing.equals(facing.DOWN)){
    		return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    	}else{
    		return super.hasCapability(capability, facing);
    	}
    }
	
	@Override
    public void readFromNBT(NBTTagCompound tagCompound){
        super.readFromNBT(tagCompound);
        if(!tagCompound.hasKey("Empty")){
        	this.tankInfo = new FluidTankInfo(FluidStack.loadFluidStackFromNBT(tagCompound), emptyTankInfo.capacity);
        }else{
            this.tankInfo = emptyTankInfo;
        }
    }
    
	@Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
        super.writeToNBT(tagCompound);
        if(tankInfo.fluid != null){
        	tankInfo.fluid.writeToNBT(tagCompound);
        }else{
        	tagCompound.setString("Empty", "");
        }
		return tagCompound;
    }
}
