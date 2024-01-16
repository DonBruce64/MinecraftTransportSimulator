package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.components.APacketPlayer;
import minecrafttransportsimulator.systems.LanguageSystem;
import minecrafttransportsimulator.systems.LanguageSystem.LanguageEntry;

/**
 * Packet used for sending the player chat messages from the server.  Mainly for informing them
 * about things they did to a vehicle they interacted with.  Do NOT send this packet to the server
 * or it will crash when it tries to display chat messages on something without a screen!
 *
 * @author don_bruce
 */
public class PacketPlayerChatMessage extends APacketPlayer {
    private final LanguageEntry language;
    private final String[] message;

    public PacketPlayerChatMessage(IWrapperPlayer player, LanguageEntry language, Object... args) {
        super(player);
        this.language = language;
        this.message = new String[args.length];
        for (byte i = 0; i < args.length; ++i) {
            message[i] = args[i].toString();
        }
    }

    public PacketPlayerChatMessage(ByteBuf buf) {
        super(buf);
        this.language = LanguageSystem.coreLanguageEntires.get(readStringFromBuffer(buf));
        this.message = new String[buf.readByte()];
        for (byte i = 0; i < message.length; ++i) {
            message[i] = readStringFromBuffer(buf);
        }
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeStringToBuffer(language.key, buf);
        buf.writeByte(message.length);
        for (byte i = 0; i < message.length; ++i) {
            writeStringToBuffer(message[i], buf);
        }
    }

    @Override
    public void handle(AWrapperWorld world, IWrapperPlayer player) {
        player.displayChatMessage(language, (Object[]) message);
    }
}
