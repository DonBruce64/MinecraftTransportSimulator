package mcinterface1122;

import minecrafttransportsimulator.baseclasses.FluidTank;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

class WrapperItemStack implements IWrapperItemStack{
	final ItemStack stack;
	
	WrapperItemStack(ItemStack stack){
		this.stack = stack;
	}
	
	@Override
	public AItemBase getItem(){
		return stack.getItem() instanceof BuilderItem ? ((BuilderItem) stack.getItem()).item : null;
	}
	
	@Override
	public int getSize(){
		return stack.getCount();
	}
	
	@Override
	public int getMaxSize(){
		return stack.getMaxStackSize();
	}
	
	@Override
	public WrapperNBT getData(){
		return stack.hasTagCompound() ? new WrapperNBT(stack.getTagCompound()) : new WrapperNBT(new NBTTagCompound());
	}
	
	@Override
	public void setData(WrapperNBT data){
		stack.setTagCompound(((WrapperNBT) data).tag);
	}
	
	@Override
	public double interactWithTank(FluidTank tank, IWrapperPlayer player){
		if(stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)){
			//If we are sneaking, drain this tank.  If we are not, fill it.
			if(!player.isSneaking()){
				//Item can provide fluid.  Check if the tank can accept it.
				IFluidHandlerItem handler = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
				FluidStack drainedStack = handler.drain(Integer.MAX_VALUE, false);
				if(drainedStack != null){
					//Able to take fluid from item, attempt to do so.
					int amountToDrain = (int) tank.fill(drainedStack.getFluid().getName(), drainedStack.amount, false);
					drainedStack = handler.drain(amountToDrain, !player.isCreative());
					if(drainedStack != null){
						//Was able to provide liquid from item.  Fill the tank.
						double amountFilled = tank.fill(drainedStack.getFluid().getName(), drainedStack.amount, true);
						((WrapperPlayer) player).player.setHeldItem(EnumHand.MAIN_HAND, handler.getContainer());
						return amountFilled;
					}
				}
			}else{
				//Item can hold fluid.  Check if we can fill it.
				IFluidHandlerItem handler = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
				FluidStack containedStack = FluidRegistry.getFluidStack(tank.getFluid(), (int) tank.getFluidLevel());
				int amountFilled = handler.fill(containedStack, !player.isCreative());
				if(amountFilled > 0){
					//Were able to fill the item.  Apply state change to tank and item.
					double amountDrained = tank.drain(tank.getFluid(), amountFilled, true);
					((WrapperPlayer) player).player.setHeldItem(EnumHand.MAIN_HAND, handler.getContainer());
					return amountDrained;
				}
			}
		}
		return 0;
	}
}