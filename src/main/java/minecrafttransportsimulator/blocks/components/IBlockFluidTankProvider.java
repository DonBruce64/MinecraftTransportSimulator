
package minecrafttransportsimulator.blocks.components;

import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityFluidTankProvider;

/**Interface that allows a block to create tile entities with fluid tanks.
 * Such Tile Entities must implemnt {@link ITileEntityFluidTankProvider}.
 * This interface does nothing except flag the block for a specific registration
 * in the MC interface to let it know the block will contain such a Tile Entity.
 *
 * @author don_bruce
 */
public interface IBlockFluidTankProvider{
}