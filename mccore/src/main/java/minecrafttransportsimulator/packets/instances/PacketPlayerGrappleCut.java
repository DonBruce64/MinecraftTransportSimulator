package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.instances.PartGrapple;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.components.APacketPlayer;

/**
 * Packet sent to the server when a player attacks while holding something sharp and a grapple is hooked into
 * an entity.  Clients don't know which entity a grapple is hooked into, so the server checks, and snaps the
 * line of any grapple hooked into the player.  See {@link PartGrapple#canCutLines(IWrapperPlayer)}.
 */
public class PacketPlayerGrappleCut extends APacketPlayer {

    public PacketPlayerGrappleCut(IWrapperPlayer player) {
        super(player);
    }

    public PacketPlayerGrappleCut(ByteBuf buf) {
        super(buf);
    }

    @Override
    public void handle(AWrapperWorld world, IWrapperPlayer player) {
        if (PartGrapple.canCutLines(player)) {
            PartGrapple.cutLinesHookedInto(player);
        }
    }
}
