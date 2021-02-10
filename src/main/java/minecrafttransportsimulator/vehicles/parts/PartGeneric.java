package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.baseclasses.AEntityE_Multipart;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.WrapperNBT;

public final class PartGeneric extends APart{
	
	public PartGeneric(AEntityE_Multipart<?> entityOn, JSONPart definition, JSONPartDefinition packVehicleDef, WrapperNBT data, APart parentPart){
		super(entityOn, definition, packVehicleDef, data, parentPart);
	}
}
