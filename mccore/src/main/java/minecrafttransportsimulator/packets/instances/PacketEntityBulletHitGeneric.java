package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketBase;

/**
 * Packet sent when a bullet hits something.
 *
 * @author don_bruce
 */
public class PacketEntityBulletHitGeneric extends APacketBase {
    private final UUID gunID;
    private final int bulletNumber;
    private final Point3D position;
    private final EntityBullet.HitType hitType;

    public PacketEntityBulletHitGeneric(PartGun gun, int bulletNumber, Point3D position, EntityBullet.HitType hitType) {
        super(null);
        this.gunID = gun.uniqueUUID;
        this.bulletNumber = bulletNumber;
        this.position = position;
        this.hitType = hitType;
    }

    public PacketEntityBulletHitGeneric(ByteBuf buf) {
        super(buf);
        this.gunID = readUUIDFromBuffer(buf);
        this.bulletNumber = buf.readInt();
        this.position = readPoint3dFromBuffer(buf);
        this.hitType = EntityBullet.HitType.values()[buf.readByte()];
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeUUIDToBuffer(gunID, buf);
        buf.writeInt(bulletNumber);
        writePoint3dToBuffer(position, buf);
        buf.writeByte(hitType.ordinal());
    }

    @Override
    public void handle(AWrapperWorld world) {
        EntityBullet.performGenericHitLogic(world.getBulletGun(gunID), bulletNumber, position, hitType);
    }
}
