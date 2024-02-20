package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.AWrapperWorld.BlockHitResult;
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
    private final BlockHitResult hitResult;

    public PacketEntityBulletHitGeneric(PartGun gun, int bulletNumber, Point3D position, EntityBullet.HitType hitType, BlockHitResult hitResult) {
        super(null);
        this.gunID = gun.uniqueUUID;
        this.bulletNumber = bulletNumber;
        this.position = position;
        this.hitType = hitType;
        this.hitResult = hitResult;
    }

    public PacketEntityBulletHitGeneric(ByteBuf buf) {
        super(buf);
        this.gunID = readUUIDFromBuffer(buf);
        this.bulletNumber = buf.readInt();
        this.position = readPoint3dFromBuffer(buf);
        this.hitType = EntityBullet.HitType.values()[buf.readByte()];
        if(buf.readBoolean()) {
        	this.hitResult = new BlockHitResult(position, ABlockBase.Axis.values()[buf.readByte()]);
        }else {
        	this.hitResult = null;
        }
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeUUIDToBuffer(gunID, buf);
        buf.writeInt(bulletNumber);
        writePoint3dToBuffer(position, buf);
        buf.writeByte(hitType.ordinal());
        if(hitResult != null) {
        	buf.writeBoolean(true);
        	buf.writeByte(hitResult.side.ordinal());
        }else {
        	buf.writeBoolean(false);
        }
    }

    @Override
    public void handle(AWrapperWorld world) {
        EntityBullet.performGenericHitLogic(world.getBulletGun(gunID), bulletNumber, position, hitType, hitResult);
    }
}
