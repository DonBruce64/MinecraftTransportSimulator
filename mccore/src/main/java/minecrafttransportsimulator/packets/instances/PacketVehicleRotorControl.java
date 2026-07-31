package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Synchronizes the world-space horizontal direction used to vector helicopter
 * rotor thrust in arcade mode.  Both components travel in one packet so they
 * cannot be mixed between input ticks.
 */
public class PacketVehicleRotorControl extends APacketEntity<EntityVehicleF_Physics> {
    private final double directionX;
    private final double directionZ;

    public PacketVehicleRotorControl(EntityVehicleF_Physics vehicle, double directionX, double directionZ) {
        super(vehicle);
        this.directionX = directionX;
        this.directionZ = directionZ;
    }

    public PacketVehicleRotorControl(ByteBuf buf) {
        super(buf);
        this.directionX = buf.readDouble();
        this.directionZ = buf.readDouble();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeDouble(directionX);
        buf.writeDouble(directionZ);
    }

    @Override
    public boolean handle(AWrapperWorld world, EntityVehicleF_Physics vehicle) {
        vehicle.setArcadeRotorDirection(directionX, directionZ);
        return true;
    }
}
