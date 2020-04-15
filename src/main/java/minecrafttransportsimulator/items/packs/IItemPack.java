package minecrafttransportsimulator.items.packs;

import minecrafttransportsimulator.jsondefs.AJSONItem;
import net.minecraft.item.Item;

/**Interface that defines an item as a pack-based item.  Such items will be constructed
 * and registered with the MTS pack system for loading into the game.
 * 
 * @author don_bruce
 */
public interface IItemPack<JSONDefinition extends AJSONItem<? extends AJSONItem<?>.General>>{
	
	public JSONDefinition getDefinition();
	
	public default Item getItem(){
		return (Item) this;
	}
}
