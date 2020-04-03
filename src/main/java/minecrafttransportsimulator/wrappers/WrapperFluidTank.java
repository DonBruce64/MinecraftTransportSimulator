package minecrafttransportsimulator.wrappers;

import javax.annotation.Nullable;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityFluidTank;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidEvent;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

/**Wrapper for tile entities that contain fluids.
 *
 * @author don_bruce
 */
public class WrapperFluidTank extends WrapperTileEntityTickable implements IFluidTank, IFluidHandler{
    private final ATileEntityFluidTank tank;
	    
	WrapperFluidTank(ATileEntityFluidTank tileEntity){
		super(tileEntity);
		this.tank = tileEntity;
	}

	@Override
	public IFluidTankProperties[] getTankProperties(){
		return FluidTankProperties.convert(new FluidTankInfo[]{getInfo()});
	}

	@Override
	public FluidStack getFluid(){
		return !tank.getFluid().isEmpty() ? new FluidStack(FluidRegistry.getFluid(tank.getFluid()), tank.getFluidLevel()) : null;
	}

	@Override
	public int getFluidAmount(){
		return tank.getFluidLevel();
	}

	@Override
	public int getCapacity(){
		return tank.getFluidLevel();
	}

	@Override
	public FluidTankInfo getInfo(){
		return new FluidTankInfo(this);
	}

	@Override
	public int fill(FluidStack stack, boolean doFill){
		int fillAmount = tank.fill(stack.getFluid().getName(), stack.amount, !doFill);
		if(fillAmount > 0){
			FluidEvent.fireEvent(new FluidEvent.FluidFillingEvent(new FluidStack(getFluid().getFluid(), fillAmount), world, getPos(), this, fillAmount));
		}
		return fillAmount;
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain){
		if(tank.getFluidLevel() > 0){
			return this.drain(new FluidStack(getFluid().getFluid(), maxDrain), doDrain);
		}
		return null;
	}
	
	@Override
	public FluidStack drain(FluidStack stack, boolean doDrain){
		int drainAmount = tank.drain(tank.getFluid(), stack.amount, !doDrain);
		if(drainAmount > 0){
			FluidEvent.fireEvent(new FluidEvent.FluidDrainingEvent(new FluidStack(getFluid().getFluid(), drainAmount), world, getPos(), this, drainAmount));
		}
		return new FluidStack(stack.getFluid(), drainAmount);
	}
	
    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing){
    	//Only let fluid be interacted with on the bottom face.
    	if(facing != null && facing.equals(EnumFacing.DOWN)){
    		return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    	}else{
    		return super.hasCapability(capability, facing);
    	}
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing){
    	if(facing != null && facing.equals(EnumFacing.DOWN)){
    		if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY){
    			return (T) this;
    		}
    	}
    	return super.getCapability(capability, facing);
    }
}
