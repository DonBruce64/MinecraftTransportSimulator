package minecrafttransportsimulator.blocks.components;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;

/**Interface for blocks that have tile entities.
 *
 * @author don_bruce
 */
public interface IBlockTileEntity{
	
	/**
	 *  Gets a new Tile Entity for this block.
	 */
	public ATileEntityBase createTileEntity();
	
	/**
	 *  If you add a TE model, that model will render with the main block model.
	 *  If you don't want the main block model to render, make this return false.
	 */
	public default boolean renderBlockModel(){
		return true;
	}
}
