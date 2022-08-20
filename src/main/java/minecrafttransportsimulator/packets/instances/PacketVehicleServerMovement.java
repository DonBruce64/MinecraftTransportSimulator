package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet used to send server vehicle movement to clients.  This packet doesn't directly
 * move the vehicle (such an action results in horrid movement lag due to network
 * latency and packet handling pipelines), rather it simply tells the client how much
 * the vehicle has moved since it was first spawned.  This way clients can adjust the
 * vehicle position while at the same time running their own calculations to "soft-sync"
 * the position and rotation.  This system of syncing has the side-effect of significant
 * rubberbanding when server TPS suffers or networking goes bad, but it's far better than
 * the alternatives when the connection is good, hence why we use it.
 *
 * @author don_bruce
 */
public class PacketVehicleServerMovement extends APacketEntity<EntityVehicleF_Physics> {
    private final Point3D motion;
    private final Point3D rotation;
    private final double pathing;

    public PacketVehicleServerMovement(EntityVehicleF_Physics vehicle, Point3D motion, Point3D rotation, double pathing) {
        super(vehicle);
        this.motion = motion;
        this.rotation = rotation;
        this.pathing = pathing;
    }

    public PacketVehicleServerMovement(ByteBuf buf) {
        super(buf);
        this.motion = readPoint3dFromBuffer(buf);
        this.rotation = readPoint3dFromBuffer(buf);
        this.pathing = buf.readDouble();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writePoint3dToBuffer(motion, buf);
        writePoint3dToBuffer(rotation, buf);
        buf.writeDouble(pathing);
    }

    @Override
    public boolean handle(AWrapperWorld world, EntityVehicleF_Physics vehicle) {
        vehicle.addToServerDeltas(motion, rotation, pathing);
        return false;
    }
}
