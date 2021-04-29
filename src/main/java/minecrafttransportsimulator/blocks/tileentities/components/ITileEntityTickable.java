package minecrafttransportsimulator.blocks.tileentities.components;

/**Interface that allows the Tile Entity to be updated every tick.
 *
 * @author don_bruce
 */
public interface ITileEntityTickable{
	/**
	 *  Called every tick for updates.
	 */
	public boolean update();
}
