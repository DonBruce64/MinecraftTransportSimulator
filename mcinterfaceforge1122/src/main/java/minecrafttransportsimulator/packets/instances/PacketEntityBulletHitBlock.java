package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.components.APacketBase;

/**
 * Packet sent when a bullet hits a block.
 * Really all this is for is to play break sounds.
 *
 * @author don_bruce
 */
public class PacketEntityBulletHitBlock extends APacketBase {
    private final Point3D hitPosition;

    public PacketEntityBulletHitBlock(Point3D hitPosition) {
        super(null);
        this.hitPosition = hitPosition;
    }

    public PacketEntityBulletHitBlock(ByteBuf buf) {
        super(buf);
        this.hitPosition = readPoint3dFromBuffer(buf);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writePoint3dToBuffer(hitPosition, buf);
    }

    @Override
    public void handle(AWrapperWorld world) {
        InterfaceManager.clientInterface.playBlockBreakSound(hitPosition);
    }
}
