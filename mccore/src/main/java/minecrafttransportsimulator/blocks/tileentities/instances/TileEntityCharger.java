package minecrafttransportsimulator.blocks.tileentities.instances;

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
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.LanguageSystem;

public class TileEntityCharger extends ATileEntityFuelPump implements ITileEntityEnergyCharger {

    public int internalBuffer;

    //Maintain one "bucket" worth of fuel to allow batteries to charge from the charger.
    private static final int MAX_BUFFER = (int) (1000 / ConfigSystem.settings.general.rfToElectricityFactor.value);

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
    public void fuelVehicle(double amount) {
        if (!isCreative) {
            double amountPurchasedRemaining = fuelPurchased - fuelDispensedThisPurchase;
            if (amount > amountPurchasedRemaining) {
                amount = (int) amountPurchasedRemaining;
            }
        }
        int bufferToUse = (int) (amount / ConfigSystem.settings.general.rfToElectricityFactor.value);
        if (bufferToUse > internalBuffer) {
            bufferToUse = internalBuffer;
        }
        amount = bufferToUse * ConfigSystem.settings.general.rfToElectricityFactor.value;
        internalBuffer -= bufferToUse;
        double amountFilled = connectedVehicle.fuelTank.fill(PartEngine.ELECTRICITY_FUEL, amount, true);
        fuelDispensedThisConnection += amountFilled;
        fuelDispensedThisPurchase += amount;
    }

    @Override
    public int getChargeAmount() {
        return MAX_BUFFER - internalBuffer;
    }

    @Override
    public void chargeEnergy(int amount) {
        internalBuffer += amount;
    }

    @Override
    public double getRawVariableValue(String variable, float partialTicks) {
        switch (variable) {
            case ("charger_active"):
                return connectedVehicle != null ? 1 : 0;
            case ("charger_dispensed"):
                return fuelDispensedThisConnection;
            case ("charger_free"):
                return isCreative ? 1 : 0;
            case ("charger_purchased"):
                return fuelPurchased;
            case ("charger_vehicle_percentage"):
                return connectedVehicle != null ? connectedVehicle.fuelTank.getFluidLevel() / connectedVehicle.fuelTank.getMaxLevel() : 0;
        }

        return super.getRawVariableValue(variable, partialTicks);
    }
}
