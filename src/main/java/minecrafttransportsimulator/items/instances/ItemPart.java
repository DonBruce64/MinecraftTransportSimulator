package minecrafttransportsimulator.items.instances;

import java.util.List;

import minecrafttransportsimulator.baseclasses.FluidTank;
import minecrafttransportsimulator.guis.components.InterfaceGUI;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.items.components.IItemEntityProvider;
import minecrafttransportsimulator.items.components.IItemVehicleInteractable;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPart.InteractableComponentType;
import minecrafttransportsimulator.jsondefs.JSONPart.PartType;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
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
import net.minecraft.item.ItemStack;

public class ItemPart extends AItemSubTyped<JSONPart> implements IItemEntityProvider<EntityPlayerGun>, IItemVehicleInteractable{
	private final PartType partType;
	
	public ItemPart(JSONPart definition, String subName, String sourcePackID){
		super(definition, subName, sourcePackID);
		try{
			if(definition.general.type.indexOf("_") != -1){
				this.partType = PartType.valueOf(definition.general.type.substring(0, definition.general.type.indexOf("_")).toUpperCase());
			}else{
				this.partType = PartType.valueOf(definition.general.type.toUpperCase());
			}
		}catch(Exception e){
			throw new IllegalArgumentException(definition.general.type + " is not a valid type for creating a part.");
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
				switch(partType){
					case BULLET : return packVehicleDef.minValue <= definition.bullet.diameter && packVehicleDef.maxValue >= definition.bullet.diameter;
					case EFFECTOR: return true;
					case ENGINE : return packVehicleDef.minValue <= definition.engine.fuelConsumption && packVehicleDef.maxValue >= definition.engine.fuelConsumption;
					case GENERIC : return ((packVehicleDef.minValue <= definition.generic.height && packVehicleDef.maxValue >= definition.generic.height) || (packVehicleDef.minValue == 0 && packVehicleDef.maxValue == 0));
					case GROUND : return packVehicleDef.minValue <= definition.ground.height && packVehicleDef.maxValue >= definition.ground.height;
					case GUN : return packVehicleDef.minValue <= definition.gun.diameter && packVehicleDef.maxValue >= definition.gun.diameter;
					case INTERACTABLE : return packVehicleDef.minValue <= definition.interactable.inventoryUnits && packVehicleDef.maxValue >= definition.interactable.inventoryUnits;
					case PROPELLER : return packVehicleDef.minValue <= definition.propeller.diameter && packVehicleDef.maxValue >= definition.propeller.diameter;
					case SEAT: return true;
				}
			}
		}
		return false;
	}
	
	public APart createPart(EntityVehicleF_Physics vehicle, VehiclePart packVehicleDef, WrapperNBT partData, APart parentPart){
		switch(partType){
			case GENERIC : return new PartGeneric(vehicle, packVehicleDef, this, partData, parentPart);
			//Note that this case is invalid, as bullets are NOT parts that can be placed on vehicles.
			//Rather, they are items that get loaded into the gun, so they never actually become parts themselves.
			case BULLET : return null;
			case EFFECTOR : return new PartEffector(vehicle, packVehicleDef, this, partData, parentPart);
			case ENGINE : return new PartEngine(vehicle, packVehicleDef, this, partData, parentPart);
			case GROUND : return new PartGroundDevice(vehicle, packVehicleDef, this, partData, parentPart);
			case GUN : return new PartGun(vehicle, packVehicleDef, this, partData, parentPart);
			case INTERACTABLE : return new PartInteractable(vehicle, packVehicleDef, this, partData, parentPart);
			case PROPELLER : return new PartPropeller(vehicle, packVehicleDef, this, partData, parentPart);
			case SEAT : return new PartSeat(vehicle, packVehicleDef, this, partData, parentPart);
		}
		//We'll never get here, but it makes the complier happy.
		return null;
    }
	
	@Override
	public void addTooltipLines(List<String> tooltipLines, WrapperNBT data){
		super.addTooltipLines(tooltipLines, data);
		switch(partType){
			case BULLET : {
				for(String type : definition.bullet.types) {
  					tooltipLines.add(InterfaceCore.translate("info.item.bullet.type." + type));
				}
				tooltipLines.add(InterfaceCore.translate("info.item.bullet.diameter") + definition.bullet.diameter);
				tooltipLines.add(InterfaceCore.translate("info.item.bullet.quantity") + definition.bullet.quantity);
				break;
			}
			case EFFECTOR : break; //No tooltip for effectors.
			case ENGINE : {
				if(data.getBoolean("isCreative")){
					tooltipLines.add(InterfaceGUI.getFormattingCode("dark_purple") + InterfaceCore.translate("info.item.engine.creative"));
				}
				tooltipLines.add(InterfaceCore.translate("info.item.engine.maxrpm") + definition.engine.maxRPM);
				tooltipLines.add(InterfaceCore.translate("info.item.engine.maxsaferpm") + PartEngine.getSafeRPMFromMax(definition.engine.maxRPM));
				tooltipLines.add(InterfaceCore.translate("info.item.engine.fuelconsumption") + definition.engine.fuelConsumption);
				if(definition.engine.jetPowerFactor > 0){
					tooltipLines.add(InterfaceCore.translate("info.item.engine.jetpowerfactor") + (int) (100*definition.engine.jetPowerFactor) + "%");
					tooltipLines.add(InterfaceCore.translate("info.item.engine.bypassratio") + definition.engine.bypassRatio);
				}
				tooltipLines.add(InterfaceCore.translate("info.item.engine.fueltype") + definition.engine.fuelType);
				tooltipLines.add(InterfaceCore.translate("info.item.engine.hours") + Math.round(data.getDouble("hours")*100D)/100D);
				
				if(definition.engine.gearRatios.length > 3){
					tooltipLines.add(definition.engine.isAutomatic ? InterfaceCore.translate("info.item.engine.automatic") : InterfaceCore.translate("info.item.engine.manual"));
					tooltipLines.add(InterfaceCore.translate("info.item.engine.gearratios"));
					for(byte i=0; i<definition.engine.gearRatios.length; i+=5){
						String gearRatios = "";
						for(byte j=i; j<i+5 && j<definition.engine.gearRatios.length; ++j){
							gearRatios += String.valueOf(definition.engine.gearRatios[j]) + ",  ";
						}
						tooltipLines.add(gearRatios);
					}
					
				}else{
					tooltipLines.add(InterfaceCore.translate("info.item.engine.gearratios") + definition.engine.gearRatios[definition.engine.gearRatios.length - 1]);
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
				break;
			}
			case GENERIC : break; //No tooltips for generics.
			case GROUND : {
				tooltipLines.add(InterfaceCore.translate("info.item.ground_device.diameter") + definition.ground.height);
				tooltipLines.add(InterfaceCore.translate("info.item.ground_device.motivefriction") + definition.ground.motiveFriction);
				tooltipLines.add(InterfaceCore.translate("info.item.ground_device.lateralfriction") + definition.ground.lateralFriction);
				tooltipLines.add(InterfaceCore.translate(definition.ground.isWheel ? "info.item.ground_device.rotatesonshaft_true" : "info.item.ground_device.rotatesonshaft_false"));
				tooltipLines.add(InterfaceCore.translate(definition.ground.canFloat ? "info.item.ground_device.canfloat_true" : "info.item.ground_device.canfloat_false"));
				break;
			}
			case GUN : {
				tooltipLines.add(InterfaceCore.translate("info.item.gun.type." + definition.general.type.substring("gun_".length())));
				tooltipLines.add(InterfaceCore.translate("info.item.gun.diameter") + definition.gun.diameter);
				tooltipLines.add(InterfaceCore.translate("info.item.gun.length") + definition.gun.length);
				tooltipLines.add(InterfaceCore.translate("info.item.gun.fireDelay") + definition.gun.fireDelay);
				tooltipLines.add(InterfaceCore.translate("info.item.gun.muzzleVelocity") + definition.gun.muzzleVelocity);
				tooltipLines.add(InterfaceCore.translate("info.item.gun.capacity") + definition.gun.capacity);
				if(definition.gun.autoReload){
					tooltipLines.add(InterfaceCore.translate("info.item.gun.autoReload"));
				}
				tooltipLines.add(InterfaceCore.translate("info.item.gun.yawRange") + definition.gun.minYaw + "-" + definition.gun.maxYaw);
				tooltipLines.add(InterfaceCore.translate("info.item.gun.pitchRange") + definition.gun.minPitch + "-" + definition.gun.maxPitch);
				break;
			}
			case INTERACTABLE : {
				switch(definition.interactable.interactionType){
					case CRATE : {
						tooltipLines.add(InterfaceCore.translate("info.item.interactable.capacity") + definition.interactable.inventoryUnits*9);
						break;
					}
					case BARREL : {
						tooltipLines.add(InterfaceCore.translate("info.item.interactable.capacity") + definition.interactable.inventoryUnits*10000 + "mb");
						break;
					}
					case JERRYCAN : {
						tooltipLines.add(InterfaceCore.translate("info.item.jerrycan.fill"));
						tooltipLines.add(InterfaceCore.translate("info.item.jerrycan.drain"));
						String jerrycanFluid = data.getString("jerrycanFluid");
						if(jerrycanFluid.isEmpty()){
							tooltipLines.add(InterfaceCore.translate("info.item.jerrycan.empty"));
						}else{
							tooltipLines.add(InterfaceCore.translate("info.item.jerrycan.contains") + InterfaceCore.getFluidName(jerrycanFluid));
						}
						break;
					}
					default : {
						//Don't add tooltips for other things.
					}
				}
				break;
			}
			case PROPELLER : {
				tooltipLines.add(InterfaceCore.translate(definition.propeller.isDynamicPitch ? "info.item.propeller.dynamicPitch" : "info.item.propeller.staticPitch"));
				tooltipLines.add(InterfaceCore.translate("info.item.propeller.pitch") + definition.propeller.pitch);
				tooltipLines.add(InterfaceCore.translate("info.item.propeller.diameter") + definition.propeller.diameter);
				tooltipLines.add(InterfaceCore.translate("info.item.propeller.maxrpm") + Math.round(60*340.29/(0.0254*Math.PI*definition.propeller.diameter)));
				tooltipLines.add(InterfaceCore.translate("info.item.propeller.health") + (definition.propeller.startingHealth - data.getDouble("damage")));
				 break;
			}
			case SEAT : break;//No tooltips for seats.
		}
	}
	
	@Override
	public void getDataBlocks(List<WrapperNBT> dataBlocks){
		//If this is an engine, add a creative variant.
		if(partType.equals(PartType.ENGINE)){
			WrapperNBT data = new WrapperNBT();
			data.setBoolean("isCreative", true);
			dataBlocks.add(data);
		}
	}
	
	@Override
	public EntityPlayerGun createEntity(WrapperWorld world, WrapperEntity wrapper, WrapperPlayer playerSpawning, WrapperNBT data){
		return new EntityPlayerGun(world, wrapper, playerSpawning, data);
	}

	@Override
	public Class<EntityPlayerGun> getEntityClass(){
		return EntityPlayerGun.class;
	}
	
	@Override
	public CallbackType doVehicleInteraction(EntityVehicleF_Physics vehicle, APart part, WrapperPlayer player, PlayerOwnerState ownerState, boolean rightClick){
		if(partType.equals(PartType.INTERACTABLE) && definition.interactable.interactionType.equals(InteractableComponentType.JERRYCAN)){
			if(!vehicle.world.isClient()){
				if(rightClick){
					ItemStack stack = player.getHeldStack();
					WrapperNBT data = new WrapperNBT(stack);
					String jerrrycanFluid = data.getString("jerrycanFluid");
					
					//If we clicked a tank on the vehicle, attempt to pull from it rather than fill the vehicle.
					if(part instanceof PartInteractable){
						FluidTank tank = ((PartInteractable) part).tank;
						if(tank != null){
							if(jerrrycanFluid.isEmpty()){
								if(tank.getFluidLevel() >= 1000){
									data.setString("jerrycanFluid", tank.getFluid());
									stack.setTagCompound(data.tag);
									tank.drain(tank.getFluid(), 1000, true);
								}
							}
						}
					}else if(!jerrrycanFluid.isEmpty()){
						if(vehicle.fuelTank.getFluid().isEmpty() || vehicle.fuelTank.getFluid().equals(jerrrycanFluid)){
							if(vehicle.fuelTank.getFluidLevel() + 1000 > vehicle.fuelTank.getMaxLevel()){
								player.sendPacket(new PacketPlayerChatMessage("interact.jerrycan.toofull"));
							}else{
								vehicle.fuelTank.fill(jerrrycanFluid, 1000, true);
								data.setString("jerrycanFluid", "");
								stack.setTagCompound(data.tag);
								player.sendPacket(new PacketPlayerChatMessage("interact.jerrycan.success"));
							}
						}else{
							player.sendPacket(new PacketPlayerChatMessage("interact.jerrycan.wrongtype"));
						}
					}else{
						player.sendPacket(new PacketPlayerChatMessage("interact.jerrycan.empty"));
					}
				}
			}
			return CallbackType.NONE;
		}
		return CallbackType.SKIP;
	}
	
	public boolean isHandHeldGun(){
		return definition.gun != null && definition.gun.handHeld;
	}
}
