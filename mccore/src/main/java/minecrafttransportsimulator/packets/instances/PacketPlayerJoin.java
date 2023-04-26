package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.components.APacketPlayer;

/**
 * Packet used for sending the server a notice that the player has joined it and is ready to
 * receive data from it to their client.
 *
 * @author don_bruce
 */
public class PacketPlayerJoin extends APacketPlayer {

    public PacketPlayerJoin(IWrapperPlayer player) {
        super(player);
    }

    public PacketPlayerJoin(ByteBuf buf) {
        super(buf);
    }

    @Override
    public void handle(AWrapperWorld world, IWrapperPlayer player) {
        player.getWorld().onPlayerJoin(player);
    }
}
