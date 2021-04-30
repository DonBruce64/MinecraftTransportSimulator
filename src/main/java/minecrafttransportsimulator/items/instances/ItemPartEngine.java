package minecrafttransportsimulator.items.instances;

import java.util.List;

import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.guis.components.InterfaceGUI;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperNBT;

public class ItemPartEngine extends AItemPart{
	
	public ItemPartEngine(JSONPart definition, String subName, String sourcePackID){
		super(definition, subName, sourcePackID);
	}

	@Override
	public boolean isPartValidForPackDef(JSONPartDefinition placementDefinition, String subNameToPlaceOn, boolean checkMinMax){
		return super.isPartValidForPackDef(placementDefinition, subNameToPlaceOn, checkMinMax) && (!checkMinMax || (placementDefinition.minValue <= definition.engine.fuelConsumption && placementDefinition.maxValue >= definition.engine.fuelConsumption));
	}
	
	@Override
	public PartEngine createPart(AEntityE_Multipart<?> entity, JSONPartDefinition packVehicleDef, WrapperNBT partData, APart parentPart){
		return new PartEngine(entity, packVehicleDef, validateData(partData), parentPart);
	}
	
	@Override
	public void addTooltipLines(List<String> tooltipLines, WrapperNBT data){
		super.addTooltipLines(tooltipLines, data);
		if(data.getBoolean("isCreative")){
			tooltipLines.add(InterfaceGUI.getFormattingCode("dark_purple") + InterfaceCore.translate("info.item.engine.creative"));
		}
		tooltipLines.add(InterfaceCore.translate("info.item.engine.maxrpm") + definition.engine.maxRPM);
		tooltipLines.add(InterfaceCore.translate("info.item.engine.maxsaferpm") + definition.engine.maxSafeRPM);
		tooltipLines.add(InterfaceCore.translate("info.item.engine.fuelconsumption") + definition.engine.fuelConsumption);
		if(definition.engine.jetPowerFactor > 0){
			tooltipLines.add(InterfaceCore.translate("info.item.engine.jetpowerfactor") + (int) (100*definition.engine.jetPowerFactor) + "%");
			tooltipLines.add(InterfaceCore.translate("info.item.engine.bypassratio") + definition.engine.bypassRatio);
		}
		tooltipLines.add(InterfaceCore.translate("info.item.engine.fueltype") + definition.engine.fuelType);
		tooltipLines.add(InterfaceCore.translate("info.item.engine.hours") + Math.round(data.getDouble("hours")*100D)/100D);
		
		if(definition.engine.gearRatios.size() > 3){
			tooltipLines.add(definition.engine.isAutomatic ? InterfaceCore.translate("info.item.engine.automatic") : InterfaceCore.translate("info.item.engine.manual"));
			tooltipLines.add(InterfaceCore.translate("info.item.engine.gearratios"));
			for(byte i=0; i<definition.engine.gearRatios.size() ; i+=5){
				String gearRatios = "";
				for(byte j=i; j<i+5 && j<definition.engine.gearRatios.size() ; ++j){
					gearRatios += String.valueOf(definition.engine.gearRatios.get(j)) + ",  ";
				}
				tooltipLines.add(gearRatios);
			}
			
		}else{
			tooltipLines.add(InterfaceCore.translate("info.item.engine.gearratios") + definition.engine.gearRatios.get(definition.engine.gearRatios.size() - 1));
		}
		
		if(data.getBoolean("oilLeak")){
			tooltipLines.add(InterfaceGUI.getFormattingCode("red") + InterfaceCore.translate("info.item.engine.oilleak"));
		}
		if(data.getBoolean("fuelLeak")){
			tooltipLines.add(InterfaceGUI.getFormattingCode("red") + InterfaceCore.translate("info.item.engine.fuelleak"));
		}
		if(data.getBoolean("brokenStarter")){
			tooltipLines.add(InterfaceGUI.getFormattingCode("red") + InterfaceCore.translate("info.item.engine.brokenstarter"));
		}
	}
	
	@Override
	public void getDataBlocks(List<WrapperNBT> dataBlocks){
		//Add a creative variant.
		WrapperNBT data = new WrapperNBT();
		data.setBoolean("isCreative", true);
		dataBlocks.add(data);
	}
	
	public static final AItemPartCreator CREATOR = new AItemPartCreator(){
		@Override
		public boolean isCreatorValid(JSONPart definition){
			return definition.generic.type.startsWith("engine");
		}
		@Override
		public ItemPartEngine createItem(JSONPart definition, String subName, String sourcePackID){
			return new ItemPartEngine(definition, subName, sourcePackID);
		}
	};
}
