package mcinterface;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.FluidTank;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

/**Wrapper for the base ItemStack class.  This class allows for interaction with stack 
 * properties and NBT data, as well as handling some stack-specific operations.
 *
 * @author don_bruce
 */
public class WrapperItemStack{
	final ItemStack stack;
	
	WrapperItemStack(ItemStack stack){
		this.stack = stack;
	}
	
	public WrapperItemStack(AItemBase item){
		this.stack = new ItemStack(BuilderItem.itemWrapperMap.get(item));
	}
	
	/**
	 *  Returns a list of wrappers created from the JSON definition listing.  This is version-independent,
	 *  so as long as the definition contains the proper formatting, this will always return the correct value.
	 */
	public static List<WrapperItemStack> parseFromJSON(AJSONItem<?> packDef){
		List<WrapperItemStack> stackList = new ArrayList<WrapperItemStack>();
		try{
	    	for(String itemText : packDef.general.materials){
				int itemQty = Integer.valueOf(itemText.substring(itemText.lastIndexOf(':') + 1));
				itemText = itemText.substring(0, itemText.lastIndexOf(':'));
				
				int itemMetadata = Integer.valueOf(itemText.substring(itemText.lastIndexOf(':') + 1));
				itemText = itemText.substring(0, itemText.lastIndexOf(':'));
				stackList.add(new WrapperItemStack(new ItemStack(Item.getByNameOrId(itemText), itemQty, itemMetadata)));
			}
	    	return stackList;
		}catch(Exception e){
			throw new NullPointerException("ERROR: Could not parse crafting ingredients for item: " + packDef.packID + packDef.systemName + ".  Report this to the pack author!");
		}
	}
	
	/**
	 *  Returns the item that this stack is made of.  Only valid for {@link AItemBase} items.
	 */
	public AItemBase getItem(){
		return stack.getItem() instanceof BuilderItem ? ((BuilderItem) stack.getItem()).item : null;
	}
	
	/**
	 *  Returns the number of items in this stack.
	 */
	public int getSize(){
		return stack.getCount();
	}
	
	/**
	 *  Returns the max number of items that could be in this stack.
	 */
	public int getMaxSize(){
		return stack.getMaxStackSize();
	}
	
	/**
	 *  Returns the NBT data for this stack, as a wrapper.  If no NBT data is present,
	 *  then a new, blank,  wrapper instance is created.
	 */
	public WrapperNBT getData(){
		return stack.hasTagCompound() ? new WrapperNBT(stack.getTagCompound()) : new WrapperNBT();
	}
	
	/**
	 *  Sets the stack's data to the passed-in data.  This should be called after modifying any data
	 *  values as it cannot be assumed that the data returned from {@link #getData()} was not a
	 *  newly-created data block.
	 */
	public void setData(WrapperNBT data){
		stack.setTagCompound(data.tag);
	}
	
	/**
	 *  Attempts to fill the passed-in tank with this stack's contents, or drain the tank
	 *  into the stack for storage.  Returns the amount filled or drained if successful.
	 */
	public double interactWithTank(FluidTank tank, WrapperPlayer player){
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
						player.player.setHeldItem(EnumHand.MAIN_HAND, handler.getContainer());
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
					player.player.setHeldItem(EnumHand.MAIN_HAND, handler.getContainer());
					return amountDrained;
				}
			}
		}
		return 0;
	}
}