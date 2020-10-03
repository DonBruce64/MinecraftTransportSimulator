package minecrafttransportsimulator.mcinterface;

import minecrafttransportsimulator.baseclasses.FluidTank;
import minecrafttransportsimulator.items.components.AItemBase;

/**Wrapper for the base ItemStack class.  This class allows for interaction with stack 
 * properties and NBT data, as well as handling some stack-specific operations.
 *
 * @author don_bruce
 */
public interface IWrapperItemStack{
	
	/**
	 *  Returns the item that this stack is made of.  Only valid for {@link AItemBase} items.
	 */
	public AItemBase getItem();
	
	/**
	 *  Returns the number of items in this stack.
	 */
	public int getSize();
	
	/**
	 *  Returns the max number of items that could be in this stack.
	 */
	public int getMaxSize();
	
	/**
	 *  Returns the NBT data for this stack, as a wrapper.  If no NBT data is present,
	 *  then a new, blank, wrapper instance is created.
	 */
	public IWrapperNBT getData();
	
	/**
	 *  Sets the stack's data to the passed-in data.  This should be called after modifying any data
	 *  values as it cannot be assumed that the data returned from {@link #getData()} was not a
	 *  newly-created data block that wasn't part of the item.
	 */
	public void setData(IWrapperNBT data);
	
	/**
	 *  Attempts to fill the passed-in tank with this stack's contents, or drain the tank
	 *  into the stack for storage.  Returns the amount filled or drained if successful.
	 */
	public double interactWithTank(FluidTank tank, IWrapperPlayer player);
}