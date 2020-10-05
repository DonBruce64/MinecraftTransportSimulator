package minecrafttransportsimulator.items.instances;

import java.util.List;

import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.packloading.PackResourceLoader.ItemClassification;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartCustom;
import minecrafttransportsimulator.vehicles.parts.PartEngine;
import minecrafttransportsimulator.vehicles.parts.PartGroundDevice;
import minecrafttransportsimulator.vehicles.parts.PartGroundEffector;
import minecrafttransportsimulator.vehicles.parts.PartGun;
import minecrafttransportsimulator.vehicles.parts.PartInteractable;
import minecrafttransportsimulator.vehicles.parts.PartPropeller;
import minecrafttransportsimulator.vehicles.parts.PartSeat;

public class ItemPart extends AItemSubTyped<JSONPart>{
	private final String partPrefix;
	
	public ItemPart(JSONPart definition, String subName){
		super(definition, ItemClassification.PART, subName);
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
	
	public APart createPart(EntityVehicleF_Physics vehicle, VehiclePart packVehicleDef, IWrapperNBT partData, APart parentPart){
    	if(definition.general.type.startsWith("engine_")){
    		return new PartEngine(vehicle, packVehicleDef, this, partData, parentPart);
    	}else if(definition.general.type.startsWith("gun_")){
    		return new PartGun(vehicle, packVehicleDef, this, partData, parentPart);
    	}else if(definition.general.type.startsWith("ground_")){
    		return new PartGroundDevice(vehicle, packVehicleDef, this, partData, parentPart);
    	}else if(definition.general.type.startsWith("interactable_")){
        	return new PartInteractable(vehicle, packVehicleDef, this, partData, parentPart);
    	}else if(definition.general.type.startsWith("effector_")){
           	return new PartGroundEffector(vehicle, packVehicleDef, this, partData, parentPart);
    	}else{
	    	switch(definition.general.type){
				case "propeller": return new PartPropeller(vehicle, packVehicleDef, this, partData, parentPart);
				case "seat": return new PartSeat(vehicle, packVehicleDef, this, partData, parentPart);
				//Note that this case is invalid, as bullets are NOT parts that can be placed on vehicles.
				//Rather, they are items that get loaded into the gun, so they never actually become parts themselves.
				//case "bullet": return new PartBullet(vehicle, packVehicleDef, definition, partData, parentPart);
				case "custom": return new PartCustom(vehicle, packVehicleDef, this, partData, parentPart);
			}
    	}
    	throw new IllegalArgumentException(definition.general.type + " is not a valid type for creating a part.");
    }
	
	@Override
	public void addTooltipLines(List<String> tooltipLines, IWrapperNBT data){
		super.addTooltipLines(tooltipLines, data);
		switch(partPrefix){
			case("bullet") : {
				tooltipLines.add(MasterLoader.coreInterface.translate("info.item.bullet.type." + definition.bullet.type));
				tooltipLines.add(MasterLoader.coreInterface.translate("info.item.bullet.diameter") + definition.bullet.diameter);
				tooltipLines.add(MasterLoader.coreInterface.translate("info.item.bullet.quantity") + definition.bullet.quantity);
				break;
			}
			case("engine") : {
				if(data.getBoolean("isCreative")){
					tooltipLines.add(MasterLoader.guiInterface.getFormattingCode("dark_purple") + MasterLoader.coreInterface.translate("info.item.engine.creative"));
				}
				tooltipLines.add(MasterLoader.coreInterface.translate("info.item.engine.maxrpm") + definition.engine.maxRPM);
				tooltipLines.add(MasterLoader.coreInterface.translate("info.item.engine.maxsaferpm") + PartEngine.getSafeRPMFromMax(definition.engine.maxRPM));
				tooltipLines.add(MasterLoader.coreInterface.translate("info.item.engine.fuelconsumption") + definition.engine.fuelConsumption);
				if(definition.engine.jetPowerFactor > 0){
					tooltipLines.add(MasterLoader.coreInterface.translate("info.item.engine.jetpowerfactor") + (int) (100*definition.engine.jetPowerFactor) + "%");
					tooltipLines.add(MasterLoader.coreInterface.translate("info.item.engine.bypassratio") + definition.engine.bypassRatio);
				}
				tooltipLines.add(MasterLoader.coreInterface.translate("info.item.engine.fueltype") + definition.engine.fuelType);
				tooltipLines.add(MasterLoader.coreInterface.translate("info.item.engine.hours") + Math.round(data.getDouble("hours")*100D)/100D);
				
				if(definition.engine.gearRatios.length > 3){
					tooltipLines.add(definition.engine.isAutomatic ? MasterLoader.coreInterface.translate("info.item.engine.automatic") : MasterLoader.coreInterface.translate("info.item.engine.manual"));
					tooltipLines.add(MasterLoader.coreInterface.translate("info.item.engine.gearratios"));
					for(byte i=0; i<definition.engine.gearRatios.length; i+=5){
						String gearRatios = "";
						for(byte j=i; j<i+5 && j<definition.engine.gearRatios.length; ++j){
							gearRatios += String.valueOf(definition.engine.gearRatios[j]) + ",  ";
						}
						tooltipLines.add(gearRatios);
					}
					
				}else{
					tooltipLines.add(MasterLoader.coreInterface.translate("info.item.engine.gearratios") + definition.engine.gearRatios[definition.engine.gearRatios.length - 1]);
				}
				
				if(data.getBoolean("oilLeak")){
					tooltipLines.add(MasterLoader.guiInterface.getFormattingCode("red") + MasterLoader.coreInterface.translate("info.item.engine.oilleak"));
				}
				if(data.getBoolean("fuelLeak")){
					tooltipLines.add(MasterLoader.guiInterface.getFormattingCode("red") + MasterLoader.coreInterface.translate("info.item.engine.fuelleak"));
				}
				if(data.getBoolean("brokenStarter")){
					tooltipLines.add(MasterLoader.guiInterface.getFormattingCode("red") + MasterLoader.coreInterface.translate("info.item.engine.brokenstarter"));
				}
				break;
			}
			case("ground") : {
				tooltipLines.add(MasterLoader.coreInterface.translate("info.item.ground_device.diameter") + definition.ground.height);
				tooltipLines.add(MasterLoader.coreInterface.translate("info.item.ground_device.motivefriction") + definition.ground.motiveFriction);
				tooltipLines.add(MasterLoader.coreInterface.translate("info.item.ground_device.lateralfriction") + definition.ground.lateralFriction);
				tooltipLines.add(MasterLoader.coreInterface.translate(definition.ground.isWheel ? "info.item.ground_device.rotatesonshaft_true" : "info.item.ground_device.rotatesonshaft_false"));
				tooltipLines.add(MasterLoader.coreInterface.translate(definition.ground.canFloat ? "info.item.ground_device.canfloat_true" : "info.item.ground_device.canfloat_false"));
				break;
			}
			case("gun") : {
				tooltipLines.add(MasterLoader.coreInterface.translate("info.item.gun.type." + definition.general.type.substring("gun_".length())));
				tooltipLines.add(MasterLoader.coreInterface.translate("info.item.gun.diameter") + definition.gun.diameter);
				tooltipLines.add(MasterLoader.coreInterface.translate("info.item.gun.length") + definition.gun.length);
				tooltipLines.add(MasterLoader.coreInterface.translate("info.item.gun.fireDelay") + definition.gun.fireDelay);
				tooltipLines.add(MasterLoader.coreInterface.translate("info.item.gun.muzzleVelocity") + definition.gun.muzzleVelocity);
				tooltipLines.add(MasterLoader.coreInterface.translate("info.item.gun.capacity") + definition.gun.capacity);
				if(definition.gun.autoReload){
					tooltipLines.add(MasterLoader.coreInterface.translate("info.item.gun.autoReload"));
				}
				tooltipLines.add(MasterLoader.coreInterface.translate("info.item.gun.yawRange") + definition.gun.minYaw + "-" + definition.gun.maxYaw);
				tooltipLines.add(MasterLoader.coreInterface.translate("info.item.gun.pitchRange") + definition.gun.minPitch + "-" + definition.gun.maxPitch);
				break;
			}
			case("interactable") : {
				if(definition.interactable.interactionType.equals("crate")){
					tooltipLines.add(MasterLoader.coreInterface.translate("info.item.interactable.capacity") + definition.interactable.inventoryUnits*9);
				}else if(definition.interactable.interactionType.equals("barrel")){
					tooltipLines.add(MasterLoader.coreInterface.translate("info.item.interactable.capacity") + definition.interactable.inventoryUnits*10000 + "mb");
				}
				break;
			}
			case("propeller") : {
				tooltipLines.add(MasterLoader.coreInterface.translate(definition.propeller.isDynamicPitch ? "info.item.propeller.dynamicPitch" : "info.item.propeller.staticPitch"));
				tooltipLines.add(MasterLoader.coreInterface.translate("info.item.propeller.pitch") + definition.propeller.pitch);
				tooltipLines.add(MasterLoader.coreInterface.translate("info.item.propeller.diameter") + definition.propeller.diameter);
				tooltipLines.add(MasterLoader.coreInterface.translate("info.item.propeller.maxrpm") + Math.round(60*340.29/(0.0254*Math.PI*definition.propeller.diameter)));
				tooltipLines.add(MasterLoader.coreInterface.translate("info.item.propeller.health") + (definition.propeller.startingHealth - data.getDouble("damage")));
			}
		}
	}
	
	@Override
	public void getDataBlocks(List<IWrapperNBT> dataBlocks){
		//If this is an engine, add a creative variant.
		if(partPrefix.equals("engine")){
			IWrapperNBT data = MasterLoader.coreInterface.createNewTag();
			data.setBoolean("isCreative", true);
			dataBlocks.add(data);
		}
	}
}
