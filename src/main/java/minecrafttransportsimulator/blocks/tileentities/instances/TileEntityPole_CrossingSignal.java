package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;

/**Crossing signal component for poles.  This doesn't tick, as the state of the signal
 * is by default a flashing nowalk hand until changed by a {@link TileEntitySignalController}.
 * 
 * @author don_bruce
 */
public class TileEntityPole_CrossingSignal extends ATileEntityPole_Component{
	
	public CrossingState state = CrossingState.FLASHING_DONTWALK;
	
	public TileEntityPole_CrossingSignal(JSONPoleComponent definition){
		super(definition);
	}
	
	@Override
	public float lightLevel(){
		return !state.equals(CrossingState.OFF) ? 12F/15F : 0.0F;
	}

	public static enum CrossingState{
		OFF,
		WALK,
		FLASHING_DONTWALK,
		DONTWALK;
	}
}
