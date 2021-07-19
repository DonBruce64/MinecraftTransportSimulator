package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.mcinterface.WrapperNBT;

/**Lighted pole component.  Renders a constant beam when turned on.
 * 
 * @author don_bruce
 */
public class TileEntityPole_StreetLight extends ATileEntityPole_Component{
	
	public TileEntityPole_StreetLight(TileEntityPole core, WrapperNBT data){
		super(core, data);
	}

	@Override
	public float getLightProvided(){
		return 12F/15F;
	}
}
