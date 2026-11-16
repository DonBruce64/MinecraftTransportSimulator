package mcinterface1211;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketBase;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PacketReplayEngines extends APacketBase {
    private final List<UUID> uuids;
    private final List<Boolean> running;
    private final List<Float> rpms;

    public PacketReplayEngines(List<UUID> uuids, List<Boolean> running, List<Float> rpms) {
        super(null);
        this.uuids = uuids;
        this.running = running;
        this.rpms = rpms;
    }

    public PacketReplayEngines(ByteBuf buf) {
        super(buf);
        int count = buf.readInt();
        this.uuids = new ArrayList<>(count);
        this.running = new ArrayList<>(count);
        this.rpms = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            uuids.add(readUUIDFromBuffer(buf));
            running.add(buf.readBoolean());
            rpms.add(buf.readFloat());
        }
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeInt(uuids.size());
        for (int i = 0; i < uuids.size(); i++) {
            writeUUIDToBuffer(uuids.get(i), buf);
            buf.writeBoolean(running.get(i));
            buf.writeFloat(rpms.get(i));
        }
    }

    @Override
    public void handle(AWrapperWorld world) {
        if (!FlashbackCompat.isReplayClient()) {
            return;
        }
        for (int i = 0; i < uuids.size(); ++i) {
            AEntityB_Existing entity = world.getWorld().getEntity(uuids.get(i));
            if (entity instanceof PartEngine) {
                ((PartEngine) entity).setReplayState(running.get(i), rpms.get(i));
            }
        }
    }
}
