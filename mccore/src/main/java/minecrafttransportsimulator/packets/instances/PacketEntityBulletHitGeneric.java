package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntityBullet;

/**
 * Packet sent when a bullet hits something.
 *
 * @author don_bruce
 */
public class PacketEntityBulletHitGeneric extends APacketEntityBullet {
    private final Point3D position;
    private final EntityBullet.HitType hitType;

    public PacketEntityBulletHitGeneric(EntityBullet bullet, Point3D position, EntityBullet.HitType hitType) {
        super(bullet);
        this.position = position;
        this.hitType = hitType;
    }

    public PacketEntityBulletHitGeneric(ByteBuf buf) {
        super(buf);
        this.position = readPoint3dFromBuffer(buf);
        this.hitType = EntityBullet.HitType.values()[buf.readByte()];
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writePoint3dToBuffer(position, buf);
        buf.writeByte(hitType.ordinal());
    }

    @Override
    public boolean handle(AWrapperWorld world, EntityBullet bullet) {
        bullet.performGenericHitLogic(position, hitType);
        return true;
    }
}
