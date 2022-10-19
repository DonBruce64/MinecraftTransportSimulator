package minecrafttransportsimulator.packets.components;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityBulletHitBlock;
import minecrafttransportsimulator.packets.instances.PacketEntityColorChange;
import minecrafttransportsimulator.packets.instances.PacketEntityGUIRequest;
import minecrafttransportsimulator.packets.instances.PacketEntityInstrumentChange;
import minecrafttransportsimulator.packets.instances.PacketEntityInteract;
import minecrafttransportsimulator.packets.instances.PacketEntityRiderChange;
import minecrafttransportsimulator.packets.instances.PacketEntityTextChange;
import minecrafttransportsimulator.packets.instances.PacketEntityTowingChange;
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
import minecrafttransportsimulator.packets.instances.PacketPartInteractableInteract;
import minecrafttransportsimulator.packets.instances.PacketPartSeat;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketPlayerCraftItem;
import minecrafttransportsimulator.packets.instances.PacketPlayerItemTransfer;
import minecrafttransportsimulator.packets.instances.PacketRadioStateChange;
import minecrafttransportsimulator.packets.instances.PacketTileEntityFuelPumpConnection;
import minecrafttransportsimulator.packets.instances.PacketTileEntityFuelPumpDispense;
import minecrafttransportsimulator.packets.instances.PacketTileEntityLoaderConnection;
import minecrafttransportsimulator.packets.instances.PacketTileEntityPoleChange;
import minecrafttransportsimulator.packets.instances.PacketTileEntityPoleCollisionUpdate;
import minecrafttransportsimulator.packets.instances.PacketTileEntityRoadChange;
import minecrafttransportsimulator.packets.instances.PacketTileEntityRoadCollisionUpdate;
import minecrafttransportsimulator.packets.instances.PacketTileEntityRoadConnectionUpdate;
import minecrafttransportsimulator.packets.instances.PacketTileEntitySignalControllerChange;
import minecrafttransportsimulator.packets.instances.PacketVehicleBeaconChange;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlNotification;
import minecrafttransportsimulator.packets.instances.PacketVehicleServerMovement;
import minecrafttransportsimulator.packets.instances.PacketWorldSavedDataRequest;
import minecrafttransportsimulator.packets.instances.PacketWorldSavedDataUpdate;
import minecrafttransportsimulator.packloading.PackParser;

/**
 * Base packet class.  All packets must extend this class to be used with the
 * {@link InterfaceManager.packetInterface}.  This allows for standard packet handling across
 * all MC versions.
 *
 * @author don_bruce
 */
public abstract class APacketBase {

    /**
     * Constructs the packet from the buffer.  This should
     * populate all fields to be used by {@link #handle(AWrapperWorld)} and
     * is used to create this packet from a buffer after it is
     * received on the other end of the network line.  Note that
     * the network system expects this constructor, so leaving it
     * out will cause crashes!
     */
    public APacketBase(ByteBuf buf) {
    }

    /**
     * Normally, all packets will need to run on the main game thread
     * for proper I/O access.  However, sometimes the data they access
     * is read-only, or may be thread-safe.  In that case, it is prefered
     * to have the packet be handled on the networking thread to help reduce
     * main game load.  If so, then false should be returned here.
     */
    public boolean runOnMainThread() {
        return true;
    }

    /**
     * This is called to write the field values from this class into a buffer.
     * Used prior to sending the packet off over the network.  Make sure to
     * call super should you override this, as this puts the packetID in
     * the buffer so the network knows what packet class this packet goes to!
     */
    public void writeToBuffer(ByteBuf buf) {
        buf.writeByte(InterfaceManager.packetInterface.getPacketIndex(this));
    }

    /**
     * This is called to handle the logic of this packet.  An instance of
     * the world is passed-in here for referencing objects.
     */
    public abstract void handle(AWrapperWorld world);

    /**
     * Helper method to write a string to the buffer.
     */
    protected static void writeStringToBuffer(String string, ByteBuf buf) {
        byte[] stringAsBytes = string.getBytes(StandardCharsets.UTF_8);
        if (stringAsBytes.length > Short.MAX_VALUE) {
            throw new IndexOutOfBoundsException("Tried to write a string of: " + stringAsBytes.length + " bytes to a packet.  Max string byte size is: " + Short.MAX_VALUE);
        } else {
            buf.writeShort(stringAsBytes.length);
            buf.writeBytes(stringAsBytes);
        }
    }

    /**
     * Helper method to read a string from the buffer.
     */
    protected static String readStringFromBuffer(ByteBuf buf) {
        short stringLength = buf.readShort();
        String returnString = buf.toString(buf.readerIndex(), stringLength, StandardCharsets.UTF_8);
        //Need to increment the index as the read doesn't do that automatically.
        buf.readerIndex(buf.readerIndex() + stringLength);
        return returnString;
    }

