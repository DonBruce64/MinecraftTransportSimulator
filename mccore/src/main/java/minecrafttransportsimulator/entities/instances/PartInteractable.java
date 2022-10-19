package minecrafttransportsimulator.entities.instances;

import java.util.HashSet;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage.LanguageEntry;
import minecrafttransportsimulator.jsondefs.JSONPart.InteractableComponentType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketFurnaceFuelAdd;
import minecrafttransportsimulator.packets.instances.PacketPartInteractable;
import minecrafttransportsimulator.packets.instances.PacketPartInteractableInteract;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.systems.ConfigSystem;

public final class PartInteractable extends APart {
    public final EntityFurnace furnace;
    public final EntityInventoryContainer inventory;
    public final EntityFluidTank tank;
    public String jerrycanFluid;
    public PartInteractable linkedPart;
    public EntityVehicleF_Physics linkedVehicle;
    public Set<IWrapperPlayer> playersInteracting = new HashSet<>();

    public PartInteractable(AEntityF_Multipart<?> entityOn, IWrapperPlayer placingPlayer, JSONPartDefinition placementDefinition, IWrapperNBT data) {
        super(entityOn, placingPlayer, placementDefinition, data);
        if (definition.interactable.interactionType.equals(InteractableComponentType.FURNACE)) {
            this.furnace = new EntityFurnace(world, data.getDataOrNew("furnace"), definition.interactable);
            this.inventory = furnace;
            world.addEntity(furnace);
        } else {
            this.furnace = null;
            if (definition.interactable.interactionType.equals(InteractableComponentType.CRATE)) {
                this.inventory = new EntityInventoryContainer(world, data.getDataOrNew("inventory"), (int) (definition.interactable.inventoryUnits * 9F));
                world.addEntity(inventory);
            } else {
                this.inventory = null;
            }
        }
        if (definition.interactable.interactionType.equals(InteractableComponentType.BARREL)) {
            this.tank = new EntityFluidTank(world, data.getDataOrNew("tank"), (int) definition.interactable.inventoryUnits * 10000);
            world.addEntity(tank);
        } else {
            this.tank = null;
        }
        this.jerrycanFluid = data.getString("jerrycanFluid");
    }

