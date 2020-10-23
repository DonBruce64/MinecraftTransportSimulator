package minecrafttransportsimulator.blocks.tileentities.components;

import minecrafttransportsimulator.items.instances.ItemRoadComponent;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent;

/**Class for components that can go on road segments.  Not actually a TE, just part of one.
 * 
 * @author don_bruce
 */
public class TileEntityRoad_Component{
	
	public final ItemRoadComponent item;
	public final JSONRoadComponent definition;
	
	public TileEntityRoad_Component(ItemRoadComponent item){
		this.item = item;
		this.definition = item.definition;
	}
}
