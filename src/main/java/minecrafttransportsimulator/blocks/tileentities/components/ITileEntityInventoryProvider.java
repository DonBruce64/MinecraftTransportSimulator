package minecrafttransportsimulator.blocks.tileentities.components;

import minecrafttransportsimulator.entities.instances.EntityInventoryContainer;

/**
 * Interface that allows an object to contain an inventory.
 * Actual saving/loading of the inventory is up to the object.  This interface
 * only contains a getter for the inventory itself.  Null may NOT be returned.
 * If you haven't loaded inventory data, return an empty inventory instead.
 *
 * @author don_bruce
 */
public interface ITileEntityInventoryProvider {
    /**
     * Gets the inventory for this interface.
     */
    EntityInventoryContainer getInventory();
}
