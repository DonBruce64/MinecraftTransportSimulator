package mcinterface;

import java.util.Map;

import minecrafttransportsimulator.items.packs.parts.ItemPartBullet;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

/**Wrapper for inventories.  This class has multiple constructors to allow for access
 * to multiple inventory containers.  However, some constructors are not visible,
 * as they are for objects that are not assured to have inventories, and as such should
 * not attempt to cast said objects as inventories.
 *
 * @author don_bruce
 */
public class WrapperInventory{
	final IInventory inventory;
	
	public WrapperInventory(WrapperPlayer player){
		super();
		this.inventory = player.player.inventory;
	}
	
	private WrapperInventory(WrapperTileEntity tile){
		super();
		this.inventory = (IInventory) tile.tile;
	}
	
	/**
	 *  Returns an instance for TEs.
	 */
	public static WrapperInventory getTileEntityInventory(WrapperTileEntity tile){
		return tile.tile instanceof IInventory ? new WrapperInventory(tile) : null;
	}
	
	/**
	 *  Returns the total number of stacks that can fit into this inventory.
	 */
	public int getSize(){
		return inventory.getSizeInventory();
	}
	
	/**
	 *  Returns the stack in the specified slot.
	 */
	public ItemStack getStackInSlot(int slot){
		//TODO change this when we get wrapper itemstacks.
		return inventory.getStackInSlot(slot);
	}
	
	/**
	 *  Adds the passed-in stack to this inventory.  If not possible, false is returned.
	 *  Slot may be -1 to allow the stack to go into any slot rather than a specific one.
	 *  If the slot is specified, then the method will check to make sure the item in
	 *  the stack is compatible with the existing stack, and the max size won't be exceeded.
	 */
	public boolean addStack(ItemStack stack, int slot){
		if(stack.isItemDamaged()){
            //Damaged items can't be stacked, so we need a free slot for them.
			//If we didn't specify the slot, try to find the next empty one now.
			if(slot == -1){
            	for(int i = 0; i < inventory.getSizeInventory(); ++i){
                    if(inventory.getStackInSlot(i).isEmpty()){
                        slot = i;
                        break;
                    }
                }
            }
			
            if(slot >= 0){
            	//Free slot found.  Add stack and return true.
                inventory.setInventorySlotContents(slot, stack.copy());
                stack.setCount(0);
                inventory.markDirty();
                return true;
            }
        }else{
        	//Attempt to add the stack to as many partial slots as possible.
        	//If we ever get to the point where the passed-in stack has a count of 0,
        	//we know we stored all the items in that stack.
        	int amountRemaining = stack.getCount();
        	for(int i = 0; i < inventory.getSizeInventory(); ++i){
        		ItemStack currentStack = inventory.getStackInSlot(i);
        		if(currentStack.isEmpty()){
        			//Found an empty slot.  Add the whole stack.
        			inventory.setInventorySlotContents(i, stack.copy());
        			stack.setCount(0);
        			inventory.markDirty();
        			return true;
        		}else if(currentStack.isItemEqual(stack) && (currentStack.hasTagCompound() ? currentStack.getTagCompound().equals(stack.getTagCompound()) : !stack.hasTagCompound())){
        			amountRemaining -= stack.getMaxStackSize() - currentStack.getCount();
        		}
        	}
        	
        	if(amountRemaining <= 0){
        		//We can store all the items in this inventory.  Do so now.
        		for(int i = 0; i < inventory.getSizeInventory(); ++i){
        			ItemStack currentStack = inventory.getStackInSlot(i);
        			if(currentStack.isItemEqual(stack) && (currentStack.hasTagCompound() ? currentStack.getTagCompound().equals(stack.getTagCompound()) : !stack.hasTagCompound())){
            			currentStack.setCount(currentStack.getCount() + stack.splitStack(Math.min(currentStack.getMaxStackSize() - currentStack.getCount(), stack.getCount())).getCount());
            			if(stack.isEmpty()){
            				inventory.markDirty();
            				return true;
            			}
            		}
        		}
        	}
        }
    	//Not able to add stack, either due to no free slots or the slot asked for wasn't free.
    	return false;
	}
	
	/**
	 *  Attempts to remove the passed-in stack from this inventory.
	 *  If the passed-in stack has less than the stack in the passed-in
	 *  slot, then the stack in the slot is decremented rather than removed.
	 *  A slot value of -1 will result in the items being removed from
	 *  all stacks.  This can be combined with a stack value larger
	 *  than the max stack size to allow for bulk item removal.
	 */
	public boolean removeStack(ItemStack stack, int slot){
		//Don't want to modify the passed-in stack.
		stack = stack.copy();
		int amountRemaining = stack.getCount();
    	for(int i = 0; i < inventory.getSizeInventory(); ++i){
    		ItemStack currentStack = inventory.getStackInSlot(i);
    		if(currentStack.isItemEqual(stack) && (currentStack.hasTagCompound() ? currentStack.getTagCompound().equals(stack.getTagCompound()) : !stack.hasTagCompound())){
    			amountRemaining -= stack.getMaxStackSize() - currentStack.getCount();
    		}
    	}
    	
    	if(amountRemaining <= 0){
    		//We can remove the required quantity.  Do so now.
    		for(int i = 0; i < inventory.getSizeInventory(); ++i){
    			ItemStack currentStack = inventory.getStackInSlot(i);
    			if(currentStack.isItemEqual(stack) && (currentStack.hasTagCompound() ? currentStack.getTagCompound().equals(stack.getTagCompound()) : !stack.hasTagCompound())){
    				stack.setCount(stack.getCount() - currentStack.splitStack(stack.getCount()).getCount());
        			if(stack.isEmpty()){
        				inventory.markDirty();
        				return true;
        			}
        		}
    		}
    	}
    	
    	//Not able to remove all items.
    	return false;
	}
	
	/**
	 *  Removes a single item from the specified slot.
	 */
	public void decrement(int slot){
		inventory.getStackInSlot(slot).setCount(inventory.getStackInSlot(slot).getCount() - 1);
		inventory.markDirty();
	}
	
	/**
	 * Gets the weight of this inventory.
	 */
	public float getInventoryWeight(Map<String, Double> heavyItems){
		float weight = 0;
		for(int i=0; i<inventory.getSizeInventory(); ++i){
			ItemStack stack = inventory.getStackInSlot(i);
			if(stack != null){
				double weightMultiplier = 1.0;
				for(String heavyItemName : heavyItems.keySet()){
					if(stack.getItem().getRegistryName().toString().contains(heavyItemName)){
						weightMultiplier = heavyItems.get(heavyItemName);
						break;
					}
				}
				weight += 5F*stack.getCount()/stack.getMaxStackSize()*weightMultiplier;
			}
		}
		return weight;
	}
	
	/**
	 *  Gets the explosive power of this inventory.  Used when this container is blown up.
	 *  For our calculations, only ammo is checked.  While we could check for fuel, we assume
	 *  that fuel-containing items are stable enough to not blow up when this container is hit.
	 */
	public double getExplosiveness(){
		double explosivePower = 0;
		for(int i=0; i<inventory.getSizeInventory(); ++i){
			ItemStack stack = getStackInSlot(i);
			if(stack.getItem() instanceof ItemPartBullet){
				explosivePower += stack.getCount()*((ItemPartBullet) stack.getItem()).definition.bullet.diameter/10D;
			}
		}
		return explosivePower;
	}
}