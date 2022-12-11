package minecrafttransportsimulator.items.instances;

import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable.PlayerOwnerState;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.EntityFluidTank;
import minecrafttransportsimulator.entities.instances.EntityFurnace;
import minecrafttransportsimulator.entities.instances.EntityInventoryContainer;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartInteractable;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.items.components.IItemEntityInteractable;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPart.InteractableComponentType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketFurnaceFuelAdd;
import minecrafttransportsimulator.packets.instances.PacketItemInteractable;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.systems.ConfigSystem;

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
        return new PartInteractable(entity, placingPlayer, packVehicleDef, partData);
    }

    @Override
    public void addTooltipLines(List<String> tooltipLines, IWrapperNBT data) {
        super.addTooltipLines(tooltipLines, data);
        switch (definition.interactable.interactionType) {
            case CRATE: {
                tooltipLines.add(JSONConfigLanguage.ITEMINFO_INTERACTABLE_CAPACITY.value + definition.interactable.inventoryUnits * 9);
                break;
            }
            case BARREL: {
                tooltipLines.add(JSONConfigLanguage.ITEMINFO_INTERACTABLE_CAPACITY.value + definition.interactable.inventoryUnits * 10000 + "mb");
                break;
            }
            case JERRYCAN: {
                tooltipLines.add(JSONConfigLanguage.ITEMINFO_JERRYCAN_FILL.value);
                tooltipLines.add(JSONConfigLanguage.ITEMINFO_JERRYCAN_DRAIN.value);
                String jerrycanFluid = data.getString("jerrycanFluid");
                if (jerrycanFluid.isEmpty()) {
                    tooltipLines.add(JSONConfigLanguage.ITEMINFO_JERRYCAN_EMPTY.value);
                } else {
                    tooltipLines.add(JSONConfigLanguage.ITEMINFO_JERRYCAN_CONTAINS.value + InterfaceManager.clientInterface.getFluidName(jerrycanFluid));
                }
                break;
            }
            default: {
                //Don't add tooltips for other things.
            }
        }
    }

    @Override
    public CallbackType doEntityInteraction(AEntityE_Interactable<?> entity, BoundingBox hitBox, IWrapperPlayer player, PlayerOwnerState ownerState, boolean rightClick) {
        if (definition.interactable.interactionType.equals(InteractableComponentType.JERRYCAN)) {
            if (!entity.world.isClient()) {
                if (rightClick) {
                    IWrapperItemStack stack = player.getHeldStack();
                    IWrapperNBT data = stack.getData();
                    String jerrrycanFluid = data.getString("jerrycanFluid");

                    //If we clicked a tank part, attempt to pull from it rather than fill a vehicle.
                    //Unless this is a liquid furnace, in which case we fill that instead.
                    if (entity instanceof PartInteractable) {
                        EntityFluidTank tank = ((PartInteractable) entity).tank;
                        if (tank != null) {
                            if (jerrrycanFluid.isEmpty()) {
                                if (tank.getFluidLevel() >= 1000) {
                                    data.setString("jerrycanFluid", tank.getFluid());
                                    stack.setData(data);
                                    tank.drain(tank.getFluid(), 1000, true);
                                }
                            }
                        }

                        EntityFurnace furnace = ((PartInteractable) entity).furnace;
                        if (furnace != null && !jerrrycanFluid.isEmpty()) {
                            if (ConfigSystem.settings.fuel.fuels.get(EntityFurnace.FURNACE_FUEL_NAME).containsKey(jerrrycanFluid)) {
                                //Packet assumes we add at 0, need to "fool" it.
                                int addedFuel = (int) (ConfigSystem.settings.fuel.fuels.get(EntityFurnace.FURNACE_FUEL_NAME).get(jerrrycanFluid) * 1000 * 20 * furnace.definition.furnaceEfficiency);
                                int priorFuel = furnace.ticksLeftOfFuel;
                                furnace.ticksLeftOfFuel = addedFuel;
                                InterfaceManager.packetInterface.sendToAllClients(new PacketFurnaceFuelAdd(furnace));
                                furnace.ticksLeftOfFuel += priorFuel;
                                furnace.ticksAddedOfFuel = furnace.ticksLeftOfFuel;

                                data.setString("jerrycanFluid", "");
                                stack.setData(data);
                                player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_JERRYCAN_SUCCESS));
                            } else {
                                player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_JERRYCAN_WRONGTYPE));
                            }
                        }
                    } else if (!jerrrycanFluid.isEmpty()) {
                        if (entity instanceof EntityVehicleF_Physics) {
                            EntityVehicleF_Physics vehicle = (EntityVehicleF_Physics) entity;
                            switch (vehicle.checkFuelTankCompatibility(jerrrycanFluid)) {
                                case VALID: {
                                    if (vehicle.fuelTank.getFluidLevel() + 1000 > vehicle.fuelTank.getMaxLevel()) {
                                        player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_JERRYCAN_TOOFULL));
                                    } else {
                                        vehicle.fuelTank.fill(jerrrycanFluid, 1000, true);
                                        data.setString("jerrycanFluid", "");
                                        stack.setData(data);
                                        player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_JERRYCAN_SUCCESS));
                                    }
                                    break;
                                }
                                case INVALID: {
                                    player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_JERRYCAN_WRONGENGINES));
                                    break;
                                }
                                case MISMATCH: {
                                    player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_JERRYCAN_WRONGTYPE));
                                    break;
                                }
                            }
                        }
                    } else {
                        player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_JERRYCAN_EMPTY));
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
                EntityInventoryContainer inventory = new EntityInventoryContainer(world, player.getHeldStack().getData().getDataOrNew("inventory"), (int) (definition.interactable.inventoryUnits * 9F));
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
