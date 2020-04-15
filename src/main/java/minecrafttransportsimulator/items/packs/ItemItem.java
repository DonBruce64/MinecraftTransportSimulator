package minecrafttransportsimulator.items.packs;

import minecrafttransportsimulator.jsondefs.JSONItem;

public class ItemItem extends AItemPack<JSONItem>{
	
	public ItemItem(JSONItem definition){
		super(definition);
	}
	
	@Override
	public String getModelLocation(){
		return null;
	}
	
	@Override
	public String getTextureLocation(){
		return null;
	}
}
