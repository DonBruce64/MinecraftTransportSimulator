package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;

/**Lighted pole component.  Renders a constant beam when turned on.
 * 
 * @author don_bruce
 */
public class TileEntityPole_StreetLight extends ATileEntityPole_Component{
	
	public TileEntityPole_StreetLight(TileEntityPole core, WrapperPlayer placingPlayer, Axis axis, WrapperNBT data){
		super(core, placingPlayer, axis, data);
	}

	@Override
	public float getLightProvided(){
		return 12F/15F;
	}
}
