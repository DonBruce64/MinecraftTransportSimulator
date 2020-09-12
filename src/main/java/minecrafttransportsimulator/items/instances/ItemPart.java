package minecrafttransportsimulator.items.instances;

import java.util.List;

import mcinterface.BuilderGUI;
import mcinterface.WrapperNBT;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.vehicles.parts.PartEngine;
import net.minecraft.util.text.TextFormatting;

public class ItemPart extends AItemPack<JSONPart>{
	private final String partPrefix;
	
	public ItemPart(JSONPart definition){
		super(definition);
		if(definition.general.type.indexOf("_") != -1){
			this.partPrefix = definition.general.type.substring(0, definition.general.type.indexOf("_"));
		}else{
			this.partPrefix = definition.general.type;
		}
	}
	
	public boolean isPartValidForPackDef(VehiclePart packVehicleDef){
		//Check if our custom type matches, or if we aren't a custom type and the definition doesn't care.
		boolean customTypesValid;
		if(packVehicleDef.customTypes == null){
			customTypesValid = definition.general.customType == null;
		}else if(definition.general.customType == null){
			customTypesValid = packVehicleDef.customTypes == null || packVehicleDef.customTypes.contains("");
		}else{
			customTypesValid = packVehicleDef.customTypes.contains(definition.general.customType);
		}
		
		//Do extra part checks for specific part types, or just return the custom def.
		switch(partPrefix){
			case("custom") : return packVehicleDef.customTypes != null && packVehicleDef.customTypes.contains(definition.general.customType);
			case("bullet") : return customTypesValid && packVehicleDef.minValue <= definition.bullet.diameter && packVehicleDef.maxValue >= definition.bullet.diameter;
			case("engine") : return customTypesValid && packVehicleDef.minValue <= definition.engine.fuelConsumption && packVehicleDef.maxValue >= definition.engine.fuelConsumption;
			case("ground") : return customTypesValid && packVehicleDef.minValue <= definition.ground.height && packVehicleDef.maxValue >= definition.ground.height;
			case("gun") : return customTypesValid && packVehicleDef.minValue <= definition.gun.diameter && packVehicleDef.maxValue >= definition.gun.diameter;
			case("interactable") : return customTypesValid && packVehicleDef.minValue <= definition.interactable.inventoryUnits && packVehicleDef.maxValue >= definition.interactable.inventoryUnits;
			case("propeller") : return customTypesValid && packVehicleDef.minValue <= definition.propeller.diameter && packVehicleDef.maxValue >= definition.propeller.diameter;
			default : return customTypesValid;
		}
		
	}
	
