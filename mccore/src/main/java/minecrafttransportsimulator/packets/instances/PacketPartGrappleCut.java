package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.instances.PartGrapple;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.components.APacketEntityInteract;

/**
 * Packet sent to the server when a player attacks a grapple's line while holding something sharp.  The client
 * works out which line they hit from where it's drawn, and the server checks they're holding something sharp
 * and are close enough to the line before snapping it.  See {@link PartGrapple#getLineAttacked(IWrapperPlayer)}.
 */
public class PacketPartGrappleCut extends APacketEntityInteract<PartGrapple, IWrapperPlayer> {

    public PacketPartGrappleCut(PartGrapple grapple, IWrapperPlayer player) {
        super(grapple, player);
    }

    public PacketPartGrappleCut(ByteBuf buf) {
        super(buf);
    }

    @Override
    public boolean handle(AWrapperWorld world, PartGrapple grapple, IWrapperPlayer player) {
        //Clients see the snap through the grapple's synced variables, so nothing to send on.
        grapple.cutLine(player);
        return false;
    }
}
