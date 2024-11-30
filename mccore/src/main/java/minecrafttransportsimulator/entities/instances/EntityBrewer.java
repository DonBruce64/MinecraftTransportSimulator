package minecrafttransportsimulator.entities.instances;

import minecrafttransportsimulator.jsondefs.JSONPart.JSONPartInteractable;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;

/**
 * Basic brewing stand class.  Class is essentially an inventory that holds state of smelting
 * operations.  Has a method for ticking
 *
 * @author don_bruce
 */
public class EntityBrewer extends AEntityCrafter {
    private static final int[] BOTTLE_ITEM_SLOTS = new int[] { 1, 2, 3 };
    private static final int MODIFIER_ITEM_SLOT = 4;
    public static final String BREWER_FUEL_NAME = "brewing_stand";

    public EntityBrewer(AWrapperWorld world, IWrapperNBT data, JSONPartInteractable definition) {
        super(world, data, 5, definition, BOTTLE_ITEM_SLOTS, BOTTLE_ITEM_SLOTS);
    }

    @Override
    protected IWrapperItemStack getResultForSlot(int index) {
        return getStack(index).getBrewedItem(getStack(MODIFIER_ITEM_SLOT));
    }

    @Override
    public String getFuelName() {
        return BREWER_FUEL_NAME;
    }

    @Override
    protected int getFuelTime(IWrapperItemStack stack) {
        return stack.isBrewingFuel() ? 8000 : 0; //20 operations.
    }

    @Override
    protected int getTimeForItem(IWrapperItemStack stack) {
        return 400;
    }

    @Override
    protected void performPostCraftingOperations() {
        removeFromSlot(MODIFIER_ITEM_SLOT, 1);
    }

    @Override
    public boolean isStackValid(IWrapperItemStack stackToCheck, int index) {
        for (int slot = 0; slot < BOTTLE_ITEM_SLOTS.length; ++slot) {
            if (BOTTLE_ITEM_SLOTS[slot] == index) {
                return stackToCheck.isBrewingVessel() && getStack(index).isEmpty();
            }
        }
        if (index == MODIFIER_ITEM_SLOT) {
            return stackToCheck.isBrewingModifier();
        } else {
            return super.isStackValid(stackToCheck, index);
        }
    }
}
