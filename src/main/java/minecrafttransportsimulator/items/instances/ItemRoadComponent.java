package minecrafttransportsimulator.items.instances;

import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent;
import minecrafttransportsimulator.packloading.PackResourceLoader.ItemClassification;

public class ItemRoadComponent extends AItemPack<JSONRoadComponent>{
	
	public ItemRoadComponent(JSONRoadComponent definition){
		super(definition, ItemClassification.ROAD);
	}
}
