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
    private UUID initiatingPlayerID;

    public PacketEntityVariableIncrement(ComputedVariable variable, double incrementValue, double minValue, double maxValue) {
        this(variable, incrementValue, minValue, maxValue, null);
    }

    public PacketEntityVariableIncrement(ComputedVariable variable, double incrementValue, double minValue, double maxValue, UUID initiatingPlayerID) {
        super(variable.entity);
        this.variableKey = variable.variableKey;
        this.incrementValue = incrementValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.initiatingPlayerID = initiatingPlayerID;
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
        this.initiatingPlayerID = buf.readBoolean() ? readUUIDFromBuffer(buf) : null;
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeStringToBuffer(variableKey, buf);
        buf.writeDouble(incrementValue);
        buf.writeDouble(minValue);
        buf.writeDouble(maxValue);
        buf.writeBoolean(initiatingPlayerID != null);
        if (initiatingPlayerID != null) {
            writeUUIDToBuffer(initiatingPlayerID, buf);
        }
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityD_Definable<?> entity) {
        boolean changed = entity.getOrCreateVariable(variableKey).increment(incrementValue, minValue, maxValue, false);
        if (changed) {
            PlayerAnimationSystem.recordVariableInteraction(entity, variableKey, initiatingPlayerID);
        }
        return changed;
    }

    @Override
    public void handleFromClient(AWrapperWorld world, IWrapperPlayer sendingPlayer) {
        initiatingPlayerID = sendingPlayer.getID();
        super.handleFromClient(world, sendingPlayer);
    }
}
