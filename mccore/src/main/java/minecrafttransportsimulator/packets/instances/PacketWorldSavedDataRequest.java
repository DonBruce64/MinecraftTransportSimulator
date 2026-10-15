package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.instances.EntityParticleEmitter;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.components.APacketPlayer;

/**
 * Packet used to request world NBT data from the server, and to send that data back to clients.
 * This only happens when the world is first created.  Subsequent updates happen on-demand when
 * the data changes and will use their own packet, {@link PacketWorldSavedDataUpdate}
 *
 * @author don_bruce
 */
public class PacketWorldSavedDataRequest extends APacketPlayer {

    public PacketWorldSavedDataRequest(IWrapperPlayer player) {
        super(player);
    }

    public PacketWorldSavedDataRequest(ByteBuf buf) {
        super(buf);
    }

    @Override
    public void handle(AWrapperWorld world, IWrapperPlayer player) {
        //Send back packet in data-block batches.
        IWrapperNBT savedData = world.getData("");
        for (String dataName : savedData.getAllNames()) {
            player.sendPacket(new PacketWorldSavedDataUpdate(dataName, savedData.getData(dataName)));
        }
        //The same request is issued whenever a client enters a world, making it a
        //natural late-join/dimension-change snapshot for active particle emitters.
        EntityParticleEmitter.sendActiveEmittersTo(world, player);
    }

    @Override
    public boolean runOnMainThread() {
        //Emitter snapshots include mutable transforms and must be captured on the
        //world thread.  This packet only runs on join/dimension change.
        return true;
    }
}