    @Override
    public boolean interact(IWrapperPlayer player) {
        if (!masterEntity.locked) {
            if (definition.interactable.interactionType.equals(InteractableComponentType.CRATE) || definition.interactable.interactionType.equals(InteractableComponentType.CRAFTING_BENCH) || definition.interactable.interactionType.equals(InteractableComponentType.FURNACE)) {
                player.sendPacket(new PacketPartInteractable(this, player));
            } else if (definition.interactable.interactionType.equals(InteractableComponentType.CRAFTING_TABLE)) {
                player.openCraftingGUI();
            } else if (definition.interactable.interactionType.equals(InteractableComponentType.JERRYCAN)) {
                entityOn.removePart(this, null);
                IWrapperNBT data = InterfaceManager.coreInterface.getNewNBTWrapper();
                save(data);
                world.spawnItem(getItem(), data, position);
            } else if (tank != null) {
                player.getHeldStack().interactWith(tank, player);
            }
        } else {
            player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_VEHICLE_LOCKED));
        }
        return true;
    }

    @Override
    public LanguageEntry checkForRemoval() {
        if (!definition.interactable.canBeOpenedInHand && getMass() > definition.generic.mass) {
            return JSONConfigLanguage.INTERACT_VEHICLE_CANTREMOVEINVENTORY;
        } else {
            return super.checkForRemoval();
        }
    }

    @Override
    public void attack(Damage damage) {
        super.attack(damage);
        if (!damage.isWater && (damage.amount > 25 || damageAmount > definition.general.health)) {
            destroy(damage.box);
        }
    }

    @Override
    public void destroy(BoundingBox box) {
        double explosivePower = getExplosiveContribution();
        if (explosivePower > 0 && isValid) {
            super.destroy(box);
            if (definition.interactable.hasBlowoutPanels) {
                world.spawnExplosion(position, 0.25, false);
            } else {
                world.spawnExplosion(position, explosivePower, true);
                entityOn.destroy(boundingBox);
            }
        } else {
            super.destroy(box);
        }
    }

    @Override
    public void update() {
        super.update();
        if (furnace != null) {
            furnace.update();
            //Only look for fuel when we're processing and don't have any.
            if (!world.isClient() && furnace.ticksLeftOfFuel == 0 && furnace.ticksLeftToSmelt > 0) {
                addFurnaceFuel();
            }
            if (vehicleOn != null) {
                vehicleOn.electricUsage += furnace.powerToDrawPerTick;
            }
        }

        //Check to see if we are linked and need to send fluid to the linked tank.
        //Only do checks on the server.  Clients get packets.
        if (!world.isClient()) {
            EntityFluidTank linkedTank = null;
            LanguageEntry linkedMessage = null;
            if (linkedVehicle != null) {
                if (!linkedVehicle.position.isDistanceToCloserThan(position, 16)) {
                    linkedMessage = JSONConfigLanguage.INTERACT_FUELHOSE_LINKDROPPED;
                } else {
                    linkedTank = linkedVehicle.fuelTank;
                }
            } else if (linkedPart != null) {
                if (linkedPart.position.isDistanceToCloserThan(position, 16)) {
                    linkedMessage = JSONConfigLanguage.INTERACT_FUELHOSE_LINKDROPPED;
                } else {
                    linkedTank = linkedPart.tank;
                }
            }

            //If we have a linked tank to transfer to, do so now.
            if (linkedTank != null) {
                String fluidToTransfer = tank.getFluid();
                if (!fluidToTransfer.isEmpty()) {
                    double amountToTransfer = linkedTank.fill(fluidToTransfer, 10, false);
                    if (amountToTransfer > 0) {
                        amountToTransfer = tank.drain(fluidToTransfer, amountToTransfer, true);
                        if (amountToTransfer > 0) {
                            linkedTank.fill(fluidToTransfer, amountToTransfer, true);
                        } else {
                            linkedMessage = JSONConfigLanguage.INTERACT_FUELHOSE_TANKEMPTY;
                        }
                    } else {
                        linkedMessage = JSONConfigLanguage.INTERACT_FUELHOSE_TANKFULL;
                    }
                } else {
                    linkedMessage = JSONConfigLanguage.INTERACT_FUELHOSE_TANKEMPTY;
                }
            }

            //If we have an error message, display it an null our our linkings.
            if (linkedMessage != null) {
                linkedVehicle = null;
                linkedPart = null;
                for (IWrapperEntity entity : world.getEntitiesWithin(new BoundingBox(position, 16, 16, 16))) {
                    if (entity instanceof IWrapperPlayer) {
                        ((IWrapperPlayer) entity).sendPacket(new PacketPlayerChatMessage((IWrapperPlayer) entity, linkedMessage));
                    }
                }
            }
        }
        
        //Verify interacting players are still interacting.
        //Server checks if players are still interacting, client checks if current player doesn't have a GUI open.
        if (!playersInteracting.isEmpty()) {
            if (world.isClient()) {
                IWrapperPlayer thisClient = InterfaceManager.clientInterface.getClientPlayer();
                if (playersInteracting.contains(thisClient) && !InterfaceManager.clientInterface.isGUIOpen()) {
                    InterfaceManager.packetInterface.sendToServer(new PacketPartInteractableInteract(this, thisClient, false));
                    playersInteracting.remove(thisClient);
                }
            } else {
                for (IWrapperPlayer player : playersInteracting) {
                    if (!player.isValid() || !player.getWorld().equals(world)) {
                        InterfaceManager.packetInterface.sendToServer(new PacketPartInteractableInteract(this, player, false));
                        playersInteracting.remove(player);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Helper method to check for fuels for furnaces and add them.
     * Only call on the server-side, except for electric furnaces.
     */
    private void addFurnaceFuel() {
        //Try to fill the furnace with the appropriate fuel type, if we have it.
        switch (furnace.definition.furnaceType) {
            case STANDARD: {
                IWrapperItemStack currentFuel = furnace.getStack(EntityFurnace.FUEL_ITEM_SLOT);
                if (currentFuel.isEmpty() || currentFuel.getMaxSize() < currentFuel.getSize()) {
                    //Try to find a matching burnable item from the entity.
                    for (APart part : linkedParts) {
                        if (part instanceof PartInteractable) {
                            if (part.isActive && part.definition.interactable.feedsVehicles && part.definition.interactable.interactionType.equals(InteractableComponentType.CRATE)) {
                                PartInteractable crate = (PartInteractable) part;
                                for (int i = 0; i < crate.inventory.getSize(); ++i) {
                                    IWrapperItemStack stack = crate.inventory.getStack(i);
                                    if (stack.getFuelValue() != 0 && (currentFuel.isEmpty() || stack.isCompleteMatch(currentFuel))) {
                                        furnace.ticksAddedOfFuel = stack.getFuelValue();
                                        furnace.ticksLeftOfFuel = furnace.ticksAddedOfFuel;
                                        crate.inventory.removeFromSlot(i, 1);
                                        InterfaceManager.packetInterface.sendToAllClients(new PacketFurnaceFuelAdd(furnace));
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
                break;
            }
            case FUEL: {
                //Try to find a barrel with fuel in it.
                for (APart part : linkedParts) {
                    if (part instanceof PartInteractable) {
                        if (part.isActive && part.definition.interactable.feedsVehicles && part.definition.interactable.interactionType.equals(InteractableComponentType.BARREL)) {
                            PartInteractable barrel = (PartInteractable) part;
                            if (barrel.tank.getFluidLevel() > 0 && ConfigSystem.settings.fuel.fuels.get(EntityFurnace.FURNACE_FUEL_NAME).containsKey(barrel.tank.getFluid())) {
                                furnace.ticksAddedOfFuel = (int) (ConfigSystem.settings.fuel.fuels.get(EntityFurnace.FURNACE_FUEL_NAME).get(barrel.tank.getFluid()) * 20 * furnace.definition.furnaceEfficiency);
                                furnace.ticksLeftOfFuel = furnace.ticksAddedOfFuel;
                                barrel.tank.drain(barrel.tank.getFluid(), 1, true);
                                InterfaceManager.packetInterface.sendToAllClients(new PacketFurnaceFuelAdd(furnace));
                            }
                        }
                    }
                }
                break;
            }
            case ELECTRIC: {
                //Reduce electric power from vehicle, if we can.
                furnace.powerToDrawPerTick = 0;
                if (vehicleOn != null && vehicleOn.electricPower > 1) {
                    int ticksToDrawPower = (int) (500 * furnace.definition.furnaceEfficiency);
                    furnace.powerToDrawPerTick = 1D / ticksToDrawPower;
                    furnace.ticksAddedOfFuel = ticksToDrawPower;
                    furnace.ticksLeftOfFuel = furnace.ticksAddedOfFuel;
                    InterfaceManager.packetInterface.sendToAllClients(new PacketFurnaceFuelAdd(furnace));
                }
                break;
            }
        }
    }

    @Override
    public double getMass() {
        //Return our mass, plus our inventory or tank.
        double currentMass = super.getMass();
        if (inventory != null) {
            currentMass += inventory.getMass();
        } else if (tank != null) {
            currentMass += tank.getMass();
        }
        return currentMass;
    }

    @Override
    public double getRawVariableValue(String variable, float partialTicks) {
        switch (variable) {
            case ("interactable_count"): {
                if (inventory != null) {
                    return inventory.getCount();
                } else {
                    return 0;
                }
            }
            case ("interactable_percent"): {
                if (inventory != null) {
                    return inventory.getCount() / (double) inventory.getSize();
                } else if (tank != null) {
                    return tank.getFluidLevel() / tank.getMaxLevel();
                } else {
                    return 0;
                }
            }
            case ("interactable_capacity"): {
                if (inventory != null) {
                    return inventory.getSize();
                } else if (tank != null) {
                    return tank.getMaxLevel() / 1000;
                } else {
                    return 0;
                }
            }
            case ("interactable_active"): {
                return !playersInteracting.isEmpty() ? 1 : 0;
            }
        }

        return super.getRawVariableValue(variable, partialTicks);
    }

    /**
     * Gets the explosive power of this part.  Used when it is blown up or attacked.
     * For our calculations, only ammo is checked.  While we could check for fuel, we assume
     * that fuel-containing items are stable enough to not blow up when this container is hit.
     */
    public double getExplosiveContribution() {
        if (inventory != null) {
            return inventory.getExplosiveness();
        } else if (tank != null) {
            return tank.getExplosiveness();
        } else {
            return 0;
        }
    }

    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        if (furnace != null) {
            data.setData("furnace", furnace.save(InterfaceManager.coreInterface.getNewNBTWrapper()));
        } else if (inventory != null) {
            data.setData("inventory", inventory.save(InterfaceManager.coreInterface.getNewNBTWrapper()));
        } else if (tank != null) {
            data.setData("tank", tank.save(InterfaceManager.coreInterface.getNewNBTWrapper()));
        } else if (definition.interactable.interactionType.equals(InteractableComponentType.JERRYCAN)) {
            data.setString("jerrycanFluid", jerrycanFluid);
        }
        return data;
    }
}
