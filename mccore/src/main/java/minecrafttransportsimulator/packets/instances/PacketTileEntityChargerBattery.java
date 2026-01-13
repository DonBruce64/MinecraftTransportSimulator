package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityCharger;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet sent to chargers on clients when they charge up a battery.
 * Used to reset the buffer so they don't de-sync with clients.
 *
 * @author don_bruce
 */
public class PacketTileEntityChargerBattery extends APacketEntity<TileEntityCharger> {

    public PacketTileEntityChargerBattery(TileEntityCharger charger) {
        super(charger);
    }

    public PacketTileEntityChargerBattery(ByteBuf buf) {
        super(buf);
    }

    @Override
    protected boolean handle(AWrapperWorld world, TileEntityCharger charger) {
        charger.internalBuffer = 0;
        return false;
    }
}
