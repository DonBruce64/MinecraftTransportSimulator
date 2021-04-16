package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;

/**Base Tile Entity rendering class (TESR).  
 * This should be used with all blocks that need fancy rendering that can't be done with JSON.
 *
 * @author don_bruce
 */
public abstract class ARenderTileEntityBase<RenderedTileEntity extends ATileEntityBase<?>> extends ARenderEntity<RenderedTileEntity>{
	
	/**
	 *  Returns true if this TE should translated down 0.5 units to sink on half-slabs.
	 */
	public boolean translateToSlabs(){
		return true;
	}
	
	@Override
	public void adjustPositionRotation(RenderedTileEntity entity, float partialTicks, Point3d entityPosition, Point3d entityRotation){
		//Offset X and Z to be centered in the block.
		entityPosition.x += 0.5;
		entityPosition.z += 0.5;
		if(translateToSlabs() && entity.world.isBlockBottomSlab(entity.position.copy().add(0, -1, 0))){
			entityPosition.y -= 0.5D;			
		}
	}
}