    /**
     * Helper method to write a UUID to the buffer.
     */
    protected static void writeUUIDToBuffer(UUID uniqueUUID, ByteBuf buf) {
        buf.writeLong(uniqueUUID.getMostSignificantBits());
        buf.writeLong(uniqueUUID.getLeastSignificantBits());
    }

    /**
     * Helper method to read a UUID from the buffer.
     */
    protected static UUID readUUIDFromBuffer(ByteBuf buf) {
        return new UUID(buf.readLong(), buf.readLong());
    }

    /**
     * Helper method to write a pack item to the buffer.
     */
    protected static void writeItemToBuffer(AItemPack<?> item, ByteBuf buf) {
        writeStringToBuffer(item.definition.packID, buf);
        writeStringToBuffer(item.definition.systemName, buf);
        writeStringToBuffer(item instanceof AItemSubTyped ? ((AItemSubTyped<?>) item).subDefinition.subName : "", buf);
    }

    /**
     * Helper method to read a UUID from the buffer.
     */
    @SuppressWarnings("unchecked")
    protected static <T extends AItemPack<?>> T readItemFromBuffer(ByteBuf buf) {
        return (T) PackParser.getItem(readStringFromBuffer(buf), readStringFromBuffer(buf), readStringFromBuffer(buf));
    }

    /**
     * Helper method to write a Point3d to the buffer.
     */
    protected static void writePoint3dToBuffer(Point3D point, ByteBuf buf) {
        buf.writeDouble(point.x);
        buf.writeDouble(point.y);
        buf.writeDouble(point.z);
    }

    /**
     * Helper method to read a Point3d from the buffer.
     */
    protected static Point3D readPoint3dFromBuffer(ByteBuf buf) {
        return new Point3D(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    /**
     * Helper method to write a Point3i to the buffer.
     * Does so in a compact way by casting-down the doubles to ints.
     * Useful if you don't need the floating-point and want to save on bandwidth.
     */
    protected static void writePoint3dCompactToBuffer(Point3D point, ByteBuf buf) {
        buf.writeInt((int) point.x);
        buf.writeInt((int) point.y);
        buf.writeInt((int) point.z);
    }

    /**
     * Helper method to read a compact Point3d from the buffer.
     */
    protected static Point3D readPoint3dCompactFromBuffer(ByteBuf buf) {
        return new Point3D(buf.readInt(), buf.readInt(), buf.readInt());
    }

    /**
     * Forwarder to interface method for cleaner packet code.
     */
    protected static void writeDataToBuffer(IWrapperNBT data, ByteBuf buf) {
        InterfaceManager.packetInterface.writeDataToBuffer(data, buf);
    }

    /**
     * Forwarder to interface method for cleaner packet code.
     */
    protected static IWrapperNBT readDataFromBuffer(ByteBuf buf) {
        return InterfaceManager.packetInterface.readDataFromBuffer(buf);
    }

    /**
     * Called during network init to register packets.  Internal packets
     * will have already been registered, and the packet index will already
     * have been incremented to the appropriate value.
     */
    public static void initPackets(byte packetIndex) {
        //Register all classes in the minecrafttransportsimulator.packets.instances package.
        //Ideally this could be done via reflection, but it doesn't work too well so we don't do that.

        //Entity packets.
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketEntityColorChange.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketEntityInstrumentChange.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketEntityRiderChange.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketEntityTextChange.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketEntityTowingChange.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketEntityVariableIncrement.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketEntityVariableSet.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketEntityVariableToggle.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketEntityInteract.class);

        //Bullet packets.
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketEntityBulletHitBlock.class);

        //Fluid tank packets.
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketFluidTankChange.class);

        //Inventory container packets.
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketInventoryContainerChange.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketItemInteractable.class);

        //Furnace packets.
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketFurnaceFuelAdd.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketFurnaceTimeSet.class);

        //GUI packets.
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketGUIRequest.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketEntityGUIRequest.class);

        //Part packets.
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketPartChange.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketPartGun.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketPartEffector.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketPartEngine.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketPartGroundDevice.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketPartInteractable.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketPartInteractableInteract.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketPartSeat.class);

        //Player packets.
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketPlayerChatMessage.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketPlayerCraftItem.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketPlayerItemTransfer.class);

        //Radio packets.
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketRadioStateChange.class);

        //Tile entity packets.
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketTileEntityLoaderConnection.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketTileEntityFuelPumpConnection.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketTileEntityFuelPumpDispense.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketTileEntityRoadCollisionUpdate.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketTileEntityPoleChange.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketTileEntityPoleCollisionUpdate.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketTileEntityRoadChange.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketTileEntityRoadConnectionUpdate.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketTileEntitySignalControllerChange.class);

        //Vehicle packets.
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketVehicleBeaconChange.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketVehicleControlNotification.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketVehicleServerMovement.class);

        //World packets.
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketWorldSavedDataRequest.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketWorldSavedDataUpdate.class);
    }
}
