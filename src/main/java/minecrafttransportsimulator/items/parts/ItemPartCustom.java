package minecrafttransportsimulator.items.parts;

import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.systems.PackParserSystem;

public class ItemPartCustom extends AItemPart{
	
	public ItemPartCustom(String partName){
		super(partName);
	}
	
	@Override
	public boolean isPartValidForPackDef(VehiclePart packPart){
		JSONPart itemPack = PackParserSystem.getPartPack(partName);
		return packPart.customTypes != null && packPart.customTypes.contains(itemPack.general.customType);
	}
}
