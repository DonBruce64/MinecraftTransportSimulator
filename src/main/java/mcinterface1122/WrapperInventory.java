package mcinterface1122;

import java.util.Map;

import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.mcinterface.IWrapperInventory;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;

class WrapperInventory implements IWrapperInventory{
	final IInventory inventory;
	
	WrapperInventory(IInventory inventory){
		this.inventory = inventory;
	}
	
	@Override
	public int getSize(){
		return inventory.getSizeInventory();
	}
	
	@Override
	public WrapperItemStack getStackInSlot(int slot){
		return new WrapperItemStack(inventory.getStackInSlot(slot));
	}
	
	@Override
	public AItemBase getItemInSlot(int slot){
		Item item = inventory.getStackInSlot(slot).getItem();
		return item instanceof BuilderItem ? ((BuilderItem) item).item : null;
	}
	
	@Override
	public void decrementSlot(int slot){
		inventory.getStackInSlot(slot).setCount(inventory.getStackInSlot(slot).getCount() - 1);
		inventory.markDirty();
	}

	@Override
	public boolean hasItem(AItemBase itemToFind){
		for(int i=0; i<getSize(); ++i){
			ItemStack currentStack = inventory.getStackInSlot(i);
			if(BuilderItem.itemMap.get(itemToFind).equals(currentStack.getItem())){
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean addStack(IWrapperItemStack wrapperStack){
		ItemStack stack = ((WrapperItemStack) wrapperStack).stack;
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
	
	@Override
	public boolean addItem(AItemBase item, IWrapperNBT data){
		WrapperItemStack stack = new WrapperItemStack(new ItemStack(BuilderItem.itemMap.get(item)));
		if(data != null){
			stack.setData(data);
		}
		return addStack(stack);
	}
	
	@Override
	public boolean removeStack(IWrapperItemStack stack, int qtyToRemove){
		Item item = ((WrapperItemStack) stack).stack.getItem();
		int meta = ((WrapperItemStack) stack).stack.getMetadata();
		NBTTagCompound nbt = ((WrapperItemStack) stack).stack.getTagCompound();
		int qtyRemoved = 0;
        for(int i=0; i<getSize(); ++i){
            ItemStack currentStack = inventory.getStackInSlot(i);
            if(currentStack.getItem().equals(item) && (meta <= -1 || currentStack.getMetadata() == meta) && (nbt == null || NBTUtil.areNBTEquals(nbt, currentStack.getTagCompound(), true))){
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
	
	@Override
	public boolean removeItem(AItemBase item, IWrapperNBT data){
		WrapperItemStack stack = new WrapperItemStack(new ItemStack(BuilderItem.itemMap.get(item)));
		if(data != null){
			stack.setData(data);
		}
		return removeStack(stack, 1);
	}
	
	@Override
	public boolean hasMaterials(AItemPack<?> item, boolean includeMain, boolean includeSub){
		for(IWrapperItemStack materialStack : MasterInterface.coreInterface.parseFromJSON(item, includeMain, includeSub)){
			int requiredMaterialCount = materialStack.getSize();
			for(int i=0; i<getSize(); ++i){
				ItemStack stack = inventory.getStackInSlot(i);
				if(ItemStack.areItemsEqual(stack, ((WrapperItemStack) materialStack).stack)){
					requiredMaterialCount -= stack.getCount();
				}
			}
			if(requiredMaterialCount > 0){
				return false;
			}
		}
		return true;
	}
	
	@Override
	public void removeMaterials(AItemPack<?> item, boolean includeMain, boolean includeSub){
		for(IWrapperItemStack materialStack : MasterInterface.coreInterface.parseFromJSON(item, includeMain, includeSub)){
			removeStack(materialStack, materialStack.getSize());
		}
		inventory.markDirty();
	}
	
	@Override
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
}