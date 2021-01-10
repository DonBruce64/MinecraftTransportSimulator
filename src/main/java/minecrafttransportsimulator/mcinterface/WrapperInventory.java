package minecrafttransportsimulator.mcinterface;

import java.util.Map;

import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.packloading.PackMaterialComponent;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

public class WrapperInventory{
	private final IInventory inventory;
	
	public WrapperInventory(IInventory inventory){
		this.inventory = inventory;
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
		return inventory.getStackInSlot(slot);
	}
	
	/**
	 *  Returns the item in the specified slot.  Only valid for {@link AItemBase} items.
	 */
	public AItemBase getItemInSlot(int slot){
		Item item = inventory.getStackInSlot(slot).getItem();
		return item instanceof BuilderItem ? ((BuilderItem) item).item : null;
	}
	
	/**
	 *  Removes a single item from the passed-in slot.
	 */
	public void decrementSlot(int slot){
		inventory.getStackInSlot(slot).setCount(inventory.getStackInSlot(slot).getCount() - 1);
		inventory.markDirty();
	}

	/**
	 *  Returns true if this inventory contains the passed-in item.
	 */
	public boolean hasItem(AItemBase itemToFind){
		for(int i=0; i<getSize(); ++i){
			ItemStack currentStack = inventory.getStackInSlot(i);
			if(itemToFind.getBuilder().equals(currentStack.getItem())){
				return true;
			}
		}
		return false;
	}
	
	/**
	 *  Adds the passed-in stack to this inventory.  If not possible, false is returned.
	 */
	public boolean addStack(ItemStack stack){
		if(stack.isItemDamaged()){
            //Damaged items can't be stacked, so we need a free slot for them.
			//Try to find the next empty one now.
			int slot = -1;
        	for(int i = 0; i < inventory.getSizeInventory(); ++i){
                if(inventory.getStackInSlot(i).isEmpty()){
                    slot = i;
                    break;
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
	 *  Adds the passed-in item to this inventory.  Does not care about slot position.
	 *  Returns true if addition was successful.
	 */
	public boolean addItem(AItemBase item, WrapperNBT data){
		ItemStack stack = item.getNewStack();
		if(data != null){
			stack.setTagCompound(data.tag);
		}
		return addStack(stack);
	}
	
	/**
	 *  Attempts to remove the passed-in number of items matching those in the stack
	 *  from this inventory.  Returns true if all the items were removed, false if
	 *  only some of the items were removed.
	 */
	public boolean removeStack(ItemStack stack, int qtyToRemove){
		int qtyRemoved = 0;
        for(int i=0; i<getSize(); ++i){
            ItemStack currentStack = inventory.getStackInSlot(i);
            if(OreDictionary.itemMatches(stack, currentStack, false)){
                int qtyRemovedFromStack = Math.min(qtyToRemove - qtyRemoved, currentStack.getCount());
                qtyRemoved += qtyRemovedFromStack;
                if(qtyToRemove != 0){
                    currentStack.shrink(qtyRemovedFromStack);
                    if(currentStack.isEmpty()){
                        inventory.setInventorySlotContents(i, ItemStack.EMPTY);
                        inventory.markDirty();
                    }
                    if(qtyRemoved == qtyToRemove){
                        return true;
                    }
                }
            }
        }
        return false;
	}
	
	/**
	 *  Attempts to remove the passed-in item from this inventory.
	 *  Returns true if removal was successful.
	 */
	public boolean removeItem(AItemBase item, WrapperNBT data){
		ItemStack stack = item.getNewStack();
		if(data != null){
			stack.setTagCompound(data.tag);
		}
		return removeStack(stack, 1);
	}
	
	/**
	 *  Returns true if this inventory has all the materials to make the pack-based item.
	 */
	public boolean hasMaterials(AItemPack<?> item, boolean includeMain, boolean includeSub){
		for(PackMaterialComponent material : PackMaterialComponent.parseFromJSON(item, includeMain, includeSub, true)){
			int requiredMaterialCount = material.qty;
			for(ItemStack stack : material.possibleItems){
				for(int i=0; i<getSize(); ++i){
					ItemStack testStack = inventory.getStackInSlot(i);
					if(OreDictionary.itemMatches(stack, testStack, false)){
						requiredMaterialCount -= stack.getCount();
					}
				}
			}
			if(requiredMaterialCount > 0){
				return false;
			}
		}
		return true;
	}
	
	/**
	 *  Removes all materials from the inventory required to craft the passed-in item.
	 *  {@link #hasMaterials(AItemPack, boolean, boolean)} MUST be called before this method to ensure
	 *  the the inventory actually has the required materials.  Failure to do so will
	 *  result in the this method removing the incorrect number of materials.
	 */
	public void removeMaterials(AItemPack<?> item, boolean includeMain, boolean includeSub){
		for(PackMaterialComponent material : PackMaterialComponent.parseFromJSON(item, includeMain, includeSub, true)){
			for(ItemStack stack : material.possibleItems){
				removeStack(stack, material.qty);
			}
		}
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
		for(int i=0; i<getSize(); ++i){
			AItemBase item = getItemInSlot(i);
			if(item instanceof ItemPart){
				ItemPart part = ((ItemPart) item);
				if(part.definition.bullet != null){
					explosivePower += getStackInSlot(i).getCount()*part.definition.bullet.diameter/10D;
				}
			}
		}
		return explosivePower;
	}
}