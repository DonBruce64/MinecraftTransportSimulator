package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.rendering.instances.RenderDecor;

/**Decor tile entity.  Contains the definition so we know how
 * to render this in the TESR call, as well as if we need to do
 * crafting operations if we are a crafting decor type.
 *
 * @author don_bruce
 */
public class TileEntityDecor extends ATileEntityBase<JSONDecor>{
	public final BoundingBox[] boundingBoxes = new BoundingBox[4];

	//Generic text variables.
	protected final List<String> textLines;
	
	public TileEntityDecor(IWrapperWorld world, Point3i position, IWrapperNBT data){
		super(world, position, data);
		//Add a bounding box for each rotation.
		this.boundingBoxes[0] = new BoundingBox(new Point3d(0, 0, 0), definition.general.width/2D, definition.general.height/2D, definition.general.depth/2D);
		this.boundingBoxes[1] = new BoundingBox(new Point3d(0, 0, 0), definition.general.depth/2D, definition.general.height/2D, definition.general.width/2D);
		this.boundingBoxes[2] = boundingBoxes[0];
		this.boundingBoxes[3] = boundingBoxes[1];
		if(definition.general.textObjects != null){
			this.textLines = data.getStrings("textLines", definition.general.textObjects.size());
		}else{
			this.textLines = new ArrayList<String>();
		}
	}
	
	public List<String> getTextLines(){
		return textLines;
	}
	
	public void setTextLines(List<String> textLinesToSet){
		this.textLines.clear();
		this.textLines.addAll(textLinesToSet);
	}
	
	@Override
	public RenderDecor getRenderer(){
		return new RenderDecor();
	}
	
	@Override
	public void save(IWrapperNBT data){
		super.save(data);
		data.setStrings("textLines", textLines);
	}
}
