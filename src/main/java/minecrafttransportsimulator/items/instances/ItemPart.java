package minecrafttransportsimulator.items.instances;

import java.util.List;

import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.items.components.IItemEntityProvider;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.vehicles.main.EntityPlayerGun;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartEffector;
import minecrafttransportsimulator.vehicles.parts.PartEngine;
import minecrafttransportsimulator.vehicles.parts.PartGeneric;
import minecrafttransportsimulator.vehicles.parts.PartGroundDevice;
import minecrafttransportsimulator.vehicles.parts.PartGun;
import minecrafttransportsimulator.vehicles.parts.PartInteractable;
import minecrafttransportsimulator.vehicles.parts.PartPropeller;
import minecrafttransportsimulator.vehicles.parts.PartSeat;

public class ItemPart extends AItemSubTyped<JSONPart> implements IItemEntityProvider<EntityPlayerGun>{
	private final String partPrefix;
	
	public ItemPart(JSONPart definition, String subName){
		super(definition, subName);
		if(definition.general.type.indexOf("_") != -1){
			this.partPrefix = definition.general.type.substring(0, definition.general.type.indexOf("_"));
		}else{
			this.partPrefix = definition.general.type;
		}
	}
	
	public boolean isPartValidForPackDef(VehiclePart packVehicleDef){
		//First make sure we are the right type.
		if(packVehicleDef.types.contains(definition.general.type)){
			//Check if our custom type matches, or if we aren't a custom type and the definition doesn't care.
			boolean customTypesValid;
			if(packVehicleDef.customTypes == null){
				customTypesValid = definition.general.customType == null;
			}else if(definition.general.customType == null){
				customTypesValid = packVehicleDef.customTypes == null || packVehicleDef.customTypes.contains("");
			}else{
				customTypesValid = packVehicleDef.customTypes.contains(definition.general.customType);
			}
			
			if(customTypesValid){
				//Do extra part checks for specific part types, or just return the custom def.
				switch(partPrefix){
					case("bullet") : return packVehicleDef.minValue <= definition.bullet.diameter && packVehicleDef.maxValue >= definition.bullet.diameter;
					case("engine") : return packVehicleDef.minValue <= definition.engine.fuelConsumption && packVehicleDef.maxValue >= definition.engine.fuelConsumption;
					case("generic") : return ((packVehicleDef.minValue <= definition.generic.height && packVehicleDef.maxValue >= definition.generic.height) || (packVehicleDef.minValue == 0 && packVehicleDef.maxValue == 0));
					case("ground") : return packVehicleDef.minValue <= definition.ground.height && packVehicleDef.maxValue >= definition.ground.height;
					case("gun") : return packVehicleDef.minValue <= definition.gun.diameter && packVehicleDef.maxValue >= definition.gun.diameter;
					case("interactable") : return packVehicleDef.minValue <= definition.interactable.inventoryUnits && packVehicleDef.maxValue >= definition.interactable.inventoryUnits;
					case("propeller") : return packVehicleDef.minValue <= definition.propeller.diameter && packVehicleDef.maxValue >= definition.propeller.diameter;
					default : return true;
				}
			}
		}
		return false;
	}
	
	public APart createPart(EntityVehicleF_Physics vehicle, VehiclePart packVehicleDef, IWrapperNBT partData, APart parentPart){
		switch(partPrefix){
			case("generic") : return new PartGeneric(vehicle, packVehicleDef, this, partData, parentPart);
			//Note that this case is invalid, as bullets are NOT parts that can be placed on vehicles.
			//Rather, they are items that get loaded into the gun, so they never actually become parts themselves.
			//case("bullet") : return new PartBullet(vehicle, packVehicleDef, definition, partData, parentPart);
			case("effector") : return new PartEffector(vehicle, packVehicleDef, this, partData, parentPart);
			case("engine") : return new PartEngine(vehicle, packVehicleDef, this, partData, parentPart);
			case("ground") : return new PartGroundDevice(vehicle, packVehicleDef, this, partData, parentPart);
			case("gun") : return new PartGun(vehicle, packVehicleDef, this, partData, parentPart);
			case("interactable") : return new PartInteractable(vehicle, packVehicleDef, this, partData, parentPart);
			case("propeller") : return new PartPropeller(vehicle, packVehicleDef, this, partData, parentPart);
			case("seat") : return new PartSeat(vehicle, packVehicleDef, this, partData, parentPart);
			default : throw new IllegalArgumentException(definition.general.type + " is not a valid type for creating a part.");
		}
    }
	
	@Override
	public void addTooltipLines(List<String> tooltipLines, IWrapperNBT data){
		super.addTooltipLines(tooltipLines, data);
		switch(partPrefix){
			case("bullet") : {
				for(String type : definition.bullet.types) {
  					tooltipLines.add(MasterLoader.coreInterface.translate("info.item.bullet.type." + type));
				}
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
	
	@Override
	public boolean onUsed(IWrapperWorld world, IWrapperPlayer player){
		if(isHandHeldGun()){
			if(!world.isClient()){
				EntityPlayerGun.playerServerGuns.get(player.getUUID()).fireCommand = true;
			}
			return true;
		}else{
			return false;
		}
    }
	
	@Override
	public void onStoppedUsing(IWrapperWorld world, IWrapperPlayer player){
		if(isHandHeldGun() && !world.isClient()){
			EntityPlayerGun.playerServerGuns.get(player.getUUID()).fireCommand = false;
		}
	}
	
	@Override
	public EntityPlayerGun createEntity(IWrapperWorld world, IWrapperEntity wrapper, IWrapperPlayer playerSpawning, IWrapperNBT data){
		return new EntityPlayerGun(world, wrapper, playerSpawning, data);
	}

	@Override
	public Class<EntityPlayerGun> getEntityClass(){
		return EntityPlayerGun.class;
	}
	
	public boolean isHandHeldGun(){
		return definition.gun != null && definition.gun.handHeld;
	}
}
