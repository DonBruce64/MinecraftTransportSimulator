package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.components.APacketPlayer;
import minecrafttransportsimulator.systems.summoning.VehicleSummonManager;

/**
 * Packet sent from the phone GUI to the server to call, stop calling, or remove
 * a vehicle registered on the phone the player is holding.
 */
public class PacketVehiclePhoneAction extends APacketPlayer {
    private final PhoneAction action;
    private final UUID vehicleID;

    public PacketVehiclePhoneAction(IWrapperPlayer player, PhoneAction action, UUID vehicleID) {
        super(player);
        this.action = action;
        this.vehicleID = vehicleID;
    }

    public PacketVehiclePhoneAction(ByteBuf buf) {
        super(buf);
        this.action = PhoneAction.values()[buf.readByte()];
        this.vehicleID = readUUIDFromBuffer(buf);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeByte(action.ordinal());
        writeUUIDToBuffer(vehicleID, buf);
    }

    @Override
    public void handle(AWrapperWorld world, IWrapperPlayer player) {
        VehicleSummonManager.handleAction(world, player, action, vehicleID);
    }

    public enum PhoneAction {
        SUMMON,
        STOP,
        REMOVE
    }
}
