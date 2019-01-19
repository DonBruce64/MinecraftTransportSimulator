package minecrafttransportsimulator.items.parts;

import minecrafttransportsimulator.dataclasses.PackVehicleObject.PackPart;
import minecrafttransportsimulator.dataclasses.PackPartObject;
import minecrafttransportsimulator.systems.PackParserSystem;

public class ItemPartCustom extends AItemPart{
	
	public ItemPartCustom(String partName){
		super(partName);
	}
	
	@Override
	public boolean isPartValidForPackDef(PackPart packPart){
		PackPartObject itemPack = PackParserSystem.getPartPack(partName);
		return packPart.customTypes != null && packPart.customTypes.contains(itemPack.general.customType);
	}
}
