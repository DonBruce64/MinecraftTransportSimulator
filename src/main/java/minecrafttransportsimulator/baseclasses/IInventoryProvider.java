package minecrafttransportsimulator.baseclasses;

import java.util.Map;

import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.instances.ItemBullet;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperItemStack;
import minecrafttransportsimulator.packloading.PackMaterialComponent;
import minecrafttransportsimulator.systems.ConfigSystem;

/**Interface that is common to all inventories in this mod.  This includes both internal
 * and wrapped inventories.
 *
 * @author don_bruce
 */
public interface IInventoryProvider{
	
	/**
	 * Returns the mass of this inventory.  Basically, what {@link AEntityA_Base#getMass()}
	 * returns, but re-names to avoid name collisions in classes that implement this interface
	 * and extend that class.
	 */
	public default double getInventoryMass(){
		Map<String, Double> heavyItems = ConfigSystem.configObject.general.itemWeights.weights;
		double currentMass = 0;
		for(int i=0; i<getSize(); ++i){
			WrapperItemStack stack = getStack(i);
			double weightMultiplier = 1.0;
			for(String heavyItemName : heavyItems.keySet()){
				if(InterfaceCore.getStackItemName(stack).contains(heavyItemName)){
					weightMultiplier = heavyItems.get(heavyItemName);
					break;
				}
			}
			currentMass += 5F*stack.getSize()/stack.getMaxSize()*weightMultiplier;
		}
		return currentMass;
	}
	
	/**
	 *  Gets the max number of item stacks this inventory can contain.
	 */
	public int getSize();
	
	/**
	 *  Gets the number of items currently in this container. 
	 */
	public default int getCount(){
		int count = 0;
		for(int i=0; i<getSize(); ++i){
			if(!getStack(i).isEmpty()){
				++count;
			}
		}
		return count;
	}
	
	/**
	 *  Returns true if this stack can be added to the specified slot.  Normally true for all stacks,
	 *  but can be used to limit what goes where.
	 */
	public default boolean isStackValid(WrapperItemStack stackToCheck, int index){
		return true;
	}
	
	/**
	 *  Returns the stack in the specified slot.  This may be used to view the items in this inventory.
	 *  Modifications should not happen directly to the items, instead, use the methods in this interface.
	 */
	public WrapperItemStack getStack(int index);
	
	/**
	 *  Sets the stack in the inventory, overwriting anything that was previously in this slot.
	 *  Mainly used for packet operations, as it can result in the destruction of items.
	 */
	public void setStack(WrapperItemStack stackToSet, int index);
	
	/**
	 *  Tries to add the passed-in stack to this inventory. Attempts to add up to qty items
	 *  from the stack, but may or may not add all of them.
	 *  As such, true is returned if all items were added, false if not.  The passed-
	 *  in stack will have its items removed upon calling, so this may be referenced 
	 *  for actual items removed. Note that this operation may be simulated by passing 
	 *  in false for doAdd.  This is useful if you need to check if this inventory could 
	 *  store the stack you are wishing to add without actually adding it.
	 */
	public default boolean addStack(WrapperItemStack stackToAdd, int qty, boolean doAdd){
		for(int i=0; i<getSize(); ++i){
			if(isStackValid(stackToAdd, i)){
				WrapperItemStack stack = getStack(i);
				if(stack.isEmpty()){
					if(doAdd){
						setStack(stackToAdd.split(qty), i);
						qty = 0;
					}
					return true;
				}else if(stackToAdd.isCompleteMatch(stack)){
					int amountToAdd = Math.min(stack.getMaxSize() - stack.getSize(), qty);
					if(amountToAdd > 0){
						if(doAdd){
							stack.add(amountToAdd);
							//This will flag updates if needed.
							setStack(stack, i);
							stackToAdd.add(-amountToAdd);
							qty -= amountToAdd;
						}else{
							return true;
						}
					}
				}
			}
		}
		return qty == 0;
	}
	
	/**
	 *  A pass-down method that just adds the whole stack to this inventory without the extra parameters.
	 */
	public default boolean addStack(WrapperItemStack stackToAdd){
		return addStack(stackToAdd, stackToAdd.getSize(), true);
	}
	
