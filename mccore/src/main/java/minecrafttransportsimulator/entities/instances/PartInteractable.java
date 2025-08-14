package minecrafttransportsimulator.entities.instances;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.items.instances.ItemPartInteractable;
import minecrafttransportsimulator.jsondefs.JSONPart.InteractableComponentType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketCrafterFuelAdd;
import minecrafttransportsimulator.packets.instances.PacketEntityInteractGUI;
import minecrafttransportsimulator.packets.instances.PacketPartInteractable;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.LanguageSystem;
import minecrafttransportsimulator.systems.LanguageSystem.LanguageEntry;

public final class PartInteractable extends APart {
    public final AEntityCrafter crafter;
    public final EntityInventoryContainer inventory;
    public final EntityFluidTank tank;
    public String jerrycanFluid;
    public boolean batteryCharged;
    public PartInteractable linkedPart;
    public EntityVehicleF_Physics linkedVehicle;

    public static final String JERRYCAN_FLUID_NAME = "jerrycanFluid";
    public static final String BATTERY_CHARGED_NAME = "batteryCharged";

    public PartInteractable(AEntityF_Multipart<?> entityOn, IWrapperPlayer placingPlayer, JSONPartDefinition placementDefinition, ItemPartInteractable item, IWrapperNBT data) {
        super(entityOn, placingPlayer, placementDefinition, item, data);
        switch (definition.interactable.interactionType) {
            case FURNACE: {
                this.crafter = new EntityFurnace(world, data != null ? data.getData("crafter") : null, definition.interactable);
                this.inventory = crafter;
                this.tank = null;
                world.addEntity(crafter);
                break;
            }
            case BREWER: {
                this.crafter = new EntityBrewer(world, data != null ? data.getData("crafter") : null, definition.interactable);
                this.inventory = crafter;
                this.tank = null;
                world.addEntity(crafter);
                break;
            }
            case CRATE: {
                this.crafter = null;
                this.inventory = new EntityInventoryContainer(world, data != null ? data.getData("inventory") : null, (int) (definition.interactable.inventoryUnits * 9F), definition.interactable.inventoryStackSize > 0 ? definition.interactable.inventoryStackSize : 64);
                this.tank = null;
                world.addEntity(inventory);
                break;
            }
            case BARREL: {
                this.crafter = null;
                this.inventory = null;
                this.tank = new EntityFluidTank(world, data != null ? data.getData("tank") : null, (int) (definition.interactable.inventoryUnits * 10000));
                world.addEntity(tank);
                break;
            }
            case JERRYCAN: {
                if (data != null) {
                    this.jerrycanFluid = data.getString(JERRYCAN_FLUID_NAME);
                } else {
                    this.jerrycanFluid = "";
                }
                this.crafter = null;
                this.inventory = null;
                this.tank = null;
                break;
            }
            case BATTERY: {
                if (data != null) {
                    this.batteryCharged = data.getBoolean(BATTERY_CHARGED_NAME);
                } else {
                    this.batteryCharged = false;
                }
                //No break statement here, fall-down to default to null things.
            }
            default: {
                this.crafter = null;
                this.inventory = null;
                this.tank = null;
            }
        }
    }

