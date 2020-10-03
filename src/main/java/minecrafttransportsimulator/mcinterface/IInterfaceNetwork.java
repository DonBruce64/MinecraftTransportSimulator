package minecrafttransportsimulator.mcinterface;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.packets.components.APacketBase;

/**Interface to the MC networking system.  This interface allows us to send packets
 * around without actually creating packet classes.  Instead, we simply pass-in an
 * object to send over, which contains a handler for how to handle said object.
 * Forge packets do something similar, but Forge can't be bothered to keep networking
 * code the same, so we roll our own here. 
 *
 * @author don_bruce
 */
public interface IInterfaceNetwork{
	
	/**
	 *  Registers the passed-in packet with the interface.
	 */
	public void registerPacket(byte index, Class<? extends APacketBase> packetClass);
	
	/**
	 *  Gets the index for the passed-in packet from the mapping.
	 */
	public byte getPacketIndex(APacketBase packet);
	
	/**
	 *  Sends the passed-in packet to the server.
	 */
	public void sendToServer(APacketBase packet);
	
	/**
	 *  Sends the passed-in packet to all clients.
	 */
	public void sendToAllClients(APacketBase packet);
	
	/**
	 *  Creates an NBT tag from a data buffer.
	 */
	public IWrapperNBT createDataFromBuffer(ByteBuf buf);
}
