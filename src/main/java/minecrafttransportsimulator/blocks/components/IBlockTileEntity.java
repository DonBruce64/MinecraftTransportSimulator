package minecrafttransportsimulator.blocks.components;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.jsondefs.AJSONItem;

/**Interface for blocks that have tile entities.  Such tile entities
 * are pack-based, so are linked to a specific pack definition.
 *
 * @author don_bruce
 */
public interface IBlockTileEntity<JSONDefinition extends AJSONItem<? extends AJSONItem<?>.General>>{
	
	/**
	 *  Gets a new Tile Entity for this block.
	 */
	public ATileEntityBase<JSONDefinition> createTileEntity();
}
