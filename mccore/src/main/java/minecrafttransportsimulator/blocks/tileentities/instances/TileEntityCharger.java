package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityEnergyCharger;
import minecrafttransportsimulator.entities.instances.AEntityVehicleE_Powered.FuelTankResult;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.entities.instances.PartInteractable;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.instances.ItemDecor;
import minecrafttransportsimulator.items.instances.ItemPartInteractable;
import minecrafttransportsimulator.jsondefs.JSONPart.EngineType;
import minecrafttransportsimulator.jsondefs.JSONPart.InteractableComponentType;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketTileEntityChargerBattery;
import minecrafttransportsimulator.systems.LanguageSystem;

public class TileEntityCharger extends ATileEntityFuelPump implements ITileEntityEnergyCharger {

    public double internalBuffer;

    //Maintain one "bucket" worth of fuel to allow batteries to charge from the charger.
    private static final int MAX_BUFFER = 1000;

    public TileEntityCharger(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, ItemDecor item, IWrapperNBT data) {
        super(world, position, placingPlayer, item, data);
    }

    @Override
    public boolean interact(IWrapperPlayer player) {
        //Check if the item is a battery.
        IWrapperItemStack stack = player.getHeldStack();
        AItemBase item = stack.getItem();
        if (item instanceof ItemPartInteractable) {
            ItemPartInteractable interactable = (ItemPartInteractable) item;
            if (interactable.definition.interactable.interactionType == InteractableComponentType.BATTERY) {
                if (internalBuffer == MAX_BUFFER) {
                    IWrapperNBT data = stack.getData();
                    if (data == null) {
                        data = InterfaceManager.coreInterface.getNewNBTWrapper();
                    }
                    if (!data.getBoolean(PartInteractable.BATTERY_CHARGED_NAME)) {
                        data.setBoolean(PartInteractable.BATTERY_CHARGED_NAME, true);
                        stack.setData(data);
                        internalBuffer = 0;
                        InterfaceManager.packetInterface.sendToAllClients(new PacketTileEntityChargerBattery(this));
                        player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_BATTERY_CHARGED));
                    }
                } else {
                    player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_BATTERY_CHARGERLOW));
                }
                return true;
            }
        }

        //Special cases checked, do normal cases.
        return super.interact(player);
    }

    @Override
    protected boolean hasFuel() {
        return internalBuffer > 0;
    }

    @Override
    protected FuelTankResult checkPump(EntityVehicleF_Physics vehicle) {
        //Just check that the engines are electric. If so, the vehicle will have the right fuel.
        for (APart part : vehicle.allParts) {
            if (part instanceof PartEngine) {
                if (part.definition.engine.type == EngineType.ELECTRIC) {
                    return FuelTankResult.VALID;
                }
            }
        }
        return FuelTankResult.INVALID;
    }

    @Override
    public double fuelVehicle(double amount) {
        if (amount > internalBuffer) {
            amount = internalBuffer;
        }
        amount = connectedVehicle.fuelTank.fill(PartEngine.ELECTRICITY_FUEL, amount, true);
        internalBuffer -= amount;
        return amount;
    }

    @Override
    public double getChargeAmount() {
        double amount = MAX_BUFFER - internalBuffer;
        //Don't let the charger fill the buffer to charge things if we haven't purchased fuel.
        if (!isCreative) {
            double amountPurchasedRemaining = fuelPurchased - fuelDispensedThisPurchase - internalBuffer;
            if (amount > amountPurchasedRemaining) {
                amount = amountPurchasedRemaining;
            }
        }
        return amount;
    }

    @Override
    public void chargeEnergy(double amount) {
        internalBuffer += amount;
    }

    @Override
    public ComputedVariable createComputedVariable(String variable, boolean createDefaultIfNotPresent) {
        switch (variable) {
            case ("charger_active"):
                return new ComputedVariable(this, variable, partialTicks -> connectedVehicle != null ? 1 : 0, false);
            case ("charger_dispensed"):
                return new ComputedVariable(this, variable, partialTicks -> fuelDispensedThisConnection, false);
            case ("charger_free"):
                return new ComputedVariable(this, variable, partialTicks -> isCreative ? 1 : 0, false);
            case ("charger_purchased"):
                return new ComputedVariable(this, variable, partialTicks -> fuelPurchased, false);
            case ("charger_vehicle_percentage"):
                return new ComputedVariable(this, variable, partialTicks -> connectedVehicle != null ? connectedVehicle.fuelTank.getFluidLevel() / connectedVehicle.fuelTank.getMaxLevel() : 0, false);
            default:
                return super.createComputedVariable(variable, createDefaultIfNotPresent);
        }
    }
}
