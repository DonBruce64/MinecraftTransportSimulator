package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet used to request the entity update its camera index.
 * Entity will handle any logic for the update; this just sends the update.
 *
 * @author don_bruce
 */
public class PacketEntityCameraChange extends APacketEntity<AEntityB_Existing> {

    public PacketEntityCameraChange(AEntityB_Existing entity) {
        super(entity);
    }

    public PacketEntityCameraChange(ByteBuf buf) {
        super(buf);
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityB_Existing entity) {
        ++entity.cameraIndex;
        return true;
    }
}
