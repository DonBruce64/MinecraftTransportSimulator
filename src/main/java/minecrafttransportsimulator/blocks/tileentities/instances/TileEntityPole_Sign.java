package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;

/**Sign pole component.  Renders a sign texture and text.
*
* @author don_bruce
*/
public class TileEntityPole_Sign extends ATileEntityPole_Component{
	
	public final List<String> textLines = new ArrayList<String>();
	
	public TileEntityPole_Sign(JSONPoleComponent definition){
		super(definition);
	}
}
