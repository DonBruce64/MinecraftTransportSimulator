package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.packets.components.APacketBase;

/**
 * Packet used to update save data on clients from the server.
 * The string passed in will be the block to update.
 *
 * @author don_bruce
 */
public class PacketWorldSavedDataUpdate extends APacketBase {
    private final String name;
    private final IWrapperNBT data;

    public PacketWorldSavedDataUpdate(String name, IWrapperNBT data) {
        super(null);
        this.name = name;
        this.data = data;
    }

    public PacketWorldSavedDataUpdate(ByteBuf buf) {
        super(buf);
        this.name = readStringFromBuffer(buf);
        if (buf.readBoolean()) {
            this.data = readDataFromBuffer(buf);
        } else {
            this.data = null;
        }
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeStringToBuffer(name, buf);
        if (data != null) {
            buf.writeBoolean(true);
            writeDataToBuffer(data, buf);
        } else {
            buf.writeBoolean(false);
        }
    }

    @Override
    public void handle(AWrapperWorld world) {
        world.setData(name, data);
    }

    @Override
    public boolean runOnMainThread() {
        return false;
    }
}
