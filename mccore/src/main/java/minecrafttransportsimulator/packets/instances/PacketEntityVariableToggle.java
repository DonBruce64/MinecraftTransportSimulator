package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.ComputedVariable;

/**
 * Packet used to toggle variable states.  Sent from clients to servers to
 * tell them to change the custom state of an entity variable, and then sent
 * back to all clients to have them update those states.
 *
 * @author don_bruce
 */
public class PacketEntityVariableToggle extends APacketEntityVariable {
    public PacketEntityVariableToggle(ComputedVariable variable) {
        super(variable, Operation.TOGGLE, 0, 0, 0);
    }

    public PacketEntityVariableToggle(ByteBuf buf) {
        super(buf, Operation.TOGGLE);
    }
}
