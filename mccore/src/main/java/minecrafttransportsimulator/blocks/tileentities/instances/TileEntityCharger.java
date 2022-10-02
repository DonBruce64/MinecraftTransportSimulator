package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityEnergyCharger;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.jsondefs.JSONConfigSettings.ConfigFuel.FuelDefaults;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.systems.ConfigSystem;

public class TileEntityCharger extends ATileEntityFuelPump implements ITileEntityEnergyCharger {
    private static final String ENERGY_TYPE = FuelDefaults.ELECTRICITY.name().toLowerCase();

    private double amountInVehicleWhenConnected;
    private double amountDispensed;

    public TileEntityCharger(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, IWrapperNBT data) {
        super(world, position, placingPlayer, data);
    }

    @Override
    public void update() {
        super.update();
        if (connectedVehicle != null) {
            amountDispensed = connectedVehicle.fuelTank.getFluidLevel() - amountInVehicleWhenConnected;
        }
    }

    @Override
    protected PumpResult checkPump(EntityVehicleF_Physics vehicle) {
        //Assume fuel type matches, just check for electric engines instead.
        for (APart part : vehicle.parts) {
            if (part instanceof PartEngine) {
                if (part.definition.engine.fuelType.equals(ENERGY_TYPE)) {
                    return PumpResult.VALID;
                }
            }
        }
        return PumpResult.INVALID;
    }

    @Override
    public void setConnection(EntityVehicleF_Physics newVehicle) {
        super.setConnection(newVehicle);
        if (newVehicle != null) {
            amountInVehicleWhenConnected = connectedVehicle.fuelTank.getFluidLevel();
            amountDispensed = 0;
        }
    }

    @Override
    public int getChargeAmount() {
        double amount = connectedVehicle != null ? connectedVehicle.fuelTank.getMaxLevel() - connectedVehicle.fuelTank.getFluidLevel() : 0;
        if (amount > definition.decor.pumpRate) {
            amount = definition.decor.pumpRate;
        }
        return (int) (amount / ConfigSystem.settings.general.rfToElectricityFactor.value);
    }

    @Override
    public void chargeEnergy(int amount) {
        connectedVehicle.fuelTank.fill(connectedVehicle.fuelTank.getFluid(), amount * ConfigSystem.settings.general.rfToElectricityFactor.value, true);
    }

    @Override
    public double getRawVariableValue(String variable, float partialTicks) {
        switch (variable) {
            case ("charger_active"):
                return connectedVehicle != null ? 1 : 0;
            case ("charger_dispensed"):
                return amountDispensed;
            case ("charger_free"):
                return isCreative ? 1 : 0;
            case ("charger_purchased"):
                return fuelPurchasedRemaining;
        }

        return super.getRawVariableValue(variable, partialTicks);
    }
}
