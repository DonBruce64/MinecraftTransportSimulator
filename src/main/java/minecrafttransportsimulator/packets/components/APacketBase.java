package minecrafttransportsimulator.packets.components;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.mcinterface.InterfacePacket;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.instances.PacketEntityBulletHit;
import minecrafttransportsimulator.packets.instances.PacketEntityBulletHitBlock;
import minecrafttransportsimulator.packets.instances.PacketEntityBulletHitEntity;
import minecrafttransportsimulator.packets.instances.PacketEntityBulletHitWrapper;
import minecrafttransportsimulator.packets.instances.PacketEntityColorChange;
import minecrafttransportsimulator.packets.instances.PacketEntityGUIRequest;
import minecrafttransportsimulator.packets.instances.PacketEntityInstrumentChange;
import minecrafttransportsimulator.packets.instances.PacketEntityRiderChange;
import minecrafttransportsimulator.packets.instances.PacketEntityTextChange;
import minecrafttransportsimulator.packets.instances.PacketEntityTrailerChange;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableIncrement;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableSet;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableToggle;
import minecrafttransportsimulator.packets.instances.PacketFluidTankChange;
import minecrafttransportsimulator.packets.instances.PacketFurnaceFuelAdd;
import minecrafttransportsimulator.packets.instances.PacketFurnaceTimeSet;
import minecrafttransportsimulator.packets.instances.PacketGUIRequest;
import minecrafttransportsimulator.packets.instances.PacketInventoryContainerChange;
import minecrafttransportsimulator.packets.instances.PacketItemInteractable;
import minecrafttransportsimulator.packets.instances.PacketPartChange;
import minecrafttransportsimulator.packets.instances.PacketPartEffector;
import minecrafttransportsimulator.packets.instances.PacketPartEngine;
import minecrafttransportsimulator.packets.instances.PacketPartGroundDevice;
import minecrafttransportsimulator.packets.instances.PacketPartGun;
import minecrafttransportsimulator.packets.instances.PacketPartInteractable;
import minecrafttransportsimulator.packets.instances.PacketPartSeat;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketPlayerCraftItem;
import minecrafttransportsimulator.packets.instances.PacketPlayerItemTransfer;
import minecrafttransportsimulator.packets.instances.PacketRadioStateChange;
import minecrafttransportsimulator.packets.instances.PacketTileEntityLoaderConnection;
import minecrafttransportsimulator.packets.instances.PacketTileEntityFuelPumpConnection;
import minecrafttransportsimulator.packets.instances.PacketTileEntityFuelPumpDispense;
import minecrafttransportsimulator.packets.instances.PacketTileEntityPoleChange;
import minecrafttransportsimulator.packets.instances.PacketTileEntityRoadChange;
import minecrafttransportsimulator.packets.instances.PacketTileEntityRoadCollisionUpdate;
import minecrafttransportsimulator.packets.instances.PacketTileEntityRoadConnectionUpdate;
import minecrafttransportsimulator.packets.instances.PacketTileEntitySignalControllerChange;
import minecrafttransportsimulator.packets.instances.PacketVehicleBeaconChange;
import minecrafttransportsimulator.packets.instances.PacketVehicleInteract;
import minecrafttransportsimulator.packets.instances.PacketVehicleServerMovement;
import minecrafttransportsimulator.packets.instances.PacketWorldSavedDataCSHandshake;

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
	 *  Helper method to write a UUID to the buffer.
	 */
	protected static void writeUUIDToBuffer(UUID uniqueUUID, ByteBuf buf){
		buf.writeLong(uniqueUUID.getMostSignificantBits());
		buf.writeLong(uniqueUUID.getLeastSignificantBits());
	}
	
	/**
	 *  Helper method to read a UUID from the buffer.
	 */
	protected static UUID readUUIDFromBuffer(ByteBuf buf){
		return new UUID(buf.readLong(), buf.readLong());
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
	 *  Forwarder to interface method for cleaner packet code.
	 */
	protected static void writeDataToBuffer(WrapperNBT data, ByteBuf buf){
		InterfacePacket.writeDataToBuffer(data, buf);
	}
	
	/**
	 *  Forwarder to interface method for cleaner packet code.
	 */
	protected static WrapperNBT readDataFromBuffer(ByteBuf buf){
		return InterfacePacket.readDataFromBuffer(buf);
	}
	
	/**
	 *  Called during network init to register packets.  Internal packets
	 *  will have already been registered, and the packet index will already
	 *  have been incremented to the appropriate value.
	 */
	public static void initPackets(byte packetIndex){
		//Register all classes in the minecrafttransportsimulator.packets.instances package.
		//Ideally this could be done via reflection, but it doesn't work too well so we don't do that.
		
		//Entity packets.
		InterfacePacket.registerPacket(packetIndex++, PacketEntityColorChange.class);
		InterfacePacket.registerPacket(packetIndex++, PacketEntityInstrumentChange.class);
		InterfacePacket.registerPacket(packetIndex++, PacketEntityRiderChange.class);
		InterfacePacket.registerPacket(packetIndex++, PacketEntityTextChange.class);
		InterfacePacket.registerPacket(packetIndex++, PacketEntityTrailerChange.class);
		InterfacePacket.registerPacket(packetIndex++, PacketEntityVariableIncrement.class);
		InterfacePacket.registerPacket(packetIndex++, PacketEntityVariableSet.class);
		InterfacePacket.registerPacket(packetIndex++, PacketEntityVariableToggle.class);
		
		//Bullet packets.
		InterfacePacket.registerPacket(packetIndex++, PacketEntityBulletHit.class);
		InterfacePacket.registerPacket(packetIndex++, PacketEntityBulletHitBlock.class);
		InterfacePacket.registerPacket(packetIndex++, PacketEntityBulletHitEntity.class);
		InterfacePacket.registerPacket(packetIndex++, PacketEntityBulletHitWrapper.class);
		
		//Fluid tank packets.
		InterfacePacket.registerPacket(packetIndex++, PacketFluidTankChange.class);
		
		//Inventory container packets.
		InterfacePacket.registerPacket(packetIndex++, PacketInventoryContainerChange.class);
		InterfacePacket.registerPacket(packetIndex++, PacketItemInteractable.class);
		
		//Furnace packets.
		InterfacePacket.registerPacket(packetIndex++, PacketFurnaceFuelAdd.class);
		InterfacePacket.registerPacket(packetIndex++, PacketFurnaceTimeSet.class);
		
		//GUI packets.
		InterfacePacket.registerPacket(packetIndex++, PacketGUIRequest.class);
		InterfacePacket.registerPacket(packetIndex++, PacketEntityGUIRequest.class);
		
		//Part packets.
		InterfacePacket.registerPacket(packetIndex++, PacketPartChange.class);
		InterfacePacket.registerPacket(packetIndex++, PacketPartGun.class);
		InterfacePacket.registerPacket(packetIndex++, PacketPartEffector.class);
		InterfacePacket.registerPacket(packetIndex++, PacketPartEngine.class);
		InterfacePacket.registerPacket(packetIndex++, PacketPartGroundDevice.class);
		InterfacePacket.registerPacket(packetIndex++, PacketPartInteractable.class);
		InterfacePacket.registerPacket(packetIndex++, PacketPartSeat.class);
		
		//Player packets.
		InterfacePacket.registerPacket(packetIndex++, PacketPlayerChatMessage.class);
		InterfacePacket.registerPacket(packetIndex++, PacketPlayerCraftItem.class);
		InterfacePacket.registerPacket(packetIndex++, PacketPlayerItemTransfer.class);
		
		//Radio packets.
		InterfacePacket.registerPacket(packetIndex++, PacketRadioStateChange.class);
		
		//Tile entity packets.
		InterfacePacket.registerPacket(packetIndex++, PacketTileEntityLoaderConnection.class);
		InterfacePacket.registerPacket(packetIndex++, PacketTileEntityFuelPumpConnection.class);
		InterfacePacket.registerPacket(packetIndex++, PacketTileEntityFuelPumpDispense.class);
		InterfacePacket.registerPacket(packetIndex++, PacketTileEntityRoadCollisionUpdate.class);
		InterfacePacket.registerPacket(packetIndex++, PacketTileEntityPoleChange.class);
		InterfacePacket.registerPacket(packetIndex++, PacketTileEntityRoadChange.class);
		InterfacePacket.registerPacket(packetIndex++, PacketTileEntityRoadConnectionUpdate.class);
		InterfacePacket.registerPacket(packetIndex++, PacketTileEntitySignalControllerChange.class);
		
		//Vehicle packets.
		InterfacePacket.registerPacket(packetIndex++, PacketVehicleBeaconChange.class);
		InterfacePacket.registerPacket(packetIndex++, PacketVehicleInteract.class);
		InterfacePacket.registerPacket(packetIndex++, PacketVehicleServerMovement.class);
		
		//World packets.
		InterfacePacket.registerPacket(packetIndex++, PacketWorldSavedDataCSHandshake.class);
	}
}
