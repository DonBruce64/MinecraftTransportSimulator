package minecrafttransportsimulator.items.packs.parts;

import minecrafttransportsimulator.items.packs.AItemPack;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;

public abstract class AItemPart extends AItemPack<JSONPart>{
	
	public AItemPart(JSONPart definition){
		super(definition);
	}
	
	public boolean isPartValidForPackDef(VehiclePart partDefinition){
		if(partDefinition.customTypes == null){
			return definition.general.customType == null;
		}else if(definition.general.customType == null){
			return partDefinition.customTypes == null || partDefinition.customTypes.contains("");
		}else{
			return partDefinition.customTypes.contains(definition.general.customType);
		}
	}
}
