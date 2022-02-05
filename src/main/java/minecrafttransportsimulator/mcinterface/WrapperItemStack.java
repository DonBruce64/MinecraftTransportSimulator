package minecrafttransportsimulator.mcinterface;

import java.util.List;

import minecrafttransportsimulator.entities.instances.EntityFluidTank;
import minecrafttransportsimulator.items.components.AItemBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

/**Wrapper for item stacks.  Contains only a few methods, and ones that are
 * better named than the default ones in stacks.  Also, they WON'T change
 * on version changes.
 *
 * @author don_bruce
 */
public class WrapperItemStack{
	private static final TileEntityFurnace VANILLA_FAKE_FURNACE = new TileEntityFurnace();
	
	protected final ItemStack stack;
	
	protected WrapperItemStack(ItemStack stack){
		this.stack = stack;
	}
	
	/**
	 *  Returns true if the passed-in stack and this stack are a match for
	 *  all properties (excluding stack size).  This takes into account item,
	 *  damage, and data.  Used for checking if stacks can be combined.
	 */
	public boolean isCompleteMatch(WrapperItemStack other){
		return other.stack.isItemEqual(stack) && (other.stack.hasTagCompound() ? other.stack.getTagCompound().equals(stack.getTagCompound()) : !stack.hasTagCompound());
	}
	
	/**
	 *  Returns true if the passed-in stack and this stack are a match for
	 *  each other's items.  Does not take into account other properties.
	 */
	public boolean isItemMatch(WrapperItemStack other){
		return stack.getItem().equals(other.getItem());
	}
	
	/**
	 *  Returns the fuel amount (in ticks) for this item.
	 *  Only returns the value for one item in the stack, not all items.
	 */
	public int getFuelValue(){
		return TileEntityFurnace.getItemBurnTime(stack);
	}
	
	/**
	 *  Returns the item that this item can be smelted to make, or an empty stack
	 *  if this item cannot be smelted.  Note the the returned stack is a new instance
	 *  and may be modified without affecting future calls to this method.
	 */
	public WrapperItemStack getSmeltedItem(){
		return new WrapperItemStack(FurnaceRecipes.instance().getSmeltingResult(stack).copy());
	}
	
	/**
	 *  Returns the time it takes to smelt this item.  Note that due to Vanilla MC jank,
	 *  this value MAY be non-zero even if {@link #getSmeltedItem()} returns nothing.  As such,
	 *  that method should be checked before this one.
	 */
	public int getSmeltingTime(){
		return VANILLA_FAKE_FURNACE.getCookTime(stack);
	}
	
	/**
	 *  Returns the item for this stack.
	 *  Only valid for base items, not external ones.
	 */
	public AItemBase getItem(){
		Item item = stack.getItem();
		return item instanceof IBuilderItemInterface ? ((IBuilderItemInterface) item).getItem() : null;
	}
	
	/**
	 *  Returns true if the stack doesn't have any items.
	 *  Essentially, this is a stack of 0, but there's special
	 *  logic that has to happen with this in tandem with a 0
	 *  stack size to make MC think it's a "blank" stack for
	 *  inventories.  In a nutshell, inventories can't have null
	 *  stacks, only empty stacks, so we will never get a null
	 *  stack and will never have a null wrapper
	 */
	public boolean isEmpty(){
		return stack.isEmpty();
	}
	
	/**
	 *  Returns the size of the stack.
	 */
	public int getSize(){
		return stack.getCount();
	}
	
	/**
	 *  Returns the max possible size of the stack.
	 */
	public int getMaxSize(){
		return stack.getMaxStackSize();
	}
	
