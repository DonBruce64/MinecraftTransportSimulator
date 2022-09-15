package minecrafttransportsimulator.packets.components;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

/**
 * Packet class that includes a default implementation for transmitting a player
 * with a packet.  Useful for packets where something happens to the player, or the player
 * triggers something.
 * Similar to {@link APacketEntityInteract}, but without an entity to interact with and
 * without callback functionality, as player-based packets are usually one-way state changes,
 * and not synncing or actions in the world.
 *
 * @author don_bruce
 */
public abstract class APacketPlayer extends APacketBase {
    private final UUID playerID;

    public APacketPlayer(IWrapperPlayer player) {
        super(null);
        this.playerID = player.getID();
    }

    public APacketPlayer(ByteBuf buf) {
        super(buf);
        this.playerID = readUUIDFromBuffer(buf);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeUUIDToBuffer(playerID, buf);
    }

    @Override
    public void handle(AWrapperWorld world) {
        IWrapperPlayer player = (IWrapperPlayer) world.getExternalEntity(playerID);
        if (player != null) {
            handle(world, player);
        }
    }

    /**
     * Handler method with an extra parameter for the player for this packet.
     * If the player is null,  then this method won't be called.
     * Saves having to do null checks for every packet type.
     */
    protected abstract void handle(AWrapperWorld world, IWrapperPlayer player);
}
