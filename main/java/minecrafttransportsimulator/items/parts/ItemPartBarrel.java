package minecrafttransportsimulator.items.parts;

import minecrafttransportsimulator.dataclasses.PackVehicleObject.PackPart;
import minecrafttransportsimulator.systems.PackParserSystem;

public class ItemPartBarrel extends AItemPart{
	
	public ItemPartBarrel(String partName){
		super(partName);
	}
	
	@Override
	public boolean isPartValidForPackDef(PackPart packPart){
		int capacity = PackParserSystem.getPartPack(partName).barrel.capacity;
		return packPart.minValue <= capacity && packPart.maxValue >= capacity ? super.isPartValidForPackDef(packPart) : false;
	}
}
