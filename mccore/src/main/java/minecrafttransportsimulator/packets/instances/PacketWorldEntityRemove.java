package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet sent to client worlds to notify them of an entity being removed.
 * Only sent for tracked entities like vehicles and inventories.  Not sent
 * for untracked entities like particles.
 *
 * @author don_bruce
 */
public class PacketWorldEntityRemove extends APacketEntity<AEntityA_Base> {

    public PacketWorldEntityRemove(AEntityA_Base entity) {
        super(entity);
    }

    public PacketWorldEntityRemove(ByteBuf buf) {
        super(buf);
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityA_Base entity) {
        world.removeEntity(entity);
        return false;
    }

    @Override
    public boolean runOnMainThread() {
        //We can run this on networking thread since entity lists are thread-safe.
        return false;
    }
}
