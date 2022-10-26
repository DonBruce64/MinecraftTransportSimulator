package minecrafttransportsimulator.items.instances;

import java.util.List;
import java.util.UUID;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable.PlayerOwnerState;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.entities.instances.PartInteractable;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.items.components.IItemEntityInteractable;
import minecrafttransportsimulator.items.components.IItemFood;
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

public class ItemItem extends AItemPack<JSONItem> implements IItemEntityInteractable, IItemFood {
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
    public CallbackType doEntityInteraction(AEntityE_Interactable<?> entity, BoundingBox hitBox, IWrapperPlayer player, PlayerOwnerState ownerState, boolean rightClick) {
        switch (definition.item.type) {
            case WRENCH: {
                if (!entity.world.isClient()) {
                    //If the player isn't the owner of the entity, they can't interact with it.
                    if (!ownerState.equals(PlayerOwnerState.USER)) {
                        if (rightClick) {
                            //Right-clicking opens GUIs.
                            if (entity instanceof EntityVehicleF_Physics) {
                                EntityVehicleF_Physics vehicle = (EntityVehicleF_Physics) entity;
                                if (ConfigSystem.settings.general.devMode.value && vehicle.allParts.contains(player.getEntityRiding())) {
                                    player.sendPacket(new PacketEntityGUIRequest(vehicle, player, PacketEntityGUIRequest.EntityGUIType.PACK_EXPORTER));
                                } else {
                                    player.sendPacket(new PacketEntityGUIRequest(vehicle, player, PacketEntityGUIRequest.EntityGUIType.INSTRUMENTS));
                                }
                            } else if (player.isSneaking()) {
                                player.sendPacket(new PacketEntityGUIRequest(entity, player, PacketEntityGUIRequest.EntityGUIType.TEXT_EDITOR));
                            }
                        } else {
                            //Left clicking removes parts, or removes vehicles, if we were sneaking.
                            if(player.isSneaking()) {
                                EntityVehicleF_Physics vehicle;
                                if(entity instanceof APart) {
                                    vehicle = ((APart) entity).vehicleOn;
                                }else if(entity instanceof EntityVehicleF_Physics) {
                                    vehicle = (EntityVehicleF_Physics) entity;
                                } else {
                                    vehicle = null;
                                }
                                if(vehicle != null) {
                                    if ((!ConfigSystem.settings.general.opPickupVehiclesOnly.value || ownerState.equals(PlayerOwnerState.ADMIN)) && (!ConfigSystem.settings.general.creativePickupVehiclesOnly.value || player.isCreative()) && entity.isValid) {
                                        vehicle.disconnectAllConnections();
                                        vehicle.world.spawnItem(vehicle.getItem(), vehicle.save(InterfaceManager.coreInterface.getNewNBTWrapper()), hitBox.globalCenter);
                                        vehicle.remove();
                                    }
                                }
                            }else {
                                if (entity instanceof APart) {
                                    APart part = (APart) entity;
                                    if (!part.isPermanent && part.isValid) {
                                        LanguageEntry partResult = part.checkForRemoval();
                                        if (partResult != null) {
                                            player.sendPacket(new PacketPlayerChatMessage(player, partResult));
                                            return CallbackType.NONE;
                                        } else {
                                            //Player can remove part, spawn item in the world and remove part.
                                            AItemPart droppedItem = part.getItem();
                                            if (droppedItem != null) {
                                                part.entityOn.world.spawnItem(droppedItem, part.save(InterfaceManager.coreInterface.getNewNBTWrapper()), part.position);
                                            }
                                            part.entityOn.removePart(part, null);
                                        }
                                    }
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
                if (!entity.world.isClient() && rightClick) {
                    //If the player isn't the owner of the entity, they can't interact with it.
                    if (!ownerState.equals(PlayerOwnerState.USER)) {
                        player.sendPacket(new PacketEntityGUIRequest(entity, player, PacketEntityGUIRequest.EntityGUIType.PAINT_GUN));
                    } else {
                        player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_VEHICLE_OWNED));
                    }
                }
                return CallbackType.NONE;
            }
            case KEY: {
                if (rightClick && !entity.world.isClient()) {
                    //Try to lock the entity.
                    //First check to see if we need to set this key's entity.
                    IWrapperItemStack stack = player.getHeldStack();
                    IWrapperNBT data = stack.getData();
                    UUID keyVehicleUUID = data.getUUID("vehicle");
                    if (keyVehicleUUID == null) {
                        //Check if we are the owner before making this a valid key.
                        if (entity.ownerUUID != null && ownerState.equals(PlayerOwnerState.USER)) {
                            player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_KEY_NOTOWNER));
                        } else {
                            keyVehicleUUID = entity.uniqueUUID;
                            data.setUUID("vehicle", keyVehicleUUID);
                            stack.setData(data);
                            player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_KEY_BIND));
                        }
                        return CallbackType.NONE;
                    }

                    //Try to lock or unlock this entity.
                    //If we succeed, send callback to clients to change locked state.
                    if (!keyVehicleUUID.equals(entity.uniqueUUID)) {
                        player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_KEY_WRONGKEY));
                    } else {
                        if (entity instanceof PartSeat) {
                            //Entity clicked is a seat, don't do locking changes, instead, change seat.
                            //Returning skip will make the seat-clicking code activate in the packet.
                            return CallbackType.SKIP;
                        } else if (entity.locked) {
                            //Unlock entity and process hitbox action if it's a closed door.
                            entity.toggleLock();
                            player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_KEY_UNLOCK));
                            if (hitBox.definition != null) {
                                if (hitBox.definition.variableName != null && !entity.isVariableActive(hitBox.definition.variableName) && hitBox.definition.variableName.startsWith("door")) {
                                    return CallbackType.SKIP;
                                }
                            }
                        } else {
                            //Lock vehicle.  Don't interact with hitbox unless it's NOT a door, as the locking code will close doors.
                            //If we skipped, we'd just re-open the closed door.
                            entity.toggleLock();
                            player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_KEY_LOCK));
                            if (hitBox.definition != null) {
                                if (hitBox.definition.variableName != null && entity.isVariableActive(hitBox.definition.variableName) && !hitBox.definition.variableName.startsWith("door")) {
                                    return CallbackType.SKIP;
                                }
                            }
                        }
                    }
                }
                return CallbackType.NONE;
            }
            case TICKET: {
                if (!entity.world.isClient() && rightClick) {
                    if (player.isSneaking()) {
                        if (entity instanceof PartSeat) {
                            if (entity.rider != null) {
                                entity.removeRider();
                            }
                        } else if (entity instanceof AEntityF_Multipart) {
                            for (APart otherPart : ((AEntityF_Multipart<?>) entity).allParts) {
                                if (otherPart.rider != null) {
                                    otherPart.removeRider();
                                }
                            }
                        }
                    } else {
                        entity.world.loadEntities(new BoundingBox(player.getPosition(), 8D, 8D, 8D), entity);
                    }
                }
                return CallbackType.NONE;
            }
            case FUEL_HOSE: {
                if (!entity.world.isClient() && rightClick) {
                    if (firstPartClicked == null) {
                        if (entity instanceof PartInteractable) {
                            PartInteractable interactable = (PartInteractable) entity;
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
                        if (entity instanceof PartInteractable) {
                            PartInteractable interactable = (PartInteractable) entity;
                            if (interactable.tank != null && !interactable.equals(firstPartClicked)) {
                                if (interactable.linkedPart == null && interactable.linkedVehicle == null) {
                                    if (interactable.position.isDistanceToCloserThan(firstPartClicked.position, 16)) {
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
                        } else if (entity instanceof EntityVehicleF_Physics) {
                            EntityVehicleF_Physics vehicle = (EntityVehicleF_Physics) entity;
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
                if (!entity.world.isClient() && rightClick) {
                    if (entity instanceof PartEngine) {
                        PartEngine engine = (PartEngine) entity;
                        if (engine.vehicleOn != null) {
                            if (engine.linkedEngine == null) {
                                if (firstEngineClicked == null) {
                                    firstEngineClicked = engine;
                                    player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_JUMPERCABLE_FIRSTLINK));
                                } else if (!firstEngineClicked.equals(engine)) {
                                    if (firstEngineClicked.vehicleOn.equals(engine.vehicleOn)) {
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
                }
                return CallbackType.NONE;
            }
            case JUMPER_PACK: {
                if (rightClick) {
                    if (entity instanceof EntityVehicleF_Physics) {
                        //Use jumper on vehicle.
                        ((EntityVehicleF_Physics) entity).electricPower = 12;
                        if (!entity.world.isClient()) {
                            InterfaceManager.packetInterface.sendToPlayer(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_VEHICLE_JUMPERPACK), player);
                            if (!player.isCreative()) {
                                player.getInventory().removeFromSlot(player.getHotbarIndex(), 1);
                            }
                            return CallbackType.ALL;
                        }
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
                    vehicle.engines.forEach(engine -> {
                        if (engine.isVariableActive(PartEngine.MAGNETO_VARIABLE)) {
                            engine.setVariable(PartEngine.MAGNETO_VARIABLE, 0);
                            InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableToggle(engine, PartEngine.MAGNETO_VARIABLE));
                        }
                    });
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
