package minecrafttransportsimulator.blocks.tileentities.instances;

import mcinterface.WrapperNBT;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.rendering.instances.RenderDecor;

/**Decor tile entity.  Contains the definition so we know how
 * to render this in the TESR call, as well as if we need to do
 * crafting operations if we are a crafting decor type.
 *
 * @author don_bruce
 */
public class TileEntityDecor extends ATileEntityBase<JSONDecor>{
	public final BoundingBox[] boundingBoxes = new BoundingBox[4];
	
	public TileEntityDecor(WrapperWorld world, Point3i position, WrapperNBT data){
		super(world, position, data);
		//Add a bounding box for each rotation.
		boundingBoxes[0] = new BoundingBox(new Point3d(0, 0, 0), definition.general.width/2D, definition.general.height/2D, definition.general.depth/2D);
		boundingBoxes[1] = new BoundingBox(new Point3d(0, 0, 0), definition.general.depth/2D, definition.general.height/2D, definition.general.width/2D);
		boundingBoxes[2] = boundingBoxes[0];
		boundingBoxes[3] = boundingBoxes[1];
	}
	
	@Override
	public RenderDecor getRenderer(){
		return new RenderDecor();
	}
}
