package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.ComputedVariable;

/**
 * Packet used to set variable states.  Sent from clients to servers to
 * tell them to change the custom state of an entity variable, and then sent
 * back to all clients to have them update those states.  May also be sent directly
 * from a server to all clients if the server is the one that changed the state.
 * Note that due to networking routing it is possible for packets to arrive out of
 * order, so it is not advised to send multiple Set packets if they are used to adjust
 * a variable to a specific value.  Rather, use the short-hand form of {@link PacketEntityVariableIncrement}.
 *
 * @author don_bruce
 */
public class PacketEntityVariableSet extends APacketEntityVariable {
    public PacketEntityVariableSet(ComputedVariable variable, double variableValue) {
        super(variable, Operation.SET, variableValue, 0, 0);
    }

    public PacketEntityVariableSet(ByteBuf buf) {
        super(buf, Operation.SET);
    }
}
