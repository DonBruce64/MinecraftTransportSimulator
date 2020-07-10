package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.packets.general.PacketChat;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fluids.FluidEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

public final class PartBarrel extends APart implements IFluidTank, IFluidHandler{
	
	private FluidTankInfo tankInfo;
	private final FluidTankInfo emptyTankInfo;
	
	public PartBarrel(EntityVehicleF_Physics vehicle, VehiclePart packVehicleDef, JSONPart definition, NBTTagCompound dataTag){
		super(vehicle, packVehicleDef, definition, dataTag);
		this.emptyTankInfo =  new FluidTankInfo(null, definition.barrel.capacity);
		if(dataTag.hasKey("FluidName")){
        	this.tankInfo = new FluidTankInfo(FluidStack.loadFluidStackFromNBT(dataTag), emptyTankInfo.capacity);
        }else{
            this.tankInfo = emptyTankInfo;
        }
	}
	
	@Override
	public boolean interactPart(EntityPlayer player){
		if(!vehicle.locked){
			ItemStack stack = player.getHeldItemMainhand();
			if(stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)){
				IFluidHandlerItem handler = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
				//If we are empty or sneaking, drain the barrel, otherwise fill.
				//We use 1000 here for the test as buckets will only drain that amount.
				FluidStack drainedTestStack = handler.drain(1000, false);
				if(player.isSneaking() || drainedTestStack == null || drainedTestStack.amount == 0){
					if(this.getFluid() != null){
						this.drain(handler.fill(this.getFluid(), true), true);
					}							
				}else{
					if(this.getFluid() != null){
						this.fill(handler.drain(new FluidStack(this.getFluid().getFluid(), this.getCapacity() - this.getFluidAmount()), true), true);
					}else{
						this.fill(handler.drain(this.getCapacity() - this.getFluidAmount(), true), true);
					}
				}
				player.setHeldItem(EnumHand.MAIN_HAND, handler.getContainer());
			}
		}else{
			MTS.MTSNet.sendTo(new PacketChat("interact.failure.vehiclelocked"), (EntityPlayerMP) player);
		}
		return true;
    }
	
	@Override
	public NBTTagCompound getData(){
		NBTTagCompound dataTag = new NBTTagCompound();
        if(tankInfo.fluid != null){
        	tankInfo.fluid.writeToNBT(dataTag);
        }
		return dataTag;
	}
	
	@Override
	public float getWidth(){
		return 1.0F;
	}

	@Override
	public float getHeight(){
		return 1.0F;
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
		return tankInfo.fluid != null ? tankInfo.fluid.amount : 0;
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
		if(stack != null && (tankInfo.fluid == null || stack.isFluidEqual(tankInfo.fluid))){
			int amountAbleToFill = tankInfo.capacity - (tankInfo.fluid != null ? tankInfo.fluid.amount : 0);
			int amountToFill = Math.min(amountAbleToFill, stack.amount);
			if(doFill){
				if(tankInfo.fluid == null){
					tankInfo = new FluidTankInfo(new FluidStack(stack.getFluid(), 0), emptyTankInfo.capacity);
				}
				tankInfo.fluid.amount += amountToFill;
				FluidEvent.fireEvent(new FluidEvent.FluidFillingEvent(tankInfo.fluid, vehicle.world, vehicle.getPosition(), this, amountToFill));
			}
			return amountToFill;
		}else{
			return 0;
		}
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain){
		return this.getFluid() != null ? this.drain(new FluidStack(this.getFluid(),  maxDrain), doDrain) : null;
	}
	
	@Override
	public FluidStack drain(FluidStack stack, boolean doDrain){
		if(this.getFluid() != null && this.getFluid().getFluid().equals(stack.getFluid())){
			int amountToDrain = Math.max(this.getFluidAmount(), stack.amount);
			if(doDrain){
				tankInfo.fluid.amount -= amountToDrain;
				if(tankInfo.fluid.amount == 0){
					 this.tankInfo = emptyTankInfo;
				}
			}
			FluidStack returnStack = new FluidStack(stack.getFluid(), amountToDrain);
			FluidEvent.fireEvent(new FluidEvent.FluidDrainingEvent(returnStack, vehicle.world, vehicle.getPosition(), this, amountToDrain));
			return returnStack;
		}
		return null;
	}
}
