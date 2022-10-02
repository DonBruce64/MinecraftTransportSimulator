package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityFluidTankProvider;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityFluidTank;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.instances.ItemPartInteractable;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONPart.InteractableComponentType;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketTileEntityFuelPumpConnection;
import minecrafttransportsimulator.systems.ConfigSystem;

public class TileEntityFuelPump extends ATileEntityFuelPump implements ITileEntityFluidTankProvider {
    private final EntityFluidTank tank;

    public TileEntityFuelPump(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, IWrapperNBT data) {
        super(world, position, placingPlayer, data);
        this.tank = new EntityFluidTank(world, data.getDataOrNew("tank"), definition.decor.fuelCapacity) {
            @Override
            public double fill(String fluid, double maxAmount, boolean doFill) {
                if (!isCreative) {
                    //We are a pump with a set cost, ensure we have purchased fuel.
                    //Make sure to add a small amount to ensure that the pump displays the name of the fluid in it.
                    if (fuelPurchasedRemaining == 0 && connectedVehicle == null && getFluidLevel() == 0) {
                        maxAmount = 1;
                    } else if (maxAmount > fuelPurchasedRemaining) {
                        maxAmount = fuelPurchasedRemaining;
                    }
                    double amountFilled = super.fill(fluid, maxAmount, doFill);
                    if (doFill && fuelPurchasedRemaining > 0) {
                        fuelPurchasedRemaining -= amountFilled;
                    }
                    return amountFilled;
                }
                return super.fill(fluid, maxAmount, doFill);
            }
        };
        world.addEntity(tank);
    }

    @Override
    public void update() {
        super.update();

        //Do fuel checks.  Fuel checks only occur on servers.  Clients get packets for state changes.
        if (connectedVehicle != null && !world.isClient()) {
            //If we have room for fuel, try to add it to the vehicle.
            if (isCreative ? tank.getFluidLevel() > 0 : (fuelPurchasedRemaining > 0 || tank.getFluidLevel() > 1)) {
                double amountToFill = connectedVehicle.fuelTank.fill(tank.getFluid(), definition.decor.pumpRate, false);
                if (amountToFill > 0) {
                    double amountToDrain = tank.drain(tank.getFluid(), amountToFill, false);
                    connectedVehicle.fuelTank.fill(tank.getFluid(), amountToDrain, true);
                    tank.drain(tank.getFluid(), amountToDrain, true);
                }
            } else {
                //No more fuel in tank or purchased.  Disconnect vehicle.
                setConnection(null);
                InterfaceManager.packetInterface.sendToAllClients(new PacketTileEntityFuelPumpConnection(this));
                for (IWrapperEntity entity : world.getEntitiesWithin(new BoundingBox(position, 16, 16, 16))) {
                    if (entity instanceof IWrapperPlayer) {
                        ((IWrapperPlayer) entity).sendPacket(new PacketPlayerChatMessage((IWrapperPlayer) entity, JSONConfigLanguage.INTERACT_FUELPUMP_EMPTY));
                    }
                }
            }
        }
    }

    @Override
    public boolean interact(IWrapperPlayer player) {
        //If we are holding a fluid-holding item, interact with the pump.
        if (player.getHeldStack().interactWith(tank, player)) {
            return true;
        }

        //Check if the item is a jerrycan.
        IWrapperItemStack stack = player.getHeldStack();
        AItemBase item = stack.getItem();
        if (item instanceof ItemPartInteractable) {
            ItemPartInteractable interactable = (ItemPartInteractable) item;
            if (interactable.definition.interactable.interactionType.equals(InteractableComponentType.JERRYCAN)) {
                IWrapperNBT data = stack.getData();
                if (data.getString("jerrycanFluid").isEmpty()) {
                    if (tank.getFluidLevel() >= 1000) {
                        data.setString("jerrycanFluid", tank.getFluid());
                        stack.setData(data);
                        tank.drain(tank.getFluid(), 1000, true);
                    }
                }
                return true;
            }
        }

        //Special cases checked, do normal cases.
        return super.interact(player);
    }

    @Override
    protected PumpResult checkPump(EntityVehicleF_Physics vehicle) {
        //Check to make sure this vehicle can take this fuel pump's fuel type.
        if (!vehicle.fuelTank.getFluid().isEmpty()) {
            if (!tank.getFluid().equals(vehicle.fuelTank.getFluid())) {
                return PumpResult.MISMATCH;
            }
        }

        //Fuel type can be taken by vehicle, check to make sure engines can take it.
        for (APart part : vehicle.parts) {
            if (part instanceof PartEngine) {
                if (ConfigSystem.settings.fuel.fuels.get(part.definition.engine.fuelType).containsKey(tank.getFluid())) {
                    return PumpResult.VALID;
                }
            }
        }
        return PumpResult.MISMATCH;
    }

    @Override
    public void setConnection(EntityVehicleF_Physics newVehicle) {
        super.setConnection(newVehicle);
        if (newVehicle != null) {
            tank.resetAmountDispensed();
        }
    }

    @Override
    public EntityFluidTank getTank() {
        return tank;
    }

    @Override
    public double getRawVariableValue(String variable, float partialTicks) {
        switch (variable) {
            case ("fuelpump_active"):
                return connectedVehicle != null ? 1 : 0;
            case ("fuelpump_stored"):
                return tank.getFluidLevel();
            case ("fuelpump_dispensed"):
                return tank.getAmountDispensed();
            case ("fuelpump_free"):
                return isCreative ? 1 : 0;
            case ("fuelpump_purchased"):
                return fuelPurchasedRemaining;
        }

        return super.getRawVariableValue(variable, partialTicks);
    }

    @Override
    public String getRawTextVariableValue(JSONText textDef, float partialTicks) {
        if (textDef.variableName.equals("fuelpump_fluid")) {
            return tank.getFluidLevel() > 0 ? InterfaceManager.clientInterface.getFluidName(tank.getFluid()) : "";
        }

        return super.getRawTextVariableValue(textDef, partialTicks);
    }

    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        data.setData("tank", tank.save(InterfaceManager.coreInterface.getNewNBTWrapper()));
        return data;
    }
}
