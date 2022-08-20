package minecrafttransportsimulator.mcinterface;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.packets.components.APacketBase;

/**
 * Interface to the MC networking system.  This interface allows us to send packets
 * around without actually creating packet classes.  Instead, we simply pass-in an
 * object to send over, which contains a handler for how to handle said object.
 * Forge packets do something similar, but Forge can't be bothered to keep networking
 * code the same, so we roll our own here.
 *
 * @author don_bruce
 */
public interface IInterfacePacket {

    /**
     * Registers the passed-in packet with the interface.
     */
    void registerPacket(byte packetIndex, Class<? extends APacketBase> packetClass);

    /**
     * Gets the index for the passed-in packet from the mapping.
     */
    byte getPacketIndex(APacketBase packet);

    /**
     * Sends the passed-in packet to the server.
     */
    void sendToServer(APacketBase packet);

    /**
     * Sends the passed-in packet to all clients.
     */
    void sendToAllClients(APacketBase packet);

    /**
     * Sends the passed-in packet to the passed-in player.
     * Note that this may ONLY be called on the server, as
     * clients don't know about other player's network pipelines.
     */
    void sendToPlayer(APacketBase packet, IWrapperPlayer player);

    /**
     * Helper method to write NBT data to the buffer.
     * Note: there is a limit to the size of an NBT tag.
     * As such, data should NOT be sent as a large tag.
     * Instead, segment it out and only send what you need.
     * Because you probably don't need the whole tag anyways.
     */
    void writeDataToBuffer(IWrapperNBT data, ByteBuf buf);

    /**
     * Helper method to read NBT data from the buffer.
     */
    IWrapperNBT readDataFromBuffer(ByteBuf buf);
}
