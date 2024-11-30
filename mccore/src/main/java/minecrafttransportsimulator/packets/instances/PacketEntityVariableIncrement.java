package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet used to increment variable states.  Sent from clients to servers to
 * tell them to change the custom state of an entity variable, and then sent
 * back to all clients to have them update those states.  May also be sent directly
 * from a server to all clients if the server is the one that changed the state.
 *
 * @author don_bruce
 */
public class PacketEntityVariableIncrement extends APacketEntity<AEntityD_Definable<?>> {
    private final String variableKey;
    private final double incrementValue;
    private final double minValue;
    private final double maxValue;

    public PacketEntityVariableIncrement(ComputedVariable variable, double incrementValue, double minValue, double maxValue) {
        super(variable.entity);
        this.variableKey = variable.variableKey;
        this.incrementValue = incrementValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public PacketEntityVariableIncrement(ComputedVariable variable, double incrementValue) {
        this(variable, incrementValue, -Double.MAX_VALUE, Double.MAX_VALUE);
    }

    public PacketEntityVariableIncrement(ByteBuf buf) {
        super(buf);
        this.variableKey = readStringFromBuffer(buf);
        this.incrementValue = buf.readDouble();
        this.minValue = buf.readDouble();
        this.maxValue = buf.readDouble();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeStringToBuffer(variableKey, buf);
        buf.writeDouble(incrementValue);
        buf.writeDouble(minValue);
        buf.writeDouble(maxValue);
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityD_Definable<?> entity) {
        return entity.getOrCreateVariable(variableKey).increment(incrementValue, minValue, maxValue, false);
    }
}
