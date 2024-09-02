package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.instances.AEntityVehicleE_Powered.FuelTankResult;
import minecrafttransportsimulator.entities.instances.EntityInventoryContainer;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.items.instances.ItemDecor;
import minecrafttransportsimulator.jsondefs.JSONItem.ItemComponentType;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityGUIRequest;
import minecrafttransportsimulator.packets.instances.PacketEntityInteractGUI;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketTileEntityFuelPumpConnection;
import minecrafttransportsimulator.packets.instances.PacketTileEntityFuelPumpDispense;
import minecrafttransportsimulator.systems.LanguageSystem;

public abstract class ATileEntityFuelPump extends TileEntityDecor {
    protected EntityVehicleF_Physics connectedVehicle;
    protected IWrapperPlayer playerUsing;
    public final EntityInventoryContainer fuelItems;
    public final EntityInventoryContainer paymentItems;
    public final List<Integer> fuelAmounts = new ArrayList<>();
    public int fuelPurchased;
    public double fuelDispensedThisPurchase;
    public double fuelDispensedThisConnection;
    public boolean isCreative;
    public final UUID placingPlayerID;

    public ATileEntityFuelPump(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, ItemDecor item, IWrapperNBT data) {
        super(world, position, placingPlayer, item, data);
        if (data != null) {
            this.fuelItems = new EntityInventoryContainer(world, data.getData("inventory"), 6);
            this.paymentItems = new EntityInventoryContainer(world, data.getData("inventory2"), 18);
            world.addEntity(fuelItems);
            world.addEntity(paymentItems);
            for (int i = 0; i < fuelItems.getSize(); ++i) {
                this.fuelAmounts.add(data.getInteger("fuelAmount" + i));
            }
            this.fuelPurchased = data.getInteger("fuelPurchased");
            this.fuelDispensedThisPurchase = data.getDouble("fuelDispensedThisPurchase");
            this.placingPlayerID = placingPlayer != null ? placingPlayer.getID() : data.getUUID("placingPlayerID");
        } else {
            this.fuelItems = new EntityInventoryContainer(world, null, 6);
            this.paymentItems = new EntityInventoryContainer(world, null, 18);
            world.addEntity(fuelItems);
            world.addEntity(paymentItems);
            for (int i = 0; i < fuelItems.getSize(); ++i) {
                this.fuelAmounts.add(0);
            }
            this.placingPlayerID = placingPlayer != null ? placingPlayer.getID() : null;
        }
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
                playerUsing.sendPacket(new PacketPlayerChatMessage(playerUsing, LanguageSystem.INTERACT_FUELPUMP_TOOFAR));
                setConnection(null);
                return;
            }

            //Check to make sure the vehicle isn't full.
            if (connectedVehicle.fuelTank.getFluidLevel() == connectedVehicle.fuelTank.getMaxLevel()) {
                playerUsing.sendPacket(new PacketPlayerChatMessage(playerUsing, LanguageSystem.INTERACT_FUELPUMP_FULL));
                setConnection(null);
                return;
            }

            //All checks pass, fuel vehicle.
            double amountToDispenseThisTick = connectedVehicle.fuelTank.getMaxLevel() - connectedVehicle.fuelTank.getFluidLevel();
            if (amountToDispenseThisTick > definition.decor.pumpRate) {
                amountToDispenseThisTick = definition.decor.pumpRate;
            }
            amountToDispenseThisTick = fuelVehicle(amountToDispenseThisTick);
            fuelDispensedThisConnection += amountToDispenseThisTick;
            fuelDispensedThisPurchase += amountToDispenseThisTick;
            InterfaceManager.packetInterface.sendToAllClients(new PacketTileEntityFuelPumpDispense(this, amountToDispenseThisTick));

