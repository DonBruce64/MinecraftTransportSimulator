package minecrafttransportsimulator.packets.components;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.wrappers.WrapperNetwork;
import minecrafttransportsimulator.wrappers.WrapperPlayer;
import minecrafttransportsimulator.wrappers.WrapperWorld;

/**Base packet class.  All packets must extend this class to be used with the
 * {@link WrapperNetwork}.  This allows for standard packet handling across
 * all MC versions.
 *
 * @author don_bruce
 */
public abstract class APacketBase{
	
	/**
	 *  Constructs the packet from the buffer.  This should
	 *  populate all fields to be used by {@link #handle(WrapperWorld)} and
	 *  is used to create this packet from a buffer after it is
	 *  received on the other end of the network line.  Note that
	 *  the network system expects this constructor, so leaving it
	 *  out will cause crashes!
	 */
	public APacketBase(ByteBuf buf){}

	/**
	 *  This is called to write the field values from this class into a buffer.
	 *  Used prior to sending the packet off over the network.  Make sure to
	 *  call super should you override this, as this puts the packetID in
	 *  the buffer so the network knows what packet class this packet goes to!
	 */
	public void writeToBuffer(ByteBuf buf){
		buf.writeByte(WrapperNetwork.getPacketIndex(this));
	}
	
	/**
	 *  This is called to handle the logic of this packet.  An instance of
	 *  the world is passed-in here for referencing objects, as well as
	 *  an instance of the player that sent the packet if on a server,
	 *  or the current player if on a client.
	 */
	public abstract void handle(WrapperWorld world, WrapperPlayer player);
	
	/**
	 *  Helper method to write a string to the buffer.
	 */
	protected static void writeStringToBuffer(String string, ByteBuf buf){
		byte[] stringAsBytes = string.getBytes(StandardCharsets.UTF_8);
		if(stringAsBytes.length > Short.MAX_VALUE){
			throw new IndexOutOfBoundsException("ERROR: Tried to write a string of: " + stringAsBytes.length + " bytes to a packet.  Max string byte size is: " + Short.MAX_VALUE);
		}else{
			buf.writeShort(stringAsBytes.length);
	        buf.writeBytes(stringAsBytes);
		}
	}
	
	/**
	 *  Helper method to read a string from the buffer.
	 */
	protected static String readStringFromBuffer(ByteBuf buf){
		short stringLength = buf.readShort();
		String returnString = buf.toString(buf.readerIndex(), stringLength, StandardCharsets.UTF_8);
		//Need to increment the index as the read doesn't do that automatically.
		buf.readerIndex(buf.readerIndex() + stringLength);
		return returnString;
	}
}
