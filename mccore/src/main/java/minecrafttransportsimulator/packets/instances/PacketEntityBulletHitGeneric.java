package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.entities.instances.EntityBullet.HitType;
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
    private final RotationMatrix orientation;
    private final Axis hitSide;
    private final HitType hitType;

    public PacketEntityBulletHitGeneric(PartGun gun, int bulletNumber, Point3D position, Axis hitSide, HitType hitType) {
        this(gun, bulletNumber, position, getBulletOrientation(gun, bulletNumber), hitSide, hitType);
    }

    public PacketEntityBulletHitGeneric(PartGun gun, int bulletNumber, Point3D position, RotationMatrix orientation, Axis hitSide, HitType hitType) {
        super(null);
        this.gunID = gun.uniqueUUID;
        this.bulletNumber = bulletNumber;
        this.position = position.copy();
        this.orientation = new RotationMatrix().set(orientation);
        this.hitSide = hitSide;
        this.hitType = hitType;
    }

    public PacketEntityBulletHitGeneric(ByteBuf buf) {
        super(buf);
        this.gunID = readUUIDFromBuffer(buf);
        this.bulletNumber = buf.readInt();
        this.position = readPoint3dFromBuffer(buf);
        this.hitType = HitType.values()[buf.readByte()];
        this.hitSide = Axis.values()[buf.readByte()];
        this.orientation = buf.readableBytes() >= 9 * Double.BYTES ? readRotationMatrixFromBuffer(buf) : null;
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeUUIDToBuffer(gunID, buf);
        buf.writeInt(bulletNumber);
        writePoint3dToBuffer(position, buf);
        buf.writeByte(hitType.ordinal());
        buf.writeByte(hitSide.ordinal());
        writeRotationMatrixToBuffer(orientation, buf);
    }

    @Override
    public void handle(AWrapperWorld world) {
        EntityBullet.performGenericHitLogic(world.getBulletGun(gunID), bulletNumber, position, orientation, hitSide, hitType);
    }

    private static RotationMatrix getBulletOrientation(PartGun gun, int bulletNumber) {
        EntityBullet bullet = gun.world.getBullet(gun.uniqueUUID, bulletNumber);
        return bullet != null ? bullet.orientation : new RotationMatrix();
    }

    private static void writeRotationMatrixToBuffer(RotationMatrix matrix, ByteBuf buf) {
        buf.writeDouble(matrix.m00);
        buf.writeDouble(matrix.m01);
        buf.writeDouble(matrix.m02);
        buf.writeDouble(matrix.m10);
        buf.writeDouble(matrix.m11);
        buf.writeDouble(matrix.m12);
        buf.writeDouble(matrix.m20);
        buf.writeDouble(matrix.m21);
        buf.writeDouble(matrix.m22);
    }

    private static RotationMatrix readRotationMatrixFromBuffer(ByteBuf buf) {
        RotationMatrix matrix = new RotationMatrix();
        matrix.m00 = buf.readDouble();
        matrix.m01 = buf.readDouble();
        matrix.m02 = buf.readDouble();
        matrix.m10 = buf.readDouble();
        matrix.m11 = buf.readDouble();
        matrix.m12 = buf.readDouble();
        matrix.m20 = buf.readDouble();
        matrix.m21 = buf.readDouble();
        matrix.m22 = buf.readDouble();
        matrix.convertToAngles();
        matrix.updateToAngles();
        return matrix;
    }
}
