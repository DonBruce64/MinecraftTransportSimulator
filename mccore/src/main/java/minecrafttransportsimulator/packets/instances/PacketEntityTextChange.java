package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet sent to entities to update their text.  This is sent from the
 * text GUI to servers to update the text, and then sent back to all clients for syncing.
 *
 * @author don_bruce
 */
public class PacketEntityTextChange extends APacketEntity<AEntityD_Definable<?>> {
    private final String textKey;
    private final String textValue;

    public PacketEntityTextChange(AEntityD_Definable<?> entity, String textKey, String textValue) {
        super(entity);
        this.textKey = textKey;
        this.textValue = textValue;
    }

    public PacketEntityTextChange(ByteBuf buf) {
        super(buf);
        this.textKey = readStringFromBuffer(buf);
        this.textValue = readStringFromBuffer(buf);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeStringToBuffer(textKey, buf);
        writeStringToBuffer(textValue, buf);
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityD_Definable<?> entity) {
        entity.updateText(textKey, textValue);
        return true;
    }
}
