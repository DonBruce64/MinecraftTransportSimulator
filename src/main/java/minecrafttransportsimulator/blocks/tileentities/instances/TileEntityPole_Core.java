package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.items.instances.ItemPoleComponent;
import minecrafttransportsimulator.mcinterface.WrapperNBT;

/**Core component for poles.  Allows us to change the core model.
 * 
 * @author don_bruce
 */
public class TileEntityPole_Core extends ATileEntityPole_Component{
		
	public TileEntityPole_Core(TileEntityPole core, ItemPoleComponent item, WrapperNBT data){
		super(core, item, data);
	}

	@Override
	public float lightLevel(){
		return 0;
	}
}
