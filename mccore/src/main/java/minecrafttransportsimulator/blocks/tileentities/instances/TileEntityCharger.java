package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityEnergyCharger;
import minecrafttransportsimulator.entities.instances.AEntityVehicleE_Powered.FuelTankResult;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.jsondefs.JSONPart.EngineType;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.systems.ConfigSystem;

public class TileEntityCharger extends ATileEntityFuelPump implements ITileEntityEnergyCharger {

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
        connectedVehicle.fuelTank.fill(PartEngine.ELECTRICITY_FUEL, amount * ConfigSystem.settings.general.rfToElectricityFactor.value, true);
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
            case ("charger_vehicle_percentage"):
                return connectedVehicle != null ? connectedVehicle.fuelTank.getFluidLevel() / connectedVehicle.fuelTank.getMaxLevel() : 0;
        }

        return super.getRawVariableValue(variable, partialTicks);
    }
}
