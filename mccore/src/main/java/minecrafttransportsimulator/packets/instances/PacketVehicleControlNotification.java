package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.components.APacketEntityInteract;

/**
 * Packet used to let a vehicle know that a player is controlling it.  Sent by the player to the vehicle,
 * then sent back to all clients to let them know that they are now the current controller.
 *
 * @author don_bruce
 */
public class PacketVehicleControlNotification extends APacketEntityInteract<EntityVehicleF_Physics, IWrapperPlayer> {

    public PacketVehicleControlNotification(EntityVehicleF_Physics vehicle, IWrapperPlayer player) {
        super(vehicle, player);
    }

    public PacketVehicleControlNotification(ByteBuf buf) {
        super(buf);
    }

    @Override
    public boolean handle(AWrapperWorld world, EntityVehicleF_Physics vehicle, IWrapperPlayer player) {
        vehicle.lastController = player;
        return true;
    }
}
