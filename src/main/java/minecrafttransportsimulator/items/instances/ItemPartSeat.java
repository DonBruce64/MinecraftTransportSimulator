package minecrafttransportsimulator.items.instances;

import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.WrapperNBT;

public class ItemPartSeat extends AItemPart{
	
	public ItemPartSeat(JSONPart definition, String subName, String sourcePackID){
		super(definition, subName, sourcePackID);
	}
	
	@Override
	public PartSeat createPart(AEntityE_Multipart<?> entity, JSONPartDefinition packVehicleDef, WrapperNBT partData, APart parentPart){
		return new PartSeat(entity, packVehicleDef, validateData(partData), parentPart);
	}
	
	public static final AItemPartCreator CREATOR = new AItemPartCreator(){
		@Override
		public boolean isCreatorValid(JSONPart definition){
			return definition.generic.type.startsWith("seat");
		}
		@Override
		public ItemPartSeat createItem(JSONPart definition, String subName, String sourcePackID){
			return new ItemPartSeat(definition, subName, sourcePackID);
		}
	};
}
