package minecrafttransportsimulator.blocks.core;

import javax.annotation.Nullable;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.SimpleComponent;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.packets.general.PacketChat;
import minecrafttransportsimulator.packets.tileentities.PacketFuelPumpConnection;
import minecrafttransportsimulator.packets.tileentities.PacketFuelPumpFillDrain;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
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
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;

@Optional.Interface(iface = "li.cil.oc.api.network.SimpleComponent", modid = "opencomputers")
public class TileEntityFuelPump extends TileEntityBase implements IFluidTank, IFluidHandler, ITickable, SimpleComponent {
    private EntityVehicleE_Powered connectedVehicle;
    public int totalTransfered;
    
	private FluidTankInfo tankInfo;
    private static final FluidTankInfo emptyTankInfo = new FluidTankInfo(null, 15000);
	    
	public TileEntityFuelPump(){
		super();
		clearFluid();
	}
	
	@Override
	public void update(){
		if(connectedVehicle != null){
			if(connectedVehicle.isDead){
				setConnectedVehicle(null);
				return;
			}
			//Check distance to make sure the vehicle hasn't moved away.
			if(Math.sqrt(connectedVehicle.getPosition().distanceSq(getPos())) > 20){
				setConnectedVehicle(null);
				if(!world.isRemote){
					MTS.MTSNet.sendToAllAround(new PacketChat("interact.fuelpump.toofar"), new TargetPoint(world.provider.getDimension(), this.pos.getX(), this.pos.getY(), this.pos.getZ(), 25));
				}
				return;
			}
			if(connectedVehicle.pack.motorized.fuelCapacity - connectedVehicle.fuel >= 10){
				if(tankInfo.fluid != null){
					int fuelToFill = Math.min(this.tankInfo.fluid.amount, 10);
					this.tankInfo.fluid.amount -= fuelToFill;
					connectedVehicle.fuel += fuelToFill;
					connectedVehicle.fluidName = FluidRegistry.getFluidName(getFluid().getFluid());
					totalTransfered += fuelToFill;
					if(this.tankInfo.fluid.amount == 0){
						setConnectedVehicle(null);
						this.tankInfo = emptyTankInfo;
						if(!world.isRemote){
							MTS.MTSNet.sendToAllAround(new PacketChat("interact.fuelpump.empty"), new TargetPoint(world.provider.getDimension(), this.pos.getX(), this.pos.getY(), this.pos.getZ(), 16));
						}
					}
				}else{
					setConnectedVehicle(null);
					if(!world.isRemote){
						MTS.MTSNet.sendToAllAround(new PacketChat("interact.fuelpump.empty"), new TargetPoint(world.provider.getDimension(), this.pos.getX(), this.pos.getY(), this.pos.getZ(), 16));
					}
				}
			}else{
				setConnectedVehicle(null);
				if(!world.isRemote){
					MTS.MTSNet.sendToAllAround(new PacketChat("interact.fuelpump.complete"), new TargetPoint(world.provider.getDimension(), this.pos.getX(), this.pos.getY(), this.pos.getZ(), 16));
				}
			}
		}
	}
	
	public void setFluid(Fluid fluid){
		tankInfo = new FluidTankInfo(new FluidStack(fluid, 0), emptyTankInfo.capacity);
	}
	
	public void clearFluid(){
		tankInfo = emptyTankInfo;
	}
	
	public EntityVehicleE_Powered getConnectedVehicle(){
		return this.connectedVehicle;
	}
	
	public void setConnectedVehicle(EntityVehicleE_Powered vehicle){
		if(vehicle == null){
			this.connectedVehicle = null;
		}else{
			this.connectedVehicle = vehicle;
			this.totalTransfered = 0;
		}
		if(!world.isRemote){
			MTS.MTSNet.sendToAll(new PacketFuelPumpConnection(this, connectedVehicle != null ? connectedVehicle.getEntityId() : -1, this.tankInfo.fluid != null ? this.tankInfo.fluid.amount : 0, this.totalTransfered));
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
			int amountAbleToFill = tankInfo.capacity - (tankInfo.fluid != null ? tankInfo.fluid.amount : 0);
			int amountToFill = (int) Math.min(amountAbleToFill, stack.amount);
			if(doFill){
				if(tankInfo.fluid == null){
					this.setFluid(stack.getFluid());
				}
				tankInfo.fluid.amount += amountToFill;
				FluidEvent.fireEvent(new FluidEvent.FluidFillingEvent(tankInfo.fluid, world, getPos(), this, amountToFill));
				MTS.MTSNet.sendToAll(new PacketFuelPumpFillDrain(this, new FluidStack(tankInfo.fluid, amountToFill), false));
			}
			return amountToFill;
		}else{
			return 0;
		}
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain){
		if(tankInfo.fluid != null){
			FluidStack fluidToDrain = new FluidStack(tankInfo.fluid, Math.min(maxDrain, tankInfo.fluid.amount));
			if(doDrain){
				tankInfo.fluid.amount -= fluidToDrain.amount;
				FluidEvent.fireEvent(new FluidEvent.FluidDrainingEvent(tankInfo.fluid, world, getPos(), this, fluidToDrain.amount));
				MTS.MTSNet.sendToAll(new PacketFuelPumpFillDrain(this, new FluidStack(tankInfo.fluid, fluidToDrain.amount), true));
				if(tankInfo.fluid.amount == 0){
					this.clearFluid();
				}
			}
			return fluidToDrain;
		}else{
			return null;
		}
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

	@Override
	public String getComponentName() {
		return "iv_fuelpump"; // INFO: Max length is 14 chars
	}

	/* Getter */
	@Callback(doc = "function():boolean; Returns true if there's fluid in the tank", direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] isFluidInTank(Context context, Arguments args) {
		return new Object[] { tankInfo.fluid != null };
	}

	@Callback(doc = "function():string; Returns the localized name of the fluid in the tank. Returns false if there is not fluid in the tank", direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getFluidLocalizedName(Context context, Arguments args) {
		return new Object[] { tankInfo.fluid != null ? tankInfo.fluid.getLocalizedName() : false };
	}

	@Callback(doc = "function():int; Returns the actual fluid amount in the tank.", direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getFluidAmount(Context context, Arguments args) {
		return new Object[] { tankInfo.fluid != null ? tankInfo.fluid.amount : 0 };
	}

	@Callback(doc = "function():boolean; Returns the total transferred fluid amount", direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getTotalTransfered(Context context, Arguments args) {
		return new Object[] { totalTransfered };
	}

	@Callback(doc = "function():boolean; Returns if a vehicle is connected", direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] isVehicleConnected(Context context, Arguments args) {
		return new Object[] { connectedVehicle != null };
	}
}
