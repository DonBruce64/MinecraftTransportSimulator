package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.instances.AEntityVehicleE_Powered.FuelTankResult;
import minecrafttransportsimulator.entities.instances.EntityInventoryContainer;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONItem.ItemComponentType;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityGUIRequest;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketTileEntityFuelPumpConnection;

public abstract class ATileEntityFuelPump extends TileEntityDecor {
    public EntityVehicleF_Physics connectedVehicle;
    public final EntityInventoryContainer fuelItems;
    public final EntityInventoryContainer paymentItems;
    public final List<Integer> fuelAmounts = new ArrayList<>();
    public int fuelPurchased;
    public double fuelDispensedThisPurchase;
    public double fuelDispensedThisConnection;
    public boolean isCreative;
    public UUID placingPlayerID;

    public ATileEntityFuelPump(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, IWrapperNBT data) {
        super(world, position, placingPlayer, data); 
        this.fuelItems = new EntityInventoryContainer(world, data.getDataOrNew("inventory"), 6);
        this.paymentItems = new EntityInventoryContainer(world, data.getDataOrNew("inventory2"), 18);
        world.addEntity(fuelItems);
        world.addEntity(paymentItems);
        for (int i = 0; i < fuelItems.getSize(); ++i) {
            this.fuelAmounts.add(data.getInteger("fuelAmount" + i));
        }
        this.fuelPurchased = data.getInteger("fuelPurchased");
        this.fuelDispensedThisPurchase = data.getDouble("fuelDispensedThisPurchase");
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
                setConnection(null);
                InterfaceManager.packetInterface.sendToAllClients(new PacketTileEntityFuelPumpConnection(this));
                for (IWrapperPlayer player : world.getPlayersWithin(new BoundingBox(position, 25, 25, 25))) {
                    player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_FUELPUMP_TOOFAR));
                }
                return;
            }

            //Check to make sure the vehicle isn't full.
            if (connectedVehicle.fuelTank.getFluidLevel() == connectedVehicle.fuelTank.getMaxLevel()) {
                setConnection(null);
                InterfaceManager.packetInterface.sendToAllClients(new PacketTileEntityFuelPumpConnection(this));
                for (IWrapperPlayer player : world.getPlayersWithin(new BoundingBox(position, 16, 16, 16))) {
                    player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_FUELPUMP_COMPLETE));
                }
                return;
            }

            //All checks pass, fuel vehicle.
            double amountToDispenseThisTick = connectedVehicle.fuelTank.getMaxLevel() - connectedVehicle.fuelTank.getFluidLevel();
            if (amountToDispenseThisTick > definition.decor.pumpRate) {
                amountToDispenseThisTick = definition.decor.pumpRate;
            }
            fuelVehicle(amountToDispenseThisTick);
        }
    }

    @Override
    public boolean interact(IWrapperPlayer player) {
        //Check if the item is a wrench, and the player can configure this pump.
        if (player.isHoldingItemType(ItemComponentType.WRENCH) && (player.getID().equals(placingPlayerID) || player.isOP())) {
            player.sendPacket(new PacketEntityGUIRequest(this, player, PacketEntityGUIRequest.EntityGUIType.FUEL_PUMP_CONFIG));
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
            } else {
                player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_FUELPUMP_FULLITEMS));
            }
            return true;
        }

        //If we don't have anything in our buffer, don't try and connect anything.
        if (!hasFuel()) {
            player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_FUELPUMP_NOFUEL));
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
                        player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_FUELPUMP_NOENGINE));
                        return true;
                    }
                    case VALID: {
                        setConnection(nearestVehicle);
                        InterfaceManager.packetInterface.sendToAllClients(new PacketTileEntityFuelPumpConnection(this, nearestVehicle));
                        player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_FUELPUMP_CONNECT));
                        return true;
                    }
                    case INVALID: {
                        player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_FUELPUMP_WRONGENGINES));
                        return true;
                    }
                    case MISMATCH: {
                        player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_FUELPUMP_WRONGTYPE));
                        return true;
                    }
                }
            } else {
                player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_FUELPUMP_TOOFAR));
            }
        } else {
            //Connected vehicle exists, disconnect it.
            setConnection(null);
            InterfaceManager.packetInterface.sendToAllClients(new PacketTileEntityFuelPumpConnection(this));
            player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_FUELPUMP_DISCONNECT));
        }
        return true;
    }
    
    public void setConnection(EntityVehicleF_Physics newVehicle) {
        if (newVehicle != null) {
            newVehicle.beingFueled = true;
        } else if (connectedVehicle != null) {
            connectedVehicle.beingFueled = false;
        }
        connectedVehicle = newVehicle;
        fuelDispensedThisConnection = 0;
    }

    protected abstract boolean hasFuel();

    protected abstract FuelTankResult checkPump(EntityVehicleF_Physics vehicle);

    protected abstract void fuelVehicle(double amount);

    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
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
}
