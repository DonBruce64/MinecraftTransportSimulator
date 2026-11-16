package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.components.APacketEntityInteract;

/**
 * Packet sent to entities to cause them to do custom keypress logic.  This is sent from clients doing keypresses
 * to the server to process logic.  Server will handle logic and therefore this packet is not sent to all clients.
 * The entity is the seat the keypress came from if it came from one, so keybinds can be limited to some seats.
 * The seat only counts if the player who sent this is riding it.
 *
 * @author don_bruce
 */
public class PacketEntityCustomKeypress extends APacketEntityInteract<AEntityF_Multipart<?>, IWrapperPlayer> {
    private final byte keyIndex;
    private final boolean keyPressed;

    public PacketEntityCustomKeypress(AEntityF_Multipart<?> entity, IWrapperPlayer player, int keyIndex, boolean keyPressed) {
        super(entity, player);
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
    public boolean handle(AWrapperWorld world, AEntityF_Multipart<?> entity, IWrapperPlayer player) {
        AEntityF_Multipart<?> masterEntity = entity instanceof APart ? ((APart) entity).masterEntity : entity;
        PartSeat seat = entity instanceof PartSeat && player.equals(entity.rider) ? (PartSeat) entity : null;
        masterEntity.handleCustomKeypress(keyIndex, keyPressed, seat);
        masterEntity.allParts.forEach(part -> part.handleCustomKeypress(keyIndex, keyPressed, seat));
        return false;
    }
}
