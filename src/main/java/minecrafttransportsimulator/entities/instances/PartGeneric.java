package minecrafttransportsimulator.entities.instances;

import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.WrapperNBT;

public final class PartGeneric extends APart{
	
	public PartGeneric(AEntityE_Multipart<?> entityOn, JSONPartDefinition placementDefinition, WrapperNBT data, APart parentPart){
		super(entityOn, placementDefinition, data, parentPart);
	}
}
