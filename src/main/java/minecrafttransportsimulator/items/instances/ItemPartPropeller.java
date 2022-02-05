package minecrafttransportsimulator.items.instances;

import java.util.List;

import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.PartPropeller;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;

public class ItemPartPropeller extends AItemPart{
	
	public ItemPartPropeller(JSONPart definition, String subName, String sourcePackID){
		super(definition, subName, sourcePackID);
	}

	@Override
	public boolean isPartValidForPackDef(JSONPartDefinition placementDefinition, String subNameToPlaceOn, boolean checkMinMax){
		return super.isPartValidForPackDef(placementDefinition, subNameToPlaceOn, checkMinMax) && (!checkMinMax || (placementDefinition.minValue <= definition.propeller.diameter && placementDefinition.maxValue >= definition.propeller.diameter));
	}
	
	@Override
	public PartPropeller createPart(AEntityF_Multipart<?> entity, WrapperPlayer placingPlayer, JSONPartDefinition packVehicleDef, WrapperNBT partData, APart parentPart){
		return new PartPropeller(entity, placingPlayer, packVehicleDef, partData, parentPart);
	}
	
	@Override
	public void addTooltipLines(List<String> tooltipLines, WrapperNBT data){
		super.addTooltipLines(tooltipLines, data);
		tooltipLines.add(InterfaceCore.translate(definition.propeller.isDynamicPitch ? "info.item.propeller.dynamicPitch" : "info.item.propeller.staticPitch"));
		tooltipLines.add(InterfaceCore.translate("info.item.propeller.pitch") + definition.propeller.pitch);
		tooltipLines.add(InterfaceCore.translate("info.item.propeller.diameter") + definition.propeller.diameter);
		tooltipLines.add(InterfaceCore.translate("info.item.propeller.maxrpm") + Math.round(60*340.29/(0.0254*Math.PI*definition.propeller.diameter)));
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
