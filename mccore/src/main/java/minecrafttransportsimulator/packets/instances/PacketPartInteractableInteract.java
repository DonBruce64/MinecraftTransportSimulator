package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.instances.PartInteractable;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.components.APacketEntityInteract;

/**
 * Packet used to tell interactables a player has started interacting with them.  Sent by clients when a player
 * opens a GUI, and by the server to all clients to let them know the player has started or stopped interacting.
 *
 * @author don_bruce
 */
public class PacketPartInteractableInteract extends APacketEntityInteract<PartInteractable, IWrapperPlayer> {
    private final boolean opened;

    public PacketPartInteractableInteract(PartInteractable interactable, IWrapperPlayer player, boolean opened) {
        super(interactable, player);
        this.opened = opened;
    }

    public PacketPartInteractableInteract(ByteBuf buf) {
        super(buf);
        this.opened = buf.readBoolean();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeBoolean(opened);
    }

    @Override
    public boolean handle(AWrapperWorld world, PartInteractable interactable, IWrapperPlayer player) {
        if (opened) {
            interactable.playersInteracting.add(player);
        } else {
            interactable.playersInteracting.remove(player);
        }
        return true;
    }
}
