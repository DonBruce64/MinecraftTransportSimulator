package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet sent to entities to cause them to do custom keypress logic.  This is sent from clients doing keypresses
 * to the server to process logic.  Server will handle logic and therefore this packet is not sent to all clients.
 *
 * @author don_bruce
 */
public class PacketEntityCustomKeypress extends APacketEntity<AEntityF_Multipart<?>> {
    private final byte keyIndex;
    private final boolean keyPressed;

    public PacketEntityCustomKeypress(AEntityF_Multipart<?> entity, int keyIndex, boolean keyPressed) {
        super(entity);
        this.keyIndex = (byte) keyIndex;
        this.keyPressed = keyPressed;
    }

    public PacketEntityCustomKeypress(ByteBuf buf) {
        super(buf);
        this.keyIndex = buf.readByte();
        this.keyPressed = buf.readBoolean();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeByte(keyIndex);
        buf.writeBoolean(keyPressed);
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityF_Multipart<?> entity) {
        AEntityF_Multipart<?> masterEntity = entity instanceof APart ? ((APart) entity).masterEntity : entity;
        masterEntity.handleCustomKeypress(keyIndex, keyPressed);
        masterEntity.allParts.forEach(part -> part.handleCustomKeypress(keyIndex, keyPressed));
        return false;
    }
}
