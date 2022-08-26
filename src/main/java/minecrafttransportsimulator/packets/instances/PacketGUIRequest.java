package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.guis.instances.GUIBooklet;
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

    public PacketGUIRequest(IWrapperPlayer player, GUIType guiRequested) {
        super(player);
        this.guiRequested = guiRequested;
    }

    public PacketGUIRequest(ByteBuf buf) {
        super(buf);
        this.guiRequested = GUIType.values()[buf.readByte()];
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeByte(guiRequested.ordinal());
    }

    @Override
    public void handle(AWrapperWorld world, IWrapperPlayer player) {
        switch (guiRequested) {
            case BOOKLET:
                new GUIBooklet((ItemItem) player.getHeldItem());
        }
    }

    public enum GUIType {
        BOOKLET
    }
}
