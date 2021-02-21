package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.mcinterface.WrapperNBT;

/**Traffic signal component for poles.  This doesn't tick, as the state of the light
 * is by default having the unlinked light on until changed by a {@link TileEntitySignalController}.
 * 
 * @author don_bruce
 */
public class TileEntityPole_TrafficSignal extends ATileEntityPole_Component{
	
	public TileEntityPole_TrafficSignal(TileEntityPole core,  WrapperNBT data){
		super(core, data);
	}

	@Override
	public float getLightProvided(){
		//We always have the generic light on, so account for that.
		return variablesOn.size() > 1 ? 12F/15F : 0.0F;
	}
}
