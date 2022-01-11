package minecrafttransportsimulator.packets.components;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.mcinterface.InterfacePacket;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import net.minecraft.network.PacketBuffer;

/**Base packet class.  All packets must extend this class to be used with the
 * {@link InterfacePacket}.  This allows for standard packet handling across
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
	 *  Normally, all packets will need to run on the main game thread
	 *  for proper I/O access.  However, sometimes the data they access
	 *  is read-only, or may be thread-safe.  In that case, it is prefered
	 *  to have the packet be handled on the networking thread to help reduce
	 *  main game load.  If so, then false should be returned here.
	 */
	public boolean runOnMainThread(){
		return true;
	}

	/**
	 *  This is called to write the field values from this class into a buffer.
	 *  Used prior to sending the packet off over the network.  Make sure to
	 *  call super should you override this, as this puts the packetID in
	 *  the buffer so the network knows what packet class this packet goes to!
	 */
	public void writeToBuffer(ByteBuf buf){
		buf.writeByte(InterfacePacket.getPacketIndex(this));
	}
	
	/**
	 *  This is called to handle the logic of this packet.  An instance of
	 *  the world is passed-in here for referencing objects.
	 */
	public abstract void handle(WrapperWorld world);
	
	/**
	 *  Helper method to write a string to the buffer.
	 */
	protected static void writeStringToBuffer(String string, ByteBuf buf){
		byte[] stringAsBytes = string.getBytes(StandardCharsets.UTF_8);
		if(stringAsBytes.length > Short.MAX_VALUE){
			throw new IndexOutOfBoundsException("Tried to write a string of: " + stringAsBytes.length + " bytes to a packet.  Max string byte size is: " + Short.MAX_VALUE);
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
	
	/**
	 *  Helper method to write a Point3i to the buffer.
	 *  Does so in a compact way by casting-down the doubles to ints.
	 *  Useful if you don't need the floating-point and want to save on bandwidth.
	 */
	protected static void writePoint3dCompactToBuffer(Point3d point, ByteBuf buf){
		buf.writeInt((int) point.x);
		buf.writeInt((int) point.y);
		buf.writeInt((int) point.z);
	}
	
	/**
	 *  Helper method to read a compact Point3d from the buffer.
	 */
	protected static Point3d readPoint3dCompactFromBuffer(ByteBuf buf){
		return new Point3d(buf.readInt(), buf.readInt(), buf.readInt());
	}
	
	/**
	 *  Helper method to write NBT data to the buffer.
	 *  Note: there is a limit to the size of an NBT tag.
	 *  As such, data should NOT be sent as a large tag.
	 *  Instead, segment it out and only send what you need.
	 *  Because you probably don't need the whole tag anyways.
	 */
	protected static void writeDataToBuffer(WrapperNBT data, ByteBuf buf){
		PacketBuffer pb = new PacketBuffer(buf);
		 pb.writeCompoundTag(data.tag);
	}
	
	/**
	 *  Helper method to read NBT data from the buffer.
	 */
	protected static WrapperNBT readDataFromBuffer(ByteBuf buf){
		PacketBuffer pb = new PacketBuffer(buf);
        try{
        	return new WrapperNBT(pb.readCompoundTag());
        }catch (IOException e){
            // Unpossible? --- Says Forge comments, so who knows?
            throw new RuntimeException(e);
        }
	}
}
