package minecrafttransportsimulator.packets.components;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;

/**
 * Packet class that includes a default implementation for transmitting a secondary
 * entity along with a primary entity.  Used for packets where said secondary entity interacts
 * with the primary entity.
 * Similar to {@link APacketPlayer}, but with an entity parameter and bolted-on
 * to internal entity packet logic.
 *
 * @author don_bruce
 */
public abstract class APacketEntityInteract<MainEntityType extends AEntityA_Base, InteractingEntityType extends IWrapperEntity> extends APacketEntity<MainEntityType> {
    private final UUID entityID;

    public APacketEntityInteract(MainEntityType entity, InteractingEntityType interactor) {
        super(entity);
        this.entityID = interactor.getID();
    }

    public APacketEntityInteract(ByteBuf buf) {
        super(buf);
        this.entityID = readUUIDFromBuffer(buf);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeUUIDToBuffer(entityID, buf);
    }

    @Override
    public boolean handle(AWrapperWorld world, MainEntityType entity) {
        @SuppressWarnings("unchecked")
        InteractingEntityType interactor = (InteractingEntityType) world.getExternalEntity(entityID);
        return interactor != null && handle(world, entity, interactor);
    }

    /**
     * Handler method with an extra parameter for the entity that this packet
     * is interacting with. If the entity is null,  then this method won't be called.
     * Saves having to do null checks for every packet type.  If this is handled on the
     * server, and a packet shouldn't be sent to all clients (like if the action failed due
     * to an issue) return false.  Otherwise, return true to send this packet on to all clients.
     * Return method has no function on clients.
     */
    protected abstract boolean handle(AWrapperWorld world, MainEntityType entity, InteractingEntityType interactor);
}
