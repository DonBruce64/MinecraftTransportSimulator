package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet used to request the entity update its camera index.
 * Entity will handle any logic for the update; this just sends the update.
 *
 * @author don_bruce
 */
public class PacketEntityCameraChange extends APacketEntity<AEntityE_Interactable<?>> {

    public PacketEntityCameraChange(AEntityE_Interactable<?> entity) {
        super(entity);
    }

    public PacketEntityCameraChange(ByteBuf buf) {
        super(buf);
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityE_Interactable<?> entity) {
        ++entity.cameraIndex;
        return true;
    }
}