	/**
	 *  Adds the specified qty to the stack, with negative numbers removing
	 *  items.  Returns the qty, adjusted by the items added or removed.
	 *  Example: qty=20, added 4, returns 16.  qty=-20, removed 4, returns -16.
	 *  Note that if a stack is decremented to a size of 0, it will loose
	 *  the data that tells it what item makes up the stack.
	 */
	public int add(int qty){
		if(qty < 0){
			int amountToRemove = -qty;
			if(amountToRemove > getSize()){
				amountToRemove = getSize();
			}
			stack.setCount(stack.getCount() - amountToRemove);
			return qty + amountToRemove;
		}else{
			int amountToAdd = qty;
			if(amountToAdd + getSize() > getMaxSize()){
				amountToAdd = getMaxSize() - getSize();
			}
			stack.setCount(stack.getCount() + amountToAdd);
			return qty - amountToAdd;
		}
	}
	
	/**
	 *  Splits this stack into two.  The second with qty amount
	 *  of items in it, and the same data.
	 */
	public WrapperItemStack split(int qty){
		return new WrapperItemStack(stack.splitStack(qty));
	}
	
	/**
	 *  Returns the tooltip lines for this stack.
	 *  ONLY call this on the client: servers can't get tooltips as it's
	 *  rendered text and uses game and player settings.
	 */
	public List<String> getTooltipLines(){
		List<String> tooltipText = stack.getTooltip(Minecraft.getMinecraft().player, Minecraft.getMinecraft().gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL);
    	//Add grey formatting text to non-first line tooltips.
		for(int i = 1; i < tooltipText.size(); ++i){
        	tooltipText.set(i, TextFormatting.GRAY + tooltipText.get(i));
        }
		return tooltipText;
	}
	
	/**
	 *  Tries to fill or drain from the passed-in tank into this item.
	 *  If the player is normal, then it will fill this item.
	 *  If the player is sneaking, they will drain this item into the tank.
	 *  If the player is creative, then the item won't be modified (but the tank will).
	 *  Returns true if an operation COULD occur.  This is to block other interactions.
	 *  Used only for items that used external fluid storage systems.
	 */
	public boolean interactWith(EntityFluidTank tank, WrapperPlayer player){
		IFluidHandlerItem handler = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
		if(handler != null){
			if(!player.isSneaking()){
				//Item can provide fluid.  Check if the tank can accept it.
				FluidStack drainedStack = handler.drain(Integer.MAX_VALUE, false);
				if(drainedStack != null){
					//Able to take fluid from item, attempt to do so.
					int amountToDrain = (int) tank.fill(drainedStack.getFluid().getName(), drainedStack.amount, false);
					drainedStack = handler.drain(amountToDrain, !player.isCreative());
					if(drainedStack != null){
						//Was able to provide liquid from item.  Fill the tank.
						tank.fill(drainedStack.getFluid().getName(), drainedStack.amount, true);
						player.setHeldStack(new WrapperItemStack(handler.getContainer()));
					}
				}
			}else{
				//Item can hold fluid.  Check if we can fill it.
				FluidStack containedStack = FluidRegistry.getFluidStack(tank.getFluid(), (int) tank.getFluidLevel());
				int amountFilled = handler.fill(containedStack, !player.isCreative());
				if(amountFilled > 0){
					//Were able to fill the item.  Apply state change to tank and item.
					tank.drain(tank.getFluid(), amountFilled, true);
					player.setHeldStack(new WrapperItemStack(handler.getContainer()));
				}
			}
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 *  Returns the data from the stack.
	 *  If there is no data, then a new NBT tag is returned.
	 *  If the data is modified, {@link #setData(WrapperNBT)} should 
	 *  be called as new NBT tags generated from this method aren't linked
	 *  to the stack by default.  It also ensures proper states when
	 *  interfacing with modded items.
	 */
	public WrapperNBT getData(){
		return stack.hasTagCompound() ? new WrapperNBT(stack.getTagCompound().copy()) : new WrapperNBT();
	}
	
	/**
	 *  Sets the data to this stack.
	 */
	public void setData(WrapperNBT data){
		stack.setTagCompound(data != null ? data.tag : null);
	}
}