package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.items.instances.ItemPoleComponent;

/**Core component for poles.  Allows us to change the core model.
 * 
 * @author don_bruce
 */
public class TileEntityPole_Core extends ATileEntityPole_Component{
		
	public TileEntityPole_Core(ItemPoleComponent item){
		super(item);
	}

	@Override
	public float lightLevel(){
		return 0;
	}
}
