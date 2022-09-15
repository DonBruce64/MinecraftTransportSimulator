package minecrafttransportsimulator.baseclasses;

import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.instances.ItemBullet;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packloading.PackMaterialComponent;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * Interface that is common to all inventories in this mod.  This includes both internal
 * and wrapped inventories.
 *
 * @author don_bruce
 */
public interface IInventoryProvider {

    /**
     * Returns the mass of this inventory.  Basically, what {@link AEntityA_Base#getMass()}
     * returns, but re-names to avoid name collisions in classes that implement this interface
     * and extend that class.
     */
    default double getInventoryMass() {
        Map<String, Double> heavyItems = ConfigSystem.settings.general.itemWeights.weights;
        double currentMass = 0;
        for (int i = 0; i < getSize(); ++i) {
            IWrapperItemStack stack = getStack(i);
            double weightMultiplier = 1.0;
            for (String heavyItemName : heavyItems.keySet()) {
                if (InterfaceManager.coreInterface.getStackItemName(stack).contains(heavyItemName)) {
                    weightMultiplier = heavyItems.get(heavyItemName);
                    break;
                }
            }
            currentMass += 5F * stack.getSize() / stack.getMaxSize() * weightMultiplier;
        }
        return currentMass;
    }

    /**
     * Gets the max number of item stacks this inventory can contain.
     */
    int getSize();

    /**
     * Gets the number of items currently in this container.
     */
    default int getCount() {
        int count = 0;
        for (int i = 0; i < getSize(); ++i) {
            if (!getStack(i).isEmpty()) {
                ++count;
            }
        }
        return count;
    }

    /**
     * Returns true if this stack can be added to the specified slot.  Normally true for all stacks,
     * but can be used to limit what goes where.
     */
    default boolean isStackValid(IWrapperItemStack stackToCheck, int index) {
        return true;
    }

    /**
     * Returns the stack in the specified slot.  This may be used to view the items in this inventory.
     * Modifications should not happen directly to the items, instead, use the methods in this interface.
     */
    IWrapperItemStack getStack(int index);

    /**
     * Sets the stack in the inventory, overwriting anything that was previously in this slot.
     * Mainly used for packet operations, as it can result in the destruction of items.
     */
    void setStack(IWrapperItemStack stackToSet, int index);

    /**
     * Tries to add the passed-in stack to this inventory. Attempts to add up to qty items
     * from the stack, but may or may not add all of them.
     * As such, true is returned if all items were added, false if not.  The passed-
     * in stack will have its items removed upon calling, so this may be referenced
     * for actual items removed. Note that this operation may be simulated by passing
     * in false for doAdd.  This is useful if you need to check if this inventory could
     * store the stack you are wishing to add without actually adding it.
     */
    default boolean addStack(IWrapperItemStack stackToAdd, int qty, boolean doAdd) {
        for (int i = 0; i < getSize(); ++i) {
            if (isStackValid(stackToAdd, i)) {
                IWrapperItemStack stack = getStack(i);
                if (stack.isEmpty()) {
                    if (doAdd) {
                        setStack(stackToAdd.split(qty), i);
                    }
                    return true;
                } else if (stackToAdd.isCompleteMatch(stack)) {
                    int amountToAdd = Math.min(stack.getMaxSize() - stack.getSize(), qty);
                    if (amountToAdd > 0) {
                        if (doAdd) {
                            stack.add(amountToAdd);
                            //This will flag updates if needed.
                            setStack(stack, i);
                            stackToAdd.add(-amountToAdd);
                            qty -= amountToAdd;
                        } else {
                            return true;
                        }
                    }
                }
            }
        }
        return qty == 0;
    }

    /**
     * A pass-down method that just adds the whole stack to this inventory without the extra parameters.
     */
    default boolean addStack(IWrapperItemStack stackToAdd) {
        return addStack(stackToAdd, stackToAdd.getSize(), true);
    }

    /**
     * Attempts to remove the passed-in number of items matching those in the stack
     * from this inventory.  Returns true if all the items were removed, false if
     * there are not enough items to remove according to the quantity.  If there
     * arenm't enough items, then the inventory is not modified.
     * Note that unlike {@link #addStack(IWrapperItemStack, int, boolean)}, the passed-in stack
     * is not modified as it is assumed it's a reference variable rather than
     * one that represents an actual stack.  This method also uses OreDict
     * lookup, as it assumes removal it for crafting or usage where "fuzzy"
     * matches are desired.
     */
    default boolean removeStack(IWrapperItemStack referenceStack, int qtyToRemove) {
        //Check items for number we can remove.
        int qtyFound = qtyToRemove;
        for (int i = 0; i < getSize(); ++i) {
            IWrapperItemStack stack = getStack(i);
            if (InterfaceManager.coreInterface.isOredictMatch(stack, referenceStack)) {
                qtyFound += stack.getSize();
            }
        }
        if (qtyFound > qtyToRemove) {
            qtyToRemove = -qtyToRemove;
            for (int i = 0; i < getSize(); ++i) {
                IWrapperItemStack stack = getStack(i);
                if (InterfaceManager.coreInterface.isOredictMatch(stack, referenceStack)) {
                    qtyToRemove = stack.add(qtyToRemove);
                    setStack(stack, i);
                }
            }
        }
        return qtyToRemove == 0;
    }

