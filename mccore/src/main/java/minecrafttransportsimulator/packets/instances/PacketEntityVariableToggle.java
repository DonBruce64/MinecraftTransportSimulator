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
 * Packet used to toggle variable states.  Sent from clients to servers to
 * tell them to change the custom state of an entity variable, and then sent
 * back to all clients to have them update those states.
 *
 * @author don_bruce
 */
public class PacketEntityVariableToggle extends APacketEntity<AEntityD_Definable<?>> {
    private final String variableKey;
    private UUID initiatingPlayerID;

    public PacketEntityVariableToggle(ComputedVariable variable) {
        this(variable, null);
    }

    public PacketEntityVariableToggle(ComputedVariable variable, UUID initiatingPlayerID) {
        super(variable.entity);
        this.variableKey = variable.variableKey;
        this.initiatingPlayerID = initiatingPlayerID;
    }

    public PacketEntityVariableToggle(ByteBuf buf) {
        super(buf);
        this.variableKey = readStringFromBuffer(buf);
        this.initiatingPlayerID = buf.readBoolean() ? readUUIDFromBuffer(buf) : null;
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeStringToBuffer(variableKey, buf);
        buf.writeBoolean(initiatingPlayerID != null);
        if (initiatingPlayerID != null) {
            writeUUIDToBuffer(initiatingPlayerID, buf);
        }
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityD_Definable<?> entity) {
        ComputedVariable variable = entity.getOrCreateVariable(variableKey);
        double previousValue = variable.currentValue;
        variable.toggle(false);
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
