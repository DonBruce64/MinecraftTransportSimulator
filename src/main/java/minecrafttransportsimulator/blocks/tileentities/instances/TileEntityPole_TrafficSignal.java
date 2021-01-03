package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.items.instances.ItemPoleComponent;
import minecrafttransportsimulator.rendering.components.LightType;

/**Traffic signal component for poles.  This doesn't tick, as the state of the light
 * is by default having the unlinked light on until changed by a {@link TileEntitySignalController}.
 * 
 * @author don_bruce
 */
public class TileEntityPole_TrafficSignal extends ATileEntityPole_Component{
	
	public LightType lightOn;
	
	public TileEntityPole_TrafficSignal(TileEntityPole core, ItemPoleComponent item){
		super(core, item);
		lightOn = LightType.UNLINKEDLIGHT;
	}

	@Override
	public float lightLevel(){
		return lightOn != null ? 12F/15F : 0.0F;
	}
}
