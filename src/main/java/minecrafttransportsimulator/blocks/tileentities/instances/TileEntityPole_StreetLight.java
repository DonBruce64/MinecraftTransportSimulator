package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;

/**Lighted pole component.  Renders a constant beam when turned on.
 * 
 * @author don_bruce
 */
public class TileEntityPole_StreetLight extends ATileEntityPole_Component{
	
	public LightState state = LightState.ON;
	
	public TileEntityPole_StreetLight(JSONPoleComponent definition){
		super(definition);
	}

	public static enum LightState{
		OFF,
		ON;
	}
}
