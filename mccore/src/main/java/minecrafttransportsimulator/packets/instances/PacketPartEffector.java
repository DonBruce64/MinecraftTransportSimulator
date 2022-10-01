package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.instances.PartEffector;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet used to send signals to effectors.  Currently, this is only used to increment the number of blocks
 * broken for drill-type effectors to allow for animations to visibility states.
 *
 * @author don_bruce
 */
public class PacketPartEffector extends APacketEntity<PartEffector> {

    public PacketPartEffector(PartEffector effector) {
        super(effector);
    }

    public PacketPartEffector(ByteBuf buf) {
        super(buf);
    }

    @Override
    public boolean handle(AWrapperWorld world, PartEffector effector) {
        ++effector.blocksBroken;
        return true;
    }
}
