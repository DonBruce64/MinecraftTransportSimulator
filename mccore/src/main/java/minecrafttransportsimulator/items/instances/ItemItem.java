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
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.entities.instances.PartInteractable;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.IItemEntityInteractable;
import minecrafttransportsimulator.items.components.IItemFood;
import minecrafttransportsimulator.jsondefs.JSONItem;
import minecrafttransportsimulator.jsondefs.JSONItem.ItemComponentType;
import minecrafttransportsimulator.jsondefs.JSONPotionEffect;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityGUIRequest;
import minecrafttransportsimulator.packets.instances.PacketEntityKeyChange;
import minecrafttransportsimulator.packets.instances.PacketGUIRequest;
import minecrafttransportsimulator.packets.instances.PacketPartEngine;
import minecrafttransportsimulator.packets.instances.PacketPartInteractable;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.LanguageSystem;
import minecrafttransportsimulator.systems.LanguageSystem.LanguageEntry;

public class ItemItem extends AItemPack<JSONItem> implements IItemEntityInteractable, IItemFood {
    /**Current page of this item, if it's a booklet.  Kept here locally as only one item class is constructed for each booklet definition.**/
    public int pageNumber;
    public List<LanguageEntry> languageTitle;
    public List<LanguageEntry> languagePageTitle;
    public List<List<LanguageEntry>> languagePageText;

    /**First engine clicked for jumper cable items.  Kept here locally as only one item class is constructed for each jumper cable definition.**/
    private static PartEngine firstEngineClicked;
    /**First part clicked for fuel hose items.  Kept here locally as only one item class is constructed for each jumper cable definition.**/
    private static PartInteractable firstPartClicked;

    public static final String KEY_UUID_TAG = "keyUUID";

    public ItemItem(JSONItem definition) {
        super(definition, null);
    }

    @Override
    public boolean canBreakBlocks() {
        return !(definition.item.type.equals(ItemComponentType.WRENCH) || definition.item.type.equals(ItemComponentType.SCREWDRIVER));
    }

    @Override
    public void addTooltipLines(List<String> tooltipLines, IWrapperNBT data) {
        super.addTooltipLines(tooltipLines, data);
        switch (definition.item.type) {
            case SCANNER: {
                tooltipLines.add(LanguageSystem.ITEMINFO_SCANNER.getCurrentValue());
                break;
            }
            case WRENCH: {
                tooltipLines.add(LanguageSystem.ITEMINFO_WRENCH.getCurrentValue());
                break;
            }
            case SCREWDRIVER: {
                tooltipLines.add(LanguageSystem.ITEMINFO_SCREWDRIVER.getCurrentValue());
                break;
            }
            case PAINT_GUN: {
                tooltipLines.add(LanguageSystem.ITEMINFO_PAINTGUN.getCurrentValue());
                break;
            }
            case KEY: {
                tooltipLines.add(LanguageSystem.ITEMINFO_KEY.getCurrentValue());
                break;
            }
            case TICKET: {
                tooltipLines.add(LanguageSystem.ITEMINFO_TICKET.getCurrentValue());
                break;
            }
            case FUEL_HOSE: {
                tooltipLines.add(LanguageSystem.ITEMINFO_FUELHOSE.getCurrentValue());
                break;
            }
            case JUMPER_CABLES: {
                tooltipLines.add(LanguageSystem.ITEMINFO_JUMPERCABLES.getCurrentValue());
                break;
            }
            case JUMPER_PACK: {
                tooltipLines.add(LanguageSystem.ITEMINFO_JUMPERPACK.getCurrentValue());
                break;
            }
            case REPAIR_PACK: {
                tooltipLines.add(LanguageSystem.ITEMINFO_REPAIRPACK.getCurrentValue() + definition.repair.amount + " HP");
                if (definition.repair.canRepairTotaled) {
                    tooltipLines.add(LanguageSystem.ITEMINFO_REPAIRPACK_UNTOTAL.getCurrentValue());
                }
                break;
            }
            case Y2K_BUTTON: {
                tooltipLines.add(LanguageSystem.ITEMINFO_Y2KBUTTON.getCurrentValue());
                break;
            }
            default: //Do nothing.
        }
    }

