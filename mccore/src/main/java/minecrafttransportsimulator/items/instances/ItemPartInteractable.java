package minecrafttransportsimulator.items.instances;

import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.AEntityCrafter;
import minecrafttransportsimulator.entities.instances.EntityFluidTank;
import minecrafttransportsimulator.entities.instances.EntityInventoryContainer;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.entities.instances.PartInteractable;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.items.components.IItemEntityInteractable;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPart.InteractableComponentType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketCrafterFuelAdd;
import minecrafttransportsimulator.packets.instances.PacketItemInteractable;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.LanguageSystem;

public class ItemPartInteractable extends AItemPart implements IItemEntityInteractable {

    public ItemPartInteractable(JSONPart definition, JSONSubDefinition subDefinition, String sourcePackID) {
        super(definition, subDefinition, sourcePackID);
    }

    @Override
    public boolean isPartValidForPackDef(JSONPartDefinition placementDefinition, JSONSubDefinition subDefinition, boolean checkMinMax) {
        return super.isPartValidForPackDef(placementDefinition, subDefinition, checkMinMax) && (!checkMinMax || (placementDefinition.minValue <= definition.interactable.inventoryUnits && placementDefinition.maxValue >= definition.interactable.inventoryUnits));
    }

    @Override
    public PartInteractable createPart(AEntityF_Multipart<?> entity, IWrapperPlayer placingPlayer, JSONPartDefinition packVehicleDef, IWrapperNBT partData) {
        return new PartInteractable(entity, placingPlayer, packVehicleDef, this, partData);
    }

    @Override
    public void addTooltipLines(List<String> tooltipLines, IWrapperNBT data) {
        super.addTooltipLines(tooltipLines, data);
        switch (definition.interactable.interactionType) {
            case CRATE: {
                tooltipLines.add(LanguageSystem.ITEMINFO_INTERACTABLE_CAPACITY.getCurrentValue() + definition.interactable.inventoryUnits * 9);
                break;
            }
            case BARREL: {
                tooltipLines.add(LanguageSystem.ITEMINFO_INTERACTABLE_CAPACITY.getCurrentValue() + definition.interactable.inventoryUnits * 10000 + "mb");
                IWrapperNBT tankData = data.getData("tank");
                if (tankData != null) {
                    String tankFluidName = tankData.getString("currentFluid");
                    double tankFluidLevel = tankData.getDouble("fluidLevel");
                    if (tankFluidLevel != 0) {
                        tooltipLines.add(LanguageSystem.ITEMINFO_INTERACTABLE_CONTENTS.getCurrentValue() + tankFluidName + ":" + tankFluidLevel + "mb");
                    }
                }
                break;
            }
            case JERRYCAN: {
                tooltipLines.add(LanguageSystem.ITEMINFO_JERRYCAN_FILL.getCurrentValue());
                tooltipLines.add(LanguageSystem.ITEMINFO_JERRYCAN_DRAIN.getCurrentValue());
                String jerrycanFluid = data.getString(PartInteractable.JERRYCAN_FLUID_NAME);
                if (jerrycanFluid.isEmpty()) {
                    tooltipLines.add(LanguageSystem.ITEMINFO_JERRYCAN_EMPTY.getCurrentValue());
                } else {
                    tooltipLines.add(LanguageSystem.ITEMINFO_JERRYCAN_CONTAINS.getCurrentValue() + InterfaceManager.clientInterface.getFluidName(jerrycanFluid, EntityFluidTank.WILDCARD_FLUID_MOD));
                }
                break;
            }
            case BATTERY: {
                tooltipLines.add(LanguageSystem.ITEMINFO_BATTERY_FILL.getCurrentValue());
                tooltipLines.add(LanguageSystem.ITEMINFO_BATTERY_DRAIN.getCurrentValue());
                if (data.getBoolean(PartInteractable.BATTERY_CHARGED_NAME)) {
                    tooltipLines.add(LanguageSystem.ITEMINFO_BATTERY_FULL.getCurrentValue());
                } else {
                    tooltipLines.add(LanguageSystem.ITEMINFO_BATTERY_EMPTY.getCurrentValue());
                }
                break;
            }
            default: {
                //Don't add tooltips for other things.
            }
        }
    }

