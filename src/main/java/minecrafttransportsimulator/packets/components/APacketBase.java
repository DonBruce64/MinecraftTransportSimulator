package minecrafttransportsimulator.packets.components;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.mcinterface.IInterfaceNetwork;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.mcinterface.MasterLoader;

/**Base packet class.  All packets must extend this class to be used with the
 * {@link IInterfaceNetwork}.  This allows for standard packet handling across
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
		buf.writeByte(MasterLoader.networkInterface.getPacketIndex(this));
	}
	
	/**
	 *  This is called to handle the logic of this packet.  An instance of
	 *  the world is passed-in here for referencing objects, as well as
	 *  an instance of the player that sent the packet if on a server,
	 *  or the current player if on a client.
	 */
	public abstract void handle(IWrapperWorld world, IWrapperPlayer player);
	
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
	
	/**
	 *  Helper method to write a Point3d to the buffer.
	 */
	protected static void writePoint3dToBuffer(Point3d point, ByteBuf buf){
		buf.writeDouble(point.x);
		buf.writeDouble(point.y);
		buf.writeDouble(point.z);
	}
	
	/**
	 *  Helper method to read a Point3d from the buffer.
	 */
	protected static Point3d readPoint3dFromBuffer(ByteBuf buf){
		return new Point3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
	}
}
