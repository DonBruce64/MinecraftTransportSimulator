package minecrafttransportsimulator.items.instances;

import java.util.List;
import java.util.UUID;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3D;
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
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage.LanguageEntry;
import minecrafttransportsimulator.jsondefs.JSONItem;
import minecrafttransportsimulator.jsondefs.JSONItem.ItemComponentType;
import minecrafttransportsimulator.jsondefs.JSONPotionEffect;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityGUIRequest;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableSet;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableToggle;
import minecrafttransportsimulator.packets.instances.PacketGUIRequest;
import minecrafttransportsimulator.packets.instances.PacketPartEngine;
import minecrafttransportsimulator.packets.instances.PacketPartInteractable;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.systems.ConfigSystem;

public class ItemItem extends AItemPack<JSONItem> implements IItemVehicleInteractable, IItemFood {
    /*Current page of this item, if it's a booklet.  Kept here locally as only one item class is constructed for each booklet definition.*/
    public int pageNumber;
    /*First engine clicked for jumper cable items.  Kept here locally as only one item class is constructed for each jumper cable definition.*/
    private static PartEngine firstEngineClicked;
    /*First part clicked for fuel hose items.  Kept here locally as only one item class is constructed for each jumper cable definition.*/
    private static PartInteractable firstPartClicked;

    public ItemItem(JSONItem definition) {
        super(definition, null);
    }

    @Override
    public boolean canBreakBlocks() {
        return !definition.item.type.equals(ItemComponentType.WRENCH);
    }

