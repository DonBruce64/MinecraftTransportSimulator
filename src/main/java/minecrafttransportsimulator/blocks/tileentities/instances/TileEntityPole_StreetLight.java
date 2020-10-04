package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.items.instances.ItemPoleComponent;

/**Lighted pole component.  Renders a constant beam when turned on.
 * 
 * @author don_bruce
 */
public class TileEntityPole_StreetLight extends ATileEntityPole_Component{
	
	public LightState state = LightState.ON;
	
	public TileEntityPole_StreetLight(ItemPoleComponent item){
		super(item);
	}

	@Override
	public float lightLevel(){
		return !state.equals(LightState.OFF) ? 12F/15F : 0.0F;
	}
	
	public static enum LightState{
		OFF,
		ON;
	}
}
