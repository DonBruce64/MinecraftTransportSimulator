package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;

/**Base Tile Entity rendering class (TESR).  
 * This should be used with all blocks that need fancy rendering that can't be done with JSON.
 *
 * @author don_bruce
 */
public abstract class ARenderTileEntityBase<RenderedTileEntity extends ATileEntityBase<?>> extends ARenderEntity<RenderedTileEntity>{
	
	/**
	 *  Returns true if this TE should be rotated to the rotation of the block.
	 */
	public boolean rotateToBlock(){
		return true;
	}
	
	/**
	 *  Returns true if this TE should translated down 0.5 units to sink on half-slabs.
	 */
	public boolean translateToSlabs(){
		return true;
	}
}
