package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

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
public class PacketEntityVariableSet extends APacketEntity<AEntityD_Definable<?>> {
    private final String variableName;
    private final double variableValue;

    public PacketEntityVariableSet(AEntityD_Definable<?> entity, String variableName, double variableValue) {
        super(entity);
        this.variableName = variableName;
        this.variableValue = variableValue;
    }

    public PacketEntityVariableSet(ByteBuf buf) {
        super(buf);
        this.variableName = readStringFromBuffer(buf);
        this.variableValue = buf.readDouble();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeStringToBuffer(variableName, buf);
        buf.writeDouble(variableValue);
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityD_Definable<?> entity) {
        entity.setVariable(variableName, variableValue);
        return true;
    }
}
