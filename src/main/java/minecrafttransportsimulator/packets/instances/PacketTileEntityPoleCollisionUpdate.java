package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet sent to poles to update their collision.  This gets sent when a neighbor block updates.
 * This can either be a change in the block itself, or a pole changing components.
 *
 * @author don_bruce
 */
public class PacketTileEntityPoleCollisionUpdate extends APacketEntity<TileEntityPole> {

    public PacketTileEntityPoleCollisionUpdate(TileEntityPole pole) {
        super(pole);
    }

    public PacketTileEntityPoleCollisionUpdate(ByteBuf buf) {
        super(buf);
    }

    @Override
    protected boolean handle(AWrapperWorld world, TileEntityPole pole) {
        pole.updateCollision(false);
        return false;
    }
}