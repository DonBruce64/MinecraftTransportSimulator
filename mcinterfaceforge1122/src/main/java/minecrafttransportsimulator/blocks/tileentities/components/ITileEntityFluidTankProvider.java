package minecrafttransportsimulator.blocks.tileentities.components;

import minecrafttransportsimulator.entities.instances.EntityFluidTank;

/**
 * Interface that allows an object to contain a fluid tank.
 * Actual saving/loading of the tank is up to the object.  This interface
 * only contains a getter for the tank itself.  Null may NOT be returned.
 * If you haven't loaded tank data, return an empty tank instead.
 *
 * @author don_bruce
 */
public interface ITileEntityFluidTankProvider {
    /**
     * Gets the tank for this interface.
     */
    EntityFluidTank getTank();
}
