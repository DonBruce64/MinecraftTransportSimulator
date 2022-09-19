package minecrafttransportsimulator.mcinterface;

import minecrafttransportsimulator.entities.instances.EntityFluidTank;
import minecrafttransportsimulator.items.components.AItemBase;

/**
 * IWrapper for item stacks.  Contains only a few methods, and ones that are
 * better named than the default ones in stacks.  Also, they WON'T change
 * on version changes.
 *
 * @author don_bruce
 */
public interface IWrapperItemStack {

    /**
     * Returns true if the passed-in stack and this stack are a match for
     * all properties (excluding stack size).  This takes into account item,
     * damage, and data.  Used for checking if stacks can be combined.
     */
    boolean isCompleteMatch(IWrapperItemStack other);

    /**
     * Returns the fuel amount (in ticks) for this item.
     * Only returns the value for one item in the stack, not all items.
     */
    int getFuelValue();

    /**
     * Returns the item that this item can be smelted to make, or an empty stack
     * if this item cannot be smelted.  Note the the returned stack is a new instance
     * and may be modified without affecting future calls to this method.
     */
    IWrapperItemStack getSmeltedItem(AWrapperWorld world);

    /**
     * Returns the time it takes to smelt this item.  Note that due to Vanilla MC jank,
     * this value MAY be non-zero even if {@link #getSmeltedItem()} returns nothing.  As such,
     * that method should be checked before this one.
     */
    int getSmeltingTime(AWrapperWorld world);

    /**
     * Returns the item for this stack.
     * Only valid for base items, not external ones.
     */
    AItemBase getItem();

    /**
     * Returns true if the stack doesn't have any items.
     * Essentially, this is a stack of 0, but there's special
     * logic that has to happen with this in tandem with a 0
     * stack size to make MC think it's a "blank" stack for
     * inventories.  In a nutshell, inventories can't have null
     * stacks, only empty stacks, so we will never get a null
     * stack and will never have a null IWrapper
     */
    boolean isEmpty();

    /**
     * Returns the size of the stack.
     */
    int getSize();

    /**
     * Returns the max possible size of the stack.
     */
    int getMaxSize();

    /**
     * Adds the specified qty to the stack, with negative numbers removing
     * items.  Returns the qty, adjusted by the items added or removed.
     * Example: qty=20, added 4, returns 16.  qty=-20, removed 4, returns -16.
     * Note that if a stack is decremented to a size of 0, it will loose
     * the data that tells it what item makes up the stack.
     */
    int add(int qty);

    /**
     * Splits this stack into two.  The second with qty amount
     * of items in it, and the same data.
     */
    IWrapperItemStack split(int qty);

    /**
     * Tries to fill or drain from the passed-in tank into this item.
     * If the player is normal, then it will fill this item.
     * If the player is sneaking, they will drain this item into the tank.
     * If the player is creative, then the item won't be modified (but the tank will).
     * Returns true if an operation COULD occur.  This is to block other interactions.
     * Used only for items that used external fluid storage systems.
     */
    boolean interactWith(EntityFluidTank tank, IWrapperPlayer player);

    /**
     * Returns the data from the stack.
     * If there is no data, then a new NBT tag is returned.
     * If the data is modified, {@link #setData(AIWrapperNBT)} should
     * be called as new NBT tags generated from this method aren't linked
     * to the stack by default.  It also ensures proper states when
     * interfacing with modded items.
     */
    IWrapperNBT getData();

    /**
     * Sets the data to this stack.
     */
    void setData(IWrapperNBT data);
}