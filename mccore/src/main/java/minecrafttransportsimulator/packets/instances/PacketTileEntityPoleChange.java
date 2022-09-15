package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.items.instances.ItemPoleComponent.PoleComponentType;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.components.APacketEntityInteract;

/**
 * Packet sent to poles to change their states.  This gets sent when a player clicks a pole on the server
 * and a component is added or removed from the pole.
 *
 * @author don_bruce
 */
public class PacketTileEntityPoleChange extends APacketEntityInteract<TileEntityPole, IWrapperPlayer> {
    private final Axis axis;
    private final IWrapperNBT data;

    public PacketTileEntityPoleChange(TileEntityPole pole, IWrapperPlayer player, Axis axis, IWrapperNBT data) {
        super(pole, player);
        this.axis = axis;
        this.data = data;
    }

    public PacketTileEntityPoleChange(ByteBuf buf) {
        super(buf);
        this.axis = Axis.values()[buf.readByte()];
        if (buf.readBoolean()) {
            this.data = readDataFromBuffer(buf);
        } else {
            this.data = null;
        }
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeByte(axis.ordinal());
        if (data != null) {
            buf.writeBoolean(true);
            writeDataToBuffer(data, buf);
        } else {
            buf.writeBoolean(false);
        }
    }

    @Override
    protected boolean handle(AWrapperWorld world, TileEntityPole pole, IWrapperPlayer player) {
        if (data != null) {
            //Player clicked with a component.  Add it.
            pole.changeComponent(axis, PoleComponentType.createComponent(pole, player, axis, data));
        } else {
            pole.changeComponent(axis, null);
        }
        return false;
    }
}