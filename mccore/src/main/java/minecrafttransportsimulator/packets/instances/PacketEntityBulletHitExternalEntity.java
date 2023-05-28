package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.packets.components.APacketEntityBullet;

/**
 * Packet sent when a bullet hits an external entity.
 *
 * @author don_bruce
 */
public class PacketEntityBulletHitExternalEntity extends APacketEntityBullet {
    private final UUID entityID;

    public PacketEntityBulletHitExternalEntity(EntityBullet bullet, IWrapperEntity entity) {
        super(bullet);
        this.entityID = entity.getID();
    }

    public PacketEntityBulletHitExternalEntity(ByteBuf buf) {
        super(buf);
        this.entityID = readUUIDFromBuffer(buf);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeUUIDToBuffer(entityID, buf);
    }

    @Override
    public boolean handle(AWrapperWorld world, EntityBullet bullet) {
        IWrapperEntity entity = world.getExternalEntity(entityID);
        if (entity != null) {
            bullet.performExternalEntityHitLogic(entity);
            return true;
        } else {
            return false;
        }
    }
}
