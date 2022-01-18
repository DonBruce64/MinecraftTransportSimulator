package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.baseclasses.Matrix4dPlus;
import minecrafttransportsimulator.baseclasses.Point3dPlus;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;

/**Base Tile Entity rendering class (TESR).  
 * This should be used with all blocks that need fancy rendering that can't be done with JSON.
 *
 * @author don_bruce
 */
public abstract class ARenderTileEntityBase<RenderedTileEntity extends ATileEntityBase<?>> extends ARenderEntityDefinable<RenderedTileEntity>{
	
	/**
	 *  Returns true if this TE should translated down 0.5 units to sink on half-slabs.
	 */
	public boolean translateToSlabs(){
		return true;
	}
	
	@Override
	public void adjustPositionOrientation(RenderedTileEntity entity, Point3dPlus entityPositionDelta, Matrix4dPlus interpolatedOrientation, float partialTicks){
		//Offset X and Z to be centered in the block.
		entityPositionDelta.x += 0.5;
		entityPositionDelta.z += 0.5;
		if(translateToSlabs() && entity.world.isBlockBelowBottomSlab(entity.position)){
			entityPositionDelta.y -= 0.5D;			
		}
	}
	
	@Override
	protected void renderBoundingBoxes(RenderedTileEntity entity, Matrix4dPlus transform){
		super.renderBoundingBoxes(entity, transform);
		//FIXME figure out why we need a 1/2 block offset here.  Shouldn't this already be offset?
		entity.getCollisionBox().renderWireframe(entity, transform, null);
	}
}
