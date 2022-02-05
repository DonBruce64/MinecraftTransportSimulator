package minecrafttransportsimulator.items.instances;

import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.PartGeneric;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;

public class ItemPartGeneric extends AItemPart{
	
	public ItemPartGeneric(JSONPart definition, String subName, String sourcePackID){
		super(definition, subName, sourcePackID);
	}
	
	@Override
	public boolean isPartValidForPackDef(JSONPartDefinition placementDefinition, String subNameToPlaceOn, boolean checkMinMax){
		return super.isPartValidForPackDef(placementDefinition, subNameToPlaceOn, checkMinMax) && (!checkMinMax || ((placementDefinition.minValue <= definition.generic.height && placementDefinition.maxValue >= definition.generic.height) || (placementDefinition.minValue == 0 && placementDefinition.maxValue == 0)));
	}

	@Override
	public PartGeneric createPart(AEntityF_Multipart<?> entity, WrapperPlayer placingPlayer, JSONPartDefinition packVehicleDef, WrapperNBT partData, APart parentPart){
		return new PartGeneric(entity, placingPlayer, packVehicleDef, partData, parentPart);
	}
	
	public static final AItemPartCreator CREATOR = new AItemPartCreator(){
		@Override
		public boolean isCreatorValid(JSONPart definition){
			return definition.generic.type.startsWith("generic");
		}
		@Override
		public ItemPartGeneric createItem(JSONPart definition, String subName, String sourcePackID){
			return new ItemPartGeneric(definition, subName, sourcePackID);
		}
	};
}
