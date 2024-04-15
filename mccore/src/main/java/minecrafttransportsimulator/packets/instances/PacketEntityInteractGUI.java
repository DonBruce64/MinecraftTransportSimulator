package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.components.APacketEntityInteract;

/**
 * Packet used to tell entity a player has started interacting with a GUI on them.  Sent by clients when a player
 * opens a GUI, and by the server to all clients to let them know the player has started or stopped interacting.
 *
 * @author don_bruce
 */
public class PacketEntityInteractGUI extends APacketEntityInteract<AEntityD_Definable<?>, IWrapperPlayer> {
    private final boolean opened;

    public PacketEntityInteractGUI(AEntityD_Definable<?> entity, IWrapperPlayer player, boolean opened) {
        super(entity, player);
        this.opened = opened;
    }

    public PacketEntityInteractGUI(ByteBuf buf) {
        super(buf);
        this.opened = buf.readBoolean();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeBoolean(opened);
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityD_Definable<?> entity, IWrapperPlayer player) {
        if (opened) {
            entity.playersInteracting.add(player);
        } else {
            entity.playersInteracting.remove(player);
        }
        return true;
    }
}
