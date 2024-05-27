package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet sent to entities to update their key linking.  This is sent from the
 * server to all clients when a new key is bound.
 *
 * @author don_bruce
 */
public class PacketEntityKeyChange extends APacketEntity<EntityVehicleF_Physics> {
    private final UUID keyUUID;

    public PacketEntityKeyChange(EntityVehicleF_Physics entity, UUID keyUUID) {
        super(entity);
        this.keyUUID = keyUUID;
    }

    public PacketEntityKeyChange(ByteBuf buf) {
        super(buf);
        this.keyUUID = readUUIDFromBuffer(buf);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeUUIDToBuffer(keyUUID, buf);
    }

    @Override
    public boolean handle(AWrapperWorld world, EntityVehicleF_Physics entity) {
        entity.keyUUID = keyUUID;
        return false;
    }
}
