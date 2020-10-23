package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.blocks.tileentities.components.TileEntityRoad_Component;
import minecrafttransportsimulator.items.instances.ItemRoadComponent;

/**Core component for roads.  Allows us to change the core model.
 * This is the model that is used for the base portion of the road.
 * 
 * @author don_bruce
 */
public class TileEntityRoad_Core extends TileEntityRoad_Component{
		
	public TileEntityRoad_Core(ItemRoadComponent item){
		super(item);
	}
}
