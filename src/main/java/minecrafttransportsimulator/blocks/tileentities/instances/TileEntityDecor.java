package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.rendering.blocks.RenderDecor;

/**Decor tile entity.  Just contains the definiton so we know how
 * to render this in the TESR call.
 *
 * @author don_bruce
 */
public class TileEntityDecor extends ATileEntityBase<JSONDecor>{
	public final BoundingBox[] boundingBoxes = new BoundingBox[4];
	
	@Override
	public void setDefinition(JSONDecor definition){
		super.setDefinition(definition);
		//Add a bounding box for each rotation.
		boundingBoxes[0] = new BoundingBox(0, 0, 0, definition.general.width/2D, definition.general.height/2D, definition.general.depth/2D);
		boundingBoxes[1] = boundingBoxes[0].getRotated90();
		boundingBoxes[2] = boundingBoxes[0];
		boundingBoxes[3] = boundingBoxes[1];
	}
	
	@Override
	public RenderDecor getRenderer(){
		return new RenderDecor();
	}
}
