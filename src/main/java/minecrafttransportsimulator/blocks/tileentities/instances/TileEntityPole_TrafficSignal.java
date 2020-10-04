package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.items.instances.ItemPoleComponent;

/**Traffic signal component for poles.  This doesn't tick, as the state of the light
 * is by default a flashing red until changed by a {@link TileEntitySignalController}.
 * 
 * @author don_bruce
 */
public class TileEntityPole_TrafficSignal extends ATileEntityPole_Component{
	
	public SignalState state = SignalState.UNLINKED;
	
	public TileEntityPole_TrafficSignal(ItemPoleComponent item){
		super(item);
	}

	@Override
	public float lightLevel(){
		return !state.equals(SignalState.OFF) ? 12F/15F : 0.0F;
	}
	
	public static enum SignalState{
		UNLINKED,
		GREEN,
		YELLOW,
		RED,
		OFF;
	}
}
