package minecrafttransportsimulator.items.packs;

import minecrafttransportsimulator.jsondefs.JSONInstrument;

public class ItemInstrument extends AItemPack<JSONInstrument>{
	
	public ItemInstrument(JSONInstrument definition){
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
