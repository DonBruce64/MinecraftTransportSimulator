package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityFluidTankProvider;
import minecrafttransportsimulator.entities.instances.*;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.instances.ItemPartInteractable;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONItem.ItemComponentType;
import minecrafttransportsimulator.jsondefs.JSONPart.InteractableComponentType;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.*;
import minecrafttransportsimulator.packets.instances.PacketEntityGUIRequest;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketTileEntityFuelPumpConnection;
import minecrafttransportsimulator.systems.ConfigSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TileEntityFuelPump extends TileEntityDecor implements ITileEntityFluidTankProvider {
    public EntityVehicleF_Physics connectedVehicle;
    private final EntityFluidTank tank;
    public final EntityInventoryContainer fuelItems;
    public final List<Integer> fuelAmounts = new ArrayList<>();
    public int fuelPurchasedRemaining;
    public boolean isCreative;
    public UUID placingPlayerID;

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
        this.fuelItems = new EntityInventoryContainer(world, data.getDataOrNew("inventory"), 10);
        world.addEntity(fuelItems);
        for (int i = 0; i < fuelItems.getSize(); ++i) {
            this.fuelAmounts.add(data.getInteger("fuelAmount" + i));
        }
        this.fuelPurchasedRemaining = data.getInteger("fuelPurchasedRemaining");
        this.placingPlayerID = placingPlayer != null ? placingPlayer.getID() : data.getUUID("placingPlayerID");
    }

    @Override
    public void update() {
        super.update();
        //Update creative status.
        isCreative = true;
        for (int i = 0; i < fuelItems.getSize(); ++i) {
            if (!fuelItems.getStack(i).isEmpty()) {
                isCreative = false;
            }
        }

        //Do fuel checks.  Fuel checks only occur on servers.  Clients get packets for state changes.
        if (connectedVehicle != null && !world.isClient()) {
            //Don't fuel vehicles that don't exist.
            if (!connectedVehicle.isValid) {
                connectedVehicle.beingFueled = false;
                connectedVehicle = null;
                return;
            }

            //Check distance to make sure the vehicle hasn't moved away.
            if (!connectedVehicle.position.isDistanceToCloserThan(position, 15)) {
                InterfaceManager.packetInterface.sendToAllClients(new PacketTileEntityFuelPumpConnection(this, false));
                for (IWrapperEntity entity : world.getEntitiesWithin(new BoundingBox(position, 25, 25, 25))) {
                    if (entity instanceof IWrapperPlayer) {
                        ((IWrapperPlayer) entity).sendPacket(new PacketPlayerChatMessage((IWrapperPlayer) entity, JSONConfigLanguage.INTERACT_FUELPUMP_TOOFAR));
                    }
                }
                connectedVehicle.beingFueled = false;
                connectedVehicle = null;
                return;
            }
            //If we have room for fuel, try to add it to the vehicle.
            if (tank.getFluidLevel() > 0) {
                double amountToFill = connectedVehicle.fuelTank.fill(tank.getFluid(), definition.decor.pumpRate, false);
                if (amountToFill > 0) {
                    double amountToDrain = tank.drain(tank.getFluid(), amountToFill, false);
                    connectedVehicle.fuelTank.fill(tank.getFluid(), amountToDrain, true);
                    tank.drain(tank.getFluid(), amountToDrain, true);
                } else {
                    //No more room in the vehicle.  Disconnect.
                    InterfaceManager.packetInterface.sendToAllClients(new PacketTileEntityFuelPumpConnection(this, false));
                    connectedVehicle.beingFueled = false;
                    connectedVehicle = null;
                    for (IWrapperEntity entity : world.getEntitiesWithin(new BoundingBox(position, 16, 16, 16))) {
                        if (entity instanceof IWrapperPlayer) {
                            ((IWrapperPlayer) entity).sendPacket(new PacketPlayerChatMessage((IWrapperPlayer) entity, JSONConfigLanguage.INTERACT_FUELPUMP_COMPLETE));
                        }
                    }
                }
            } else {
                //No more fuel.  Disconnect vehicle.
                InterfaceManager.packetInterface.sendToAllClients(new PacketTileEntityFuelPumpConnection(this, false));
                connectedVehicle.beingFueled = false;
                connectedVehicle = null;
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
        //If we are holding an item, interact with the pump.
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

        //Check if the item is a wrench, and the player can configure this pump..
        if (player.isHoldingItemType(ItemComponentType.WRENCH) && (player.getID().equals(placingPlayerID) || player.isOP())) {
            player.sendPacket(new PacketEntityGUIRequest(this, player, PacketEntityGUIRequest.EntityGUIType.FUEL_PUMP_CONFIG));
            return true;
        }

        //If we aren't a creative pump, and we don't have fuel, bring up the GUI so the player can buy some.
        if (!isCreative && fuelPurchasedRemaining == 0 && tank.getFluidLevel() <= 1) {
            player.sendPacket(new PacketEntityGUIRequest(this, player, PacketEntityGUIRequest.EntityGUIType.FUEL_PUMP));
            return true;
        }

        //We don't have a vehicle connected.  Try to connect one now.
        if (connectedVehicle == null) {
            //Get the closest vehicle within a 16-block radius.
            EntityVehicleF_Physics nearestVehicle = null;
            double lowestDistance = 16D;
            for (EntityVehicleF_Physics testVehicle : world.getEntitiesOfType(EntityVehicleF_Physics.class)) {
                double vehicleDistance = testVehicle.position.distanceTo(position);
                if (vehicleDistance < lowestDistance) {
                    lowestDistance = vehicleDistance;
                    nearestVehicle = testVehicle;
                }
            }

            //Have a vehicle, try to connect to it.
            if (nearestVehicle != null) {
                if (tank.getFluidLevel() == 0) {
                    //No fuel in the pump.
                    player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_FUELPUMP_NOFUEL));
                } else {
                    //Check to make sure this vehicle can take this fuel pump's fuel type.
                    if (!nearestVehicle.fuelTank.getFluid().isEmpty()) {
                        if (!tank.getFluid().equals(nearestVehicle.fuelTank.getFluid())) {
                            player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_FUELPUMP_WRONGTYPE));
                            return true;
                        }
                    }

                    //Fuel type can be taken by vehicle, check to make sure engines can take it.
                    for (APart part : nearestVehicle.parts) {
                        if (part instanceof PartEngine) {
                            if (ConfigSystem.settings.fuel.fuels.get(part.definition.engine.fuelType).containsKey(tank.getFluid())) {
                                connectedVehicle = nearestVehicle;
                                connectedVehicle.beingFueled = true;
                                tank.resetAmountDispensed();
                                InterfaceManager.packetInterface.sendToAllClients(new PacketTileEntityFuelPumpConnection(this, true));
                                player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_FUELPUMP_CONNECT));
                                return true;
                            }
                        }
                    }
                    player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_FUELPUMP_WRONGENGINES));
                }
            } else {
                player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_FUELPUMP_TOOFAR));
            }
        } else {
            //Connected vehicle exists, disconnect it.
            InterfaceManager.packetInterface.sendToAllClients(new PacketTileEntityFuelPumpConnection(this, false));
            connectedVehicle.beingFueled = false;
            connectedVehicle = null;
            player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_FUELPUMP_DISCONNECT));
        }
        return true;
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
        data.setData("inventory", fuelItems.save(InterfaceManager.coreInterface.getNewNBTWrapper()));
        for (int i = 0; i < fuelItems.getSize(); ++i) {
            data.setInteger("fuelAmount" + i, fuelAmounts.get(i));
        }
        data.setInteger("fuelPurchasedRemaining", fuelPurchasedRemaining);
        if (placingPlayerID != null) {
            data.setUUID("placingPlayerID", placingPlayerID);
        }
        return data;
    }
}