    @Override
    public CallbackType doEntityInteraction(AEntityE_Interactable<?> entity, BoundingBox hitBox, IWrapperPlayer player, boolean rightClick) {
        EntityVehicleF_Physics vehicle = entity instanceof EntityVehicleF_Physics ? (EntityVehicleF_Physics) entity : (entity instanceof APart ? ((APart) entity).vehicleOn : null);
        switch (definition.item.type) {
	    	case WRENCH: 
	    	case SCREWDRIVER: {
                if (!entity.world.isClient()) {
                    //If the vehicle isn't unlocked, or the player isn't OP, they can't interact with it.
                    if (vehicle == null || !vehicle.lockedVar.isActive) {
                        if (rightClick) {
                            //Right-clicking opens GUIs.
                            if (player.isSneaking()) {
                                //If we clicked a part without text, use the master entity instead.  This allows players to click wheels and engines.
                                if (entity instanceof APart && entity.text.isEmpty()) {
                                    player.sendPacket(new PacketEntityGUIRequest(((APart) entity).masterEntity, player, PacketEntityGUIRequest.EntityGUIType.TEXT_EDITOR));
                                } else {
                                    player.sendPacket(new PacketEntityGUIRequest(entity, player, PacketEntityGUIRequest.EntityGUIType.TEXT_EDITOR));
                                }
                            } else if (vehicle != null) {
                                if (ConfigSystem.settings.general.devMode.value && vehicle.allParts.contains(player.getEntityRiding())) {
                                    player.sendPacket(new PacketEntityGUIRequest(vehicle, player, PacketEntityGUIRequest.EntityGUIType.PACK_EXPORTER));
                                } else if (!vehicle.allParts.contains(player.getEntityRiding())) {
                                    player.sendPacket(new PacketEntityGUIRequest(vehicle, player, PacketEntityGUIRequest.EntityGUIType.INSTRUMENTS));
                                }
                            }
                        } else {
                            //Left clicking removes parts, or removes vehicles, if we were sneaking.
                            if(player.isSneaking()) {
                                if(vehicle != null) {
                                    if ((!ConfigSystem.settings.general.opPickupVehiclesOnly.value || player.isOP()) && (!ConfigSystem.settings.general.creativePickupVehiclesOnly.value || player.isCreative()) && entity.isValid) {
                                        vehicle.disconnectAllConnections();
                                        vehicle.world.spawnItemStack(vehicle.getStack(), hitBox.globalCenter, null);
                                        vehicle.remove();
                                    }
                                }
                            }else {
                                if (entity instanceof APart) {
                                    APart part = (APart) entity;
                                    if (!part.isPermanent && part.isValid) {
                                        LanguageEntry partResult = part.checkForRemoval(player);
                                        if (partResult != null) {
                                            player.sendPacket(new PacketPlayerChatMessage(player, partResult));
                                            return CallbackType.NONE;
                                        } else {
                                            //Player can remove part, spawn item in the world and remove part.
                                            part.entityOn.world.spawnItemStack(part.getStack(), part.position, null);
                                            part.entityOn.removePart(part, true, null);
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_VEHICLE_LOCKED));
                    }
                }
                return CallbackType.NONE;
            }
            case PAINT_GUN: {
                if (!entity.world.isClient() && rightClick) {
                    if (vehicle == null || !vehicle.lockedVar.isActive) {
                        player.sendPacket(new PacketEntityGUIRequest(entity, player, PacketEntityGUIRequest.EntityGUIType.PAINT_GUN));
                    } else {
                        player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_VEHICLE_LOCKED));
                    }
                }
                return CallbackType.NONE;
            }
            case KEY: {
                if (vehicle != null) {
                    if (rightClick && !entity.world.isClient()) {
                        //Try to lock the entity.
                        //First check to see if we need to set this key's entity.
                        IWrapperItemStack stack = player.getHeldStack();
                        IWrapperNBT data = stack.getData();
                        UUID keyUUID = data != null ? data.getUUID(KEY_UUID_TAG) : null;
                        if (keyUUID == null) {
                            if (vehicle.keyUUID != null && !player.isOP()) {
                                player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_KEY_HASKEY));
                            } else {
                                keyUUID = UUID.randomUUID();
                                vehicle.keyUUID = keyUUID;
                                data = InterfaceManager.coreInterface.getNewNBTWrapper();
                                data.setUUID(KEY_UUID_TAG, keyUUID);
                                stack.setData(data);
                                player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_KEY_BIND));
                                InterfaceManager.packetInterface.sendToAllClients(new PacketEntityKeyChange(vehicle, keyUUID));
                            }
                            return CallbackType.NONE;
                        }

                        //Try to lock or unlock this entity.
                        //If we succeed, send callback to clients to change locked state.
                        if (!keyUUID.equals(vehicle.keyUUID)) {
                            player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_KEY_WRONGKEY));
                        } else {
                            if (entity instanceof PartSeat) {
                                //Entity clicked is a seat, don't do locking changes, instead, change seat.
                                //Returning skip will make the seat-clicking code activate in the packet.
                                return CallbackType.SKIP;
                            } else if (vehicle.lockedVar.isActive) {
                                //Unlock entity and process hitbox action if it's a closed door.
                                vehicle.toggleLock();
                                player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_KEY_UNLOCK));
                                if (hitBox.definition != null) {
                                    if (hitBox.definition.variableName != null && !entity.getVariable(hitBox.definition.variableName).isActive && hitBox.definition.variableName.startsWith("door")) {
                                        return CallbackType.SKIP;
                                    }
                                }
                            } else {
                                //Lock vehicle.  Don't interact with hitbox unless it's NOT a door, as the locking code will close doors.
                                //If we skipped, we'd just re-open the closed door.
                                vehicle.toggleLock();
                                player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_KEY_LOCK));
                                if (hitBox.definition != null) {
                                    if (hitBox.definition.variableName != null && entity.getVariable(hitBox.definition.variableName).isActive && !hitBox.definition.variableName.startsWith("door")) {
                                        return CallbackType.SKIP;
                                    }
                                }
                            }
                        }
                    }
                }
                return CallbackType.NONE;
            }
            case TICKET: {
                if (!entity.world.isClient() && rightClick) {
                    if (entity instanceof PartSeat) {
                        if (player.isSneaking()) {
                            if (entity.rider != null) {
                                entity.removeRider();
                            }
                        } else {
                            if (entity.rider == null) {
                                entity.world.loadEntities(new BoundingBox(player.getPosition(), 8D, 8D, 8D), entity);
                            }
                        }
                    } else {
                        AEntityF_Multipart<?> master;
                        if (entity instanceof APart) {
                            master = ((APart) entity).masterEntity;
                        } else {
                            master = (AEntityF_Multipart<?>) entity;
                        }
                        if (player.isSneaking()) {
                            for (APart otherPart : master.allParts) {
                                if (otherPart.rider != null) {
                                    otherPart.removeRider();
                                }
                            }
                        } else {
                            master.world.loadEntities(new BoundingBox(player.getPosition(), 8D, 8D, 8D), master);
                        }
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
                                    player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_FUELHOSE_FIRSTLINK));
                                } else {
                                    player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_FUELHOSE_ALREADYLINKED));
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
                                            player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_FUELHOSE_SECONDLINK));
                                            firstPartClicked = null;
                                        } else {
                                            firstPartClicked = null;
                                            player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_FUELHOSE_DIFFERENTFLUIDS));
                                        }
                                    } else {
                                        firstPartClicked = null;
                                        player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_FUELHOSE_TOOFAR));
                                    }
                                } else {
                                    firstPartClicked = null;
                                    player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_FUELHOSE_ALREADYLINKED));
                                }
                            }
                        } else if (vehicle != null) {
                            if (vehicle.position.isDistanceToCloserThan(firstPartClicked.position, 16)) {
                                if (vehicle.fuelTank.getFluid().isEmpty() || firstPartClicked.tank.getFluid().isEmpty() || vehicle.fuelTank.getFluid().equals(firstPartClicked.tank.getFluid())) {
                                    firstPartClicked.linkedVehicle = vehicle;
                                    InterfaceManager.packetInterface.sendToAllClients(new PacketPartInteractable(firstPartClicked, player));
                                    player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_FUELHOSE_SECONDLINK));
                                    firstPartClicked = null;
                                } else {
                                    firstPartClicked = null;
                                    player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_FUELHOSE_DIFFERENTFLUIDS));
                                }
                            } else {
                                firstPartClicked = null;
                                player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_FUELHOSE_TOOFAR));
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
                                    player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_JUMPERCABLE_FIRSTLINK));
                                } else if (!firstEngineClicked.equals(engine)) {
                                    if (firstEngineClicked.vehicleOn.equals(engine.vehicleOn)) {
                                        firstEngineClicked = null;
                                        player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_JUMPERCABLE_SAMEVEHICLE));
                                    } else if (engine.position.isDistanceToCloserThan(firstEngineClicked.position, 15)) {
                                        engine.linkedEngine = firstEngineClicked;
                                        firstEngineClicked.linkedEngine = engine;
                                        InterfaceManager.packetInterface.sendToAllClients(new PacketPartEngine(engine, firstEngineClicked));
                                        InterfaceManager.packetInterface.sendToAllClients(new PacketPartEngine(firstEngineClicked, engine));
                                        firstEngineClicked = null;
                                        player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_JUMPERCABLE_SECONDLINK));
                                    } else {
                                        firstEngineClicked = null;
                                        player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_JUMPERCABLE_TOOFAR));
                                    }
                                }
                            } else {
                                player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_JUMPERCABLE_ALREADYLINKED));
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
                            InterfaceManager.packetInterface.sendToPlayer(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_VEHICLE_JUMPERPACK), player);
                            if (!player.isCreative()) {
                                player.getInventory().removeFromSlot(player.getHotbarIndex(), 1);
                            }
                            return CallbackType.ALL;
                        }
                    }
                }
                return CallbackType.NONE;
            }
            case REPAIR_PACK: {
                if (rightClick && !entity.world.isClient()) {
                    if (entity instanceof APart) {
                        entity = ((APart) entity).vehicleOn;
                    }
                    if (vehicle != null) {
                        if (vehicle.repairCooldownTicks == 0) {
                            if (!vehicle.outOfHealth || definition.repair.canRepairTotaled) {
                                if (entity.damageVar.currentValue == 0) {
                                    InterfaceManager.packetInterface.sendToPlayer(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_REPAIR_NONEED), player);
                                    return CallbackType.NONE;
                                } else {
                                    double amountRepaired = definition.repair.amount;
                                    if (vehicle.damageVar.currentValue < amountRepaired) {
                                        amountRepaired = vehicle.damageVar.currentValue;
                                    }
                                    double newDamage = vehicle.damageVar.currentValue - amountRepaired;
                                    vehicle.damageVar.setTo(newDamage, true);
                                    vehicle.repairCooldownTicks = 200;
                                    InterfaceManager.packetInterface.sendToPlayer(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_REPAIR_PASS, new Object[] { amountRepaired, entity.definition.general.health - newDamage, entity.definition.general.health }), player);
                                    if (!player.isCreative()) {
                                        player.getInventory().removeFromSlot(player.getHotbarIndex(), 1);
                                    }
                                }
                            } else {
                                InterfaceManager.packetInterface.sendToPlayer(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_REPAIR_TOTALED), player);
                            }
                        } else {
                            InterfaceManager.packetInterface.sendToPlayer(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_REPAIR_TOOSOON), player);
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
        if (definition.item.type == ItemComponentType.PAINT_GUN) {
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
        } else if (definition.item.type.equals(ItemComponentType.BOOKLET)) {
            if (!world.isClient()) {
                player.sendPacket(new PacketGUIRequest(player, PacketGUIRequest.GUIType.BOOKLET));
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
                    vehicle.throttleVar.setTo(0, true);
                    vehicle.parkingBrakeVar.setTo(0, true);
                    vehicle.engines.forEach(engine -> engine.magnetoVar.setTo(0, true));
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
