package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.mcinterface.WrapperNBT;

/**Sign pole component.  Renders a sign texture and text.
*
* @author don_bruce
*/
public class TileEntityPole_Sign extends ATileEntityPole_Component{
	
	public TileEntityPole_Sign(TileEntityPole core, WrapperNBT data){
		super(core, data);
	}
	
	@Override
	public float getLightProvided(){
		return 0;
	}
}