	@Override
	public void addTooltipLines(List<String> tooltipLines, WrapperNBT data){
		super.addTooltipLines(tooltipLines, data);
		switch(partPrefix){
			case("bullet") : {
				tooltipLines.add(BuilderGUI.translate("info.item.bullet.type." + definition.bullet.type));
				tooltipLines.add(BuilderGUI.translate("info.item.bullet.diameter") + definition.bullet.diameter);
				tooltipLines.add(BuilderGUI.translate("info.item.bullet.quantity") + definition.bullet.quantity);
				break;
			}
			case("engine") : {
				if(data.getBoolean("isCreative")){
					//TODO make this part of the GUI builder for abstraction.
					tooltipLines.add(TextFormatting.DARK_PURPLE + BuilderGUI.translate("info.item.engine.creative"));
				}
				tooltipLines.add(BuilderGUI.translate("info.item.engine.maxrpm") + definition.engine.maxRPM);
				tooltipLines.add(BuilderGUI.translate("info.item.engine.maxsaferpm") + PartEngine.getSafeRPMFromMax(definition.engine.maxRPM));
				tooltipLines.add(BuilderGUI.translate("info.item.engine.fuelconsumption") + definition.engine.fuelConsumption);
				if(definition.engine.jetPowerFactor > 0){
					tooltipLines.add(BuilderGUI.translate("info.item.engine.jetpowerfactor") + (int) (100*definition.engine.jetPowerFactor) + "%");
					tooltipLines.add(BuilderGUI.translate("info.item.engine.bypassratio") + definition.engine.bypassRatio);
				}
				tooltipLines.add(BuilderGUI.translate("info.item.engine.fueltype") + definition.engine.fuelType);
				tooltipLines.add(BuilderGUI.translate("info.item.engine.hours") + Math.round(data.getDouble("hours")*100D)/100D);
				
				if(definition.engine.gearRatios.length > 3){
					tooltipLines.add(definition.engine.isAutomatic ? BuilderGUI.translate("info.item.engine.automatic") : BuilderGUI.translate("info.item.engine.manual"));
					tooltipLines.add(BuilderGUI.translate("info.item.engine.gearratios"));
					for(byte i=0; i<definition.engine.gearRatios.length; i+=5){
						String gearRatios = "";
						for(byte j=i; j<i+5 && j<definition.engine.gearRatios.length; ++j){
							gearRatios += String.valueOf(definition.engine.gearRatios[j]) + ",  ";
						}
						tooltipLines.add(gearRatios);
					}
					
				}else{
					tooltipLines.add(BuilderGUI.translate("info.item.engine.gearratios") + definition.engine.gearRatios[definition.engine.gearRatios.length - 1]);
				}
				
				if(data.getBoolean("oilLeak")){
					tooltipLines.add(TextFormatting.RED + BuilderGUI.translate("info.item.engine.oilleak"));
				}
				if(data.getBoolean("fuelLeak")){
					tooltipLines.add(TextFormatting.RED + BuilderGUI.translate("info.item.engine.fuelleak"));
				}
				if(data.getBoolean("brokenStarter")){
					tooltipLines.add(TextFormatting.RED + BuilderGUI.translate("info.item.engine.brokenstarter"));
				}
				break;
			}
			case("ground") : {
				tooltipLines.add(BuilderGUI.translate("info.item.ground_device.diameter") + definition.ground.height);
				tooltipLines.add(BuilderGUI.translate("info.item.ground_device.motivefriction") + definition.ground.motiveFriction);
				tooltipLines.add(BuilderGUI.translate("info.item.ground_device.lateralfriction") + definition.ground.lateralFriction);
				tooltipLines.add(BuilderGUI.translate(definition.ground.isWheel ? "info.item.ground_device.rotatesonshaft_true" : "info.item.ground_device.rotatesonshaft_false"));
				tooltipLines.add(BuilderGUI.translate(definition.ground.canFloat ? "info.item.ground_device.canfloat_true" : "info.item.ground_device.canfloat_false"));
				break;
			}
			case("gun") : {
				tooltipLines.add(BuilderGUI.translate("info.item.gun.type." + definition.general.type.substring("gun_".length())));
				tooltipLines.add(BuilderGUI.translate("info.item.gun.diameter") + definition.gun.diameter);
				tooltipLines.add(BuilderGUI.translate("info.item.gun.length") + definition.gun.length);
				tooltipLines.add(BuilderGUI.translate("info.item.gun.fireDelay") + definition.gun.fireDelay);
				tooltipLines.add(BuilderGUI.translate("info.item.gun.muzzleVelocity") + definition.gun.muzzleVelocity);
				tooltipLines.add(BuilderGUI.translate("info.item.gun.capacity") + definition.gun.capacity);
				if(definition.gun.autoReload){
					tooltipLines.add(BuilderGUI.translate("info.item.gun.autoReload"));
				}
				tooltipLines.add(BuilderGUI.translate("info.item.gun.yawRange") + definition.gun.minYaw + "-" + definition.gun.maxYaw);
				tooltipLines.add(BuilderGUI.translate("info.item.gun.pitchRange") + definition.gun.minPitch + "-" + definition.gun.maxPitch);
				break;
			}
			case("interactable") : {
				if(definition.interactable.interactionType.equals("crate")){
					tooltipLines.add(BuilderGUI.translate("info.item.interactable.capacity") + definition.interactable.inventoryUnits*9);
				}else if(definition.interactable.interactionType.equals("barrel")){
					tooltipLines.add(BuilderGUI.translate("info.item.interactable.capacity") + definition.interactable.inventoryUnits*10000 + "mb");
				}
				break;
			}
			case("propeller") : {
				tooltipLines.add(BuilderGUI.translate(definition.propeller.isDynamicPitch ? "info.item.propeller.dynamicPitch" : "info.item.propeller.staticPitch"));
				tooltipLines.add(BuilderGUI.translate("info.item.propeller.pitch") + definition.propeller.pitch);
				tooltipLines.add(BuilderGUI.translate("info.item.propeller.diameter") + definition.propeller.diameter);
				tooltipLines.add(BuilderGUI.translate("info.item.propeller.maxrpm") + Math.round(60*340.29/(0.0254*Math.PI*definition.propeller.diameter)));
				tooltipLines.add(BuilderGUI.translate("info.item.propeller.health") + (definition.propeller.startingHealth - data.getDouble("damage")));
			}
		}
	}
	
	@Override
	public String getModelLocation(){
		return definition.general.modelName != null ? "objmodels/parts/" + definition.general.modelName + ".obj" : "objmodels/parts/" + definition.systemName + ".obj";
	}
	
	@Override
	public String getTextureLocation(){
		return "textures/parts/" + definition.systemName + ".png";
	}
}
