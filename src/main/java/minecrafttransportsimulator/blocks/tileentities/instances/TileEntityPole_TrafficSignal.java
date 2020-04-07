package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;

/**Crossing signal component for poles.  This doesn't tick, as the state of the light
 * is by default a flashing red until changed by a {@link TileEntitySignalController}.
 * 
 * @author don_bruce
 */
public class TileEntityPole_TrafficSignal extends ATileEntityPole_Component{
	
	public SignalState state = SignalState.FLASHING_RED;
	
	public TileEntityPole_TrafficSignal(JSONPoleComponent definition){
		super(definition);
	}
	
	public static enum SignalState{
		OFF,
		GREEN,
		YELLOW,
		RED,
		FLASHING_YELLOW,
		FLASHING_RED;
	}
}
