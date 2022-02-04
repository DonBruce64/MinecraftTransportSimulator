package minecrafttransportsimulator.items.instances;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable.PlayerOwnerState;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.entities.instances.PartInteractable;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.items.components.IItemFood;
import minecrafttransportsimulator.items.components.IItemVehicleInteractable;
import minecrafttransportsimulator.jsondefs.JSONItem;
import minecrafttransportsimulator.jsondefs.JSONItem.ItemComponentType;
import minecrafttransportsimulator.jsondefs.JSONPotionEffect;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.InterfacePacket;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperItemStack;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.instances.PacketEntityGUIRequest;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableSet;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableToggle;
import minecrafttransportsimulator.packets.instances.PacketGUIRequest;
import minecrafttransportsimulator.packets.instances.PacketPartEngine;
import minecrafttransportsimulator.packets.instances.PacketPartInteractable;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.systems.ConfigSystem;

public class ItemItem extends AItemPack<JSONItem> implements IItemVehicleInteractable, IItemFood{
	/*Current page of this item, if it's a booklet.  Kept here locally as only one item class is constructed for each booklet definition.*/
	public int pageNumber;
	/*First engine clicked for jumper cable items.  Kept here locally as only one item class is constructed for each jumper cable definition.*/
	private static PartEngine firstEngineClicked;
	/*First part clicked for fuel hose items.  Kept here locally as only one item class is constructed for each jumper cable definition.*/
	private static PartInteractable firstPartClicked;
	
	public ItemItem(JSONItem definition){
		super(definition, null);
	}
	
	@Override
	public boolean canBreakBlocks(){
		return !definition.item.type.equals(ItemComponentType.WRENCH);
	}
	
