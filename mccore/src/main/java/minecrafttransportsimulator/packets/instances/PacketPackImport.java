package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketBase;
import minecrafttransportsimulator.packloading.JSONParser;

/**
 * Packet sent to the server to have it update JSONs when devmode is imported.
 *
 * @author don_bruce
 */
public class PacketPackImport extends APacketBase {

    public PacketPackImport() {
        super(null);
    }

    public PacketPackImport(ByteBuf buf) {
        super(buf);
    }

    @Override
    public void handle(AWrapperWorld world) {
        JSONParser.applyImports(world);
    }

    @Override
    public boolean runOnMainThread() {
        return false;
    }
}
