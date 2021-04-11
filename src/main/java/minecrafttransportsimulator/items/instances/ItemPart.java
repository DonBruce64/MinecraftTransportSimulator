package minecrafttransportsimulator.items.instances;

import java.util.List;

import minecrafttransportsimulator.baseclasses.FluidTank;
import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEffector;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.entities.instances.PartGeneric;
import minecrafttransportsimulator.entities.instances.PartGroundDevice;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.entities.instances.PartInteractable;
import minecrafttransportsimulator.entities.instances.PartPropeller;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.guis.components.InterfaceGUI;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.items.components.IItemEntityProvider;
import minecrafttransportsimulator.items.components.IItemVehicleInteractable;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPart.InteractableComponentType;
import minecrafttransportsimulator.jsondefs.JSONPart.PartType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import net.minecraft.item.ItemStack;

public class ItemPart extends AItemSubTyped<JSONPart> implements IItemEntityProvider<EntityPlayerGun>, IItemVehicleInteractable{
	private final PartType partType;
	
	public ItemPart(JSONPart definition, String subName, String sourcePackID){
		super(definition, subName, sourcePackID);
		try{
			if(definition.generic.type.indexOf("_") != -1){
				this.partType = PartType.valueOf(definition.generic.type.substring(0, definition.generic.type.indexOf("_")).toUpperCase());
			}else{
				this.partType = PartType.valueOf(definition.generic.type.toUpperCase());
			}
		}catch(Exception e){
			throw new IllegalArgumentException(definition.generic.type + " is not a valid type for creating a part.");
		}
	}
	
	public boolean isPartValidForPackDef(JSONPartDefinition placementDefinition, String subNameToPlaceOn, boolean checkMinMax){
		//First make sure we are the right type.
		if(placementDefinition.types.contains(definition.generic.type) && (placementDefinition.validSubNames == null || placementDefinition.validSubNames.contains(subNameToPlaceOn))){
			//Check if our custom type matches, or if we aren't a custom type and the definition doesn't care.
			boolean customTypesValid;
			if(placementDefinition.customTypes == null){
				customTypesValid = definition.generic.customType == null;
			}else if(definition.generic.customType == null){
				customTypesValid = placementDefinition.customTypes == null || placementDefinition.customTypes.contains("");
			}else{
				customTypesValid = placementDefinition.customTypes.contains(definition.generic.customType);
			}
			
			if(customTypesValid){
				if(checkMinMax){
					//Do extra part checks for specific part types, or just return the custom def.
					switch(partType){
						case BULLET : return placementDefinition.minValue <= definition.bullet.diameter && placementDefinition.maxValue >= definition.bullet.diameter;
						case EFFECTOR: return true;
						case ENGINE : return placementDefinition.minValue <= definition.engine.fuelConsumption && placementDefinition.maxValue >= definition.engine.fuelConsumption;
						case GENERIC : return ((placementDefinition.minValue <= definition.generic.height && placementDefinition.maxValue >= definition.generic.height) || (placementDefinition.minValue == 0 && placementDefinition.maxValue == 0));
						case GROUND : return placementDefinition.minValue <= definition.ground.height && placementDefinition.maxValue >= definition.ground.height;
						case GUN : return placementDefinition.minValue <= definition.gun.diameter && placementDefinition.maxValue >= definition.gun.diameter;
						case INTERACTABLE : return placementDefinition.minValue <= definition.interactable.inventoryUnits && placementDefinition.maxValue >= definition.interactable.inventoryUnits;
						case PROPELLER : return placementDefinition.minValue <= definition.propeller.diameter && placementDefinition.maxValue >= definition.propeller.diameter;
						case SEAT: return true;
					}
				}else{
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Creates a new part from the saved data.  This is used both in the construction of new parts, and loading
	 * of saved parts from data.  In both cases, the passed-in data can be whatever is present, as data-validation
	 * is performed on the data to ensure it has all the required bits.
	 */
	public APart createPart(AEntityE_Multipart<?> entity, JSONPartDefinition packVehicleDef, WrapperNBT partData, APart parentPart){
		partData = validateData(partData);
		switch(partType){
			case GENERIC : return new PartGeneric(entity, packVehicleDef, partData, parentPart);
			//Note that this case is invalid, as bullets are NOT parts that can be placed on entities.
			//Rather, they are items that get loaded into the gun, so they never actually become parts themselves.
			case BULLET : return null;
			case EFFECTOR : return new PartEffector(entity, packVehicleDef, partData, parentPart);
			case ENGINE : return new PartEngine(entity, packVehicleDef, partData, parentPart);
			case GROUND : return new PartGroundDevice(entity, packVehicleDef, partData, parentPart);
			case GUN : return new PartGun(entity, packVehicleDef, partData, parentPart);
			case INTERACTABLE : return new PartInteractable(entity, packVehicleDef, partData, parentPart);
			case PROPELLER : return new PartPropeller(entity, packVehicleDef, partData, parentPart);
			case SEAT : return new PartSeat(entity, packVehicleDef, partData, parentPart);
		}
		//We'll never get here, but it makes the complier happy.
		return null;
    }
	
	@Override
	public boolean canBreakBlocks(){
		return !isHandHeldGun();
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
				tooltipLines.add(InterfaceCore.translate("info.item.bullet.caseLength") + definition.bullet.caseLength);
				tooltipLines.add(InterfaceCore.translate("info.item.bullet.penetration") + definition.bullet.armorPenetration);
				tooltipLines.add(InterfaceCore.translate("info.item.bullet.quantity") + definition.bullet.quantity);
				break;
			}
			case EFFECTOR : break; //No tooltip for effectors.
			case ENGINE : {
				if(data.getBoolean("isCreative")){
					tooltipLines.add(InterfaceGUI.getFormattingCode("dark_purple") + InterfaceCore.translate("info.item.engine.creative"));
				}
				tooltipLines.add(InterfaceCore.translate("info.item.engine.maxrpm") + definition.engine.maxRPM);
				tooltipLines.add(InterfaceCore.translate("info.item.engine.maxsaferpm") + PartEngine.getSafeRPM(definition.engine));
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
				tooltipLines.add(InterfaceCore.translate("info.item.gun.diameter") + definition.gun.diameter);
				tooltipLines.add(InterfaceCore.translate("info.item.gun.length") + definition.gun.length);
				tooltipLines.add(InterfaceCore.translate("info.item.gun.caseRange") + definition.gun.minCaseLength + "-" + definition.gun.maxCaseLength);
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
	public EntityPlayerGun createEntity(WrapperWorld world, WrapperNBT data){
		return new EntityPlayerGun(world, null, data);
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