	@Override
	public CallbackType doVehicleInteraction(EntityVehicleF_Physics vehicle, APart part, BoundingBox hitBox, WrapperPlayer player, PlayerOwnerState ownerState, boolean rightClick){
		switch(definition.item.type){
			case WRENCH : {
				if(!vehicle.world.isClient()){
					//If the player isn't the owner of the vehicle, they can't interact with it.
					if(!ownerState.equals(PlayerOwnerState.USER)){
						if(rightClick){
							if(ConfigSystem.configObject.clientControls.devMode.value && vehicle.equals(player.getEntityRiding())){
								player.sendPacket(new PacketEntityGUIRequest(vehicle, player, PacketEntityGUIRequest.EntityGUIType.PACK_EXPORTER));
							}else if(player.isSneaking()){
								player.sendPacket(new PacketEntityGUIRequest(vehicle, player, PacketEntityGUIRequest.EntityGUIType.TEXT_EDITOR));
							}else{
								player.sendPacket(new PacketEntityGUIRequest(vehicle, player, PacketEntityGUIRequest.EntityGUIType.INSTRUMENTS));
							}
						}else if(!vehicle.world.isClient()){
							if(part != null && !player.isSneaking() && !part.placementDefinition.isPermanent && part.isValid){
								//Double-check the part isn't an inventory container with inventory.
								if(part instanceof PartInteractable){
									PartInteractable interactable = (PartInteractable) part;
									if(!interactable.definition.interactable.canBeOpenedInHand && interactable.getMass() > interactable.definition.generic.mass){
										player.sendPacket(new PacketPlayerChatMessage(player, InterfaceCore.translate("interact.failure.cantremoveinventory")));
										return CallbackType.NONE;
									}
								}
								//Player can remove part, spawn item in the world and remove part.
								//Make sure to remove the part before spawning the item.  Some parts
								//care about this order and won't spawn items unless they've been removed.
								part.disconnectAllConnections();
								vehicle.removePart(part, null);
								AItemPart droppedItem = part.getItem();
								if(droppedItem != null){
									WrapperNBT partData = new WrapperNBT();
									part.save(partData);
									vehicle.world.spawnItem(droppedItem, partData, part.position);
								}
							}else if(player.isSneaking()){
								//Attacker is a sneaking player with a wrench.
								//Remove this vehicle if possible.
								if((!ConfigSystem.configObject.general.opPickupVehiclesOnly.value || ownerState.equals(PlayerOwnerState.ADMIN)) && (!ConfigSystem.configObject.general.creativePickupVehiclesOnly.value || player.isCreative()) && vehicle.isValid){
									vehicle.disconnectAllConnections();
									for(APart vehiclePart : vehicle.parts){
										vehiclePart.disconnectAllConnections();
									}
									
									ItemVehicle vehicleItem = vehicle.getItem();
									WrapperNBT vehicleData = new WrapperNBT();
									vehicle.save(vehicleData);
									vehicle.world.spawnItem(vehicleItem, vehicleData, vehicle.position);
									vehicle.remove();
								}
							}
						}
					}else{
						player.sendPacket(new PacketPlayerChatMessage(player, "interact.failure.vehicleowned"));
					}
				}
				return CallbackType.NONE;
			}
			case PAINT_GUN : {
				if(!vehicle.world.isClient() && rightClick){
					//If the player isn't the owner of the vehicle, they can't interact with it.
					if(!ownerState.equals(PlayerOwnerState.USER)){
						if(part != null){
							player.sendPacket(new PacketEntityGUIRequest(part, player, PacketEntityGUIRequest.EntityGUIType.PAINT_GUN));
						}else{
							player.sendPacket(new PacketEntityGUIRequest(vehicle, player, PacketEntityGUIRequest.EntityGUIType.PAINT_GUN));
						}
					}else{
						player.sendPacket(new PacketPlayerChatMessage(player, "interact.failure.vehicleowned"));
					}
				}
				return CallbackType.NONE;
			}
			case KEY : {
				if(!vehicle.world.isClient() && rightClick){
					//Try to lock the vehicle.
					//First check to see if we need to set this key's vehicle.
					WrapperItemStack stack = player.getHeldStack();
					WrapperNBT data = stack.getData();
					UUID keyVehicleUUID = data.getUUID("vehicle");
					if(keyVehicleUUID == null){
						//Check if we are the owner before making this a valid key.
						if(vehicle.ownerUUID != null && ownerState.equals(PlayerOwnerState.USER)){
							player.sendPacket(new PacketPlayerChatMessage(player, "interact.key.failure.notowner"));
							return CallbackType.NONE;
						}
						
						keyVehicleUUID = vehicle.uniqueUUID;
						data.setUUID("vehicle", keyVehicleUUID);
						stack.setData(data);
					}
					
					//Try to lock or unlock this vehicle.
					//If we succeed, send callback to clients to change locked state.
					if(!keyVehicleUUID.equals(vehicle.uniqueUUID)){
						player.sendPacket(new PacketPlayerChatMessage(player, "interact.key.failure.wrongkey"));
					}else{
						if(part instanceof PartSeat){
							//Part is a seat, don't do locking changes, instead, change seat.
							//Returning skip will make the seat-clicking code activate in the packet.
							return CallbackType.SKIP;
						}else if(vehicle.locked){
							vehicle.locked = false;
							player.sendPacket(new PacketPlayerChatMessage(player, "interact.key.info.unlock"));
							
							//Also check collision boxes that block seats, in case we clicked one of those.
							if(hitBox.definition != null){
								if(hitBox.definition.variableName != null){
									if(!vehicle.isVariableActive(hitBox.definition.variableName) && vehicle.getVariable(hitBox.definition.variableName) == 0){
										return CallbackType.ALL_AND_MORE;
									}
								}
							}
						}else{
							vehicle.locked = true;
							player.sendPacket(new PacketPlayerChatMessage(player, "interact.key.info.lock"));
							
							//Also check collision boxes that block seats, in case we clicked one of those.
							if(hitBox.definition != null){
								if(hitBox.definition.variableName != null){
									if(vehicle.isVariableActive(hitBox.definition.variableName) || vehicle.getVariable(hitBox.definition.variableName) != 0){
										return CallbackType.ALL_AND_MORE;
									}
								}
							}
						}
						return CallbackType.ALL;
					}
				}else{
					vehicle.locked = !vehicle.locked;
				}
				return CallbackType.NONE;
			}
			case TICKET : {
				if(!vehicle.world.isClient() && rightClick){
					if(player.isSneaking()){
						Iterator<WrapperEntity> iterator = vehicle.locationRiderMap.inverse().keySet().iterator();
						while(iterator.hasNext()){
							WrapperEntity entity = iterator.next();
							if(!(entity instanceof WrapperPlayer)){
								vehicle.removeRider(entity, iterator);
							}
						}
					}else{
						vehicle.world.loadEntities(new BoundingBox(player.getPosition(), 8D, 8D, 8D), vehicle);
					}
				}
				return CallbackType.NONE;
			}
			case FUEL_HOSE : {
				if(!vehicle.world.isClient() && rightClick){
					if(firstPartClicked == null){
						if(part instanceof PartInteractable){
							PartInteractable interactable = (PartInteractable) part;
							if(interactable.tank != null){
								if(interactable.linkedPart == null && interactable.linkedVehicle == null){
									firstPartClicked = interactable;
									player.sendPacket(new PacketPlayerChatMessage(player, "interact.fuelhose.firstlink"));
								}else{
									player.sendPacket(new PacketPlayerChatMessage(player, "interact.fuelhose.alreadylinked"));
								}
							}
						}
					}else{
						if(part instanceof PartInteractable){
							PartInteractable interactable = (PartInteractable) part;
							if(interactable.tank != null && !interactable.equals(firstPartClicked)){
								if(interactable.linkedPart == null && interactable.linkedVehicle == null){
									if(part.position.distanceTo(firstPartClicked.position) < 15){
										if(interactable.tank.getFluid().isEmpty() || firstPartClicked.tank.getFluid().isEmpty() || interactable.tank.getFluid().equals(firstPartClicked.tank.getFluid())){
											firstPartClicked.linkedPart = interactable;
											InterfacePacket.sendToAllClients(new PacketPartInteractable(firstPartClicked, player));
											player.sendPacket(new PacketPlayerChatMessage(player, "interact.fuelhose.secondlink"));
											firstPartClicked = null;
										}else{
											firstPartClicked = null;
											player.sendPacket(new PacketPlayerChatMessage(player, "interact.fuelhose.differentfluids"));
										}
									}else{
										firstPartClicked = null;
										player.sendPacket(new PacketPlayerChatMessage(player, "interact.fuelhose.toofar"));
									}
								}else{
									firstPartClicked = null;
									player.sendPacket(new PacketPlayerChatMessage(player, "interact.fuelhose.alreadylinked"));
								}
							}
						}else if(part == null){
							if(vehicle.position.distanceTo(firstPartClicked.position) < 15){
								if(vehicle.fuelTank.getFluid().isEmpty() || firstPartClicked.tank.getFluid().isEmpty() || vehicle.fuelTank.getFluid().equals(firstPartClicked.tank.getFluid())){
									firstPartClicked.linkedVehicle = vehicle;
									InterfacePacket.sendToAllClients(new PacketPartInteractable(firstPartClicked, player));
									player.sendPacket(new PacketPlayerChatMessage(player, "interact.fuelhose.secondlink"));
									firstPartClicked = null;
								}else{
									firstPartClicked = null;
									player.sendPacket(new PacketPlayerChatMessage(player, "interact.fuelhose.differentfluids"));
								}
							}else{
								firstPartClicked = null;
								player.sendPacket(new PacketPlayerChatMessage(player, "interact.fuelhose.toofar"));
							}
						}
					}
				}
				return CallbackType.NONE;
			}
			case JUMPER_CABLES : {
				if(!vehicle.world.isClient() && rightClick){
					if(part instanceof PartEngine){
						PartEngine engine = (PartEngine) part;
						if(engine.linkedEngine == null){
							if(firstEngineClicked == null){
								firstEngineClicked = engine;
								player.sendPacket(new PacketPlayerChatMessage(player, "interact.jumpercable.firstlink"));
							}else if(!firstEngineClicked.equals(engine)){
								if(firstEngineClicked.entityOn.equals(engine.entityOn)){
									firstEngineClicked = null;
									player.sendPacket(new PacketPlayerChatMessage(player, "interact.jumpercable.samevehicle"));
								}else if(engine.position.distanceTo(firstEngineClicked.position) < 15){
									engine.linkedEngine = firstEngineClicked;
									firstEngineClicked.linkedEngine = engine;
									InterfacePacket.sendToAllClients(new PacketPartEngine(engine, firstEngineClicked));
									InterfacePacket.sendToAllClients(new PacketPartEngine(firstEngineClicked, engine));
									firstEngineClicked = null;
									player.sendPacket(new PacketPlayerChatMessage(player, "interact.jumpercable.secondlink"));
								}else{
									firstEngineClicked = null;
									player.sendPacket(new PacketPlayerChatMessage(player, "interact.jumpercable.toofar"));
								}
							}
						}else{
							player.sendPacket(new PacketPlayerChatMessage(player, "interact.jumpercable.alreadylinked"));
						}
					}
				}
				return CallbackType.NONE;
			}
			case JUMPER_PACK : {
				if(rightClick){
					//Use jumper on vehicle.
					vehicle.electricPower = 12;
					if(!vehicle.world.isClient()){
						InterfacePacket.sendToPlayer(new PacketPlayerChatMessage(player, "interact.jumperpack.success"), player);
						if(!player.isCreative()){
							player.getInventory().removeFromSlot(player.getHotbarIndex(), 1);
						}
						return CallbackType.ALL;
					}
				}
				return CallbackType.NONE;
			}
			default : return CallbackType.SKIP; 
		}
	}
	