    @Override
    public boolean interact(IWrapperPlayer player) {
        if (vehicleOn == null || !vehicleOn.lockedVar.isActive) {
            switch (definition.interactable.interactionType) {
                case CRATE:
                case CRAFTING_BENCH:
                case FURNACE:
                case BREWER: {
                    player.sendPacket(new PacketPartInteractable(this, player));
                    break;
                }
                case CRAFTING_TABLE: {
                    player.openCraftingGUI();
                    playersInteracting.add(player);
                    InterfaceManager.packetInterface.sendToAllClients(new PacketEntityInteractGUI(this, player, true));
                    break;
                }
                case JERRYCAN:
                case BATTERY: {
                    remove();
                    world.spawnItemStack(getStack(), position, null);
                    break;
                }
                case BARREL: {
                    player.getHeldStack().interactWith(tank, player);
                    break;
                }
            }
        } else {
            player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_VEHICLE_LOCKED));
        }
        return true;
    }

    @Override
    public LanguageEntry checkForRemoval(IWrapperPlayer player) {
        if (!definition.generic.canBeRemovedByHand && inventory != null && getMass() > definition.generic.mass) {
            return LanguageSystem.INTERACT_PARTREMOVE_INVENTORY;
        } else {
            return super.checkForRemoval(player);
        }
    }

    @Override
    public void attack(Damage damage) {
        super.attack(damage);
        if (!damage.isWater && damage.amount > 25) {
            destroy(damage.box);
        }
    }

    @Override
    public void remove() {
        super.remove();
        if (inventory != null) {
            inventory.remove();
        }
        if (tank != null) {
            tank.remove();
        }
    }

    @Override
    public void destroy(BoundingBox box) {
        double explosivePower = getExplosiveContribution();
        if (explosivePower > 0 && isValid) {
            super.destroy(box);
            if (!definition.interactable.hasBlowoutPanels) {
                masterEntity.destroy(masterEntity.boundingBox);
                if (ConfigSystem.settings.damage.vehicleExplosions.value) {
                    world.spawnExplosion(position, explosivePower, ConfigSystem.settings.damage.vehicleBlockBreaking.value, ConfigSystem.settings.damage.vehicleBlockBreaking.value);
                } else {
                    world.spawnExplosion(position, 0F, false, false);
                }
            }
        } else {
            super.destroy(box);
        }
    }

    @Override
    public void update() {
        super.update();
        if (crafter != null) {
            crafter.update();
            if (crafter.ticksLeftToCraft > 0) {
                //Only look for fuel when we're processing and don't have any.
                if (!world.isClient() && crafter.ticksLeftOfFuel == 0) {
                    addCrafterFuel();
                }
                if (vehicleOn != null) {
                    vehicleOn.electricUsage += crafter.powerToDrawPerTick;
                }
            }
        }

        //Check to see if we are linked and need to send fluid to the linked tank.
        //Only do checks on the server.  Clients get packets.
        if (!world.isClient()) {
            EntityFluidTank linkedTank = null;
            LanguageEntry linkedMessage = null;
            if (linkedVehicle != null) {
                if (!linkedVehicle.isValid || !linkedVehicle.position.isDistanceToCloserThan(position, 16)) {
                    linkedMessage = LanguageSystem.INTERACT_FUELHOSE_LINKDROPPED;
                } else {
                    linkedTank = linkedVehicle.fuelTank;
                }
            } else if (linkedPart != null) {
                if (!linkedPart.isValid || !linkedPart.position.isDistanceToCloserThan(position, 16)) {
                    linkedMessage = LanguageSystem.INTERACT_FUELHOSE_LINKDROPPED;
                } else {
                    linkedTank = linkedPart.tank;
                }
            }

            //If we have a linked tank to transfer to, do so now.
            if (linkedTank != null) {
                String fluidToTransfer = tank.getFluid();
                String fluidModToTransfer = tank.getFluidMod();
                if (!fluidToTransfer.isEmpty()) {
                    double amountToTransfer = linkedTank.fill(fluidToTransfer, fluidModToTransfer, 10, false);
                    if (amountToTransfer > 0) {
                        amountToTransfer = tank.drain(fluidToTransfer, fluidModToTransfer, amountToTransfer, true);
                        if (amountToTransfer > 0) {
                            linkedTank.fill(fluidToTransfer, fluidModToTransfer, amountToTransfer, true);
                        } else {
                            linkedMessage = LanguageSystem.INTERACT_FUELHOSE_TANKEMPTY;
                        }
                    } else {
                        linkedMessage = LanguageSystem.INTERACT_FUELHOSE_TANKFULL;
                    }
                } else {
                    linkedMessage = LanguageSystem.INTERACT_FUELHOSE_TANKEMPTY;
                }
            }

            //If we have an error message, display it an null our our linkings.
            if (linkedMessage != null) {
                linkedVehicle = null;
                linkedPart = null;
                for (IWrapperPlayer player : world.getPlayersWithin(new BoundingBox(position, 16, 16, 16))) {
                    player.sendPacket(new PacketPlayerChatMessage(player, linkedMessage));
                }
            }
        }
    }

    /**
     * Helper method to check for fuels for crafters and add them.
     * Only call on the server-side, except for electric crafters.
     */
    private void addCrafterFuel() {
        //Try to fill the furnace with the appropriate fuel type, if we have it.
        switch (crafter.definition.crafterType) {
            case STANDARD: {
                IWrapperItemStack currentFuel = crafter.getStack(AEntityCrafter.FUEL_ITEM_SLOT);
                if (currentFuel.isEmpty() || currentFuel.getMaxSize() < currentFuel.getSize()) {
                    //Try to find a matching burnable item from the entity.
                    for (APart part : linkedParts) {
                        if (part instanceof PartInteractable) {
                            if (part.isActive && part.definition.interactable.feedsVehicles && part.definition.interactable.interactionType == InteractableComponentType.CRATE) {
                                PartInteractable crate = (PartInteractable) part;
                                for (int i = 0; i < crate.inventory.getSize(); ++i) {
                                    IWrapperItemStack stack = crate.inventory.getStack(i);
                                    if (crafter.getFuelTime(stack) != 0 && (currentFuel.isEmpty() || stack.isCompleteMatch(currentFuel))) {
                                        crafter.ticksFuelProvides = stack.getFurnaceFuelValue();
                                        crafter.ticksLeftOfFuel = crafter.ticksFuelProvides;
                                        crate.inventory.removeFromSlot(i, 1);
                                        InterfaceManager.packetInterface.sendToAllClients(new PacketCrafterFuelAdd(crafter));
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
                        if (part.isActive && part.definition.interactable.feedsVehicles && part.definition.interactable.interactionType == InteractableComponentType.BARREL) {
                            PartInteractable barrel = (PartInteractable) part;
                            if (barrel.tank.getFluidLevel() > 0 && ConfigSystem.settings.fuel.fuels.get(EntityFurnace.FURNACE_FUEL_NAME).containsKey(barrel.tank.getFluid())) {
                                crafter.ticksFuelProvides = (int) (ConfigSystem.settings.fuel.fuels.get(EntityFurnace.FURNACE_FUEL_NAME).get(barrel.tank.getFluid()) * 20 * crafter.definition.crafterEfficiency);
                                crafter.ticksLeftOfFuel = crafter.ticksFuelProvides;
                                barrel.tank.drain(1, true);
                                InterfaceManager.packetInterface.sendToAllClients(new PacketCrafterFuelAdd(crafter));
                            }
                        }
                    }
                }
                break;
            }
            case ELECTRIC: {
                //Reduce electric power from vehicle, if we can.
                crafter.powerToDrawPerTick = 0;
                if (vehicleOn != null && vehicleOn.electricPower > 1) {
                    int ticksToDrawPower = (int) (500 * crafter.definition.crafterEfficiency);
                    crafter.powerToDrawPerTick = 1D / ticksToDrawPower;
                    crafter.ticksFuelProvides = ticksToDrawPower;
                    crafter.ticksLeftOfFuel = crafter.ticksFuelProvides;
                    InterfaceManager.packetInterface.sendToAllClients(new PacketCrafterFuelAdd(crafter));
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
    public ComputedVariable createComputedVariable(String variable, boolean createDefaultIfNotPresent) {
        switch (variable) {
            case ("interactable_count_stacks"):
                return new ComputedVariable(this, variable, partialTicks -> inventory != null ? inventory.cachedStackCount : 0, false);
            case ("interactable_count_items"):
                return new ComputedVariable(this, variable, partialTicks -> inventory != null ? inventory.cachedItemCount : 0, false);
            case ("interactable_percent"):
                return new ComputedVariable(this, variable, partialTicks -> inventory != null ? inventory.cachedStackCount / (double) inventory.getSize() : (tank != null ? tank.getFluidLevel() / tank.getMaxLevel() : 0), false);
            case ("interactable_capacity"):
                return new ComputedVariable(this, variable, partialTicks -> inventory != null ? inventory.getSize() : (tank != null ? tank.getMaxLevel() / 1000 : 0), false);
            case ("interactable_active"):
                return new ComputedVariable(this, variable, partialTicks -> !playersInteracting.isEmpty() ? 1 : 0, false);
            case ("interactable_fuel"):
                return new ComputedVariable(this, variable, partialTicks -> crafter != null ? crafter.ticksLeftOfFuel : 0, false);
            case ("interactable_remaining"):
                return new ComputedVariable(this, variable, partialTicks -> crafter != null ? crafter.ticksLeftToCraft : 0, false);
            default:
                if (variable.startsWith("interactable_fluid_")) {
                    final String fluidName = variable.substring(variable.lastIndexOf("_") + 1);
                    return new ComputedVariable(this, variable, partialTicks -> tank.getFluid().equals(fluidName) ? 1 : 0, false);
                } else {
                    return super.createComputedVariable(variable, createDefaultIfNotPresent);
                }
        }
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
        if (crafter != null) {
            data.setData("crafter", crafter.save(InterfaceManager.coreInterface.getNewNBTWrapper()));
        } else if (inventory != null) {
            data.setData("inventory", inventory.save(InterfaceManager.coreInterface.getNewNBTWrapper()));
        } else if (tank != null) {
            data.setData("tank", tank.save(InterfaceManager.coreInterface.getNewNBTWrapper()));
        } else if (definition.interactable.interactionType == InteractableComponentType.JERRYCAN) {
            data.setString(JERRYCAN_FLUID_NAME, jerrycanFluid);
        } else if (definition.interactable.interactionType == InteractableComponentType.BATTERY) {
            data.setBoolean(BATTERY_CHARGED_NAME, batteryCharged);
        }
        return data;
    }
}
