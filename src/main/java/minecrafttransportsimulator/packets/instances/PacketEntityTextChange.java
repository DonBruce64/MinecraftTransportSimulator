package minecrafttransportsimulator.packets.instances;

import java.util.LinkedHashMap;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet sent to entities to update their text lines.  This is sent from the
 * text GUI to servers to update the text, and then sent back to all clients for syncing.
 *
 * @author don_bruce
 */
public class PacketEntityTextChange extends APacketEntity<AEntityD_Definable<?>> {
    private final LinkedHashMap<String, String> textLines;

    public PacketEntityTextChange(AEntityD_Definable<?> entity, LinkedHashMap<String, String> textLines) {
        super(entity);
        this.textLines = textLines;
    }

    public PacketEntityTextChange(ByteBuf buf) {
        super(buf);
        byte textLineCount = buf.readByte();
        this.textLines = new LinkedHashMap<>();
        for (byte i = 0; i < textLineCount; ++i) {
            textLines.put(readStringFromBuffer(buf), readStringFromBuffer(buf));
        }
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeByte(textLines.size());
        textLines.forEach((key, value) -> {
            writeStringToBuffer(key, buf);
            writeStringToBuffer(value, buf);
        });
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityD_Definable<?> entity) {
        entity.updateText(textLines);
        return true;
    }
}
