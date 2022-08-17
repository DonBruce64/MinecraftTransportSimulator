package minecrafttransportsimulator.items.instances;

import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.PartEffector;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

public class ItemPartEffector extends AItemPart{
	
	public ItemPartEffector(JSONPart definition, String subName, String sourcePackID){
		super(definition, subName, sourcePackID);
	}

	@Override
	public PartEffector createPart(AEntityF_Multipart<?> entity, IWrapperPlayer placingPlayer, JSONPartDefinition packVehicleDef, IWrapperNBT partData) {
		return new PartEffector(entity, placingPlayer, packVehicleDef, partData);
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
