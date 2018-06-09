package minecrafttransportsimulator.items.parts;

import minecrafttransportsimulator.systems.PackParserSystem;

public class ItemPartGroundDevice extends AItemPart{
	
	public ItemPartGroundDevice(String partName){
		super(partName);
	}
	
	@Override
	public boolean isPartValueInRange(float minValue, float maxValue){
		float diameter = PackParserSystem.getPartPack(partName).groundDevice.diameter;
		return minValue <= diameter && maxValue >= diameter;
	}
}
