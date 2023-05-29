package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.packets.components.APacketBase;

/**
 * Packet sent when a bullet hits an external entity.
 * Sent to servers to have them process hits on said entity.
 *
 * @author don_bruce
 */
public class PacketEntityBulletHitExternalEntity extends APacketBase {
    private final UUID gunID;
    private final UUID entityID;
    private final double amount;

    public PacketEntityBulletHitExternalEntity(IWrapperEntity entity, Damage damage) {
        super(null);
        this.gunID = damage.damgeSource.uniqueUUID;
        this.entityID = entity.getID();
        this.amount = damage.amount;
    }

    public PacketEntityBulletHitExternalEntity(ByteBuf buf) {
        super(buf);
        this.gunID = readUUIDFromBuffer(buf);
        this.entityID = readUUIDFromBuffer(buf);
        this.amount = buf.readDouble();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeUUIDToBuffer(gunID, buf);
        writeUUIDToBuffer(entityID, buf);
        buf.writeDouble(amount);
    }

    @Override
    public void handle(AWrapperWorld world) {
        PartGun gun = world.getBulletGun(gunID);
        IWrapperEntity entity = world.getExternalEntity(entityID);
        if (entity != null) {
            EntityBullet.performExternalEntityHitLogic(entity, new Damage(gun, null, amount));
        }
    }
}
