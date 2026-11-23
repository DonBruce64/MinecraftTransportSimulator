package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.ComputedVariable;

/**
 * Packet used to increment variable states.  Sent from clients to servers to
 * tell them to change the custom state of an entity variable, and then sent
 * back to all clients to have them update those states.  May also be sent directly
 * from a server to all clients if the server is the one that changed the state.
 *
 * @author don_bruce
 */
public class PacketEntityVariableIncrement extends APacketEntityVariable {
    public PacketEntityVariableIncrement(ComputedVariable variable, double incrementValue, double minValue, double maxValue) {
        super(variable, Operation.INCREMENT, incrementValue, minValue, maxValue);
    }

    public PacketEntityVariableIncrement(ComputedVariable variable, double incrementValue) {
        this(variable, incrementValue, -Double.MAX_VALUE, Double.MAX_VALUE);
    }

    public PacketEntityVariableIncrement(ByteBuf buf) {
        super(buf, Operation.INCREMENT);
    }
}
