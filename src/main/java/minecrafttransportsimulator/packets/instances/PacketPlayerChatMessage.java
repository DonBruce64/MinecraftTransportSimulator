package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage.LanguageEntry;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.components.APacketPlayer;

/**
 * Packet used for sending the player chat messages from the server.  Mainly for informing them
 * about things they did to a vehicle they interacted with.  Do NOT send this packet to the server
 * or it will crash when it tries to display chat messages on something without a screen!
 *
 * @author don_bruce
 */
public class PacketPlayerChatMessage extends APacketPlayer {
    private final String message;

    public PacketPlayerChatMessage(IWrapperPlayer player, LanguageEntry language) {
        super(player);
        this.message = language.key;
    }

    public PacketPlayerChatMessage(IWrapperPlayer player, String message) {
        super(player);
        this.message = message;
    }

    public PacketPlayerChatMessage(ByteBuf buf) {
        super(buf);
        this.message = readStringFromBuffer(buf);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeStringToBuffer(message, buf);
    }

    @Override
    public void handle(AWrapperWorld world, IWrapperPlayer player) {
        LanguageEntry language = JSONConfigLanguage.coreEntries.get(message);
        if (language != null) {
            player.displayChatMessage(language);
        } else {
            player.displayChatMessage(JSONConfigLanguage.SYSTEM_DEBUG, message);
        }
    }
}
