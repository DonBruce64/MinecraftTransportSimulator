package minecrafttransportsimulator.items.instances;

import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.PartEffector;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;

public class ItemPartEffector extends AItemPart{
	
	public ItemPartEffector(JSONPart definition, String subName, String sourcePackID){
		super(definition, subName, sourcePackID);
	}

	@Override
	public PartEffector createPart(AEntityE_Multipart<?> entity, WrapperPlayer placingPlayer, JSONPartDefinition packVehicleDef, WrapperNBT partData, APart parentPart) {
		return new PartEffector(entity, placingPlayer, packVehicleDef, validateData(partData), parentPart);
	}
	
	public static final AItemPartCreator CREATOR = new AItemPartCreator(){
		@Override
		public boolean isCreatorValid(JSONPart definition){
			return definition.generic.type.startsWith("effector");
		}
		@Override
		public ItemPartEffector createItem(JSONPart definition, String subName, String sourcePackID){
			return new ItemPartEffector(definition, subName, sourcePackID);
		}
	};
}
