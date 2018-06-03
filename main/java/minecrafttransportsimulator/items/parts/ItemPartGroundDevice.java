package minecrafttransportsimulator.items.parts;

import minecrafttransportsimulator.systems.PackParserSystem;

public class ItemPartGroundDevice extends AItemPart{
	
	public ItemPartGroundDevice(String partName){
		super(partName);
	}
	
	@Override
	public float getPartValue(){
		return PackParserSystem.getPartPack(partName).groundDevice.diameter;
	}
}
