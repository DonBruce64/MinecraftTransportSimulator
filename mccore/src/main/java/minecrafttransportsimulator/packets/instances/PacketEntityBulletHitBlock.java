package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.AWrapperWorld.BlockHitResult;
import minecrafttransportsimulator.packets.components.APacketEntityBullet;

/**
 * Packet sent when a bullet hits a block.
 *
 * @author don_bruce
 */
public class PacketEntityBulletHitBlock extends APacketEntityBullet {
    private final BlockHitResult hitResult;

    public PacketEntityBulletHitBlock(EntityBullet bullet, BlockHitResult hitResult) {
        super(bullet);
        this.hitResult = hitResult;
    }

    public PacketEntityBulletHitBlock(ByteBuf buf) {
        super(buf);
        this.hitResult = new BlockHitResult(readPoint3dFromBuffer(buf), ABlockBase.Axis.values()[buf.readByte()]);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writePoint3dToBuffer(hitResult.position, buf);
        buf.writeByte(hitResult.side.ordinal());
    }

    @Override
    public boolean handle(AWrapperWorld world, EntityBullet bullet) {
        bullet.performBlockHitLogic(hitResult);
        return true;
    }
}
