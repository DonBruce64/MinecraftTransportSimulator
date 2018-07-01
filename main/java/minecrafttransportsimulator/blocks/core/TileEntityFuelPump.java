package minecrafttransportsimulator.blocks.core;

import javax.annotation.Nullable;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.multipart.main.EntityMultipartE_Vehicle;
import minecrafttransportsimulator.packets.general.ChatPacket;
import minecrafttransportsimulator.packets.tileentities.FuelPumpConnectionPacket;
import minecrafttransportsimulator.packets.tileentities.FuelPumpFillDrainPacket;
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
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;

public class TileEntityFuelPump extends TileEntityRotatable implements IFluidTank, IFluidHandler, ITickable{
    private EntityMultipartE_Vehicle connectedVehicle;
    public int totalTransfered;
    
	private FluidTankInfo tankInfo;
    private static final FluidTankInfo emptyTankInfo = new FluidTankInfo(null, 15000);
	    
	public TileEntityFuelPump(){
		super();
		this.tankInfo = emptyTankInfo;
	}
	
	@Override
	public void update(){
		if(connectedVehicle != null){
			if(connectedVehicle.isDead){
				setConnectedVehicle(null);
				return;
			}
			if(connectedVehicle.pack.motorized.fuelCapacity - connectedVehicle.fuel >= 10){
				if(this.tankInfo.fluid != null){
					int fuelToFill = Math.min(this.tankInfo.fluid.amount, 10);
					this.tankInfo.fluid.amount -= fuelToFill;
					connectedVehicle.fuel += fuelToFill;
					totalTransfered += fuelToFill;
					if(this.tankInfo.fluid.amount == 0){
						setConnectedVehicle(null);
						this.tankInfo = emptyTankInfo;
						if(!worldObj.isRemote){
							MTS.MTSNet.sendToAllAround(new ChatPacket("interact.fuelpump.empty"), new TargetPoint(worldObj.provider.getDimension(), this.pos.getX(), this.pos.getY(), this.pos.getZ(), 16));
						}
					}
				}else{
					setConnectedVehicle(null);
					if(!worldObj.isRemote){
						MTS.MTSNet.sendToAllAround(new ChatPacket("interact.fuelpump.empty"), new TargetPoint(worldObj.provider.getDimension(), this.pos.getX(), this.pos.getY(), this.pos.getZ(), 16));
					}
				}
			}else{
				setConnectedVehicle(null);
				if(!worldObj.isRemote){
					MTS.MTSNet.sendToAllAround(new ChatPacket("interact.fuelpump.complete"), new TargetPoint(worldObj.provider.getDimension(), this.pos.getX(), this.pos.getY(), this.pos.getZ(), 16));
				}
			}
		}
	}
	
	public void setFluid(Fluid fluid){
		tankInfo = new FluidTankInfo(new FluidStack(fluid, 0), emptyTankInfo.capacity);
	}
	
	public EntityMultipartE_Vehicle getConnectedVehicle(){
		return this.connectedVehicle;
	}
	
	public void setConnectedVehicle(EntityMultipartE_Vehicle vehicle){
		if(vehicle == null){
			this.connectedVehicle = null;
		}else{
			this.connectedVehicle = vehicle;
			this.totalTransfered = 0;
		}
		if(!worldObj.isRemote){
			MTS.MTSNet.sendToAll(new FuelPumpConnectionPacket(this, connectedVehicle != null ? connectedVehicle.getEntityId() : -1, this.tankInfo.fluid != null ? this.tankInfo.fluid.amount : 0, this.totalTransfered));
		}
	}

	@Override
	public IFluidTankProperties[] getTankProperties(){
		return FluidTankProperties.convert(new FluidTankInfo[]{tankInfo});
	}

	@Override
	public FluidStack getFluid(){
		return tankInfo.fluid;
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
		if(tankInfo.fluid == null || stack.isFluidEqual(tankInfo.fluid)){
			double fuelFactor = ConfigSystem.getFuelValue(FluidRegistry.getFluidName(stack.getFluid()));
			int amountAbleToFill = tankInfo.capacity - (tankInfo.fluid != null ? tankInfo.fluid.amount : 0);
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
		return null;
	}
	
	@Override
	public FluidStack drain(FluidStack stack, boolean doDrain){
		return null;
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
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing){
    	if(facing != null && facing.equals(facing.DOWN)){
    		if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY){
    			return (T) this;
    		}
    	}
    	return super.getCapability(capability, facing);
    }
	
	@Override
    public void readFromNBT(NBTTagCompound tagCompound){
        super.readFromNBT(tagCompound);
        this.totalTransfered = tagCompound.getInteger("totalTransfered");
        if(!tagCompound.hasKey("Empty")){
        	this.tankInfo = new FluidTankInfo(FluidStack.loadFluidStackFromNBT(tagCompound), emptyTankInfo.capacity);
        }else{
            this.tankInfo = emptyTankInfo;
        }
    }
    
	@Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
        super.writeToNBT(tagCompound);
        tagCompound.setInteger("totalTransfered", this.totalTransfered);
        if(tankInfo.fluid != null){
        	tankInfo.fluid.writeToNBT(tagCompound);
        }else{
        	tagCompound.setString("Empty", "");
        }
		return tagCompound;
    }
}
