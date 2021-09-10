package minecrafttransportsimulator.items.instances;

import java.util.List;

import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.PartGroundDevice;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperNBT;

public class ItemPartGroundDevice extends AItemPart{
	
	public ItemPartGroundDevice(JSONPart definition, String subName, String sourcePackID){
		super(definition, subName, sourcePackID);
	}

	@Override
	public boolean isPartValidForPackDef(JSONPartDefinition placementDefinition, String subNameToPlaceOn, boolean checkMinMax){
		return super.isPartValidForPackDef(placementDefinition, subNameToPlaceOn, checkMinMax) && (!checkMinMax || (placementDefinition.minValue <= definition.ground.height && placementDefinition.maxValue >= definition.ground.height));
	}
	
	@Override
	public PartGroundDevice createPart(AEntityE_Multipart<?> entity, JSONPartDefinition packVehicleDef, WrapperNBT partData, APart parentPart){
		return new PartGroundDevice(entity, packVehicleDef, validateData(partData), parentPart);
	}
	
	@Override
	public void addTooltipLines(List<String> tooltipLines, WrapperNBT data){
		super.addTooltipLines(tooltipLines, data);
		tooltipLines.add(InterfaceCore.translate("info.item.ground_device.diameter") + definition.ground.height);
		tooltipLines.add(InterfaceCore.translate("info.item.ground_device.motivefriction") + definition.ground.motiveFriction);
		tooltipLines.add(InterfaceCore.translate("info.item.ground_device.lateralfriction") + definition.ground.lateralFriction);
		tooltipLines.add(InterfaceCore.translate("info.item.ground_device.rainfriction") + definition.ground.rainFrictionModifier);
		tooltipLines.add(InterfaceCore.translate(definition.ground.isWheel ? "info.item.ground_device.rotatesonshaft_true" : "info.item.ground_device.rotatesonshaft_false"));
		tooltipLines.add(InterfaceCore.translate(definition.ground.canFloat ? "info.item.ground_device.canfloat_true" : "info.item.ground_device.canfloat_false"));
	}
	
	public static final AItemPartCreator CREATOR = new AItemPartCreator(){
		@Override
		public boolean isCreatorValid(JSONPart definition){
			return definition.generic.type.startsWith("ground");
		}
		@Override
		public ItemPartGroundDevice createItem(JSONPart definition, String subName, String sourcePackID){
			return new ItemPartGroundDevice(definition, subName, sourcePackID);
		}
	};
}
