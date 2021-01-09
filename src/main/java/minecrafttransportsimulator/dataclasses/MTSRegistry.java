package minecrafttransportsimulator.dataclasses;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.jsondefs.JSONPack;
import minecrafttransportsimulator.mcinterface.MasterLoader;
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
		//Check to make sure we have a packDef for this pack.  If not, we need to create one prior to returning the items.
		//If we don't, then we'll have issue with loaders.
		if(!PackParserSystem.packMap.containsKey(packID)){
			JSONPack packDef = new JSONPack();
			packDef.internallyGenerated = true;
			packDef.packID = packID;
			packDef.fileStructure = 0;
			packDef.packName = MasterLoader.coreInterface.getModName(packID);
			PackParserSystem.packMap.put(packID, packDef);
		}
		List<Item> items = new ArrayList<Item>();
		for(AItemPack<?> packItem : PackParserSystem.getAllItemsForPack(packID)){
			items.add(packItem.getBuilder());
		}
		return items;
	}
}
