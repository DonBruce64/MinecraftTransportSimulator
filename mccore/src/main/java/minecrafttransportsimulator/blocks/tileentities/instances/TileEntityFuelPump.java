package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.baseclasses.BoundingBox;
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
import minecrafttransportsimulator.packets.instances.PacketTileEntityFuelPumpConnection;
import minecrafttransportsimulator.systems.LanguageSystem;

public class TileEntityFuelPump extends ATileEntityFuelPump implements ITileEntityFluidTankProvider {
    private final EntityFluidTank tank;

    public TileEntityFuelPump(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, ItemDecor item, IWrapperNBT data) {
        super(world, position, placingPlayer, item, data);
        this.tank = new EntityFluidTank(world, data != null ? data.getData("tank") : null, definition.decor.fuelCapacity) {
            @Override
            public double fill(String fluid, double maxAmount, boolean doFill) {
                double amountFilled = maxAmount;
                if (!isCreative) {
                    double amountPurchasedRemaining = fuelPurchased - fuelDispensedThisPurchase;
                    if (maxAmount > amountPurchasedRemaining) {
                        maxAmount = amountPurchasedRemaining;
                    }
                }
                amountFilled = super.fill(fluid, maxAmount, doFill);
                if (doFill) {
                    fuelDispensedThisPurchase += amountFilled;
                }
                return amountFilled;
            }

            @Override
            public double drain(String fluid, double maxAmount, boolean doDrain) {
                double drained = super.drain(fluid, maxAmount, doDrain);
                fuelDispensedThisConnection += drained;
                return drained;
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
    public void fuelVehicle(double amount) {
        //Do fuel checks.  Fuel checks only occur on servers.  Clients get packets for state changes.
        if (!world.isClient()) {
            //If we have room for fuel, try to add it to the vehicle.
            if (amount > 0 && tank.getFluidLevel() > 0) {
                double amountToFill = connectedVehicle.fuelTank.fill(tank.getFluid(), amount, false);
                if (amountToFill > 0) {
                    //Keep fluid reference since if we drain the tank fully it won't persist to fill the vehicle tank.
                    String fluid = tank.getFluid();
                    double amountToDrain = tank.drain(tank.getFluid(), amountToFill, true);
                    connectedVehicle.fuelTank.fill(fluid, amountToDrain, true);
                }
            } else {
                //No more fuel in tank or purchased.  Disconnect vehicle.
                setConnection(null);
                InterfaceManager.packetInterface.sendToAllClients(new PacketTileEntityFuelPumpConnection(this));
                for (IWrapperPlayer player : world.getPlayersWithin(new BoundingBox(position, 16, 16, 16))) {
                    player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_FUELPUMP_EMPTY));
                }
            }
        }
    }

    @Override
    public EntityFluidTank getTank() {
        return tank;
    }

    @Override
    public ComputedVariable createComputedVariable(String variable) {
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
                return super.createComputedVariable(variable);
        }
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
