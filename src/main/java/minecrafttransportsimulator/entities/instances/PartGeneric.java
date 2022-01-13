package minecrafttransportsimulator.entities.instances;

import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;

public final class PartGeneric extends APart{
	
	public PartGeneric(AEntityF_Multipart<?> entityOn, WrapperPlayer placingPlayer, JSONPartDefinition placementDefinition, WrapperNBT data, APart parentPart){
		super(entityOn, placingPlayer, placementDefinition, data, parentPart);
	}
}
