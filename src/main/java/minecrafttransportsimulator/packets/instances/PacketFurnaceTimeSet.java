package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.instances.EntityFurnace;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet sent to furnaces to add ore set smelting time to them.
 *
 * @author don_bruce
 */
public class PacketFurnaceTimeSet extends APacketEntity<EntityFurnace> {
    private final int timeAmount;

    public PacketFurnaceTimeSet(EntityFurnace furnace) {
        super(furnace);
        this.timeAmount = furnace.ticksLeftToSmelt;
    }

    public PacketFurnaceTimeSet(ByteBuf buf) {
        super(buf);
        this.timeAmount = buf.readInt();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeInt(timeAmount);
    }

    @Override
    public boolean handle(AWrapperWorld world, EntityFurnace furnace) {
        furnace.ticksNeededToSmelt = timeAmount;
        furnace.ticksLeftToSmelt = furnace.ticksNeededToSmelt;
        return true;
    }
}
