package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.instances.EntityFluidTank;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet sent to fluid tanks on clients to update the fluid they have in their tank.
 *
 * @author don_bruce
 */
public class PacketFluidTankChange extends APacketEntity<EntityFluidTank> {
    private final String fluidName;
    private final double fluidDelta;

    public PacketFluidTankChange(EntityFluidTank tank, double fluidDelta) {
        super(tank);
        this.fluidName = tank.getFluid();
        this.fluidDelta = fluidDelta;
    }

    public PacketFluidTankChange(ByteBuf buf) {
        super(buf);
        this.fluidName = readStringFromBuffer(buf);
        this.fluidDelta = buf.readDouble();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeStringToBuffer(fluidName, buf);
        buf.writeDouble(fluidDelta);
    }

    @Override
    public boolean handle(AWrapperWorld world, EntityFluidTank tank) {
        if (fluidDelta < 0) {
            tank.drain(fluidName, -fluidDelta, true);
        } else {
            tank.fill(fluidName, fluidDelta, true);
        }
        return true;
    }
}
