package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.items.instances.ItemPoleComponent;
import minecrafttransportsimulator.mcinterface.WrapperNBT;

/**Traffic signal component for poles.  This doesn't tick, as the state of the light
 * is by default having the unlinked light on until changed by a {@link TileEntitySignalController}.
 * 
 * @author don_bruce
 */
public class TileEntityPole_TrafficSignal extends ATileEntityPole_Component{
	
	public TileEntityPole_TrafficSignal(TileEntityPole core, ItemPoleComponent item, WrapperNBT data){
		super(core, item, data);
		//FIXME move this to when we add this component to a pole.
		//variablesOn.add(LightType.UNLINKEDLIGHT.lowercaseName);
	}

	@Override
	public float lightLevel(){
		//We always have the generic light on, so account for that.
		return variablesOn.size() > 1 ? 12F/15F : 0.0F;
	}
}
