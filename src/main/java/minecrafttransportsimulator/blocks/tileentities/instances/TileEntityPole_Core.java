package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;

/**Core component for poles.  Allows us to change the core model.
 * 
 * @author don_bruce
 */
public class TileEntityPole_Core extends ATileEntityPole_Component{
		
	public TileEntityPole_Core(JSONPoleComponent definition){
		super(definition);
	}

	@Override
	public float lightLevel(){
		return 0;
	}
}