            //If we are done dispensing, disconnect the vehicle.
            if (!isCreative && fuelDispensedThisPurchase == fuelPurchased) {
                playerUsing.sendPacket(new PacketPlayerChatMessage(playerUsing, LanguageSystem.INTERACT_FUELPUMP_COMPLETE));
                setConnection(null);
                return;
            }
        }
    }

    @Override
    public boolean interact(IWrapperPlayer player) {
        //Check if the item is a wrench, and the player can configure this pump.
        if (player.isHoldingItemType(ItemComponentType.WRENCH) && (player.getID().equals(placingPlayerID) || player.isOP())) {
            player.sendPacket(new PacketEntityGUIRequest(this, player, PacketEntityGUIRequest.EntityGUIType.FUEL_PUMP_CONFIG));
            playersInteracting.add(player);
            InterfaceManager.packetInterface.sendToAllClients(new PacketEntityInteractGUI(this, player, true));
            return true;
        }

        //If we are ready for a purchase, bring up the GUI so the player can buy some.
        if (!isCreative && !hasFuel()) {
            boolean haveEmptySlot = false;
            for (int i = 0; i < paymentItems.getSize(); ++i) {
                if (paymentItems.getStack(i).isEmpty()) {
                    haveEmptySlot = true;
                    break;
                }
            }
            if (haveEmptySlot) {
                player.sendPacket(new PacketEntityGUIRequest(this, player, PacketEntityGUIRequest.EntityGUIType.FUEL_PUMP));
                playersInteracting.add(player);
                InterfaceManager.packetInterface.sendToAllClients(new PacketEntityInteractGUI(this, player, true));
            } else {
                player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_FUELPUMP_FULLITEMS));
            }
            return true;
        }

        //If we don't have anything in our buffer, don't try and connect anything.
        if (!hasFuel()) {
            player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_FUELPUMP_NOFUEL));
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
                switch (checkPump(nearestVehicle)) {
                    case NOENGINE: {
                        player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_FUELPUMP_NOENGINE));
                        return true;
                    }
                    case VALID: {
                        player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_FUELPUMP_CONNECT));
                        setConnection(nearestVehicle);
                        playerUsing = player;
                        return true;
                    }
                    case INVALID: {
                        player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_FUELPUMP_WRONGENGINES));
                        return true;
                    }
                    case MISMATCH: {
                        player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_FUELPUMP_WRONGTYPE, nearestVehicle.fuelTank.getFluid()));
                        return true;
                    }
                }
            } else {
                player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_FUELPUMP_TOOFAR));
            }
        } else {
            //Connected vehicle exists, disconnect it.
            player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_FUELPUMP_DISCONNECT));
            setConnection(null);
        }
        return true;
    }
    
    @Override
    public IWrapperItemStack getStack() {
        //Add data to the stack we return for the payment info.
        IWrapperItemStack stack = super.getStack();
        stack.setData(saveFuelData(InterfaceManager.coreInterface.getNewNBTWrapper()));
        return stack;
    }

    public void setConnection(EntityVehicleF_Physics newVehicle) {
        if (newVehicle != null) {
            newVehicle.beingFueled = true;
            fuelDispensedThisConnection = 0;
        } else if (connectedVehicle != null) {
            connectedVehicle.beingFueled = false;
            playerUsing = null;
        }
        connectedVehicle = newVehicle;
        if (!world.isClient()) {
            if (connectedVehicle != null) {
                InterfaceManager.packetInterface.sendToAllClients(new PacketTileEntityFuelPumpConnection(this, connectedVehicle));
            } else {
                InterfaceManager.packetInterface.sendToAllClients(new PacketTileEntityFuelPumpConnection(this));
            }
        }
    }

    protected abstract boolean hasFuel();

    protected abstract FuelTankResult checkPump(EntityVehicleF_Physics vehicle);

    protected abstract double fuelVehicle(double amount);

    private IWrapperNBT saveFuelData(IWrapperNBT data) {
        data.setData("inventory", fuelItems.save(InterfaceManager.coreInterface.getNewNBTWrapper()));
        data.setData("inventory2", paymentItems.save(InterfaceManager.coreInterface.getNewNBTWrapper()));
        for (int i = 0; i < fuelItems.getSize(); ++i) {
            data.setInteger("fuelAmount" + i, fuelAmounts.get(i));
        }
        data.setInteger("fuelPurchased", fuelPurchased);
        data.setDouble("fuelDispensedThisPurchase", fuelDispensedThisPurchase);
        if (placingPlayerID != null) {
            data.setUUID("placingPlayerID", placingPlayerID);
        }
        return data;
    }

    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        saveFuelData(data);
        return data;
    }
}
