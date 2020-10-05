package minecrafttransportsimulator.items.instances;

import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.jsondefs.JSONItem;
import minecrafttransportsimulator.packloading.PackResourceLoader.ItemClassification;

public class ItemItem extends AItemPack<JSONItem>{
	
	public ItemItem(JSONItem definition){
		super(definition, ItemClassification.ITEM);
	}
}
