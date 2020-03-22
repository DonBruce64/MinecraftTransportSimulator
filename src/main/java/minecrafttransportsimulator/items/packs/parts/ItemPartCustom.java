package minecrafttransportsimulator.items.packs.parts;

import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;

public class ItemPartCustom extends AItemPart{
	
	public ItemPartCustom(JSONPart definition){
		super(definition);
	}
	
	@Override
	public boolean isPartValidForPackDef(VehiclePart packPart){
		return packPart.customTypes != null && packPart.customTypes.contains(definition.general.customType);
	}
}
