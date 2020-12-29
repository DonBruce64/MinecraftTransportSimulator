package minecrafttransportsimulator.baseclasses;

import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;

/**Interface that allows an object to contain a fluid tank.
 * Actual saving/loading of the tank is up to the object.  This interface
 * only contains a getter for the tank itself.  Null may NOT be returned.
 * If you haven't loaded tank data, return an empty tank instead.
 *
 * @author don_bruce
 */
public interface IFluidTankProvider{
	/**
	 *  Gets the tank for this interface.
	 */
	public FluidTank getTank();
	
	/**
	 *  Returns true if pipes can connect to the tank on the passed-in axis.
	 */
	public boolean canConnectOnAxis(Axis axis);
}
