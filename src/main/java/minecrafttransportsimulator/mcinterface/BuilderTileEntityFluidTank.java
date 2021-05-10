package minecrafttransportsimulator.mcinterface;

import javax.annotation.Nullable;

import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityFluidTankProvider;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityTickable;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
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

/**Builder for tile entities that contain fluids.  This builder ticks.
 *
 * @author don_bruce
 */
public class BuilderTileEntityFluidTank<FluidTankTileEntity extends ATileEntityBase<? extends AJSONItem> & ITileEntityFluidTankProvider> extends BuilderTileEntity<FluidTankTileEntity> implements ITickable, IFluidTank, IFluidHandler{
	
	public BuilderTileEntityFluidTank(){
		super();
	}
	
	@Override
	public void update(){
		if(tileEntity != null){
			((ITileEntityTickable) tileEntity).update();
		}
	}

	@Override
	public IFluidTankProperties[] getTankProperties(){
		return FluidTankProperties.convert(new FluidTankInfo[]{getInfo()});
	}

	@Override
	public FluidStack getFluid(){
		return tileEntity != null && !tileEntity.getTank().getFluid().isEmpty() ? new FluidStack(FluidRegistry.getFluid(tileEntity.getTank().getFluid()), (int) tileEntity.getTank().getFluidLevel()) : null;
	}

	@Override
	public int getFluidAmount(){
		return (int) (tileEntity != null ? tileEntity.getTank().getFluidLevel() : 0);
	}

	@Override
	public int getCapacity(){
		return (int) (tileEntity != null ? tileEntity.getTank().getFluidLevel() : 0);
	}

	@Override
	public FluidTankInfo getInfo(){
		return new FluidTankInfo(this);
	}

	@Override
	public int fill(FluidStack stack, boolean doFill){
		if(tileEntity != null){
			int fillAmount = (int) tileEntity.getTank().fill(stack.getFluid().getName(), stack.amount, doFill);
			if(fillAmount > 0 && doFill){
				FluidEvent.fireEvent(new FluidEvent.FluidFillingEvent(new FluidStack(stack.getFluid(), fillAmount), world, getPos(), this, fillAmount));
			}
			return fillAmount;
		}else{
			return 0;
		}
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain){
		if(getFluidAmount() > 0){
			return this.drain(new FluidStack(getFluid().getFluid(), maxDrain), doDrain);
		}
		return null;
	}
	
	@Override
	public FluidStack drain(FluidStack stack, boolean doDrain){
		int drainAmount = (int) (tileEntity != null ? tileEntity.getTank().drain(stack.getFluid().getName(), stack.amount, doDrain) : 0);
		if(drainAmount > 0 && doDrain){
			FluidEvent.fireEvent(new FluidEvent.FluidDrainingEvent(new FluidStack(stack.getFluid(), drainAmount), world, getPos(), this, drainAmount));
		}
		return new FluidStack(stack.getFluid(), drainAmount);
	}
	
    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing){
    	if(facing != null && tileEntity != null && tileEntity.canConnectOnAxis(Axis.valueOf(facing.name()))){
    		return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    	}else{
    		return super.hasCapability(capability, facing);
    	}
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing){
    	if(facing != null && tileEntity != null && tileEntity.canConnectOnAxis(Axis.valueOf(facing.name()))){
    		if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY){
    			return (T) this;
    		}
	    }
    	return super.getCapability(capability, facing);
    }
}
