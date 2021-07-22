package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.mcinterface.WrapperNBT;

/**Core component for poles.  Allows us to change the core model.
 * 
 * @author don_bruce
 */
public class TileEntityPole_Core extends ATileEntityPole_Component{
		
	public TileEntityPole_Core(TileEntityPole core, Axis axis, WrapperNBT data){
		super(core, axis, data);
	}
}
