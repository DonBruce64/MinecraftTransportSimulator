package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.items.instances.ItemPoleComponent;
import minecrafttransportsimulator.mcinterface.WrapperNBT;

/**Sign pole component.  Renders a sign texture and text.
*
* @author don_bruce
*/
public class TileEntityPole_Sign extends ATileEntityPole_Component{
	
	public TileEntityPole_Sign(TileEntityPole core, ItemPoleComponent item, WrapperNBT data){
		super(core, item, data);
	}
	
	@Override
	public float lightLevel(){
		return 0;
	}
}
