package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.instances.AEntityCrafter;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet sent to crafters to add or set time to them.
 *
 * @author don_bruce
 */
public class PacketCrafterTimeSet extends APacketEntity<AEntityCrafter> {
    private final int timeAmount;

    public PacketCrafterTimeSet(AEntityCrafter crafter) {
        super(crafter);
        this.timeAmount = crafter.ticksLeftToCraft;
    }

    public PacketCrafterTimeSet(ByteBuf buf) {
        super(buf);
        this.timeAmount = buf.readInt();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeInt(timeAmount);
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityCrafter crafter) {
        crafter.ticksNeededToCraft = timeAmount;
        crafter.ticksLeftToCraft = crafter.ticksNeededToCraft;
        return true;
    }
}
