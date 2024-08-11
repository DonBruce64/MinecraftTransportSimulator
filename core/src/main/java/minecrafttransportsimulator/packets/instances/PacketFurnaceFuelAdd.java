package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.instances.EntityFurnace;
import minecrafttransportsimulator.jsondefs.JSONPart.FurnaceComponentType;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet sent to furnaces to add fuel to them.
 *
 * @author don_bruce
 */
public class PacketFurnaceFuelAdd extends APacketEntity<EntityFurnace> {
    private final int fuelAmount;

    public PacketFurnaceFuelAdd(EntityFurnace furnace) {
        super(furnace);
        this.fuelAmount = furnace.ticksLeftOfFuel;
    }

    public PacketFurnaceFuelAdd(ByteBuf buf) {
        super(buf);
        this.fuelAmount = buf.readInt();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeInt(fuelAmount);
    }

    @Override
    public boolean handle(AWrapperWorld world, EntityFurnace furnace) {
        furnace.ticksAddedOfFuel = fuelAmount;
        furnace.ticksLeftOfFuel += furnace.ticksAddedOfFuel;
        if (furnace.definition.furnaceType.equals(FurnaceComponentType.ELECTRIC)) {
            furnace.powerToDrawPerTick = 1D / fuelAmount;
        }
        return true;
    }
}