	@Override
	public boolean onBlockClicked(WrapperWorld world, WrapperPlayer player, Point3d position, Axis axis){
		if(definition.item.type.equals(ItemComponentType.PAINT_GUN)){
			if(!world.isClient()){
				ATileEntityBase<?> tile = world.getTileEntity(position);
				if(tile instanceof TileEntityDecor){
					player.sendPacket(new PacketEntityGUIRequest(tile, player, PacketEntityGUIRequest.EntityGUIType.PAINT_GUN));
					return true;
				}else if(tile instanceof TileEntityPole){
					TileEntityPole pole = (TileEntityPole) tile;
					//Change the axis to match the 8-dim axis for poles.  Blocks only get a 4-dim axis.
					axis = Axis.getFromRotation(player.getYaw(), pole.definition.pole.allowsDiagonals).getOpposite();
					if(pole.components.containsKey(axis)){
						player.sendPacket(new PacketEntityGUIRequest(pole.components.get(axis), player, PacketEntityGUIRequest.EntityGUIType.PAINT_GUN));
					}
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public boolean onUsed(WrapperWorld world, WrapperPlayer player){
		if(definition.item.type.equals(ItemComponentType.BOOKLET)){
			if(!world.isClient()){
				player.sendPacket(new PacketGUIRequest(player, PacketGUIRequest.GUIType.BOOKELET));
			}
		}else if(definition.item.type.equals(ItemComponentType.Y2K_BUTTON)){
			if(!world.isClient() && player.isOP()){
				for(EntityVehicleF_Physics vehicle : world.getEntitiesOfType(EntityVehicleF_Physics.class)){
					vehicle.setVariable(EntityVehicleF_Physics.THROTTLE_VARIABLE, 0);
					InterfacePacket.sendToAllClients(new PacketEntityVariableSet(vehicle, EntityVehicleF_Physics.THROTTLE_VARIABLE, 0));
					if(!vehicle.isVariableActive(EntityVehicleF_Physics.PARKINGBRAKE_VARIABLE)){
						vehicle.setVariable(EntityVehicleF_Physics.PARKINGBRAKE_VARIABLE, 1);
						InterfacePacket.sendToAllClients(new PacketEntityVariableToggle(vehicle, EntityVehicleF_Physics.PARKINGBRAKE_VARIABLE));
					}
					for(PartEngine engine : vehicle.engines.values()){
						if(engine.isVariableActive(PartEngine.MAGNETO_VARIABLE)){
							engine.setVariable(PartEngine.MAGNETO_VARIABLE, 0);
							InterfacePacket.sendToAllClients(new PacketEntityVariableToggle(engine, PartEngine.MAGNETO_VARIABLE));
						}
					}
				}
			}
		}
        return true;
    }

	@Override
	public int getTimeToEat(){
		return definition.item.type.equals(ItemComponentType.FOOD) ? definition.food.timeToEat : 0;
	}
	
	@Override
	public boolean isDrink(){
		return definition.food.isDrink;
	}

	@Override
	public int getHungerAmount(){
		return definition.food.hungerAmount;
	}

	@Override
	public float getSaturationAmount(){
		return definition.food.saturationAmount;
	}

	@Override
	public List<JSONPotionEffect> getEffects(){
		return definition.food.effects;
	}
}
