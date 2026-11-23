package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.components.APacketEntity;

/** Shared variable update used by the regular packets and the player animation packet. */
abstract class APacketEntityVariable extends APacketEntity<AEntityD_Definable<?>> {
    private final String variableKey;
    private final Operation operation;
    private final double value;
    private final double minValue;
    private final double maxValue;

    protected APacketEntityVariable(ComputedVariable variable, Operation operation, double value, double minValue, double maxValue) {
        super(variable.entity);
        this.variableKey = variable.variableKey;
        this.operation = operation;
        this.value = value;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    protected APacketEntityVariable(AEntityD_Definable<?> entity, APacketEntityVariable update) {
        super(entity);
        this.variableKey = update.variableKey;
        this.operation = update.operation;
        this.value = update.value;
        this.minValue = update.minValue;
        this.maxValue = update.maxValue;
    }

    protected APacketEntityVariable(ByteBuf buf, Operation operation) {
        super(buf);
        this.variableKey = readStringFromBuffer(buf);
        //Regular packets imply the operation from their packet ID.
        this.operation = operation != null ? operation : Operation.values()[buf.readUnsignedByte()];
        this.value = this.operation != Operation.TOGGLE ? buf.readDouble() : 0;
        this.minValue = this.operation == Operation.INCREMENT ? buf.readDouble() : 0;
        this.maxValue = this.operation == Operation.INCREMENT ? buf.readDouble() : 0;
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        writeUpdate(buf, false);
    }

    protected final void writeUpdate(ByteBuf buf, boolean includeOperation) {
        super.writeToBuffer(buf);
        writeStringToBuffer(variableKey, buf);
        if (includeOperation) {
            buf.writeByte(operation.ordinal());
        }
        if (operation != Operation.TOGGLE) {
            buf.writeDouble(value);
        }
        if (operation == Operation.INCREMENT) {
            buf.writeDouble(minValue);
            buf.writeDouble(maxValue);
        }
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityD_Definable<?> entity) {
        return applyUpdate(getVariable(entity)) || operation != Operation.INCREMENT;
    }

    @Override
    public void handleFromClient(AWrapperWorld world, IWrapperPlayer sendingPlayer) {
        AEntityD_Definable<?> entity = getEntity(world);
        if (PacketVehiclePlayerAnimation.hasActorAnimations(entity)) {
            if (applyUpdate(getVariable(entity))) {
                InterfaceManager.packetInterface.sendToAllClients(new PacketVehiclePlayerAnimation(entity, this, sendingPlayer.getID()));
            } else if (operation != Operation.INCREMENT) {
                //Preserve the normal Set/Toggle relay, even if nothing changed on the server.
                InterfaceManager.packetInterface.sendToAllClients(this);
            }
        } else {
            handleAndForward(world, entity);
        }
    }

    /** Sends a variable update already applied by a server-side action. */
    public final void sendToClients(AEntityD_Definable<?> entity, UUID playerID) {
        InterfaceManager.packetInterface.sendToAllClients(playerID != null && PacketVehiclePlayerAnimation.hasActorAnimations(entity)
                ? new PacketVehiclePlayerAnimation(entity, this, playerID) : this);
    }

    protected final ComputedVariable getVariable(AEntityD_Definable<?> entity) {
        return entity.getOrCreateVariable(variableKey);
    }

    /** Applies the same change regardless of whether it carries an animation actor. */
    protected final boolean applyUpdate(ComputedVariable variable) {
        double previousValue = variable.currentValue;
        switch (operation) {
            case TOGGLE:
                variable.toggle(false);
                break;
            case SET:
                variable.setTo(value, false);
                break;
            case INCREMENT:
                return variable.increment(value, minValue, maxValue, false);
        }
        return variable.currentValue != previousValue;
    }

    protected enum Operation {
        SET,
        TOGGLE,
        INCREMENT
    }
}
