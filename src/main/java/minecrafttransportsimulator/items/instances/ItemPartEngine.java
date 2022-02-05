package minecrafttransportsimulator.items.instances;

import java.util.List;
import java.util.Map.Entry;

import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.systems.ConfigSystem;

public class ItemPartEngine extends AItemPart{
	
	public ItemPartEngine(JSONPart definition, String subName, String sourcePackID){
		super(definition, subName, sourcePackID);
	}

	@Override
	public boolean isPartValidForPackDef(JSONPartDefinition placementDefinition, String subNameToPlaceOn, boolean checkMinMax){
		return super.isPartValidForPackDef(placementDefinition, subNameToPlaceOn, checkMinMax) && (!checkMinMax || (placementDefinition.minValue <= definition.engine.fuelConsumption && placementDefinition.maxValue >= definition.engine.fuelConsumption));
	}
	
	@Override
	public PartEngine createPart(AEntityF_Multipart<?> entity, WrapperPlayer placingPlayer, JSONPartDefinition packVehicleDef, WrapperNBT partData, APart parentPart){
		return new PartEngine(entity, placingPlayer, packVehicleDef, partData, parentPart);
	}
	
	@Override
	public void addTooltipLines(List<String> tooltipLines, WrapperNBT data){
		super.addTooltipLines(tooltipLines, data);
		if(data.getBoolean("isCreative")){
			tooltipLines.add(InterfaceCore.translate("info.item.engine.creative"));
		}
		tooltipLines.add(InterfaceCore.translate("info.item.engine.maxrpm") + definition.engine.maxRPM);
		tooltipLines.add(InterfaceCore.translate("info.item.engine.maxsaferpm") + definition.engine.maxSafeRPM);
		tooltipLines.add(InterfaceCore.translate("info.item.engine.fuelconsumption") + definition.engine.fuelConsumption);
		if(definition.engine.jetPowerFactor > 0){
			tooltipLines.add(InterfaceCore.translate("info.item.engine.jetpowerfactor") + (int) (100*definition.engine.jetPowerFactor) + "%");
			tooltipLines.add(InterfaceCore.translate("info.item.engine.bypassratio") + definition.engine.bypassRatio);
		}
		tooltipLines.add(InterfaceCore.translate("info.item.engine.fueltype") + definition.engine.fuelType);
		if(ConfigSystem.configObject.fuel.fuels.containsKey(definition.engine.fuelType)){
			String line = InterfaceCore.translate("info.item.engine.fluids");
			for(Entry<String, Double> fuelEntry : ConfigSystem.configObject.fuel.fuels.get(definition.engine.fuelType).entrySet()){
				String fluidName = InterfaceCore.getFluidName(fuelEntry.getKey());
				if(!fluidName.equals("INVALID")){
					line += InterfaceCore.getFluidName(fuelEntry.getKey()) + "@" + fuelEntry.getValue() + ", ";
				}
			}
			tooltipLines.add(line.substring(0, line.length() - 2));
		}
		tooltipLines.add(InterfaceCore.translate("info.item.engine.hours") + Math.round(data.getDouble("hours")*100D)/100D);
		
		if(definition.engine.gearRatios.size() > 3){
			tooltipLines.add(definition.engine.isAutomatic ? InterfaceCore.translate("info.item.engine.automatic") : InterfaceCore.translate("info.item.engine.manual"));
			tooltipLines.add(InterfaceCore.translate("info.item.engine.gearratios"));
			for(byte i=0; i<definition.engine.gearRatios.size() ; i+=5){
				String gearRatios = "";
				for(byte j=i; j<i+5 && j<definition.engine.gearRatios.size() ; ++j){
					gearRatios += String.valueOf(definition.engine.gearRatios.get(j));
					if(j < definition.engine.gearRatios.size() - 1){
						gearRatios += ",  ";
					}
				}
				tooltipLines.add(gearRatios);
			}
			
		}else{
			tooltipLines.add(InterfaceCore.translate("info.item.engine.gearratios") + definition.engine.gearRatios.get(definition.engine.gearRatios.size() - 1));
		}
		
		if(data.getBoolean("oilLeak")){
			tooltipLines.add(InterfaceCore.translate("info.item.engine.oilleak"));
		}
		if(data.getBoolean("fuelLeak")){
			tooltipLines.add(InterfaceCore.translate("info.item.engine.fuelleak"));
		}
		if(data.getBoolean("brokenStarter")){
			tooltipLines.add(InterfaceCore.translate("info.item.engine.brokenstarter"));
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
