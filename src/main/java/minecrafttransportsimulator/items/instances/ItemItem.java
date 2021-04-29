package minecrafttransportsimulator.items.instances;

import java.util.Iterator;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.entities.instances.PartInteractable;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.guis.components.InterfaceGUI;
import minecrafttransportsimulator.guis.instances.GUIBooklet;
import minecrafttransportsimulator.guis.instances.GUIInstruments;
import minecrafttransportsimulator.guis.instances.GUIPackExporter;
import minecrafttransportsimulator.guis.instances.GUIPaintGun;
import minecrafttransportsimulator.guis.instances.GUITextEditor;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.items.components.IItemFood;
import minecrafttransportsimulator.items.components.IItemVehicleInteractable;
import minecrafttransportsimulator.jsondefs.JSONItem;
import minecrafttransportsimulator.jsondefs.JSONPotionEffect;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableToggle;
import minecrafttransportsimulator.packets.instances.PacketPartEngine;
import minecrafttransportsimulator.packets.instances.PacketPartEngine.Signal;
import minecrafttransportsimulator.packets.instances.PacketPartInteractable;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlAnalog;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlDigital;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.rendering.components.LightType;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.item.ItemStack;

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
	public CallbackType doVehicleInteraction(EntityVehicleF_Physics vehicle, APart part, WrapperPlayer player, PlayerOwnerState ownerState, boolean rightClick){
		switch(definition.item.type){
			case WRENCH : {
				//If the player isn't the owner of the vehicle, they can't interact with it.
				if(!ownerState.equals(PlayerOwnerState.USER)){
					if(rightClick){
						if(vehicle.world.isClient()){
							if(player.equals(InterfaceClient.getClientPlayer())){
								if(ConfigSystem.configObject.clientControls.devMode.value && vehicle.equals(player.getEntityRiding())){
									InterfaceGUI.openGUI(new GUIPackExporter(vehicle));
								}else if(player.isSneaking()){
									InterfaceGUI.openGUI(new GUITextEditor(vehicle));
								}else{
									InterfaceGUI.openGUI(new GUIInstruments(vehicle));
								}
							}
						}else{
							return CallbackType.PLAYER;
						}
					}else if(!vehicle.world.isClient()){
						if(part != null && !player.isSneaking()){
							//Player can remove part.  Check that the part isn't permanent, or a part with subparts.
							//If not, spawn item in the world and remove part.
							//Make sure to remove the part before spawning the item.  Some parts
							//care about this order and won't spawn items unless they've been removed.
							if(!part.placementDefinition.isPermanent){
								part.disconnectAllConnections();
								vehicle.removePart(part, null);
								AItemPart droppedItem = part.getItem();
								if(droppedItem != null){
									WrapperNBT partData = new WrapperNBT();
									part.save(partData);
									vehicle.world.spawnItem(droppedItem, partData, part.position);
								}
							}
						}else if(player.isSneaking()){
							//Attacker is a sneaking player with a wrench.
							//Remove this vehicle if possible.
							if((!ConfigSystem.configObject.general.opPickupVehiclesOnly.value || ownerState.equals(PlayerOwnerState.ADMIN)) && (!ConfigSystem.configObject.general.creativePickupVehiclesOnly.value || player.isCreative())){
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
				return CallbackType.NONE;
			}
			case PAINT_GUN : {
				//If the player isn't the owner of the vehicle, they can't interact with it.
				if(!ownerState.equals(PlayerOwnerState.USER)){
					if(rightClick){
						if(vehicle.world.isClient()){
							if(player.equals(InterfaceClient.getClientPlayer())){
								if(part != null){
									InterfaceGUI.openGUI(new GUIPaintGun(part, player));
								}else{
									InterfaceGUI.openGUI(new GUIPaintGun(vehicle, player));
								}
							}
						}else{
							return CallbackType.PLAYER;
						}
					}
				}else{
					player.sendPacket(new PacketPlayerChatMessage(player, "interact.failure.vehicleowned"));
				}
				return CallbackType.NONE;
			}
			case KEY : {
				if(!vehicle.world.isClient()){
					if(rightClick){
						if(player.isSneaking()){
							//Try to change ownership of the vehicle.
							if(vehicle.ownerUUID.isEmpty()){
								vehicle.ownerUUID = player.getID();
								player.sendPacket(new PacketPlayerChatMessage(player, "interact.key.info.own"));
							}else{
								if(!ownerState.equals(PlayerOwnerState.USER)){
									vehicle.ownerUUID = "";
									player.sendPacket(new PacketPlayerChatMessage(player, "interact.key.info.unown"));
								}else{
									player.sendPacket(new PacketPlayerChatMessage(player, "interact.key.failure.alreadyowned"));
								}
							}
						}else{
							//Try to lock the vehicle.
							//First check to see if we need to set this key's vehicle.
							ItemStack stack = player.getHeldStack();
							WrapperNBT data = new WrapperNBT(stack);
							String keyVehicleUUID = data.getString("vehicle");
							if(keyVehicleUUID.isEmpty()){
								//Check if we are the owner before making this a valid key.
								if(!vehicle.ownerUUID.isEmpty() && ownerState.equals(PlayerOwnerState.USER)){
									player.sendPacket(new PacketPlayerChatMessage(player, "interact.key.failure.notowner"));
									return CallbackType.NONE;
								}
								
								keyVehicleUUID = vehicle.uniqueUUID;
								data.setString("vehicle", keyVehicleUUID);
								stack.setTagCompound(data.tag);
							}
							
							//Try to lock or unlock this vehicle.
							//If we succeed, send callback to clients to change locked state.
							if(!keyVehicleUUID.equals(vehicle.uniqueUUID)){
								player.sendPacket(new PacketPlayerChatMessage(player, "interact.key.failure.wrongkey"));
							}else{
								if(vehicle.locked){
									vehicle.locked = false;
									player.sendPacket(new PacketPlayerChatMessage(player, "interact.key.info.unlock"));
									//If we aren't in this vehicle, and we clicked a seat, start riding the vehicle.
									if(part instanceof PartSeat && player.getEntityRiding() == null){
										part.interact(player);
									}
								}else{
									vehicle.locked = true;
									player.sendPacket(new PacketPlayerChatMessage(player, "interact.key.info.lock"));
								}
								return CallbackType.ALL;
							}
						}
					}
				}else{
					vehicle.locked = !vehicle.locked;
				}
				return CallbackType.NONE;
			}
			case TICKET : {
				if(rightClick && !vehicle.world.isClient()){
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
				if(!vehicle.world.isClient()){
					if(rightClick){
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
				}
				return CallbackType.NONE;
			}
			case JUMPER_CABLES : {
				if(!vehicle.world.isClient()){
					if(rightClick){
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
							player.getInventory().removeStack(player.getHeldStack(), 1);
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
			if(world.isClient()){
				ATileEntityBase<?> tile = world.getTileEntity(position);
				if(tile instanceof TileEntityDecor){
					InterfaceGUI.openGUI(new GUIPaintGun(tile, player));
					return true;
				}else if(tile instanceof TileEntityPole){
					TileEntityPole pole = (TileEntityPole) tile;
					if(pole.definition.pole.allowsDiagonals){
						//Change the axis to match the 8-dim axis for poles.  Blocks only get a 4-dim axis.
						axis = Axis.getFromRotation(player.getYaw()).getOpposite();
					}
					if(pole.components.containsKey(axis)){
						InterfaceGUI.openGUI(new GUIPaintGun(pole.components.get(axis), player));
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
			if(world.isClient()){
				InterfaceGUI.openGUI(new GUIBooklet(this));
			}
		}else if(definition.item.type.equals(ItemComponentType.Y2K_BUTTON)){
			if(!world.isClient() && player.isOP()){
				for(AEntityA_Base entity : AEntityA_Base.getEntities(world)){
					if(entity instanceof EntityVehicleF_Physics){
						EntityVehicleF_Physics vehicle = (EntityVehicleF_Physics) entity;
						vehicle.throttle = 0;
						InterfacePacket.sendToAllClients(new PacketVehicleControlAnalog(vehicle, PacketVehicleControlAnalog.Controls.THROTTLE, (short) 0, (byte) 0));
						vehicle.parkingBrakeOn = true;
						InterfacePacket.sendToAllClients(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.P_BRAKE, true));
						for(PartEngine engine : vehicle.engines.values()){
							engine.setMagnetoStatus(false);
							InterfacePacket.sendToAllClients(new PacketPartEngine(engine, Signal.MAGNETO_OFF));
						}
						Iterator<String> variableIterator = vehicle.variablesOn.iterator();
						while(variableIterator.hasNext()){
							String variableName = variableIterator.next();
							for(LightType light : LightType.values()){
								if(light.lowercaseName.equals(variableName)){
									InterfacePacket.sendToAllClients(new PacketEntityVariableToggle(vehicle, variableName));
									variableIterator.remove();
									break;
								}
							}
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
	
	public static enum ItemComponentType{
		@JSONDescription("Creates an item with no functionality.")
		NONE,
		@JSONDescription("Creates a booklet, which is a book-like item.")
		BOOKLET,
		@JSONDescription("Creates an item that can be eaten.")
		FOOD,
		@JSONDescription("Creates an item that can be used as a weapon.")
		WEAPON,
		@JSONDescription("Creates an item that works as a part scanner.")
		SCANNER,
		@JSONDescription("Creates an item that works as a wrench.")
		WRENCH,
		@JSONDescription("Creates an item that works as a paint gun.")
		PAINT_GUN,
		@JSONDescription("Creates an item that works as a key.")
		KEY,
		@JSONDescription("Creates an item that works as a ticket.")
		TICKET,
		@JSONDescription("Creates an item that works as a fuel hose.")
		FUEL_HOSE,
		@JSONDescription("Creates an item that works as jumper cables.")
		JUMPER_CABLES,
		@JSONDescription("Creates an item that works as a jumper pack.")
		JUMPER_PACK,
		@JSONDescription("Creates an item that works as a Y2K button.")
		Y2K_BUTTON;
	}
}
