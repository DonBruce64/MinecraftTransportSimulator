package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityFluidTankProvider;
import minecrafttransportsimulator.entities.instances.AEntityVehicleE_Powered.FuelTankResult;
import minecrafttransportsimulator.entities.instances.EntityFluidTank;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.instances.ItemDecor;
import minecrafttransportsimulator.items.instances.ItemPartInteractable;
import minecrafttransportsimulator.jsondefs.JSONPart.InteractableComponentType;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.systems.LanguageSystem;

public class TileEntityFuelPump extends ATileEntityFuelPump implements ITileEntityFluidTankProvider {
    private final EntityFluidTank tank;

    public TileEntityFuelPump(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, ItemDecor item, IWrapperNBT data) {
        super(world, position, placingPlayer, item, data);
        this.tank = new EntityFluidTank(world, data != null ? data.getData("tank") : null, definition.decor.fuelCapacity) {
            @Override
            public double fill(String fluid, String fluidMod, double maxAmount, boolean doFill) {
                //Block filling of the tank if we haven't purchased anything.
                if (!isCreative && !world.isClient()) {
                    double amountPurchasedRemaining = fuelPurchased - fuelDispensedThisPurchase - getFluidLevel();
                    if (maxAmount > amountPurchasedRemaining) {
                        maxAmount = amountPurchasedRemaining;
                    }
                }
                return super.fill(fluid, fluidMod, maxAmount, doFill);
            }
        };
        world.addEntity(tank);
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
                if (tank.getFluidLevel() >= 1000) {
                    IWrapperNBT data = stack.getData();
                    if (data == null) {
                        data = InterfaceManager.coreInterface.getNewNBTWrapper();
                    }
                    if (data.getString("jerrycanFluid").isEmpty()) {
                        data.setString("jerrycanFluid", tank.getFluid());
                        stack.setData(data);
                        tank.drain(1000, true);
                    }
                }
                return true;
            }
        }

        //Special cases checked, do normal cases.
        return super.interact(player);
    }

    @Override
    public IWrapperItemStack getStack() {
        //Add fuel data to the stack we return.  We know we'll have data here since it's in super.
        IWrapperItemStack stack = super.getStack();
        IWrapperNBT data = stack.getData();
        data.setData("tank", tank.save(InterfaceManager.coreInterface.getNewNBTWrapper()));
        stack.setData(data);
        return stack;
    }

    @Override
    public void remove() {
        super.remove();
        tank.remove();
    }

    @Override
    protected boolean hasFuel() {
        return tank.getFluidLevel() > 0;
    }

    @Override
    protected FuelTankResult checkPump(EntityVehicleF_Physics vehicle) {
        return vehicle.checkFuelTankCompatibility(tank.getFluid());
    }

    @Override
    public double fuelVehicle(double amount) {
        //If we have room for fuel, try to add it to the vehicle.
        if (amount > 0 && tank.getFluidLevel() > 0) {
            double amountToFill = connectedVehicle.fuelTank.fill(tank.getFluid(), EntityFluidTank.WILDCARD_FLUID_MOD, amount, false);
            if (amountToFill > 0) {
                //Keep fluid reference since if we drain the tank fully it won't persist to fill the vehicle tank.
                String fluid = tank.getFluid();
                double amountToDrain = tank.drain(amountToFill, true);
                connectedVehicle.fuelTank.fill(fluid, EntityFluidTank.WILDCARD_FLUID_MOD, amountToDrain, true);
                return amountToDrain;
            }
        } else {
            //No more fuel in tank.  Disconnect vehicle.
            playerUsing.sendPacket(new PacketPlayerChatMessage(playerUsing, LanguageSystem.INTERACT_FUELPUMP_EMPTY));
            setConnection(null);
        }
        return 0;
    }

    @Override
    public EntityFluidTank getTank() {
        return tank;
    }

    @Override
    public ComputedVariable createComputedVariable(String variable, boolean createDefaultIfNotPresent) {
        switch (variable) {
            case ("fuelpump_active"):
                return new ComputedVariable(this, variable, partialTicks -> connectedVehicle != null ? 1 : 0, false);
            case ("fuelpump_dispensed"):
                return new ComputedVariable(this, variable, partialTicks -> fuelDispensedThisConnection, false);
            case ("fuelpump_free"):
                return new ComputedVariable(this, variable, partialTicks -> isCreative ? 1 : 0, false);
            case ("fuelpump_purchased"):
                return new ComputedVariable(this, variable, partialTicks -> fuelPurchased, false);
            case ("fuelpump_stored"):
                return new ComputedVariable(this, variable, partialTicks -> tank.getFluidLevel(), false);
            default:
                if (variable.startsWith("fuelpump_fluid_")) {
                    final String fluidName = variable.substring(variable.lastIndexOf("_") + 1);
                    return new ComputedVariable(this, variable, partialTicks -> tank.getFluid().equals(fluidName) ? 1 : 0, false);
                } else {
                    return super.createComputedVariable(variable, createDefaultIfNotPresent);
                }
        }
    }

    @Override
    public String getRawTextVariableValue(JSONText textDef, float partialTicks) {
        if (textDef.variableName.equals("fuelpump_fluid")) {
            return tank.getFluidLevel() > 0 ? InterfaceManager.clientInterface.getFluidName(tank.getFluid(), tank.getFluidMod()) : "";
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
