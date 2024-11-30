package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.blocks.tileentities.instances.ATileEntityFuelPump;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet sent to pumps clients to update that they've dispensed fluids.
 *
 * @author don_bruce
 */
public class PacketTileEntityFuelPumpDispense extends APacketEntity<ATileEntityFuelPump> {
    private final double amountDispensed;

    public PacketTileEntityFuelPumpDispense(ATileEntityFuelPump pump, double amountDispensed) {
        super(pump);
        this.amountDispensed = amountDispensed;
    }

    public PacketTileEntityFuelPumpDispense(ByteBuf buf) {
        super(buf);
        this.amountDispensed = buf.readDouble();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeDouble(amountDispensed);
    }

    @Override
    protected boolean handle(AWrapperWorld world, ATileEntityFuelPump pump) {
        pump.fuelDispensedThisConnection += amountDispensed;
        pump.fuelDispensedThisPurchase += amountDispensed;
        return false;
    }
}
