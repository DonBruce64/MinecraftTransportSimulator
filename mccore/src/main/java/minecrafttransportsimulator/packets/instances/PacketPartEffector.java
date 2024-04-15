package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.instances.PartEffector;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet used to send signals to effectors.  Currently, this is used to increment the number of blocks
 * broken for drill-type effectors to allow for animations to visibility states, or to notify the effector
 * on the client that it has done an operation. 
 *
 * @author don_bruce
 */
public class PacketPartEffector extends APacketEntity<PartEffector> {
    private final boolean breakBlocks;

    public PacketPartEffector(PartEffector effector, boolean breakBlocks) {
        super(effector);
        this.breakBlocks = breakBlocks;
    }

    public PacketPartEffector(ByteBuf buf) {
        super(buf);
        this.breakBlocks = buf.readBoolean();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeBoolean(breakBlocks);
    }

    @Override
    public boolean handle(AWrapperWorld world, PartEffector effector) {
        if (breakBlocks) {
            ++effector.blocksBroken;
        } else {
            effector.activatedThisTick = true;
        }
        return true;
    }
}
