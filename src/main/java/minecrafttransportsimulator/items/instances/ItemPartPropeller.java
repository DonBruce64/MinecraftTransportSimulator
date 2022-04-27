package minecrafttransportsimulator.items.instances;

import java.util.List;

import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.PartPropeller;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

public class ItemPartPropeller extends AItemPart{
	
	public ItemPartPropeller(JSONPart definition, String subName, String sourcePackID){
		super(definition, subName, sourcePackID);
	}

	@Override
	public boolean isPartValidForPackDef(JSONPartDefinition placementDefinition, String subNameToPlaceOn, boolean checkMinMax){
		return super.isPartValidForPackDef(placementDefinition, subNameToPlaceOn, checkMinMax) && (!checkMinMax || (placementDefinition.minValue <= definition.propeller.diameter && placementDefinition.maxValue >= definition.propeller.diameter));
	}
	
	@Override
	public PartPropeller createPart(AEntityF_Multipart<?> entity, IWrapperPlayer placingPlayer, JSONPartDefinition packVehicleDef, IWrapperNBT partData, APart parentPart){
		return new PartPropeller(entity, placingPlayer, packVehicleDef, partData, parentPart);
	}
	
	@Override
	public void addTooltipLines(List<String> tooltipLines, IWrapperNBT data){
		super.addTooltipLines(tooltipLines, data);
		tooltipLines.add(definition.propeller.isDynamicPitch ? JSONConfigLanguage.ITEMINFO_PROPELLER_DYNAMICPITCH.value : JSONConfigLanguage.ITEMINFO_PROPELLER_STATICPITCH.value);
		tooltipLines.add(JSONConfigLanguage.ITEMINFO_PROPELLER_PITCH.value + definition.propeller.pitch);
		tooltipLines.add(JSONConfigLanguage.ITEMINFO_PROPELLER_DIAMETER.value + definition.propeller.diameter);
	}
	
	public static final AItemPartCreator CREATOR = new AItemPartCreator(){
		@Override
		public boolean isCreatorValid(JSONPart definition){
			return definition.generic.type.startsWith("propeller");
		}
		@Override
		public ItemPartPropeller createItem(JSONPart definition, String subName, String sourcePackID){
			return new ItemPartPropeller(definition, subName, sourcePackID);
		}
	};
}
