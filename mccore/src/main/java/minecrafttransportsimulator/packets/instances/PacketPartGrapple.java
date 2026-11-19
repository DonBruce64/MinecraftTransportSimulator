package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.instances.PartGrapple;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.components.APacketEntityInteract;

/**
 * Packet used to send a grapple's aim to the server while its controller looks through a custom camera.
 * The camera shows them the world from somewhere other than their eyes, and may turn them to face along it,
 * so the server can't tell where they're really aiming from where they are and the way they face.  Sent to the
 * server by the controller's client when the aim changes, then on to all clients so they turn the launcher the
 * same way.  The player is the controller who sent it.  The offset is where the camera is and the angles are
 * where it aims, both relative to the controller's seat.  Null angles mean the controller stopped using the camera.
 */
public class PacketPartGrapple extends APacketEntityInteract<PartGrapple, IWrapperPlayer> {
    private final Point3D offset;
    private final Point3D angles;

    public PacketPartGrapple(PartGrapple grapple, IWrapperPlayer controller, Point3D offset, Point3D angles) {
        super(grapple, controller);
        boolean active = offset != null && angles != null;
        this.offset = active ? offset.copy() : null;
        this.angles = active ? angles.copy() : null;
    }

    public PacketPartGrapple(ByteBuf buf) {
        super(buf);
        if (buf.readBoolean()) {
            this.angles = new Point3D(buf.readDouble(), buf.readDouble(), 0);
            this.offset = readPoint3dFromBuffer(buf);
        } else {
            this.angles = null;
            this.offset = null;
        }
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeBoolean(angles != null);
        if (angles != null) {
            buf.writeDouble(angles.x);
            buf.writeDouble(angles.y);
            writePoint3dToBuffer(offset, buf);
        }
    }

    @Override
    public boolean handle(AWrapperWorld world, PartGrapple grapple, IWrapperPlayer controller) {
        return grapple.setCameraAim(controller.getID(), offset, angles);
    }
}
