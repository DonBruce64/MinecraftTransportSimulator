package minecrafttransportsimulator.items.instances;

import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.packloading.PackResourceLoader.ItemClassification;

public class ItemPoleComponent extends AItemPack<JSONPoleComponent>{
	
	public ItemPoleComponent(JSONPoleComponent definition){
		super(definition, ItemClassification.POLE);
	}
}
