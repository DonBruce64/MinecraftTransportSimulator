package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Reserved compatibility packet for the former direct rotor-vector arcade control.
 * New clients never send this packet; keeping its payload and registration prevents
 * all subsequent packet IDs from changing while older peers fail safely with a no-op.
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
        return false;
    }
}