    @Override
    public CallbackType doEntityInteraction(AEntityE_Interactable<?> entity, BoundingBox hitBox, IWrapperPlayer player, boolean rightClick) {
        if (definition.interactable.interactionType == InteractableComponentType.JERRYCAN) {
            if (!entity.world.isClient()) {
                if (rightClick) {
                    IWrapperItemStack stack = player.getHeldStack();
                    IWrapperNBT data = stack.getData();
                    String jerrrycanFluid = data != null ? data.getString(PartInteractable.JERRYCAN_FLUID_NAME) : "";

                    //If we clicked a tank part, attempt to pull from it rather than fill a vehicle.
                    //Unless this is a liquid crafter, in which case we fill that instead.
                    if (entity instanceof PartInteractable) {
                        EntityFluidTank tank = ((PartInteractable) entity).tank;
                        if (tank != null) {
                            if (jerrrycanFluid.isEmpty()) {
                                if (tank.getFluidLevel() >= 1000) {
                                    if (data == null) {
                                        data = InterfaceManager.coreInterface.getNewNBTWrapper();
                                    }
                                    data.setString(PartInteractable.JERRYCAN_FLUID_NAME, tank.getFluid());
                                    stack.setData(data);
                                    tank.drain(1000, true);
                                }
                            }
                        }

                        AEntityCrafter crafter = ((PartInteractable) entity).crafter;
                        if (crafter != null && !jerrrycanFluid.isEmpty()) {
                            if (ConfigSystem.settings.fuel.fuels.get(crafter.getFuelName()).containsKey(jerrrycanFluid)) {
                                //Packet assumes we add at 0, need to "fool" it.
                                int addedFuel = (int) (ConfigSystem.settings.fuel.fuels.get(crafter.getFuelName()).get(jerrrycanFluid) * 1000 * 20 * crafter.definition.crafterEfficiency);
                                int priorFuel = crafter.ticksLeftOfFuel;
                                crafter.ticksLeftOfFuel = addedFuel;
                                InterfaceManager.packetInterface.sendToAllClients(new PacketCrafterFuelAdd(crafter));
                                crafter.ticksLeftOfFuel += priorFuel;
                                crafter.ticksFuelProvides = crafter.ticksLeftOfFuel;

                                data.deleteEntry(PartInteractable.JERRYCAN_FLUID_NAME);
                                stack.setData(data);
                                player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_JERRYCAN_SUCCESS));
                            } else {
                                player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_JERRYCAN_WRONGTYPE));
                            }
                        }
                    } else if (!jerrrycanFluid.isEmpty()) {
                        if (entity instanceof EntityVehicleF_Physics) {
                            EntityVehicleF_Physics vehicle = (EntityVehicleF_Physics) entity;
                            switch (vehicle.checkFuelTankCompatibility(jerrrycanFluid)) {
                                case VALID: {
                                    if (vehicle.fuelTank.getFluidLevel() + 1000 > vehicle.fuelTank.getMaxLevel()) {
                                        player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_JERRYCAN_TOOFULL));
                                    } else {
                                        vehicle.fuelTank.fill(jerrrycanFluid, EntityFluidTank.WILDCARD_FLUID_MOD, 1000, true);
                                        data.deleteEntry(PartInteractable.JERRYCAN_FLUID_NAME);
                                        stack.setData(data);
                                        player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_JERRYCAN_SUCCESS));
                                    }
                                    break;
                                }
                                case INVALID: {
                                    player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_JERRYCAN_WRONGENGINES));
                                    break;
                                }
                                case MISMATCH: {
                                    player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_JERRYCAN_WRONGTYPE));
                                    break;
                                }
                                case NOENGINE: {
                                    player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_JERRYCAN_NOENGINE));
                                    break;
                                }
                            }
                        }
                    } else {
                        player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_JERRYCAN_EMPTY));
                    }
                }
            }
            return CallbackType.NONE;
        } else if (definition.interactable.interactionType == InteractableComponentType.BATTERY) {
            if (!entity.world.isClient()) {
                if (rightClick) {
                    IWrapperItemStack stack = player.getHeldStack();
                    IWrapperNBT data = stack.getData();
                    boolean batteryCharged = data.getBoolean(PartInteractable.BATTERY_CHARGED_NAME);

                    if (batteryCharged) {
                        if (entity instanceof EntityVehicleF_Physics) {
                            EntityVehicleF_Physics vehicle = (EntityVehicleF_Physics) entity;
                            switch (vehicle.checkFuelTankCompatibility(PartEngine.ELECTRICITY_FUEL)) {
                                case VALID: {
                                    if (vehicle.fuelTank.getFluidLevel() + 1000 > vehicle.fuelTank.getMaxLevel()) {
                                        player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_BATTERY_TOOFULL));
                                    } else {
                                        vehicle.fuelTank.fill(PartEngine.ELECTRICITY_FUEL, EntityFluidTank.WILDCARD_FLUID_MOD, 1000, true);
                                        data.deleteEntry(PartInteractable.BATTERY_CHARGED_NAME);
                                        stack.setData(data);
                                        player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_BATTERY_SUCCESS));
                                    }
                                    break;
                                }
                                case INVALID: {
                                    player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_BATTERY_WRONGENGINES));
                                    break;
                                }
                                case MISMATCH: {
                                    player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_BATTERY_WRONGENGINES));
                                    break;
                                }
                                case NOENGINE: {
                                    player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_BATTERY_NOENGINE));
                                    break;
                                }
                            }
                        }
                    } else {
                        player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_BATTERY_EMPTY));
                    }
                }
            }
            return CallbackType.NONE;
        }
        return CallbackType.SKIP;
    }

    @Override
    public boolean onUsed(AWrapperWorld world, IWrapperPlayer player) {
        if (definition.interactable.canBeOpenedInHand && definition.interactable.interactionType.equals(InteractableComponentType.CRATE) && player.isSneaking()) {
            if (!world.isClient()) {
                IWrapperNBT data = player.getHeldStack().getData();
                EntityInventoryContainer inventory = new EntityInventoryContainer(world, data != null ? data.getData("inventory") : null, (int) (definition.interactable.inventoryUnits * 9F), definition.interactable.inventoryStackSize > 0 ? definition.interactable.inventoryStackSize : 64);
                world.addEntity(inventory);
                player.sendPacket(new PacketItemInteractable(player, inventory, definition.interactable.inventoryTexture));
            }
            return true;
        } else {
            return false;
        }

    }

    public static final AItemPartCreator CREATOR = new AItemPartCreator() {
        @Override
        public boolean isCreatorValid(JSONPart definition) {
            return definition.generic.type.startsWith("interactable");
        }

        @Override
        public ItemPartInteractable createItem(JSONPart definition, JSONSubDefinition subDefinition, String sourcePackID) {
            return new ItemPartInteractable(definition, subDefinition, sourcePackID);
        }
    };
}
