package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.mcinterface.InterfaceRender;

/**Base Tile Entity rendering class (TESR).  
 * This should be used with all blocks that need fancy rendering that can't be done with JSON.
 *
 * @author don_bruce
 */
public abstract class ARenderTileEntityBase<RenderedTileEntity extends ATileEntityBase<?>>{
	
	/**
	 *  Called to render this tile entity.  The currently-bound texture is undefined, so you will need
	 *  to bind whichever texture you see fit to do so.  This can be done via {@link InterfaceRender#bindTexture(String, String)}
	 */
	public abstract void render(RenderedTileEntity tileEntity, float partialTicks);
	
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
