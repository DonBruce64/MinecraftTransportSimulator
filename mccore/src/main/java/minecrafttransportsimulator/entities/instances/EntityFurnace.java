package minecrafttransportsimulator.entities.instances;

import minecrafttransportsimulator.jsondefs.JSONPart.JSONPartInteractable;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;

/**
 * Basic furnace class.  Class is essentially an inventory that holds state of smelting
 * operations.  Has a method for ticking
 *
 * @author don_bruce
 */
public class EntityFurnace extends AEntityCrafter {
    private static final int SMELTING_ITEM_SLOT = 1;
    private static final int SMELTED_ITEM_SLOT = 2;
    public static final String FURNACE_FUEL_NAME = "furnace";

    public EntityFurnace(AWrapperWorld world, IWrapperNBT data, JSONPartInteractable definition) {
        super(world, data, 3, definition, new int[] { SMELTING_ITEM_SLOT }, new int[] { SMELTED_ITEM_SLOT });
    }

    @Override
    protected IWrapperItemStack getResultForSlot(int index) {
        //Don't worry about index here, it can only be the input slot.
        IWrapperItemStack smeltingStack = getStack(SMELTING_ITEM_SLOT);
        if (!smeltingStack.isEmpty()) {
            IWrapperItemStack smeltingResult = smeltingStack.getSmeltedItem(world);
            IWrapperItemStack stackInResult = getStack(SMELTED_ITEM_SLOT);
            //Check to make sure this fits with existing outputs, if not, don't let us smelt it.
            if (stackInResult.isEmpty() || (stackInResult.isCompleteMatch(smeltingResult) && (stackInResult.getMaxSize() - stackInResult.getSize() >= smeltingResult.getSize()))) {
                return smeltingResult;
            }
        }
        return EMPTY_STACK;
    }

    @Override
    public String getFuelName() {
        return FURNACE_FUEL_NAME;
    }

    @Override
    protected int getFuelTime(IWrapperItemStack stack) {
        return stack.getFurnaceFuelValue();
    }

    @Override
    protected int getTimeForItem(IWrapperItemStack stack) {
        return stack.getSmeltingTime(world);
    }

    @Override
    public boolean isStackValid(IWrapperItemStack stackToCheck, int index) {
        if (index == SMELTING_ITEM_SLOT) {
            return !stackToCheck.getSmeltedItem(world).isEmpty();
        } else {
            return super.isStackValid(stackToCheck, index);
        }
    }
}
