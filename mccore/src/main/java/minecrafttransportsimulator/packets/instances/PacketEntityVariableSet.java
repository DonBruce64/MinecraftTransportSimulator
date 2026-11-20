package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.components.APacketEntity;
import minecrafttransportsimulator.rendering.PlayerAnimationSystem;

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
    private final String variableKey;
    private final double variableValue;
    private UUID initiatingPlayerID;

    public PacketEntityVariableSet(ComputedVariable variable, double variableValue) {
        this(variable, variableValue, null);
    }

    public PacketEntityVariableSet(ComputedVariable variable, double variableValue, UUID initiatingPlayerID) {
        super(variable.entity);
        this.variableKey = variable.variableKey;
        this.variableValue = variableValue;
        this.initiatingPlayerID = initiatingPlayerID;
    }

    public PacketEntityVariableSet(ByteBuf buf) {
        super(buf);
        this.variableKey = readStringFromBuffer(buf);
        this.variableValue = buf.readDouble();
        this.initiatingPlayerID = buf.readBoolean() ? readUUIDFromBuffer(buf) : null;
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeStringToBuffer(variableKey, buf);
        buf.writeDouble(variableValue);
        buf.writeBoolean(initiatingPlayerID != null);
        if (initiatingPlayerID != null) {
            writeUUIDToBuffer(initiatingPlayerID, buf);
        }
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityD_Definable<?> entity) {
        ComputedVariable variable = entity.getOrCreateVariable(variableKey);
        double previousValue = variable.currentValue;
        variable.setTo(variableValue, false);
        if (variable.currentValue != previousValue) {
            PlayerAnimationSystem.recordVariableInteraction(entity, variableKey, initiatingPlayerID);
        }
        return true;
    }

    @Override
    public void handleFromClient(AWrapperWorld world, IWrapperPlayer sendingPlayer) {
        initiatingPlayerID = sendingPlayer.getID();
        super.handleFromClient(world, sendingPlayer);
    }
}
