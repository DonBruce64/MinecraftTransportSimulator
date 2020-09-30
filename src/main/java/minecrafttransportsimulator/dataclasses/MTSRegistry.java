package minecrafttransportsimulator.dataclasses;

import java.util.ArrayList;
import java.util.List;

import mcinterface.BuilderItem;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.item.Item;

/**Main registry class.  This class should be referenced by any class looking for
 * MTS items or blocks.  Adding new items and blocks is a simple as adding them
 * as a field; the init method automatically registers all items and blocks in the class
 * and orders them according to the order in which they were declared.
 * This calls the {@link PackParserSystem} to register any custom vehicles and parts
 * that were loaded by packs.
 * 
 * @author don_bruce
 */
public final class MTSRegistry{
	
	/**
	 * This is called by packs to query what items they have registered.
	 * Used to allow packs to register their own items after core mod processing.
	 * We need to cast-down the items to the Item class as a List with type Item is what
	 * the packloader is expecting.
	 */
	public static List<Item> getItemsForPack(String packID){
		List<Item> items = new ArrayList<Item>();
		for(AItemPack<?> packItem : PackParserSystem.getAllItemsForPack(packID)){
			items.add(BuilderItem.itemWrapperMap.get(packItem));
		}
		return items;
	}
}
