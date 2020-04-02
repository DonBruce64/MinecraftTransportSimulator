package minecrafttransportsimulator.blocks.tileentities.components;

/**Base Tile Entity class with the ability to be updated every tick.
 *
 * @author don_bruce
 */
public abstract class ATileEntityBaseTickable extends ATileEntityBase{
	/**
	 *  Called every tick for updates.
	 */
	public abstract void update();
}