    /**
     * Returns the slot where the passed-in item exists, or -1 if
     * this inventory doesn't contain it.  Uses OreDict for lookup operations.
     */
    default int getSlotForStack(IWrapperItemStack stackToFind) {
        for (int i = 0; i < getSize(); ++i) {
            if (InterfaceManager.coreInterface.isOredictMatch(getStack(i), stackToFind)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Adds the quantity of items to the stack in the passed-in slot.  Returns true if the stack could take
     * all the items, false if not.
     */
    default boolean addToSlot(int index, int qty) {
        IWrapperItemStack stack = getStack(index);
        if (stack.getSize() + qty < stack.getMaxSize()) {
            stack.add(qty);
            setStack(stack, index);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Removes the quantity of items from the stack in the passed-in slot.  Returns true if the removal is possible, false
     * if not (because the stack doesn't have enough items).
     */
    default boolean removeFromSlot(int index, int qty) {
        IWrapperItemStack stack = getStack(index);
        if (stack.getSize() - qty >= 0) {
            stack.add(-qty);
            setStack(stack, index);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns true if this inventory has all the materials to make the pack-based item..  Normally uses the output
     * of {@link PackMaterialComponent#parseFromJSON(AItemPack, int, boolean, boolean, boolean)}, but can use any input.
     */
    default boolean hasMaterials(List<PackMaterialComponent> materials) {
        for (PackMaterialComponent material : materials) {
            int requiredMaterialCount = material.qty;
            for (IWrapperItemStack materialStack : material.possibleItems) {
                for (int i = 0; i < getSize(); ++i) {
                    IWrapperItemStack testStack = getStack(i);
                    if (InterfaceManager.coreInterface.isOredictMatch(testStack, materialStack)) {
                        requiredMaterialCount -= testStack.getSize();
                    }
                }
            }
            if (requiredMaterialCount > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if this inventory has the specified material index to make the pack-based item.
     */
    default boolean hasSpecificMaterial(AItemPack<?> item, int recipeIndex, int index, boolean includeMain, boolean includeSub, boolean forRepair) {
        PackMaterialComponent material = PackMaterialComponent.parseFromJSON(item, recipeIndex, includeMain, includeSub, forRepair).get(index);
        int requiredMaterialCount = material.qty;
        for (IWrapperItemStack materialStack : material.possibleItems) {
            for (int i = 0; i < getSize(); ++i) {
                IWrapperItemStack testStack = getStack(i);
                if (InterfaceManager.coreInterface.isOredictMatch(testStack, materialStack)) {
                    requiredMaterialCount -= testStack.getSize();
                }
            }
        }
        return requiredMaterialCount > 0;
    }

    /**
     * Removes all materials from the inventory required to craft the passed-in item.
     * {@link #hasMaterials(AItemPack, boolean, boolean, boolean)} MUST be called before this method to ensure
     * the the inventory actually has the required materials.  Failure to do so will
     * result in the this method removing the incorrect number of materials.
     */
    default void removeMaterials(AItemPack<?> item, int recipeIndex, boolean includeMain, boolean includeSub, boolean forRepair) {
        for (PackMaterialComponent material : PackMaterialComponent.parseFromJSON(item, recipeIndex, includeMain, includeSub, forRepair)) {
            for (IWrapperItemStack stack : material.possibleItems) {
                removeStack(stack, material.qty);
            }
        }
    }

    /**
     * Returns the index in the inventory of the item to repair that matches the passed-in item.
     */
    default int getRepairIndex(AItemPack<?> item, int recipeIndex) {
        for (PackMaterialComponent material : PackMaterialComponent.parseFromJSON(item, recipeIndex, false, false, true)) {
            for (IWrapperItemStack materialStack : material.possibleItems) {
                if (materialStack.getItem().equals(item)) {
                    //Repair item in recipe found, find it in our inventory.
                    for (int i = 0; i < getSize(); ++i) {
                        IWrapperItemStack testStack = getStack(i);
                        if (InterfaceManager.coreInterface.isOredictMatch(testStack, materialStack)) {
                            return i;
                        }
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Gets the explosive power of this inventory.  Used when this inventory is blown up.
     */
    default double getExplosiveness() {
        double explosivePower = 0;
        for (int i = 0; i < getSize(); ++i) {
            IWrapperItemStack stack = getStack(i);
            AItemBase item = stack.getItem();
            if (item instanceof ItemBullet) {
                ItemBullet bullet = (ItemBullet) item;
                if (bullet.definition.bullet != null) {
                    double blastSize = bullet.definition.bullet.blastStrength == 0 ? bullet.definition.bullet.diameter / 10D : bullet.definition.bullet.blastStrength;
                    explosivePower += stack.getSize() * blastSize / 10D;
                }
            }
        }
        return explosivePower;
    }
}