    @Override
    public CallbackType doVehicleInteraction(EntityVehicleF_Physics vehicle, APart part, BoundingBox hitBox, IWrapperPlayer player, PlayerOwnerState ownerState, boolean rightClick) {
        switch (definition.item.type) {
            case WRENCH: {
                if (!vehicle.world.isClient()) {
                    //If the player isn't the owner of the vehicle, they can't interact with it.
                    if (!ownerState.equals(PlayerOwnerState.USER)) {
                        if (rightClick) {
                            if (ConfigSystem.settings.general.devMode.value && vehicle.allParts.contains(player.getEntityRiding())) {
                                player.sendPacket(new PacketEntityGUIRequest(vehicle, player, PacketEntityGUIRequest.EntityGUIType.PACK_EXPORTER));
                            } else if (player.isSneaking()) {
                                player.sendPacket(new PacketEntityGUIRequest(vehicle, player, PacketEntityGUIRequest.EntityGUIType.TEXT_EDITOR));
                            } else {
                                player.sendPacket(new PacketEntityGUIRequest(vehicle, player, PacketEntityGUIRequest.EntityGUIType.INSTRUMENTS));
                            }
                        } else {
                            if (part != null && !player.isSneaking() && !part.placementDefinition.isPermanent && part.isValid) {
                                LanguageEntry partResult = part.checkForRemoval();
                                if (partResult != null) {
                                    player.sendPacket(new PacketPlayerChatMessage(player, partResult));
                                    return CallbackType.NONE;
                                } else {
                                    //Player can remove part, spawn item in the world and remove part.
                                    //Make sure to remove the part before spawning the item.
                                    part.entityOn.removePart(part, null);
                                    AItemPart droppedItem = part.getItem();
                                    if (droppedItem != null) {
                                        part.entityOn.world.spawnItem(droppedItem, part.save(InterfaceManager.coreInterface.getNewNBTWrapper()), part.position);
                                    }
                                }
                            } else if (player.isSneaking()) {
                                //Attacker is a sneaking player with a wrench.
                                //Remove this vehicle if possible.
                                if ((!ConfigSystem.settings.general.opPickupVehiclesOnly.value || ownerState.equals(PlayerOwnerState.ADMIN)) && (!ConfigSystem.settings.general.creativePickupVehiclesOnly.value || player.isCreative()) && vehicle.isValid) {
                                    vehicle.disconnectAllConnections();
                                    vehicle.world.spawnItem(vehicle.getItem(), vehicle.save(InterfaceManager.coreInterface.getNewNBTWrapper()), vehicle.position);
                                    vehicle.remove();
                                }
                            }
                        }
                    } else {
                        player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_VEHICLE_OWNED));
                    }
                }
                return CallbackType.NONE;
            }
            case PAINT_GUN: {
                if (!vehicle.world.isClient() && rightClick) {
                    //If the player isn't the owner of the vehicle, they can't interact with it.
                    if (!ownerState.equals(PlayerOwnerState.USER)) {
                        if (part != null) {
                            player.sendPacket(new PacketEntityGUIRequest(part, player, PacketEntityGUIRequest.EntityGUIType.PAINT_GUN));
                        } else {
                            player.sendPacket(new PacketEntityGUIRequest(vehicle, player, PacketEntityGUIRequest.EntityGUIType.PAINT_GUN));
                        }
                    } else {
                        player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_VEHICLE_OWNED));
                    }
                }
                return CallbackType.NONE;
            }
            case KEY: {
                if (!vehicle.world.isClient() && rightClick) {
                    //Try to lock the vehicle.
                    //First check to see if we need to set this key's vehicle.
                    IWrapperItemStack stack = player.getHeldStack();
                    IWrapperNBT data = stack.getData();
                    UUID keyVehicleUUID = data.getUUID("vehicle");
                    if (keyVehicleUUID == null) {
                        //Check if we are the owner before making this a valid key.
                        if (vehicle.ownerUUID != null && ownerState.equals(PlayerOwnerState.USER)) {
                            player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_KEY_NOTOWNER));
                        } else {
                            keyVehicleUUID = vehicle.uniqueUUID;
                            data.setUUID("vehicle", keyVehicleUUID);
                            stack.setData(data);
                            player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_KEY_BIND));
                        }
                        return CallbackType.NONE;
                    }

                    //Try to lock or unlock this vehicle.
                    //If we succeed, send callback to clients to change locked state.
                    if (!keyVehicleUUID.equals(vehicle.uniqueUUID)) {
                        player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_KEY_WRONGKEY));
                    } else {
                        if (part instanceof PartSeat) {
                            //Part is a seat, don't do locking changes, instead, change seat.
                            //Returning skip will make the seat-clicking code activate in the packet.
                            return CallbackType.SKIP;
                        } else if (vehicle.locked) {
                            vehicle.locked = false;
                            player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_KEY_UNLOCK));

                            //Also check collision boxes that block seats, in case we clicked one of those.
                            if (hitBox.definition != null) {
                                if (hitBox.definition.variableName != null) {
                                    if (!vehicle.isVariableActive(hitBox.definition.variableName) && vehicle.getVariable(hitBox.definition.variableName) == 0) {
                                        return CallbackType.ALL_AND_MORE;
                                    }
                                }
                            }
                        } else {
                            vehicle.locked = true;
                            player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_KEY_LOCK));

                            //Also check collision boxes that block seats, in case we clicked one of those.
                            if (hitBox.definition != null) {
                                if (hitBox.definition.variableName != null) {
                                    if (vehicle.isVariableActive(hitBox.definition.variableName) || vehicle.getVariable(hitBox.definition.variableName) != 0) {
                                        return CallbackType.ALL_AND_MORE;
                                    }
                                }
                            }
                        }
                        return CallbackType.ALL;
                    }
                } else {
                    vehicle.locked = !vehicle.locked;
                }
                return CallbackType.NONE;
            }
            case TICKET: {
                if (!vehicle.world.isClient() && rightClick) {
                    if (player.isSneaking()) {
                        for (APart otherPart : vehicle.allParts) {
                            if (otherPart.rider != null) {
                                otherPart.removeRider();
                            }
                        }
                    } else {
                        vehicle.world.loadEntities(new BoundingBox(player.getPosition(), 8D, 8D, 8D), vehicle);
                    }
                }
                return CallbackType.NONE;
            }
            case FUEL_HOSE: {
                if (!vehicle.world.isClient() && rightClick) {
                    if (firstPartClicked == null) {
                        if (part instanceof PartInteractable) {
                            PartInteractable interactable = (PartInteractable) part;
                            if (interactable.tank != null) {
                                if (interactable.linkedPart == null && interactable.linkedVehicle == null) {
                                    firstPartClicked = interactable;
                                    player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_FUELHOSE_FIRSTLINK));
                                } else {
                                    player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_FUELHOSE_ALREADYLINKED));
                                }
                            }
                        }
                    } else {
                        if (part instanceof PartInteractable) {
                            PartInteractable interactable = (PartInteractable) part;
                            if (interactable.tank != null && !interactable.equals(firstPartClicked)) {
                                if (interactable.linkedPart == null && interactable.linkedVehicle == null) {
                                    if (part.position.isDistanceToCloserThan(firstPartClicked.position, 16)) {
                                        if (interactable.tank.getFluid().isEmpty() || firstPartClicked.tank.getFluid().isEmpty() || interactable.tank.getFluid().equals(firstPartClicked.tank.getFluid())) {
                                            firstPartClicked.linkedPart = interactable;
                                            InterfaceManager.packetInterface.sendToAllClients(new PacketPartInteractable(firstPartClicked, player));
                                            player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_FUELHOSE_SECONDLINK));
                                            firstPartClicked = null;
                                        } else {
                                            firstPartClicked = null;
                                            player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_FUELHOSE_DIFFERENTFLUIDS));
                                        }
                                    } else {
                                        firstPartClicked = null;
                                        player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_FUELHOSE_TOOFAR));
                                    }
                                } else {
                                    firstPartClicked = null;
                                    player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_FUELHOSE_ALREADYLINKED));
                                }
                            }
                        } else if (part == null) {
                            if (vehicle.position.isDistanceToCloserThan(firstPartClicked.position, 16)) {
                                if (vehicle.fuelTank.getFluid().isEmpty() || firstPartClicked.tank.getFluid().isEmpty() || vehicle.fuelTank.getFluid().equals(firstPartClicked.tank.getFluid())) {
                                    firstPartClicked.linkedVehicle = vehicle;
                                    InterfaceManager.packetInterface.sendToAllClients(new PacketPartInteractable(firstPartClicked, player));
                                    player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_FUELHOSE_SECONDLINK));
                                    firstPartClicked = null;
                                } else {
                                    firstPartClicked = null;
                                    player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_FUELHOSE_DIFFERENTFLUIDS));
                                }
                            } else {
                                firstPartClicked = null;
                                player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_FUELHOSE_TOOFAR));
                            }
                        }
                    }
                }
                return CallbackType.NONE;
            }
            case JUMPER_CABLES: {
                if (!vehicle.world.isClient() && rightClick) {
                    if (part instanceof PartEngine) {
                        PartEngine engine = (PartEngine) part;
                        if (engine.linkedEngine == null) {
                            if (firstEngineClicked == null) {
                                firstEngineClicked = engine;
                                player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_JUMPERCABLE_FIRSTLINK));
                            } else if (!firstEngineClicked.equals(engine)) {
                                if (firstEngineClicked.entityOn.equals(engine.entityOn)) {
                                    firstEngineClicked = null;
                                    player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_JUMPERCABLE_SAMEVEHICLE));
                                } else if (engine.position.isDistanceToCloserThan(firstEngineClicked.position, 15)) {
                                    engine.linkedEngine = firstEngineClicked;
                                    firstEngineClicked.linkedEngine = engine;
                                    InterfaceManager.packetInterface.sendToAllClients(new PacketPartEngine(engine, firstEngineClicked));
                                    InterfaceManager.packetInterface.sendToAllClients(new PacketPartEngine(firstEngineClicked, engine));
                                    firstEngineClicked = null;
                                    player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_JUMPERCABLE_SECONDLINK));
                                } else {
                                    firstEngineClicked = null;
                                    player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_JUMPERCABLE_TOOFAR));
                                }
                            }
                        } else {
                            player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_JUMPERCABLE_ALREADYLINKED));
                        }
                    }
                }
                return CallbackType.NONE;
            }
            case JUMPER_PACK: {
                if (rightClick) {
                    //Use jumper on vehicle.
                    vehicle.electricPower = 12;
                    if (!vehicle.world.isClient()) {
                        InterfaceManager.packetInterface.sendToPlayer(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_VEHICLE_JUMPERPACK), player);
                        if (!player.isCreative()) {
                            player.getInventory().removeFromSlot(player.getHotbarIndex(), 1);
                        }
                        return CallbackType.ALL;
                    }
                }
                return CallbackType.NONE;
            }
            default:
                return CallbackType.SKIP;
        }
    }

    @Override
    public boolean onBlockClicked(AWrapperWorld world, IWrapperPlayer player, Point3D position, Axis axis) {
        if (definition.item.type.equals(ItemComponentType.PAINT_GUN)) {
            if (!world.isClient()) {
                ATileEntityBase<?> tile = world.getTileEntity(position);
                if (tile instanceof TileEntityDecor) {
                    player.sendPacket(new PacketEntityGUIRequest(tile, player, PacketEntityGUIRequest.EntityGUIType.PAINT_GUN));
                    return true;
                } else if (tile instanceof TileEntityPole) {
                    TileEntityPole pole = (TileEntityPole) tile;
                    //Change the axis to match the 8-dim axis for poles.  Blocks only get a 4-dim axis.
                    axis = Axis.getFromRotation(player.getYaw(), pole.definition.pole.allowsDiagonals).getOpposite();
                    if (pole.components.containsKey(axis)) {
                        player.sendPacket(new PacketEntityGUIRequest(pole.components.get(axis), player, PacketEntityGUIRequest.EntityGUIType.PAINT_GUN));
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean onUsed(AWrapperWorld world, IWrapperPlayer player) {
        if (definition.item.type.equals(ItemComponentType.BOOKLET)) {
            if (!world.isClient()) {
                player.sendPacket(new PacketGUIRequest(player, PacketGUIRequest.GUIType.BOOKLET));
            }
        } else if (definition.item.type.equals(ItemComponentType.Y2K_BUTTON)) {
            if (!world.isClient() && player.isOP()) {
                for (EntityVehicleF_Physics vehicle : world.getEntitiesOfType(EntityVehicleF_Physics.class)) {
                    vehicle.setVariable(EntityVehicleF_Physics.THROTTLE_VARIABLE, 0);
                    InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableSet(vehicle, EntityVehicleF_Physics.THROTTLE_VARIABLE, 0));
                    if (!vehicle.isVariableActive(EntityVehicleF_Physics.PARKINGBRAKE_VARIABLE)) {
                        vehicle.setVariable(EntityVehicleF_Physics.PARKINGBRAKE_VARIABLE, 1);
                        InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableToggle(vehicle, EntityVehicleF_Physics.PARKINGBRAKE_VARIABLE));
                    }
                    for (PartEngine engine : vehicle.engines.values()) {
                        if (engine.isVariableActive(PartEngine.MAGNETO_VARIABLE)) {
                            engine.setVariable(PartEngine.MAGNETO_VARIABLE, 0);
                            InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableToggle(engine, PartEngine.MAGNETO_VARIABLE));
                        }
                    }
                }
            }
        }
        return true;
    }

    @Override
    public int getTimeToEat() {
        return definition.item.type.equals(ItemComponentType.FOOD) ? definition.food.timeToEat : 0;
    }

    @Override
    public boolean isDrink() {
        return definition.food.isDrink;
    }

    @Override
    public int getHungerAmount() {
        return definition.food.hungerAmount;
    }

    @Override
    public float getSaturationAmount() {
        return definition.food.saturationAmount;
    }

    @Override
    public List<JSONPotionEffect> getEffects() {
        return definition.food.effects;
    }
}
