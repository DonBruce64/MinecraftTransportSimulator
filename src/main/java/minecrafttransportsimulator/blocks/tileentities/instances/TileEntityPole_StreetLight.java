package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.rendering.components.LightType;

/**Lighted pole component.  Renders a constant beam when turned on.
 * 
 * @author don_bruce
 */
public class TileEntityPole_StreetLight extends ATileEntityPole_Component{
	
	public TileEntityPole_StreetLight(TileEntityPole core, WrapperNBT data){
		super(core, data);
		//FIXME move this to when we add this component to a pole.
		//variablesOn.add(LightType.STREETLIGHT.lowercaseName);
	}

	@Override
	public float getLightProvided(){
		return variablesOn.contains(LightType.STREETLIGHT.lowercaseName) ? 12F/15F : 0.0F;
	}
}
