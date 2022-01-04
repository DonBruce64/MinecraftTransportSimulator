package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;

/**Sign pole component.  Renders a sign texture and text.
*
* @author don_bruce
*/
public class TileEntityPole_Sign extends ATileEntityPole_Component{
	
	public TileEntityPole_Sign(TileEntityPole core, WrapperPlayer placingPlayer, Axis axis, WrapperNBT data){
		super(core, placingPlayer, axis, data);
	}
}
