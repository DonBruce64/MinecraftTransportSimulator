package minecrafttransportsimulator.dataclasses;

import java.util.ArrayList;
import java.util.List;

import mcinterface1122.MasterInterface;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.item.Item;

@Deprecated
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
			items.add(MasterInterface.getItem(packItem));
		}
		return items;
	}
}
