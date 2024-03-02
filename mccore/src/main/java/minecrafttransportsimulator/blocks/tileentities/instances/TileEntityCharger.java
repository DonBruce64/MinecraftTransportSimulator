package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityEnergyCharger;
import minecrafttransportsimulator.entities.instances.AEntityVehicleE_Powered.FuelTankResult;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.items.instances.ItemDecor;
import minecrafttransportsimulator.jsondefs.JSONPart.EngineType;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.systems.ConfigSystem;

public class TileEntityCharger extends ATileEntityFuelPump implements ITileEntityEnergyCharger {

    private int internalBuffer;

    public TileEntityCharger(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, ItemDecor item, IWrapperNBT data) {
        super(world, position, placingPlayer, item, data);
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
    public void fuelVehicle(double amount) {
        int bufferToUse = (int) (amount / ConfigSystem.settings.general.rfToElectricityFactor.value);
        if (bufferToUse > internalBuffer) {
            bufferToUse = internalBuffer;
        }
        amount = bufferToUse * ConfigSystem.settings.general.rfToElectricityFactor.value;
        internalBuffer -= bufferToUse;
        double amountFilled = connectedVehicle.fuelTank.fill(PartEngine.ELECTRICITY_FUEL, amount, true);
        fuelDispensedThisConnection += amountFilled;
    }

    @Override
    public int getChargeAmount() {
        int maxAmount = 200;
        if (!isCreative) {
            double amountPurchasedRemaining = fuelPurchased - fuelDispensedThisPurchase;
            if (maxAmount > amountPurchasedRemaining) {
                maxAmount = (int) amountPurchasedRemaining;
            }
        }
        return maxAmount - internalBuffer;
    }

    @Override
    public void chargeEnergy(int amount) {
        internalBuffer += amount;
        fuelDispensedThisPurchase += amount;
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
