package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.instances.AEntityCrafter;
import minecrafttransportsimulator.jsondefs.JSONPart.CrafterComponentType;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet sent to crafters to add fuel to them.
 *
 * @author don_bruce
 */
public class PacketCrafterFuelAdd extends APacketEntity<AEntityCrafter> {
    private final int fuelAmount;

    public PacketCrafterFuelAdd(AEntityCrafter crafter) {
        super(crafter);
        this.fuelAmount = crafter.ticksLeftOfFuel;
    }

    public PacketCrafterFuelAdd(ByteBuf buf) {
        super(buf);
        this.fuelAmount = buf.readInt();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeInt(fuelAmount);
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityCrafter crafter) {
        crafter.ticksFuelProvides = fuelAmount;
        crafter.ticksLeftOfFuel += crafter.ticksFuelProvides;
        if (crafter.definition.crafterType == CrafterComponentType.ELECTRIC) {
            crafter.powerToDrawPerTick = 1D / fuelAmount;
        }
        return true;
    }
}