	/**
	 *  Attempts to remove the passed-in number of items matching those in the stack
	 *  from this inventory.  Returns true if all the items were removed, false if
	 *  there are not enough items to remove according to the quantity.  If there
	 *  arenm't enough items, then the inventory is not modified.
	 *  Note that unlike {@link #addStack(WrapperItemStack, int, boolean)}, the passed-in stack
	 *  is not modified as it is assumed it's a reference variable rather than
	 *  one that represents an actual stack.  This method also uses OreDict
	 *  lookup, as it assumes removal it for crafting or usage where "fuzzy"
	 *  matches are desired.
	 */
	public default boolean removeStack(WrapperItemStack referenceStack, int qtyToRemove){
		//Check items for number we can remove.
		int qtyFound = qtyToRemove;
        for(int i=0; i<getSize(); ++i){
            WrapperItemStack stack = getStack(i);
            if(InterfaceCore.isOredictMatch(stack, referenceStack)){
            	qtyFound += stack.getSize();
            }
        }
        if(qtyFound > qtyToRemove){
        	qtyToRemove = -qtyToRemove;
        	for(int i=0; i<getSize(); ++i){
        		WrapperItemStack stack = getStack(i);
        		if(InterfaceCore.isOredictMatch(stack, referenceStack)){
        			qtyToRemove = stack.add(qtyToRemove);
        			setStack(stack, i);
                }
            }
        }
        return qtyToRemove == 0;
	}
	
	/**
	 *  Returns the slot where the passed-in item exists, or -1 if 
	 *  this inventory doesn't contain it.  Uses OreDict for lookup operations.
	 */
	public default int getSlotForStack(WrapperItemStack stackToFind){
		for(int i=0; i<getSize(); ++i){
            if(InterfaceCore.isOredictMatch(getStack(i), stackToFind)){
            	return i;
            }
        }
		return -1;
	}
	
	/**
	 *  Adds the quantity of items to the stack in the passed-in slot.  Returns true if the stack could take
	 *  all the items, false if not.
	 */
	public default boolean addToSlot(int index, int qty){
		WrapperItemStack stack = getStack(index);
		if(stack.getSize() + qty < stack.getMaxSize()){
			stack.add(qty);
			setStack(stack, index);
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 *  Removes the quantity of items from the stack in the passed-in slot.  Returns true if the removal is possible, false
	 *  if not (because the stack doesn't have enough items).
	 */
	public default boolean removeFromSlot(int index, int qty){
		WrapperItemStack stack = getStack(index);
		if(stack.getSize() - qty >= 0){
			stack.add(-qty);
			setStack(stack, index);
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 *  Returns true if this inventory has all the materials to make the pack-based item.
	 */
	public default boolean hasMaterials(AItemPack<?> item, boolean includeMain, boolean includeSub, boolean forRepair){
		for(PackMaterialComponent material : PackMaterialComponent.parseFromJSON(item, includeMain, includeSub, true, forRepair)){
			int requiredMaterialCount = material.qty;
			for(WrapperItemStack materialStack : material.possibleItems){
				for(int i=0; i<getSize(); ++i){
					WrapperItemStack testStack = getStack(i);
					if(InterfaceCore.isOredictMatch(testStack, materialStack)){
						requiredMaterialCount -= testStack.getSize();
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
	public default void removeMaterials(AItemPack<?> item, boolean includeMain, boolean includeSub, boolean forRepair){
		for(PackMaterialComponent material : PackMaterialComponent.parseFromJSON(item, includeMain, includeSub, true, forRepair)){
			for(WrapperItemStack stack : material.possibleItems){
				removeStack(stack, material.qty);
			}
		}
	}
	
	/**
	 *  Gets the explosive power of this inventory.  Used when this inventory is blown up.
	 */
	public default double getExplosiveness(){
		double explosivePower = 0;
		for(int i=0; i<getSize(); ++i){
			WrapperItemStack stack = getStack(i);
			AItemBase item = stack.getItem();
			if(item instanceof ItemBullet){
				ItemBullet bullet = (ItemBullet) item;
				if(bullet.definition.bullet != null){
					double blastSize = bullet.definition.bullet.blastStrength == 0 ? bullet.definition.bullet.diameter/10D : bullet.definition.bullet.blastStrength;
					explosivePower += stack.getSize()*blastSize/10D;
				}
			}
		}
		return explosivePower;
	}
}
