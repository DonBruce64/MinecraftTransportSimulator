package minecrafttransportsimulator.items.instances;

import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.jsondefs.JSONInstrument;
import minecrafttransportsimulator.packloading.PackResourceLoader.ItemClassification;

public class ItemInstrument extends AItemPack<JSONInstrument>{
	
	public ItemInstrument(JSONInstrument definition){
		super(definition, ItemClassification.INSTRUMENT);
	}
}
