package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.items.instances.ItemPoleComponent;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.rendering.components.LightType;

/**Lighted pole component.  Renders a constant beam when turned on.
 * 
 * @author don_bruce
 */
public class TileEntityPole_StreetLight extends ATileEntityPole_Component{
	
	public TileEntityPole_StreetLight(TileEntityPole core, ItemPoleComponent item, WrapperNBT data){
		super(core, item, data);
		//FIXME move this to when we add this component to a pole.
		//variablesOn.add(LightType.STREETLIGHT.lowercaseName);
	}

	@Override
	public float lightLevel(){
		return variablesOn.contains(LightType.STREETLIGHT.lowercaseName) ? 12F/15F : 0.0F;
	}
}
