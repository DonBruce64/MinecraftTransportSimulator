package minecrafttransportsimulator.rendering.components;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
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
	public void adjustPositionRotation(RenderedTileEntity entity, Point3d entityPositionDelta, Point3d entityRotationDelta, float partialTicks){
		//Offset X and Z to be centered in the block.
		entityPositionDelta.x += 0.5;
		entityPositionDelta.z += 0.5;
		if(translateToSlabs() && entity.world.isBlockBottomSlab(entity.position.copy().add(0, -1, 0))){
			entityPositionDelta.y -= 0.5D;			
		}
	}
	
	@Override
	protected void renderBoundingBoxes(RenderedTileEntity entity, Point3d entityPositionDelta){
		super.renderBoundingBoxes(entity, entityPositionDelta);
		
		//Render our collision box.
		BoundingBox box = entity.getCollisionBox();
		Point3d boxCenterDelta = box.globalCenter.copy().subtract(entity.position).add(entityPositionDelta);
		GL11.glTranslated(boxCenterDelta.x, boxCenterDelta.y + 0.5, boxCenterDelta.z);
		box.renderable.render();
		GL11.glTranslated(-boxCenterDelta.x, -boxCenterDelta.y - 0.5, -boxCenterDelta.z);
	}
}
