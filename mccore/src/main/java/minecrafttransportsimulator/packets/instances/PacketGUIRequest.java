package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.guis.instances.GUIBooklet;
import minecrafttransportsimulator.guis.instances.GUIVehiclePhone;
import minecrafttransportsimulator.items.instances.ItemItem;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.components.APacketPlayer;

/**
 * Packet sent to the server to open generic GUIs.  The GUI to be sent is an enum and is used to open
 * the proper GUI.  This packet is sent from servers the specific clients when they click on something
 * to open a GUI.  We do this as it lets us do validation, and prevents handling the request on multiple
 * clients, where multiple GUIs may be opened.
 *
 * @author don_bruce
 */
public class PacketGUIRequest extends APacketPlayer {
    private final GUIType guiRequested;
    private final ItemItem item;

    public PacketGUIRequest(IWrapperPlayer player, GUIType guiRequested, ItemItem item) {
        super(player);
        this.guiRequested = guiRequested;
        this.item = item;
    }

    public PacketGUIRequest(ByteBuf buf) {
        super(buf);
        this.guiRequested = GUIType.values()[buf.readByte()];
        this.item = readItemFromBuffer(buf);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeByte(guiRequested.ordinal());
        writeItemToBuffer(item, buf);
    }

    @Override
    public void handle(AWrapperWorld world, IWrapperPlayer player) {
        switch (guiRequested) {
            case BOOKLET:
                new GUIBooklet(item);
                break;
            case PHONE:
                new GUIVehiclePhone(item, player);
                break;
        }
    }

    public enum GUIType {
        BOOKLET,
        PHONE
    }
}
