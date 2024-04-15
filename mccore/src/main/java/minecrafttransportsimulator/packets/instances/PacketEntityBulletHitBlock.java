package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketBase;

/**
 * Packet sent when a bullet hits a block.
 *
 * @author don_bruce
 */
public class PacketEntityBulletHitBlock extends APacketBase {
    private final UUID gunID;
    private final int bulletNumber;
    private final Point3D blockPosition;
    private final Axis blockSide;

    public PacketEntityBulletHitBlock(PartGun gun, int bulletNumber, Point3D blockPosition, Axis blockSide) {
        super(null);
        this.gunID = gun.uniqueUUID;
        this.bulletNumber = bulletNumber;
        this.blockPosition = blockPosition;
        this.blockSide = blockSide;

    }

    public PacketEntityBulletHitBlock(ByteBuf buf) {
        super(buf);
        this.gunID = readUUIDFromBuffer(buf);
        this.bulletNumber = buf.readInt();
        this.blockPosition = readPoint3dFromBuffer(buf);
        this.blockSide = Axis.values()[buf.readByte()];
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeUUIDToBuffer(gunID, buf);
        buf.writeInt(bulletNumber);
        writePoint3dToBuffer(blockPosition, buf);
        buf.writeByte(blockSide.ordinal());
    }

    @Override
    public void handle(AWrapperWorld world) {
        EntityBullet.performBlockHitLogic(world.getBulletGun(gunID), bulletNumber, blockPosition, blockSide);
    }
}
