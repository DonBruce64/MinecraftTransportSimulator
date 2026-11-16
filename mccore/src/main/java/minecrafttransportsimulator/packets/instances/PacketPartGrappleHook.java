package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.instances.PartGrapple;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet used to tell clients which vehicle or placed part a grapple's hook is stuck in, and where on it.
 * Sent to all clients by the server when the hook sticks into one, and again with a null ID when it lets go,
 * so clients can draw the hook on their own copy of it rather than where the server has it.
 */
public class PacketPartGrappleHook extends APacketEntity<PartGrapple> {
    private final UUID hookedID;
    private final Point3D offset;

    public PacketPartGrappleHook(PartGrapple grapple, UUID hookedID, Point3D offset) {
        super(grapple);
        this.hookedID = hookedID;
        this.offset = offset != null ? offset.copy() : null;
    }

    public PacketPartGrappleHook(ByteBuf buf) {
        super(buf);
        if (buf.readBoolean()) {
            this.hookedID = readUUIDFromBuffer(buf);
            this.offset = readPoint3dFromBuffer(buf);
        } else {
            this.hookedID = null;
            this.offset = null;
        }
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeBoolean(hookedID != null);
        if (hookedID != null) {
            writeUUIDToBuffer(hookedID, buf);
            writePoint3dToBuffer(offset, buf);
        }
    }

    @Override
    public boolean handle(AWrapperWorld world, PartGrapple grapple) {
        //The server owns the hook, so it never takes this from a client.
        if (world.isClient()) {
            grapple.setHookedMultipart(hookedID, offset);
        }
        return false;
    }
}
